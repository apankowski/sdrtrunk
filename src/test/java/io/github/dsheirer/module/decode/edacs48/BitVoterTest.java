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

import io.github.dsheirer.BinaryMessageTestSupport;
import io.github.dsheirer.edac.CRC;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class BitVoterTest implements BinaryMessageTestSupport {

    final BitVoter bitVoter = new BitVoter();

    @ParameterizedTest(name = "bit voting between {0}, {1}, {2} produces {4} with {6} bit errors and result {5}")
    @CsvSource({
        "00001111,00110011,01010101,00010111,CORRECTED,6",
        "01100100,01100100,01100100,01100100,PASSED,0"
    })
    void bitVotedMessageIsCorrectlyCalculated(String m1Bits, String m2Bits, String m3Bits,
        String expectedBits, CRC expectedCrcResult, int expectedCorrectedBitCount) throws Exception {

        // given
        final var m1 = messageFromBits(m1Bits);
        final var m2 = messageFromBits(m2Bits);
        final var m3 = messageFromBits(m3Bits);

        // when
        final var bitVotedMessage = bitVoter.bitVotedMessage(m1, m2, m3);

        // then
        assertThat(bitVotedMessage.toString()).isEqualTo(expectedBits);
        assertThat(bitVotedMessage.getCorrectedBitCount()).isEqualTo(expectedCorrectedBitCount);
        assertThat(bitVotedMessage.getCRC()).isEqualTo(expectedCrcResult);
    }

    @Test
    void inputMessagesOfDifferentSizeCauseAnError() throws Exception {
        // given
        final var m1 = messageFromBits("0");
        final var m2 = messageFromBits("00000");
        final var m3 = messageFromBits("00");

        // expect
        assertThatThrownBy(() -> bitVoter.bitVotedMessage(m1, m2, m3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("different sizes: 1, 5, 2");
    }
}
