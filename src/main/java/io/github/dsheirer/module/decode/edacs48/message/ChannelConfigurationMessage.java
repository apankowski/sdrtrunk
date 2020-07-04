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

class ChannelConfigurationMessage extends EDACSMessage.Narrow {

    private static final int[] CHANNEL_BITS = {11, 12, 13, 14, 15};
    private static final int[] FCC_FREQUENCY_BITS = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    private final ChannelNumber channel;
    private final int fccFrequency;

    ChannelConfigurationMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.CHANNEL_CONFIGURATION, m);
        channel = ChannelNumber.of(m.getInt(CHANNEL_BITS));
        fccFrequency = m.getInt(FCC_FREQUENCY_BITS);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", messageType)
            .add("channel", channel)
            .add("fccFrequency", fccFrequency)
            .toString();
    }
}