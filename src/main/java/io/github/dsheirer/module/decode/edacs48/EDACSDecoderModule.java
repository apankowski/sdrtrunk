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

import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.dsp.fsk.EDACSDecoder;
import io.github.dsheirer.dsp.symbol.BinaryToByteBufferAssembler;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.edacs48.message.EDACSMessageDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableByteBufferProvider;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferListener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDACSDecoderModule extends Decoder implements ISourceEventListener,
    IReusableComplexBufferListener, IBinarySymbolProcessor,
    IReusableByteBufferProvider {

    private static final Logger log = LoggerFactory.getLogger(EDACSDecoderModule.class);
    private final EDACSDecoder decoder;
    private final SourceEventProcessor sourceEventProcessor = new SourceEventProcessor();
    private final BinaryToByteBufferAssembler byteBufferAssembler = new BinaryToByteBufferAssembler(600);
    private final PolarityAutoSelector polarityAutoselector = new PolarityAutoSelector();
    private final FrameDissector frameDissector = new FrameDissector();
    private final EDACSMessageDecoder messageDecoder = new EDACSMessageDecoder();

    public EDACSDecoderModule(double channelBandwidth, int bitRate) {
        decoder = new EDACSDecoder(channelBandwidth, bitRate, this);
        polarityAutoselector.setFrameListener(frameDissector::dissect);
        frameDissector.setFrameMessagesListener(messageDecoder::decode);
        messageDecoder.setMessageListener(getMessageListener());
    }

    @Override
    public DecoderType getDecoderType() {
        return DecoderType.EDACS48;
    }

    @Override
    public void reset() {
        decoder.reset();
        byteBufferAssembler.reset();
        polarityAutoselector.reset();
        frameDissector.reset();
    }

    @Override
    public void dispose() {
        decoder.dispose();
        polarityAutoselector.dispose();
        frameDissector.dispose();
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener() {
        return sourceEventProcessor;
    }

    @Override
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener() {
        return decoder::decode;
    }

    @Override
    public void setBufferListener(Listener<ReusableByteBuffer> listener) {
        byteBufferAssembler.setBufferListener(listener);
    }

    @Override
    public void removeBufferListener(Listener<ReusableByteBuffer> listener) {
        byteBufferAssembler.removeBufferListener(listener);
    }

    @Override
    public boolean hasBufferListeners() {
        return byteBufferAssembler.hasBufferListeners();
    }

    @Override
    public void process(boolean symbol) {
        byteBufferAssembler.process(symbol);
        polarityAutoselector.process(symbol);
    }

    /**
     * Monitors sample rate change source events and (re)creates the I/Q filter
     * that is aligned with the new sample rate.
     */
    private class SourceEventProcessor implements Listener<SourceEvent> {

        @Override
        public void receive(SourceEvent sourceEvent) {
            if (sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE) {
                decoder.setSampleRate(sourceEvent.getValue().doubleValue());
            }
        }
    }
}
