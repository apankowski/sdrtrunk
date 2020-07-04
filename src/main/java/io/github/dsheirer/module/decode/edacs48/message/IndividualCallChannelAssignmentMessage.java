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

public class IndividualCallChannelAssignmentMessage extends EDACSMessage.Wide {

    private static final int TRANSMISSION_TRUNKING_BIT = 6;
    private static final int[] CHANNEL_BITS = {8, 9, 10, 11, 12};
    private static final int CALL_TYPE_BIT = 13;
    private static final int[] LOGICAL_ID_BITS = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    private final UnitId callerUnitId;
    private final UnitId calleeUnitId;
    private final ChannelNumber channel;
    private final boolean digitalVoice;
    private final boolean transmissionTrunking;

    IndividualCallChannelAssignmentMessage(CorrectedBinaryMessage m1, CorrectedBinaryMessage m2) {
        super(EDACSMessageType.INDIVIDUAL_CALL_CHANNEL_ASSIGNMENT, m1, m2);
        callerUnitId = UnitId.from(m2.getInt(LOGICAL_ID_BITS));
        calleeUnitId = UnitId.to(m1.getInt(LOGICAL_ID_BITS));
        channel = ChannelNumber.of(m1.getInt(CHANNEL_BITS));
        digitalVoice = m1.get(CALL_TYPE_BIT);
        transmissionTrunking = m1.get(TRANSMISSION_TRUNKING_BIT);
        reserved(m1, 7, 7);
    }

    public UnitId getCallerUnitId() {
        return callerUnitId;
    }

    public UnitId getCalleeUnitId() {
        return calleeUnitId;
    }

    public ChannelNumber getChannel() {
        return channel;
    }

    public boolean isTestCall() {
        // Individual call channel assignment message with both unit IDs equal to zero
        // is a consequence of an initiated test call.
        return callerUnitId.isZero() && calleeUnitId.isZero();
    }

    @Override
    protected List<Identifier> gatherIdentifiers() {
        return List.of(callerUnitId, calleeUnitId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", getMessageType())
            .add("callerUnitId", callerUnitId)
            .add("calleeUnitId", calleeUnitId)
            .add("channel", channel)
            .add("digitalVoice", digitalVoice)
            .add("transmissionTrunking", transmissionTrunking)
            .toString();
    }
}
