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

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class EDACSTrafficChannelManager extends Module implements IChannelEventProvider,
    IChannelEventListener, IDecodeEventProvider {

    private static final Logger log = LoggerFactory.getLogger(EDACSTrafficChannelManager.class);

    /**
     * Representation of a traffic channel belonging to a pool. At any moment a pooled
     * traffic channel may be either assigned to an actual working channel (decoding
     * audio from a specific radio frequency) or unassigned (being idle).
     */
    private static class PooledChannel {

        final Channel channel;
        int channelNumber;
        Identifier<?> calleeId;
        DecodeEvent decodeEvent;
        long lastActiveTimeMillis;

        private PooledChannel(Channel channel) {
            this.channel = channel;
        }

        void assign(int channelNumber, Identifier<?> to, DecodeEvent decodeEvent) {
            this.channelNumber = channelNumber;
            this.calleeId = to;
            this.decodeEvent = decodeEvent;
        }

        void lastSeen(IMessage message) {
            lastActiveTimeMillis = message.getTimestamp();
        }

        boolean isAssigned() {
            return channelNumber != 0;
        }

        void unassign() {
            channelNumber = 0;
            calleeId = null;
            decodeEvent = null;
            lastActiveTimeMillis = 0;
        }
    }

    private final PooledChannel[] channelPool;
    private final long callTimeoutMillis;
    private final TeardownMonitor teardownMonitor = new TeardownMonitor();
    private Future<?> channelTimeoutScheduledTask;
    private Listener<ChannelEvent> channelEventListener;
    private Listener<IDecodeEvent> decodeEventListener;

    public EDACSTrafficChannelManager(Channel channel, long callTimeoutMillis) {
        if (channel.getChannelType() != Channel.ChannelType.STANDARD) {
            throw new IllegalArgumentException(
                "EDACS Traffic Channel Manager cannot be created for a traffic channel");
        }
        channelPool = createTrafficChannelPool(channel);
        this.callTimeoutMillis = callTimeoutMillis;
    }

    private static PooledChannel[] createTrafficChannelPool(Channel channel) {
        final var genericDecodeConfig = channel.getDecodeConfiguration();
        if (!(genericDecodeConfig instanceof DecodeConfigEDACS48)) {
            throw new IllegalArgumentException(
                "Tried to initialize EDACS traffic channel manager with non-EDACS decode configuration");
        }

        final var decodeConfig = (DecodeConfigEDACS48) genericDecodeConfig;
        final var poolSize = decodeConfig.getTrafficChannelPoolSize();
        final var pooledChannels = new PooledChannel[poolSize];

        for (int i = 0; i < poolSize; i++) {
            final var trafficChannel = new Channel("TRAFFIC", Channel.ChannelType.TRAFFIC);
            trafficChannel.setAliasListName(channel.getAliasListName());
            trafficChannel.setSystem(channel.getSystem());
            trafficChannel.setSite(channel.getSite());
            trafficChannel.setDecodeConfiguration(decodeConfig);
            trafficChannel.setEventLogConfiguration(channel.getEventLogConfiguration());
            trafficChannel.setRecordConfiguration(channel.getRecordConfiguration());

            pooledChannels[i] = new PooledChannel(trafficChannel);
        }

        return pooledChannels;
    }

    @Override
    public void reset() {
    }

    @Override
    public void start() {
        channelTimeoutScheduledTask = ThreadPool.SCHEDULED.scheduleAtFixedRate(
            this::stopChannelsAfterTimeout, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        channelTimeoutScheduledTask.cancel(true);
        for (final var pooledChannel : channelPool) {
            if (pooledChannel.isAssigned()) {
                final var channel = pooledChannel.channel;
                broadcast(new ChannelEvent(channel, ChannelEvent.Event.REQUEST_DISABLE));
                pooledChannel.unassign();
            }
        }
    }

    @Override
    public void dispose() {
        channelEventListener = null;
        decodeEventListener = null;
    }

    @Override
    public void setChannelEventListener(Listener<ChannelEvent> listener) {
        channelEventListener = listener;
    }

    @Override
    public void removeChannelEventListener() {
        channelEventListener = null;
    }

    private void broadcast(ChannelEvent event) {
        if (channelEventListener != null) {
            channelEventListener.receive(event);
        }
    }

    @Override
    public Listener<ChannelEvent> getChannelEventListener() {
        return teardownMonitor;
    }

    @Override
    public void addDecodeEventListener(Listener<IDecodeEvent> listener) {
        decodeEventListener = listener;
    }

    @Override
    public void removeDecodeEventListener(Listener<IDecodeEvent> listener) {
        decodeEventListener = null;
    }

    private void broadcast(IDecodeEvent decodeEvent) {
        if (decodeEventListener != null) {
            decodeEventListener.receive(decodeEvent);
        }
    }

    private PooledChannel pooledChannel(Channel channel) {
        for (final var pooledChannel : channelPool) {
            if (channel.equals(pooledChannel.channel)) {
                return pooledChannel;
            }
        }
        return null;
    }

    private PooledChannel pooledChannel(int channelNumber) {
        for (final var pooledChannel : channelPool) {
            if (pooledChannel.channelNumber == channelNumber) {
                return pooledChannel;
            }
        }
        return null;
    }

    private PooledChannel firstAvailablePooledChannel() {
        for (final var pooledChannel : channelPool) {
            if (!pooledChannel.isAssigned()) {
                return pooledChannel;
            }
        }
        return null;
    }

    void handleCall(IMessage message, DecodeEvent decodeEvent) {
        final var channelDescriptor = decodeEvent.getChannelDescriptor();
        if (channelDescriptor == null) {
            log.warn("Cannot handle call event {} - channel descriptor is not set", decodeEvent);
            return;
        } else if (!(channelDescriptor instanceof EDACSChannelDescriptor)) {
            log.warn("Cannot handle call event {} - channel descriptor is not of EDACS kind",
                decodeEvent);
            return;
        }

        final var channelNumber = ((EDACSChannelDescriptor) channelDescriptor).getChannelNumber();
        final var calleeId = decodeEvent.getIdentifierCollection().getToIdentifier();

        var pooledChannel = pooledChannel(channelNumber);
        if (pooledChannel != null) {
            if (Objects.equals(pooledChannel.calleeId, calleeId)) {
                pooledChannel.lastSeen(message);
                pooledChannel.decodeEvent.end(message.getTimestamp());
                broadcast(pooledChannel.decodeEvent);
                return;
            } else {
                log.debug("Channel {} is currently assigned to {} - re-assigning to {}",
                    channelNumber, pooledChannel.calleeId, calleeId);
                broadcast(new ChannelEvent(pooledChannel.channel, ChannelEvent.Event.REQUEST_DISABLE));
            }
        }

        if (channelDescriptor.getDownlinkFrequency() == 0) {
            decodeEvent.setDetails("Invalid channel map - no frequency for channel " + channelNumber);
            broadcast(decodeEvent);
            return;
        }

        pooledChannel = firstAvailablePooledChannel();
        if (pooledChannel == null) {
            decodeEvent.setDetails("No traffic channel available - traffic channel pool exhausted");
            broadcast(decodeEvent);
            return;
        }

        pooledChannel.assign(channelNumber, calleeId, decodeEvent);
        pooledChannel.lastSeen(message);

        final var sourceConfig = new SourceConfigTuner();
        sourceConfig.setFrequency(channelDescriptor.getDownlinkFrequency());
        pooledChannel.channel.setSourceConfiguration(sourceConfig);

        getInterModuleEventBus().post(new ChannelStartProcessingRequest(
            pooledChannel.channel,
            channelDescriptor,
            decodeEvent.getIdentifierCollection()
        ));

        broadcast(decodeEvent);
    }

    private void stopChannelsAfterTimeout() {
        final var now = System.currentTimeMillis();
        for (final var pooledChannel : channelPool) {
            if (pooledChannel.isAssigned()) {
                if (now - pooledChannel.lastActiveTimeMillis >= callTimeoutMillis) {
                    broadcast(new ChannelEvent(pooledChannel.channel, ChannelEvent.Event.REQUEST_DISABLE));
                }
            }
        }
    }

    private class TeardownMonitor implements Listener<ChannelEvent> {

        private PooledChannel lookupPooledChannel(ChannelEvent channelEvent) {
            final var channel = channelEvent.getChannel();
            if (channel == null || !channel.isTrafficChannel()) {
                return null;
            }

            final var pooledChannel = pooledChannel(channel);
            if (pooledChannel == null || !pooledChannel.isAssigned()) {
                return null;
            }

            return pooledChannel;
        }

        @Override
        public void receive(ChannelEvent channelEvent) {
            switch (channelEvent.getEvent()) {
                case NOTIFICATION_PROCESSING_STOP: {
                    final var pooledChannel = lookupPooledChannel(channelEvent);
                    if (pooledChannel != null) {
                        pooledChannel.decodeEvent.end(System.currentTimeMillis());
                        broadcast(pooledChannel.decodeEvent);
                        pooledChannel.unassign();
                    }
                    break;
                }
                case NOTIFICATION_PROCESSING_START_REJECTED: {
                    final var pooledChannel = lookupPooledChannel(channelEvent);
                    if (pooledChannel != null) {
                        pooledChannel.decodeEvent.setDetails("Traffic channel - start rejected");
                        broadcast(pooledChannel.decodeEvent);
                        pooledChannel.unassign();
                    }
                    break;
                }
            }
        }
    }
}
