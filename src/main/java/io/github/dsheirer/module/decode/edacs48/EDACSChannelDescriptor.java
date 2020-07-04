/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.edacs48;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;

/**
 * EDACS channel descriptor.
 */
class EDACSChannelDescriptor implements IChannelDescriptor {

    private static final int[] EMPTY_FREQUENCY_BAND_IDENTIFIERS = {};
    private final int channelNumber;
    private final ChannelMap channelMap;

    EDACSChannelDescriptor(int channelNumber, ChannelMap channelMap) {
        this.channelNumber = channelNumber;
        this.channelMap = channelMap;
    }

    int getChannelNumber() {
        return channelNumber;
    }

    @Override
    public long getDownlinkFrequency() {
        if (channelMap != null) {
            return channelMap.getFrequency(channelNumber);
        }
        return 0;
    }

    @Override
    public long getUplinkFrequency() {
        return 0;
    }

    @Override
    public int[] getFrequencyBandIdentifiers() {
        return EMPTY_FREQUENCY_BAND_IDENTIFIERS;
    }

    @Override
    public void setFrequencyBand(IFrequencyBand bandIdentifier) {
        throw new UnsupportedOperationException("Can't set frequency band on EDACS channel descriptor");
    }

    @Override
    public boolean isTDMAChannel() {
        return false;
    }

    @Override
    public int getTimeslotCount() {
        return 0;
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.EDACS;
    }

    @Override
    public String toString() {
        return String.valueOf(channelNumber);
    }
}
