/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.edacs48;

import io.github.dsheirer.dsp.filter.channelizer.ContinuousReusableBufferProcessor;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.record.wave.WaveWriter;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableBufferListener;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FloatWaveWriter extends Module implements IReusableBufferListener, Listener<ReusableFloatBuffer>, ISourceEventListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String prefix;
    private final boolean writeFloats;
    private final AtomicBoolean running = new AtomicBoolean();
    private final ContinuousReusableBufferProcessor<ReusableFloatBuffer> bufferProcessor =
        new ContinuousReusableBufferProcessor<>(500, 50);
    private float sampleRate;
    private WaveWriter waveWriter;

    public FloatWaveWriter(String prefix, boolean writeFloats) {
        this.prefix = prefix;
        this.writeFloats = writeFloats;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener() {
        return sourceEvent -> {
            if (sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE) {
                setSampleRate(sourceEvent.getValue().floatValue());
            }
        };
    }

    public void setSampleRate(float newSampleRate) {
        if (sampleRate == 0.0 || newSampleRate != sampleRate) {
            sampleRate = newSampleRate;
            stop();
            start();
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            bufferProcessor.setListener(this::writeBuffers);
            bufferProcessor.start();
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            bufferProcessor.stop();
            bufferProcessor.setListener(null);

            if (waveWriter != null) {
                final var waveWriterRef = waveWriter;
                waveWriter = null;
                ThreadPool.SCHEDULED.schedule(() -> {
                    try {
                        waveWriterRef.close();
                    } catch (IOException e) {
                        log.error("Error closing wave writer", e);
                    }
                }, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void dispose() {
        stop();
    }

    @Override
    public void reset() {
    }

    @Override
    public Listener<ReusableFloatBuffer> getReusableBufferListener() {
        return this;
    }

    @Override
    public void receive(ReusableFloatBuffer buffer) {
        bufferProcessor.receive(buffer);
    }

    private void writeBuffers(List<ReusableFloatBuffer> buffers) {
        if (waveWriter == null && sampleRate != 0.0) {
            final AudioFormat audioFormat;
            if (writeFloats) {
                audioFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_FLOAT,
                    sampleRate,
                    32,
                    1,
                    4,
                    sampleRate,
                    false
                );
            } else {
                audioFormat = new AudioFormat(sampleRate, 16, 1, true, false);
            }

            try {
                final var format = writeFloats ? "floats" : "signed";
                final var filePath = Path.of(prefix + "_" + TimeStamp.getTimeStamp("_") + "_" + format + ".wav");
                filePath.toFile().getParentFile().mkdirs();
                waveWriter = new WaveWriter(audioFormat, filePath);
            } catch (IOException e) {
                log.error("Error starting float wave writer", e);
                for (final var buffer : buffers) {
                    buffer.decrementUserCount();
                }
                return;
            }
        }

        var error = false;
        for (final var buffer : buffers) {
            if (!error) {
                try {
                    if (writeFloats) {
                        final var byteBuffer = ByteBuffer.allocate(buffer.getSampleCount() * 4);
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                        byteBuffer.asFloatBuffer().put(buffer.getSamples());
                        waveWriter.writeData(byteBuffer);
                    } else {
                        waveWriter.writeData(ConversionUtils.convertToSigned16BitSamples(buffer));
                    }
                } catch (IOException e) {
                    log.error("Exception while writing to wave file - stopping float wave writer", e);
                    error = true;
                    stop();
                }
            }
            buffer.decrementUserCount();
        }
    }
}
