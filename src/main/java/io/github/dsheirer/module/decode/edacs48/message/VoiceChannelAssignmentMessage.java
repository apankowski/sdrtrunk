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
import io.github.dsheirer.identifier.Identifier;

import java.util.List;

public class VoiceChannelAssignmentMessage extends EDACSMessage.Wide {

    private static final int DIGITAL_VOICE_BIT = 1;
    private static final int EMERGENCY_BIT = 2;
    private static final int[] LOGICAL_ID_BITS = {3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] CHANNEL_BITS = {11, 12, 13, 14, 15};
    private static final int TRANSMISSION_TRUNKING_BIT = 16;
    private static final int[] GROUP_ID_BITS = {17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    private final UnitId callerUnitId;
    private final GroupId groupId;
    private final ChannelNumber channel;
    private final boolean digitalVoice;
    private final boolean emergency;
    private final boolean transmissionTrunking;

    VoiceChannelAssignmentMessage(CorrectedBinaryMessage m1, CorrectedBinaryMessage m2) {
        super(EDACSMessageType.VOICE_CHANNEL_ASSIGNMENT, m1, m2);
        callerUnitId = UnitId.from(m1.getInt(LOGICAL_ID_BITS) << 6 | m2.getInt(LOGICAL_ID_BITS));
        groupId = GroupId.to(m1.getInt(GROUP_ID_BITS));
        channel = ChannelNumber.of(m1.getInt(CHANNEL_BITS));
        digitalVoice = m1.get(DIGITAL_VOICE_BIT);
        emergency = m1.get(EMERGENCY_BIT);
        transmissionTrunking = m1.get(TRANSMISSION_TRUNKING_BIT);
    }

    public UnitId getCallerUnitId() {
        return callerUnitId;
    }

    public GroupId getGroupId() {
        return groupId;
    }

    public ChannelNumber getChannel() {
        return channel;
    }

    @Override
    protected List<Identifier> gatherIdentifiers() {
        return List.of(callerUnitId, groupId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", getMessageType())
            .add("callerUnitId", callerUnitId)
            .add("groupId", groupId)
            .add("channel", channel)
            .add("digitalVoice", digitalVoice)
            .add("emergency", emergency)
            .add("transmissionTrunking", transmissionTrunking)
            .toString();
    }
}
