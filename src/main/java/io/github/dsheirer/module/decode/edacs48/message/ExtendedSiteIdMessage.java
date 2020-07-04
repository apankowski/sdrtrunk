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
package io.github.dsheirer.module.decode.edacs48.message;

import com.google.common.base.MoreObjects;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

class ExtendedSiteIdMessage extends EDACSMessage.Narrow {

    private static final int[] COLOR_CODE_BITS = {25, 26, 27};

    private final int colorCode;
    private final int initializationByte;

    ExtendedSiteIdMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.EXTENDED_SITE_ID, m);
        reserved(m, 15, 21); // "Options bitmap"
        reserved(m, 22, 24);
        colorCode = m.getInt(COLOR_CODE_BITS);
        initializationByte = decodeInitializationByte(colorCode);
    }

    private int decodeInitializationByte(int bits) {
        switch (bits) {
            // @formatter:off
            case 0b000: return 0b00000000;
            case 0b001: return 0b00000010;
            case 0b010: return 0b00000100;
            case 0b011: return 0b00001000;
            case 0b100: return 0b00010000;
            case 0b101: return 0b00100000;
            case 0b110: return 0b01000000;
            case 0b111: return 0b10000000;
            // @formatter:on
        }
        throw new IllegalArgumentException("Illegal color code bits " + binary(bits, 3));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", messageType)
            .add("colorCode", colorCode)
            .add("initializationByte", "0x" + hexadecimal(initializationByte, 2))
            .toString();
    }
}
