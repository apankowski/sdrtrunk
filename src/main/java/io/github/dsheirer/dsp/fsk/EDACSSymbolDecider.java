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
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

// TODO: Panel for decoder & decider
class EDACSSymbolDecider {

    private static final float MIN_SAMPLES_PER_SYMBOL = 3f;
    private static final Logger log = LoggerFactory.getLogger(EDACSSymbolDecider.class);
    private final float samplesPerSymbol;
    private final boolean[] buffer;
    private float currentSymbolPeriod;
    private float previousSymbolPeriod;
    private float samplesUntilSymbol;
    private float zeroCrossing;
    private int zeroCrossingCount;
    private IBinarySymbolProcessor symbolProcessor;

    EDACSSymbolDecider(float samplesPerSymbol) {
        if (samplesPerSymbol < MIN_SAMPLES_PER_SYMBOL) {
            throw new IllegalArgumentException("Samples per symbol must be at least " + MIN_SAMPLES_PER_SYMBOL);
        }

        this.samplesPerSymbol = samplesPerSymbol;

        // Initialize delay line buffer and reset
        buffer = new boolean[(int) FastMath.ceil(samplesPerSymbol * 2f)];
        reset();
    }

    void reset() {
        Arrays.fill(buffer, false);

        // These will change based on detected timing error
        currentSymbolPeriod = samplesPerSymbol;
        previousSymbolPeriod = samplesPerSymbol;

        // Set delay until symbol to 3/2 of sample period. This ensures that we'll
        // have enough samples in the delay line to not only determine current
        // symbol value but also determine timing error which uses samples between
        // middle of previous sample period to middle of current sample period.
        samplesUntilSymbol = samplesPerSymbol * 1.5f;
    }

    void setSymbolProcessor(IBinarySymbolProcessor symbolProcessor) {
        this.symbolProcessor = symbolProcessor;
    }

    void receive(float sample) {
        // Store sample in delay line buffer - shift samples to the left and store given sample at the end
        System.arraycopy(buffer, 1, buffer, 0, buffer.length - 1);
        buffer[buffer.length - 1] = sample > 0f;

        // Update delay until symbol
        samplesUntilSymbol--;
        if (samplesUntilSymbol <= 0f) {
            logInfo("Fell below symbol threshold: {}", samplesUntilSymbol);

            final var symbol = determineSymbol();
            if (symbolProcessor != null) {
                symbolProcessor.process(symbol);
            }

            final var timeError = calculateTimingError();
            previousSymbolPeriod = currentSymbolPeriod;
            currentSymbolPeriod = samplesPerSymbol + timeError * 0.1f;
            samplesUntilSymbol += currentSymbolPeriod;
            logInfo("Calculated time error is now {}, previous symbol samples {}, current symbol samples {}, new samples until symbol {}", timeError, previousSymbolPeriod, currentSymbolPeriod, samplesUntilSymbol);
        }
    }

    private int delayToIndex(float delay) {
        return (int) (buffer.length + samplesUntilSymbol - delay + 0.5f);
    }

    private boolean determineSymbol() {
        final var from = delayToIndex(currentSymbolPeriod);
        final var to = delayToIndex(0f);

        var positiveSamples = 0;
        for (var i = from; i < to; i++) {
            if (buffer[i]) positiveSamples++;
        }

        final var symbol = positiveSamples > (to - from) / 2;

        logInfo("Determined symbol {} based on samples from {} to {} with {} positive samples out of {} considered", symbol, from, to, positiveSamples, to - from);

        return symbol;
    }

    private float calculateTimingError() {
        final var from = delayToIndex(currentSymbolPeriod + previousSymbolPeriod / 2f);
        final var to = delayToIndex(currentSymbolPeriod / 2f);
        final var symbolBoundary = buffer.length + samplesUntilSymbol - currentSymbolPeriod;

        zeroCrossingCount = 0;
        zeroCrossing = Float.MAX_VALUE;
        for (var i = from; i < to; i++) {
            if (buffer[i - 1] ^ buffer[i]) {
                zeroCrossingCount++;
                final var zc = i - symbolBoundary;
                zeroCrossing = FastMath.abs(zeroCrossing) <= FastMath.abs(zc) ? zeroCrossing : zc;
            }
        }
        if (zeroCrossingCount == 0) {
            zeroCrossing = 0f;
        }

        logInfo("Calculating time error based on samples from {} to {} with symbol boundary at {}. Found {} zero crossings, closest at {}", from, to, symbolBoundary, zeroCrossingCount, zeroCrossing);

        return zeroCrossing;
    }

    private int logCounter = 0;

    // TODO: Remove
    private void logInfo(String format, Object... arguments) {
        if (logCounter < 0) {
            log.info(format, arguments);
            logCounter++;
        }
    }
}
