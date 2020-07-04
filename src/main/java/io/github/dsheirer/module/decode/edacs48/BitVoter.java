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
import io.github.dsheirer.edac.CRC;

class BitVoter {

    /**
     * Returns a binary message that is a result of bit-voting among bits in the input messages.
     * <p>
     * Returned message has the same size as all input messages. Bit on position {@code k} is
     * the one most frequently occurring among bits on position {@code k} in input messages.
     * Corrected bit count
     * <p>
     * As an example:
     * <ul>
     * <li>bits {@code 1}, {@code 0} and {@code 1} produce {@code 1},
     * <li>bits {@code 0}, {@code 1} and {@code 0} produce {@code 0}.
     * </ul>
     *
     * @throws IllegalArgumentException if messages aren't of same size
     */
    CorrectedBinaryMessage bitVotedMessage(CorrectedBinaryMessage m1, CorrectedBinaryMessage m2,
        CorrectedBinaryMessage m3) {

        if (m1.size() != m2.size() || m1.size() != m3.size()) {
            throw new IllegalArgumentException("Binary messages passed to bit voter have " +
                "different sizes: " + m1.size() + ", " + m2.size() + ", " + m3.size());
        }

        final var size = m1.size();
        final var result = new CorrectedBinaryMessage(size);
        var correctedBits = 0;
        for (var i = 0; i < size; i++) {
            final var lookupIndex = (m1.get(i) ? 1 : 0) | (m2.get(i) ? 2 : 0) | (m3.get(i) ? 4 : 0);
            result.set(i, ((0b11101000 >> lookupIndex) & 1) != 0);
            correctedBits += (0b01111110 >> lookupIndex) & 1;
        }

        result.setCRC(correctedBits == 0 ? CRC.PASSED : CRC.CORRECTED);
        result.incrementCorrectedBitCount(correctedBits);
        return result;
    }

}
