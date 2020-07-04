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

public class AssignmentToAuxiliaryControlChannelMessage extends EDACSMessage.Narrow {

    private static final int[] CONTROL_CHANNEL_BITS = {11, 12, 13, 14, 15};
    private static final int[] GROUP_ID_BITS = {17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    private final GroupId groupId;
    private final ChannelNumber controlChannel;

    AssignmentToAuxiliaryControlChannelMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.ASSIGNMENT_TO_AUXILIARY_CONTROL_CHANNEL, m);
        groupId = GroupId.to(m.getInt(GROUP_ID_BITS));
        controlChannel = ChannelNumber.of(m.getInt(CONTROL_CHANNEL_BITS));
        reserved(m, 16, 16);
    }

    public GroupId getGroupId() {
        return groupId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", messageType)
            .add("groupId", groupId)
            .add("controlChannel", controlChannel)
            .toString();
    }
}
