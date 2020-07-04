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
package io.github.dsheirer.edac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder of {@code BCH(63, 51, t=2)} code, which has the following properties:
 * <ul>
 * <li>Each codeword has 51 data bits, followed by 12 CRC bits (for a total of 63 bits).
 * <li>At most 2 bits can be corrected.
 * <li>Minimal Hamming distance between two different codewords is 5.
 * </ul>
 */
// TODO: Is the name correct?
public class BCH_63_51_2 extends BerlekampMassey_63 {

    private static final Logger log = LoggerFactory.getLogger(BCH_63_51_2.class);

    public BCH_63_51_2() {
        super(2);
    }

    /**
     * Performs error detection and correction on the first 63 bits of supplied
     * message. The message is modified in place - its bits are changed accordingly
     * and its CRC status and corrected bit count are updated.
     */
    public void checkAndCorrect(CorrectedBinaryMessage message) {
        final var original = message.toReverseIntegerArray(0, CODEWORD_LENGTH - 1);
        final var corrected = new int[CODEWORD_LENGTH];
        final var irrecoverable = decode(original, corrected);

        if (irrecoverable) {
            message.setCRC(CRC.FAILED_CRC);
            return;
        }

        var correctedBits = 0;
        for (var i = 0; i < CODEWORD_LENGTH; i++) {
            if (corrected[i] != original[i]) {
                message.flip(CODEWORD_LENGTH - i - 1);
                correctedBits++;
            }
        }

        message.setCRC(correctedBits == 0 ? CRC.PASSED : CRC.CORRECTED);
        message.incrementCorrectedBitCount(correctedBits);
    }
}
