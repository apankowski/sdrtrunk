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

public class AdjacentSiteInformationMessage extends EDACSMessage.Narrow {

    private static final int[] CONTROL_CHANNEL_BITS = {11, 12, 13, 14, 15};
    private static final int[] INDEX_BITS = {16, 17, 18};
    private static final int[] SITE_ID_BITS = {19, 20, 21, 22, 23};

    public enum Notification {
        TABLE_RESET,
        TABLE_LENGTH,
        SYSTEM_DEFINITION,
        PRIORITY_SYSTEM_DEFINITION,
    }

    private final AdjacentSiteId adjacentSiteId;
    private final ChannelNumber controlChannel;
    private final Notification notification;
    private final int index;

    AdjacentSiteInformationMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.ADJACENT_SITE_INFORMATION, m);
        adjacentSiteId = AdjacentSiteId.of(m.getInt(SITE_ID_BITS));
        controlChannel = ChannelNumber.of(m.getInt(CONTROL_CHANNEL_BITS));
        index = m.getInt(INDEX_BITS);
        notification = decodeNotification(index, adjacentSiteId.getValue());
        reserved(m, 24, 27);
    }

    private static Notification decodeNotification(int index, int siteId) {
        if (siteId == 0 && index == 0) return Notification.TABLE_RESET;
        else if (siteId == 0) return Notification.TABLE_LENGTH;
        else if (index == 0) return Notification.PRIORITY_SYSTEM_DEFINITION;
        else return Notification.SYSTEM_DEFINITION;
    }

    public Notification getNotification() {
        return notification;
    }

    @Override
    protected List<Identifier> gatherIdentifiers() {
        return List.of(adjacentSiteId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", messageType)
            .add("adjacentSiteId", adjacentSiteId)
            .add("controlChannel", controlChannel)
            .add("notification", notification)
            .add("index", index)
            .toString();
    }
}
