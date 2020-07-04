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
package io.github.dsheirer.module.decode.edacs48;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.config.WithCallTimeout;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigEDACS48 extends DecodeConfiguration implements WithCallTimeout {

    private String channelMapName;
    private int callTimeoutSeconds = DEFAULT_CALL_TIMEOUT_DELAY_SECONDS;
    private int trafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType() {
        return DecoderType.EDACS48;
    }

    @JacksonXmlProperty(localName = "channelMapName")
    public String getChannelMapName() {
        return channelMapName;
    }

    public void setChannelMapName(String name) {
        channelMapName = name;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "call_timeout")
    public int getCallTimeoutSeconds()
    {
        return callTimeoutSeconds;
    }

    /**
     * Sets the call timeout value in seconds
     */
    public void setCallTimeoutSeconds(int timeout) {
        if (CALL_TIMEOUT_MINIMUM <= timeout && timeout <= CALL_TIMEOUT_MAXIMUM) {
            callTimeoutSeconds = timeout;
        } else {
            callTimeoutSeconds = DEFAULT_CALL_TIMEOUT_DELAY_SECONDS;
        }
    }

    @JacksonXmlProperty(isAttribute = true, localName = "traffic_channel_pool_size")
    public int getTrafficChannelPoolSize()
    {
        return trafficChannelPoolSize;
    }

    /**
     * Sets the traffic channel pool size which is the maximum number of
     * simultaneous traffic channels that can be allocated.
     *
     * This limits the maximum calls so that busy systems won't cause more
     * traffic channels to be allocated than the decoder/software/host computer
     * can support.
     */
    public void setTrafficChannelPoolSize(int size)
    {
        trafficChannelPoolSize = size;
    }

    @JsonIgnore
    public ChannelSpecification getChannelSpecification() {
        // TODO: minimumSampleRate, configurable bandwidth, pass & stop Freq
        return new ChannelSpecification(4800 * 8, 6_250, 3_125, 3_250);
    }
}
