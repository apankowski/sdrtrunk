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
package io.github.dsheirer;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * A test mix-in that can be mixed into a test class to gain access to binary message
 * support methods.
 */
public interface BinaryMessageTestSupport {

    default CorrectedBinaryMessage messageFromBits(String bits) throws Exception {
        final var message = new CorrectedBinaryMessage(bits.length());
        for (final var bit : bits.toCharArray()) message.add(bit != '0');
        return message;
    }
}
