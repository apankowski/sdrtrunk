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

import io.github.dsheirer.BinaryMessageTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BCH_63_51_2Test implements BinaryMessageTestSupport {

    private final BCH_63_51_2 bch = new BCH_63_51_2();

    @ParameterizedTest(name = "decoding {0} produces {1} with {3} bit errors and result {2}")
    @CsvSource({
        "1111110101000001000100000001011110001000,1111110101000001000100000001011110001000,PASSED,0",
        "1111110001000000000000000000011011100110,1111110001000000000000000000011011100110,PASSED,0",
        "1111110101000001000100001001011110001000,1111110101000001000100000001011110001000,CORRECTED,1",
        "1111110001000000000000001000011011100110,1111110001000000000000000000011011100110,CORRECTED,1",
        "1111110101000001000000001001011110001000,1111110101000001000100000001011110001000,CORRECTED,2",
        "1111110001000000000100001000011011100110,1111110001000000000000000000011011100110,CORRECTED,2",
        "1111110101000001000000001001011110101000,1111110101000001000000001001011110101000,FAILED_CRC,0",
        "1111110001000000000100001000011011000110,1111110001000000000100001000011011000110,FAILED_CRC,0"
    })
    void verifyDecodingOfCodeword(String original, String corrected,
        CRC expectedCrcResult, int expectedCorrectedBitErrors) throws Exception {

        // given
        final var message = messageFromBits(original);

        // when
        bch.checkAndCorrect(message);

        // then
        assertThat(message.toString()).isEqualTo(corrected);
        assertThat(message.getCRC()).isEqualTo(expectedCrcResult);
        assertThat(message.getCorrectedBitCount()).isEqualTo(expectedCorrectedBitErrors);
    }

    @Test
    void correctedBitCountIsNotChangedForCorrectCodeword() throws Exception {
        // given
        final var message = messageFromBits("1111110101000001000100000001011110001000");
        final var previousCorrectedBitCount = 10;
        message.setCorrectedBitCount(previousCorrectedBitCount);

        // when
        bch.checkAndCorrect(message);

        // then
        assertThat(message.getCorrectedBitCount()).isEqualTo(previousCorrectedBitCount);
    }

    @Test
    void correctedBitCountIsIncrementedForCorrectedCodeword() throws Exception {
        // given
        final var message = messageFromBits("1111110001000000000100001000011011100110");
        message.setCorrectedBitCount(10);

        // when
        bch.checkAndCorrect(message);

        // then
        assertThat(message.getCorrectedBitCount()).isEqualTo(12);
    }
}
