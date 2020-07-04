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
package io.github.dsheirer.dsp.fsk;

import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter2;
import io.github.dsheirer.dsp.fm.FMDemodulator;
import io.github.dsheirer.module.decode.edacs48.FloatWaveWriter;
import io.github.dsheirer.record.wave.ComplexBufferWaveRecorder;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDACSDecoder {

    private static final Logger log = LoggerFactory.getLogger(EDACSDecoder.class);
    private final double channelBandwidth;
    private final int baudRate;
    private final FMDemodulator fmDemodulator = new FMDemodulator();
    private final IBinarySymbolProcessor symbolProcessor;
    private double sourceSampleRate;
    private ComplexFIRFilter2 iqFilter;
    private EDACSSymbolDecider symbolDecider;

    // TODO: Remove
    public volatile boolean record;
    private ComplexBufferWaveRecorder basebandRecorder;
    private ComplexBufferWaveRecorder filteredBasebandRecorder;
    private FloatWaveWriter audioRecorder;

    public EDACSDecoder(double channelBandwidth, int baudRate, IBinarySymbolProcessor symbolProcessor) {
        this.channelBandwidth = channelBandwidth;
        this.baudRate = baudRate;
        this.symbolProcessor = symbolProcessor;
    }

    public void reset() {
        fmDemodulator.reset();
        symbolDecider.reset();
    }

    // TODO: Remove
    public void stop() {
        if (basebandRecorder != null) {
            basebandRecorder.stop();
        }
        if (filteredBasebandRecorder != null) {
            filteredBasebandRecorder.stop();
        }
        if (audioRecorder != null) {
            audioRecorder.stop();
        }
    }

    public void dispose() {
        if (iqFilter != null) {
            iqFilter.dispose();
            iqFilter = null;
        }

        fmDemodulator.dispose();

        if (basebandRecorder != null) {
            basebandRecorder.dispose();
            basebandRecorder = null;
        }
        if (filteredBasebandRecorder != null) {
            filteredBasebandRecorder.dispose();
            filteredBasebandRecorder = null;
        }
        if (audioRecorder != null) {
            audioRecorder.dispose();
            audioRecorder = null;
        }
    }

    public void decode(ReusableComplexBuffer basebandBuffer) {
        if (iqFilter == null) {
            basebandBuffer.decrementUserCount();
            throw new IllegalStateException("Sample rate must be set before decoder can process complex sample buffers");
        }

        if (record) {
            /*
            if (basebandRecorder == null) {
                basebandRecorder = new ComplexBufferWaveRecorder((float) sourceSampleRate, "/Users/andrzej/SDRTrunk/recordings/edacs/baseband");
                basebandRecorder.start();
            }
            if (filteredBasebandRecorder == null) {
                filteredBasebandRecorder = new ComplexBufferWaveRecorder((float) sourceSampleRate, "/Users/andrzej/SDRTrunk/recordings/edacs/filtered_baseband");
                filteredBasebandRecorder.start();
            }
            */
            if (audioRecorder == null) {
                audioRecorder = new FloatWaveWriter("/Users/andrzej/SDRTrunk/recordings/edacs/audio", false);
                audioRecorder.setSampleRate((float) sourceSampleRate);
            }
        }

        if (basebandRecorder != null) {
            basebandBuffer.incrementUserCount();
            basebandRecorder.receive(basebandBuffer);
        }

        final var basebandFilteredBuffer = iqFilter.filter(basebandBuffer);

        if (filteredBasebandRecorder != null) {
            basebandFilteredBuffer.incrementUserCount();
            filteredBasebandRecorder.receive(basebandFilteredBuffer);
        }

        final var demodulatedBuffer = fmDemodulator.demodulate(basebandFilteredBuffer);

        if (audioRecorder != null) {
            demodulatedBuffer.incrementUserCount();
            audioRecorder.receive(demodulatedBuffer);
        }

        if (symbolDecider != null) {
            for (final var sample : demodulatedBuffer.getSamples()) {
                symbolDecider.receive(sample);
            }
        }

        demodulatedBuffer.decrementUserCount();
    }

    private ComplexFIRFilter2 createIQFilter(double sampleRate) {
        final var passBandStop = channelBandwidth / 2d;
        final var stopBandStart = passBandStop * 1.048;

        log.trace("Creating IQ low-pass filter for sample rate {}, pass frequency {} and stop frequency {}",
            sampleRate, passBandStop, stopBandStart);

        float[] filterTaps;

        try {
            final var filterSpec = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(sampleRate)
                .passBandCutoff(passBandStop)
                .passBandRipple(0.01)
                .stopBandStart(stopBandStart)
                .stopBandRipple(0.028) // Approximately 60 dB attenuation
                .oddLength(true)
                .build();

            filterTaps = FilterFactory.getTaps(filterSpec);
        } catch (FilterDesignException e) {
            log.error("Couldn't design Remez filter for sample rate " + sampleRate +
                ", pass frequency " + passBandStop + " and stop frequency " + stopBandStart +
                " - falling back to sinc filter");

            filterTaps = FilterFactory.getLowPass(
                sampleRate,
                (int) passBandStop,
                (int) stopBandStart,
                60,
                Window.WindowType.HAMMING,
                true
            );
        }

        return new ComplexFIRFilter2(filterTaps);
    }

    public void setSampleRate(double newSampleRate) {
        if (sourceSampleRate == newSampleRate) {
            return;
        }

        log.trace("Setting sample rate to {}", newSampleRate);
        sourceSampleRate = newSampleRate;

        if (basebandRecorder != null) basebandRecorder.setSampleRate((float) newSampleRate);
        if (filteredBasebandRecorder != null)
            filteredBasebandRecorder.setSampleRate((float) newSampleRate);
        if (audioRecorder != null) audioRecorder.setSampleRate((float) newSampleRate);

        final var samplesPerSymbol = sourceSampleRate / baudRate;
        symbolDecider = new EDACSSymbolDecider((float) samplesPerSymbol);
        symbolDecider.setSymbolProcessor(symbolProcessor);

        final var minSampleRate = 2d * channelBandwidth;
        if (sourceSampleRate < minSampleRate) {
            throw new IllegalStateException("Decoder with channel bandwidth " + channelBandwidth +
                " requires a sample rate of at least " + minSampleRate + " - sample rate of " +
                sourceSampleRate + " is not sufficient");
        }

        if (iqFilter != null) {
            iqFilter.dispose();
            iqFilter = null;
        }
        iqFilter = createIQFilter(sourceSampleRate);
    }
}
