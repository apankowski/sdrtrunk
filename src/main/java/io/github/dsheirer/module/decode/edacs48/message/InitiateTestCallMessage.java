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

public class InitiateTestCallMessage extends EDACSMessage.Narrow {

    private static final int[] CONTROL_CHANNEL_BITS = {11, 12, 13, 14, 15};
    private static final int[] WORKING_CHANNEL_BITS = {16, 17, 18, 19, 20};

    private final ChannelNumber controlChannel;
    private final ChannelNumber workingChannel;

    InitiateTestCallMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.INITIATE_TEST_CALL, m);
        controlChannel = ChannelNumber.of(m.getInt(CONTROL_CHANNEL_BITS));
        workingChannel = ChannelNumber.of(m.getInt(WORKING_CHANNEL_BITS));
        reserved(m, 21, 27);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", messageType)
            .add("controlChannel", controlChannel)
            .add("workingChannel", workingChannel)
            .toString();
    }
}
