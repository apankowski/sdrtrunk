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

import java.util.function.Consumer;

class PolarityAutoselector implements IBinarySymbolProcessor {

    private enum Polarity {
        UNKNOWN,
        NORMAL,
        INVERTED
    }

    private static final Logger log = LoggerFactory.getLogger(PolarityAutoselector.class);
    private final PolarityDelegate normalDelegate;
    private final PolarityDelegate invertedDelegate;
    private final TraceListener traceListener;
    private Listener<CorrectedBinaryMessage> frameListener;
    private Polarity polarity;

    PolarityAutoselector() {
        traceListener = log.isTraceEnabled() ? new TraceListener() : null;
        final Listener<CorrectedBinaryMessage> delegatingFrameListener = frame -> {
            if (traceListener != null) traceListener.receive(frame);
            if (frameListener != null) frameListener.receive(frame);
        };
        normalDelegate = new PolarityDelegate(Polarity.NORMAL, this::selectPolarity, delegatingFrameListener);
        invertedDelegate = new PolarityDelegate(Polarity.INVERTED, this::selectPolarity, delegatingFrameListener);
        selectPolarity(Polarity.UNKNOWN);
    }

    void reset() {
        normalDelegate.reset();
        invertedDelegate.reset();
        if (traceListener != null) traceListener.reset();
    }

    void dispose() {
        normalDelegate.dispose();
        invertedDelegate.dispose();
    }

    void setFrameListener(Listener<CorrectedBinaryMessage> frameListener) {
        this.frameListener = frameListener;
    }

    @Override
    public void process(boolean symbol) {
        // Unknown polarity is handled by both delegates
        if (polarity != Polarity.INVERTED) normalDelegate.process(symbol);
        if (polarity != Polarity.NORMAL) invertedDelegate.process(!symbol);
    }

    void selectPolarity(Polarity polarity) {
        if (this.polarity != polarity) {
            log.trace("Selecting polarity {}", polarity);
            this.polarity = polarity;
            switch (polarity) {
                case NORMAL:
                    normalDelegate.setSelected(true);
                    invertedDelegate.reset();
                    break;
                case INVERTED:
                    invertedDelegate.setSelected(true);
                    normalDelegate.reset();
                    break;
                case UNKNOWN:
                    normalDelegate.setSelected(false);
                    invertedDelegate.setSelected(false);
                    break;
            }
        }
    }

    // TODO: Soft sync detection
    private static class PolarityDelegate implements EDACSConstants, IBinarySymbolProcessor,
        ISyncDetectListener {

        private static final int FRAMES_UNTIL_POLARITY_LOCK = 10;
        private static final int SYMBOLS_UNTIL_POLARITY_LOSS = FRAME_LENGTH * 5;
        private final Polarity polarity;
        private final Consumer<Polarity> polaritySelector;
        private final MessageFramer framer;
        private boolean isSelected;
        private int syncCount;
        private int symbolsSinceLastSync;

        PolarityDelegate(Polarity polarity, Consumer<Polarity> polaritySelector,
            Listener<CorrectedBinaryMessage> frameListener) {

            this.polarity = polarity;
            this.polaritySelector = polaritySelector;
            // TODO: Sync pattern compatible with etrunker & yahoo mailing list
            framer = new MessageFramer(SyncPattern.EDACS_CONTROL.getPattern(), FRAME_LENGTH);
            framer.setSyncDetectListener(this);
            framer.addMessageListener(frameListener);
        }

        void reset() {
            framer.reset();
            isSelected = false;
            syncCount = 0;
            symbolsSinceLastSync = 0;
        }

        void dispose() {
            framer.dispose();
        }

        void setSelected(boolean selected) {
            this.isSelected = selected;
            syncCount = 0;
            symbolsSinceLastSync = 0;
        }

        @Override
        public void process(boolean symbol) {
            framer.process(symbol);

            // Check for sync loss if we're selected
            if (isSelected) {
                symbolsSinceLastSync++;
                if (symbolsSinceLastSync >= SYMBOLS_UNTIL_POLARITY_LOSS) {
                    log.trace("Reached threshold of {} symbols since last seen frame - revoking lock on {} polarity",
                        SYMBOLS_UNTIL_POLARITY_LOSS, polarity);
                    polaritySelector.accept(Polarity.UNKNOWN);
                }
            }
        }

        @Override
        public void syncDetected(int bitErrors) {
            // Check for polarity lock if we're not selected
            if (!isSelected) {
                syncCount++;
                if (syncCount >= FRAMES_UNTIL_POLARITY_LOCK) {
                    log.trace("Reached threshold of {} frames - locking on polarity {}",
                        FRAMES_UNTIL_POLARITY_LOCK, polarity);
                    polaritySelector.accept(polarity);
                }
            } else {
                // Reset symbol counter used for detecting sync loss
                symbolsSinceLastSync = 0;
            }
        }

        @Override
        public void syncLost(int bitsProcessed) {
        }
    }

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
                log.trace("Within last 2s: received {} frames, {} bits",
                    framesPerInterval, bitsPerInterval);
                lastTimestamp += 2000;
                framesPerInterval = 0;
                bitsPerInterval = 0;
            }
        }
    }
}
