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
import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.bits.SyncPattern;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.dsheirer.module.decode.edacs48.EDACSConstants.FRAME_LENGTH;

class PolarityAutoSelector implements IBinarySymbolProcessor {

    private enum Polarity {
        UNKNOWN,
        NORMAL,
        INVERTED,
    }

    private static final int FRAMES_UNTIL_POLARITY_LOCK = 10;
    private static final int SYMBOLS_UNTIL_POLARITY_LOSS = FRAME_LENGTH * 10;
    private static final Logger log = LoggerFactory.getLogger(PolarityAutoSelector.class);
    private final PolarityDelegate normalDelegate;
    private final PolarityDelegate invertedDelegate;
    private final TraceListener traceListener;
    private Listener<CorrectedBinaryMessage> frameListener;
    private Polarity polarity;

    PolarityAutoSelector() {
        traceListener = log.isTraceEnabled() ? new TraceListener() : null;
        normalDelegate = new PolarityDelegate(Polarity.NORMAL);
        invertedDelegate = new PolarityDelegate(Polarity.INVERTED);
        polarity = Polarity.UNKNOWN;
    }

    void dispose() {
        normalDelegate.dispose();
        invertedDelegate.dispose();
        frameListener = null;
    }

    void reset() {
        normalDelegate.reset();
        invertedDelegate.reset();
        if (traceListener != null) traceListener.reset();
    }

    void setFrameListener(Listener<CorrectedBinaryMessage> frameListener) {
        this.frameListener = frameListener;
    }

    private void broadcastFrame(CorrectedBinaryMessage frame) {
        if (traceListener != null) traceListener.receive(frame);
        if (frameListener != null) frameListener.receive(frame);
    }

    @Override
    public void process(boolean symbol) {
        if (polarity == Polarity.NORMAL || polarity == Polarity.UNKNOWN)
            normalDelegate.process(symbol);
        if (polarity == Polarity.INVERTED || polarity == Polarity.UNKNOWN)
            invertedDelegate.process(!symbol);
    }

    private void select(Polarity polarity) {
        if (polarity == Polarity.NORMAL) invertedDelegate.reset();
        else if (polarity == Polarity.INVERTED) normalDelegate.reset();
        else log.warn("Unexpected request to select {} polarity", polarity);
        this.polarity = polarity;
    }

    private void unselectPolarity() {
        if (polarity == Polarity.NORMAL) invertedDelegate.reset();
        else if (polarity == Polarity.INVERTED) normalDelegate.reset();
        else log.warn("Unexpected request to unselect polarity - polarity isn't selected");
        polarity = Polarity.UNKNOWN;
    }

    // TODO: Soft sync detection
    private class PolarityDelegate implements EDACSConstants, IBinarySymbolProcessor, ISyncDetectListener {

        private final Polarity polarity;
        private final MessageFramer framer;
        private int syncCount;
        private int symbolsSinceLastSync;

        PolarityDelegate(Polarity polarity) {
            this.polarity = polarity;
            // TODO: Sync pattern compatible with etrunker & yahoo mailing list
            framer = new MessageFramer(SyncPattern.EDACS_CONTROL.getPattern(), FRAME_LENGTH);
            framer.setSyncDetectListener(this);
            framer.addMessageListener(PolarityAutoSelector.this::broadcastFrame);
        }

        void dispose() {
            framer.dispose();
        }

        void reset() {
            framer.reset();
            syncCount = 0;
            symbolsSinceLastSync = 0;
        }

        @Override
        public void process(boolean symbol) {
            framer.process(symbol);
            symbolsSinceLastSync++;
            if (symbolsSinceLastSync >= SYMBOLS_UNTIL_POLARITY_LOSS) {
                log.trace("Reached threshold of {} symbols since last seen frame - unselecting polarity",
                    SYMBOLS_UNTIL_POLARITY_LOSS);
                unselectPolarity();
            }
        }

        @Override
        public void syncDetected(int bitErrors) {
            syncCount++;
            symbolsSinceLastSync = 0;
            if (syncCount >= FRAMES_UNTIL_POLARITY_LOCK) {
                log.trace("Reached threshold of {} frames - selecting {} polarity",
                    FRAMES_UNTIL_POLARITY_LOCK, polarity);
                select(polarity);
            }
        }

        @Override
        public void syncLost(int bitsProcessed) {
        }
    }

    // TODO: Remove?
    private static class TraceListener implements Listener<CorrectedBinaryMessage> {

        private long lastTimestamp;
        private int framesPerInterval;
        private int bitsPerInterval;

        void reset() {
            lastTimestamp = 0;
        }

        @Override
        public void receive(CorrectedBinaryMessage frame) {
            if (lastTimestamp == 0) lastTimestamp = System.currentTimeMillis();
            framesPerInterval++;
            bitsPerInterval += frame.size();

            if (System.currentTimeMillis() - lastTimestamp >= 2000) {
                log.trace("Within last 2s: received {} frames, {} bits", framesPerInterval, bitsPerInterval);
                lastTimestamp += 2000;
                framesPerInterval = 0;
                bitsPerInterval = 0;
            }
        }
    }
}
