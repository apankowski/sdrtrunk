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

public class InterconnectChannelAssignmentMessage extends EDACSMessage.Narrow {

    // TODO: Confirm meaning of 6th and 7th bit
    private static final int DIGITAL_VOICE_BIT = 6;
    private static final int[] CHANNEL_BITS = {8, 9, 10, 11, 12};
    private static final int INDIVIDUAL_BIT = 13;
    private static final int[] GROUP_ID_BITS = {17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] LOGICAL_ID_BITS = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    private final boolean individual;
    private final GroupId groupId;
    private final UnitId unitId;
    private final ChannelNumber channel;
    private final boolean digitalVoice;

    InterconnectChannelAssignmentMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.INTERCONNECT_CHANNEL_ASSIGNMENT, m);
        individual = m.get(INDIVIDUAL_BIT);
        if (individual) {
            groupId = null;
            unitId = UnitId.to(m.getInt(LOGICAL_ID_BITS));
        } else {
            groupId = GroupId.to(m.getInt(GROUP_ID_BITS));
            unitId = null;
        }

        channel = ChannelNumber.of(m.getInt(CHANNEL_BITS));
        digitalVoice = m.get(DIGITAL_VOICE_BIT);
        reserved(m, 7, 7);
    }

    public boolean isIndividual() {
        return individual;
    }

    public GroupId getGroupId() {
        return groupId;
    }

    public UnitId getUnitId() {
        return unitId;
    }

    public ChannelNumber getChannel() {
        return channel;
    }

    @Override
    protected List<Identifier> gatherIdentifiers() {
        return List.of(individual ? unitId : groupId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", messageType)
            .add("individual", individual)
            .add("groupId", groupId)
            .add("unitId", unitId)
            .add("channel", channel)
            .add("digitalVoice", digitalVoice)
            .toString();
    }
}
