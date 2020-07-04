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

public class SiteIdMessage extends EDACSMessage.Narrow {

    // These are based on US patent 4,939,746
    private static final int[] DELAY_BITS = {9, 10};
    private static final int[] CHANNEL_BITS = {11, 12, 13, 14, 15};
    private static final int[] PRIORITY_BITS = {16, 17, 18};
    private static final int HOMESITE_BIT = 19;
    private static final int[] FAILSOFT_BITS = {20, 21};
    private static final int[] SITE_ID_BITS = {22, 23, 24, 25, 26, 27};

    // TODO: Figure out exact meaning of unknown values
    enum Failsoft {
        UNKNOWN1,
        UNKNOWN2,
        UNKNOWN3,
        SCAT,
    }

    private final SiteId siteId;
    private final ChannelNumber channel;
    private final boolean homesite;
    private final int priority;
    private final int delayInSlots;
    private final Failsoft failsoft;

    SiteIdMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.SITE_ID, m);
        siteId = SiteId.of(m.getInt(SITE_ID_BITS));
        channel = ChannelNumber.of(m.getInt(CHANNEL_BITS));
        homesite = m.get(HOMESITE_BIT);
        priority = m.getInt(PRIORITY_BITS);
        delayInSlots = decodeDelay(m.getInt(DELAY_BITS));
        failsoft = decodeFailsoft(m.getInt(FAILSOFT_BITS));
    }

    private static int decodeDelay(int bits) {
        switch (bits) {
            // @formatter:off
            case 0b00: return 2;
            case 0b01: return 3;
            case 0b10: return 5;
            case 0b11: return 8;
            // @formatter:on
        }
        throw new IllegalArgumentException("Illegal delay bits " + binary(bits, 2));
    }

    private static Failsoft decodeFailsoft(int bits) {
        switch (bits) {
            // @formatter:off
            case 0b00: return Failsoft.UNKNOWN1;
            case 0b01: return Failsoft.UNKNOWN2;
            case 0b10: return Failsoft.UNKNOWN3;
            case 0b11: return Failsoft.SCAT;
            // @formatter:on
        }
        throw new IllegalArgumentException("Illegal failsoft bits " + binary(bits, 2));
    }

    public SiteId getSiteId() {
        return siteId;
    }

    @Override
    protected List<Identifier> gatherIdentifiers() {
        return List.of(siteId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", messageType)
            .add("siteId", siteId)
            .add("channel", channel)
            .add("homesite", homesite)
            .add("priority", priority)
            .add("delayInSlots", delayInSlots)
            .add("failsoft", failsoft)
            .toString();
    }
}
