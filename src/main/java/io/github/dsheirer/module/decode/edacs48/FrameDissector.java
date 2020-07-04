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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.BCH_63_51_2;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FrameDissector implements EDACSConstants {

    private static final Logger log = LoggerFactory.getLogger(FrameDissector.class);
    private final BCH_63_51_2 bch = new BCH_63_51_2();
    private final BitVoter bitVoter = new BitVoter();
    private final TraceListener traceListener;
    private FrameMessagesListener frameMessagesListener;

    FrameDissector() {
        traceListener = log.isTraceEnabled() ? new TraceListener() : null;
    }

    void reset() {
        if (traceListener != null) traceListener.reset();
    }

    void dispose() {
        frameMessagesListener = null;
    }

    void setFrameMessagesListener(FrameMessagesListener frameMessagesListener) {
        this.frameMessagesListener = frameMessagesListener;
    }

    void dissect(CorrectedBinaryMessage frame) {
        var offset = BARKER1_LENGTH + DOTTING_LENGTH + BARKER2_LENGTH;
        final var message1 = extractMessage(frame, offset);
        if (traceListener != null) traceListener.receive(message1);

        offset += (MESSAGE_LENGTH + CRC_LENGTH) * NUM_MESSAGE_COPIES;
        final var message2 = extractMessage(frame, offset);
        if (traceListener != null) traceListener.receive(message2);

        if (frameMessagesListener != null) frameMessagesListener.receive(message1, message2);
    }

    private CorrectedBinaryMessage extractMessage(CorrectedBinaryMessage frame, int offset) {
        // Extract copies of the message, noting that 2nd copy is inverted
        final var length = MESSAGE_LENGTH + CRC_LENGTH;
        final var copy1 = frame.getSubMessage(offset, offset + length);

        offset += length;
        final var copy2 = frame.getSubMessage(offset, offset + length);
        copy2.flip(0, length);

        offset += length;
        final var copy3 = frame.getSubMessage(offset, offset + length);

        // Try bit voting followed by BCH
        final var bitVotedMessage = bitVoter.bitVotedMessage(copy1, copy2, copy3);
        bch.checkAndCorrect(bitVotedMessage);
        if (bitVotedMessage.getCRC().passes()) return bitVotedMessage;

        // Maybe we can salvage the message from one of the copies
        bch.checkAndCorrect(copy1);
        if (copy1.getCRC().passes()) return copy1;

        bch.checkAndCorrect(copy2);
        if (copy2.getCRC().passes()) return copy2;

        bch.checkAndCorrect(copy3);
        if (copy3.getCRC().passes()) return copy3;

        return bitVotedMessage;
    }

    private static class TraceListener implements Listener<CorrectedBinaryMessage> {

        private long lastTimestamp;
        private int messagesPerInterval;
        private int errorBitsPerInterval;

        void reset() {
            lastTimestamp = 0;
        }

        @Override
        public void receive(CorrectedBinaryMessage message) {
            if (lastTimestamp == 0) lastTimestamp = System.currentTimeMillis();
            messagesPerInterval++;
            errorBitsPerInterval += message.getCorrectedBitCount();

            if (System.currentTimeMillis() - lastTimestamp >= 2000) {
                log.trace("Within last 2s: received {} messages, {} bit errors",
                    messagesPerInterval, errorBitsPerInterval);
                lastTimestamp += 2000;
                messagesPerInterval = 0;
                errorBitsPerInterval = 0;
            }
        }
    }

    /**
     * Listener of pair of binary messages extracted from each decoded frame.
     */
    interface FrameMessagesListener {

        /**
         * Receives a pair of binary messages extracted from each decoded frame.
         *
         * @param m1 first message inside decoded frame
         * @param m2 first message inside decoded frame
         */
        void receive(CorrectedBinaryMessage m1, CorrectedBinaryMessage m2);
    }
}
