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

public class UnkeyOrDropChannelMessage extends EDACSMessage.Narrow {

    private static final int NOTIFICATION_BIT = 8;
    private static final int[] CHANNEL_BITS = {9, 10, 11, 12, 13};
    private static final int[] LOGICAL_ID_BITS = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    enum Notification {
        /**
         * Unkey; hang time in force
         */
        UNKEY,
        /**
         * Drop; channel going down now
         */
        DROP,
    }

    private final Notification notification;
    private final ChannelNumber channel;
    private final UnitId unitId;

    UnkeyOrDropChannelMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.UNKEY_OR_CHANNEL_DROP, m);
        unitId = UnitId.from(m.getInt(LOGICAL_ID_BITS));
        channel = ChannelNumber.of(m.getInt(CHANNEL_BITS));
        notification = m.get(NOTIFICATION_BIT) ? Notification.DROP : Notification.UNKEY;
        reserved(m, 6, 7);
    }

    public UnitId getUnitId() {
        return unitId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", messageType)
            .add("unitId", unitId)
            .add("channel", channel)
            .add("notification", notification)
            .toString();
    }
}
