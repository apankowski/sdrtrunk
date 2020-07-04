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

import com.google.common.base.Objects;

public class ChannelNumber {

    public enum Status {
        CONVERT_TO_CALLEE,
        CALL_QUEUED,
        SYSTEM_BUSY,
        CALL_DENIED,
    }

    private final int channel;
    private final Status status;

    private ChannelNumber(int channel) {
        this.channel = channel;
        this.status = decodeStatus(channel);
    }

    static ChannelNumber of(int channel) {
        return new ChannelNumber(channel);
    }

    private static Status decodeStatus(int channel) {
        switch (channel) {
            // @formatter:off
            case 28: return Status.CONVERT_TO_CALLEE;
            case 29: return Status.CALL_QUEUED;
            case 30: return Status.SYSTEM_BUSY;
            case 31: return Status.CALL_DENIED;
            default: return null;
            // @formatter:on
        }
    }

    public int getNumber() {
        return channel;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isProperChannelNumber() {
        return 1 <= channel && channel <= 25;
    }

    @Override
    public String toString() {
        return String.valueOf(channel);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var that = (ChannelNumber) o;
        return channel == that.channel;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(channel);
    }
}
