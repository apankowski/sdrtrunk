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

public class SystemAllCallMessage extends EDACSMessage.Wide {

    private static final int[] CHANNEL_BITS = {11, 12, 13, 14, 15};
    private static final int DIGITAL_VOICE_BIT = 16;
    private static final int NOTIFICATION_BIT = 17;
    private static final int TRANSMISSION_TRUNKING_BIT = 18;
    // TODO: Diagram mentions 9 bits but text says about 8 bits... verify this elsewhere
    private static final int[] LOGICAL_ID_BITS = {19, 20, 21, 22, 23, 24, 25, 26, 27};

    public enum Notification {
        CHANNEL_ASSIGNMENT,
        UPDATE,
    }

    private final UnitId callerUnitId;
    private final ChannelNumber channel;
    private final boolean digitalVoice;
    private final boolean transmissionTrunking;
    private final Notification notification;

    SystemAllCallMessage(CorrectedBinaryMessage m1, CorrectedBinaryMessage m2) {
        super(EDACSMessageType.SYSTEM_ALL_CALL, m1, m2);
        callerUnitId = UnitId.from((m1.getInt(LOGICAL_ID_BITS) << 5) | m2.getInt(LOGICAL_ID_BITS));
        channel = ChannelNumber.of(m1.getInt(CHANNEL_BITS));
        digitalVoice = m1.get(DIGITAL_VOICE_BIT);
        transmissionTrunking = m1.get(TRANSMISSION_TRUNKING_BIT);
        notification = m1.get(NOTIFICATION_BIT) ? Notification.UPDATE : Notification.CHANNEL_ASSIGNMENT;
    }

    public UnitId getCallerUnitId() {
        return callerUnitId;
    }

    public ChannelNumber getChannel() {
        return channel;
    }

    public Notification getNotification() {
        return notification;
    }

    @Override
    protected List<Identifier> gatherIdentifiers() {
        return List.of(callerUnitId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", getMessageType())
            .add("callerUnitId", callerUnitId)
            .add("channel", channel)
            .add("digitalVoice", digitalVoice)
            .add("transmissionTrunking", transmissionTrunking)
            .add("notification", notification)
            .toString();
    }
}
