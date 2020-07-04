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

public class DataChannelAssignmentMessage extends EDACSMessage.Wide {

    private static final int CALL_TYPE_BIT = 3;
    private static final int DIRECTION_BIT = 4;
    private static final int[] PORT_BITS = {5, 6, 7};
    private static final int[] CHANNEL_BITS = {8, 9, 10, 11, 12};
    private static final int INDIVIDUAL_BIT = 13;
    private static final int[] GROUP_ID_BITS = {17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] LOGICAL_ID_BITS = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    enum CallType {
        GROUP,
        INDIVIDUAL,
    }

    enum Direction {
        INBOUND, // From port to group/unit
        OUTBOUND, // From unit to port
    }

    private final boolean individual;
    private final UnitId unitId;
    private final GroupId groupId;
    private final CallType callType;
    private final Direction direction;
    private final int port;
    private final ChannelNumber channel;

    DataChannelAssignmentMessage(CorrectedBinaryMessage m1, CorrectedBinaryMessage m2) {
        super(EDACSMessageType.DATA_CHANNEL_ASSIGNMENT, m1, m2);
        individual = m1.get(INDIVIDUAL_BIT);
        if (individual) {
            unitId = UnitId.to(m1.getInt(LOGICAL_ID_BITS));
            groupId = null;
        } else {
            unitId = null;
            groupId = GroupId.to(m1.getInt(GROUP_ID_BITS));
        }

        callType = m1.get(CALL_TYPE_BIT) ? CallType.INDIVIDUAL : CallType.GROUP;
        direction = m1.get(DIRECTION_BIT) ? Direction.OUTBOUND : Direction.INBOUND;
        port = (m1.getInt(PORT_BITS) << 3) | m2.getInt(PORT_BITS);
        channel = ChannelNumber.of(m1.getInt(CHANNEL_BITS));
    }

    public UnitId getUnitId() {
        return unitId;
    }

    public GroupId getGroupId() {
        return groupId;
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
            .add("type", getMessageType())
            .add("individual", individual)
            .add("groupId", groupId)
            .add("unitId", unitId)
            .add("callType", callType)
            .add("direction", direction)
            .add("port", port)
            .add("channel", channel)
            .toString();
    }
}
