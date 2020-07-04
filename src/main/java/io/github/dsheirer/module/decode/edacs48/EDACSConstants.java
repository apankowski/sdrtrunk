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

interface EDACSConstants {

    int BARKER1_LENGTH = 20;
    int DOTTING_LENGTH = 12;
    int BARKER2_LENGTH = 16;
    int MESSAGE_LENGTH = 28;
    int CRC_LENGTH = 12;
    int NUM_MESSAGE_COPIES = 3;
    int NUM_MESSAGES_IN_FRAME = 2;
    int FRAME_LENGTH =
            (BARKER1_LENGTH + DOTTING_LENGTH + BARKER2_LENGTH) +
                    ((MESSAGE_LENGTH + CRC_LENGTH) * NUM_MESSAGE_COPIES * NUM_MESSAGES_IN_FRAME);
}
