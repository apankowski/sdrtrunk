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

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.edacs48.message.AssignmentToAuxiliaryControlChannelMessage;
import io.github.dsheirer.module.decode.edacs48.message.CancelDynamicRegroupMessage;
import io.github.dsheirer.module.decode.edacs48.message.ChannelNumber;
import io.github.dsheirer.module.decode.edacs48.message.ChannelUpdateMessage;
import io.github.dsheirer.module.decode.edacs48.message.DataChannelAssignmentMessage;
import io.github.dsheirer.module.decode.edacs48.message.DynamicRegroupMessage;
import io.github.dsheirer.module.decode.edacs48.message.EDACSMessage;
import io.github.dsheirer.module.decode.edacs48.message.IndividualCallChannelAssignmentMessage;
import io.github.dsheirer.module.decode.edacs48.message.InterconnectChannelAssignmentMessage;
import io.github.dsheirer.module.decode.edacs48.message.LoginAcknowledgeMessage;
import io.github.dsheirer.module.decode.edacs48.message.PatchMessage;
import io.github.dsheirer.module.decode.edacs48.message.SiteIdMessage;
import io.github.dsheirer.module.decode.edacs48.message.StatusRequestOrAcknowledgeMessage;
import io.github.dsheirer.module.decode.edacs48.message.SystemAllCallMessage;
import io.github.dsheirer.module.decode.edacs48.message.UnkeyOrDropChannelMessage;
import io.github.dsheirer.module.decode.edacs48.message.VoiceChannelAssignmentMessage;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class EDACSDecoderState extends DecoderState {

    private static final Logger log = LoggerFactory.getLogger(EDACSDecoderState.class);
    private final ChannelType channelType;
    private final ChannelMap channelMap;
    private final EDACSTrafficChannelManager trafficChannelManager;
    private final Set<Integer> talkgroupIds = new TreeSet<>();
    private final Set<Integer> unitIds = new TreeSet<>();
    private final Multimap<Integer, Integer> patches = TreeMultimap.create();
    private EDACSMessage previousMessage;
    private Integer siteId;

    public EDACSDecoderState(ChannelType channelType, ChannelMap channelMap,
        EDACSTrafficChannelManager trafficChannelManager) {

        this.channelType = channelType;
        this.channelMap = channelMap;
        this.trafficChannelManager = trafficChannelManager;
    }

    @Override
    public DecoderType getDecoderType() {
        return DecoderType.EDACS48;
    }

    @Override
    public void init() {
    }

    @Override
    public void start() {
        super.start();
        if (channelType == ChannelType.TRAFFIC) {
            // Broadcast a call start event to open squelch (unmute audio)
            broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public void reset() {
        super.reset();
        resetState();
        siteId = null;
        talkgroupIds.clear();
        unitIds.clear();
        patches.clear();
        previousMessage = null;
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event) {
        if (event.getEvent() == Event.REQUEST_RESET) {
            resetState();
        }
    }

    private int traceState = 0;

    @Override
    public void receive(IMessage message) {
        if (!message.isValid() || !(message instanceof EDACSMessage)) {
            return;
        }
        if (isRepeatedMessage(previousMessage, (EDACSMessage) message)) {
            return;
        }

        final var type = ((EDACSMessage) message).getMessageType();
        switch (type) {
            case SITE_ID: {
                final var m = (SiteIdMessage) message;
                siteId = m.getSiteId().getValue();
                broadcast(new DecoderStateEvent(this, Event.START, State.CONTROL));
                break;
            }
            case VOICE_CHANNEL_ASSIGNMENT: {
                final var m = (VoiceChannelAssignmentMessage) message;
                talkgroupIds.add(m.getGroupId().getValue());
                unitIds.add(m.getCallerUnitId().getValue());

                final var decodeEvent = DecodeEvent.builder(m.getTimestamp())
                    .protocol(Protocol.EDACS)
                    .eventDescription("Call")
                    .details(channelStatusText(m.getChannel(), "Call"))
                    .channel(channelDescriptor(m.getChannel()))
                    .identifiers(buildIdentifiers(m.getIdentifiers()))
                    .build();

                broadcast(decodeEvent);
                broadcastContinuation();

                if (m.getChannel().isProperChannelNumber()) {
                    trafficChannelManager.handleCall(m, decodeEvent);
                }
                break;
            }
            case INDIVIDUAL_CALL_CHANNEL_ASSIGNMENT: {
                final var m = (IndividualCallChannelAssignmentMessage) message;
                if (m.isTestCall()) {
                    if (traceState == 0) {
                        log.trace("first test call message: {}", message);
                        traceState = 1;
                    }
                    else {
                        log.trace("last test call message: {}", message);
                        traceState = 0;
                    }
                    final DecodeEvent decodeEvent = DecodeEvent.builder(m.getTimestamp())
                        .protocol(Protocol.EDACS)
                        .eventDescription("Test Call")
                        .details(channelStatusText(m.getChannel(), "Call"))
                        .channel(channelDescriptor(m.getChannel()))
                        .identifiers(buildIdentifiers(List.of()))
                        .build();
                    broadcast(
                        decodeEvent
                    );
                    broadcastContinuation();
                    // TODO: Remove
                    trafficChannelManager.handleCall(m, decodeEvent);
                    return;
                }

                unitIds.add(m.getCallerUnitId().getValue());
                unitIds.add(m.getCalleeUnitId().getValue());

                final var decodeEvent = DecodeEvent.builder(m.getTimestamp())
                    .protocol(Protocol.EDACS)
                    .eventDescription("Individual Call")
                    .details(channelStatusText(m.getChannel(), "Call"))
                    .channel(channelDescriptor(m.getChannel()))
                    .identifiers(buildIdentifiers(m.getIdentifiers()))
                    .build();

                broadcast(decodeEvent);
                broadcastContinuation();

                if (m.getChannel().isProperChannelNumber()) {
                    trafficChannelManager.handleCall(m, decodeEvent);
                }
                break;
            }
            case CHANNEL_UPDATE: {
                final var m = (ChannelUpdateMessage) message;
                if (m.isTestCall()) {
                    broadcastContinuation();
                    return;
                }

                if (m.getGroupId() != null) {
                    talkgroupIds.add(m.getGroupId().getValue());
                }
                if (m.getUnitId() != null) {
                    unitIds.add(m.getUnitId().getValue());
                }

                if (m.getChannel().isProperChannelNumber()) {
                    final var description = m.isIndividual() ? "Individual Call" : "Call";

                    final var decodeEvent = DecodeEvent.builder(m.getTimestamp())
                        .protocol(Protocol.EDACS)
                        .eventDescription(description)
                        .details(channelStatusText(m.getChannel(), "Call") + ", late entry")
                        .channel(channelDescriptor(m.getChannel()))
                        .identifiers(buildIdentifiers(m.getIdentifiers()))
                        .build();

                    trafficChannelManager.handleCall(m, decodeEvent);
                } else {
                    log.debug("Received channel update message with unexpected status {}: {}",
                        m.getChannel().getNumber(), m.getChannel().getStatus());
                }
                broadcastContinuation();
                break;
            }
            case DATA_CHANNEL_ASSIGNMENT: {
                final var m = (DataChannelAssignmentMessage) message;

                if (m.getGroupId() != null) {
                    talkgroupIds.add(m.getGroupId().getValue());
                }
                if (m.getUnitId() != null) {
                    unitIds.add(m.getUnitId().getValue());
                }

                final var decodeEvent = DecodeEvent.builder(m.getTimestamp())
                    .protocol(Protocol.EDACS)
                    .eventDescription("Data Transfer")
                    .details(channelStatusText(m.getChannel(), "Transfer"))
                    .channel(channelDescriptor(m.getChannel()))
                    .identifiers(buildIdentifiers(m.getIdentifiers()))
                    .build();

                broadcast(decodeEvent);
                broadcastContinuation();
                break;
            }
            case INTERCONNECT_CHANNEL_ASSIGNMENT: {
                final var m = (InterconnectChannelAssignmentMessage) message;
                if (m.getGroupId() != null) {
                    talkgroupIds.add(m.getGroupId().getValue());
                }
                if (m.getUnitId() != null) {
                    unitIds.add(m.getUnitId().getValue());
                }

                final var decodeEvent = DecodeEvent.builder(m.getTimestamp())
                    .protocol(Protocol.EDACS)
                    .eventDescription("Interconnect Call")
                    .details(channelStatusText(m.getChannel(), "Call"))
                    .channel(channelDescriptor(m.getChannel()))
                    .identifiers(buildIdentifiers(m.getIdentifiers()))
                    .build();

                broadcast(decodeEvent);
                broadcastContinuation();

                if (m.getChannel().isProperChannelNumber()) {
                    trafficChannelManager.handleCall(m, decodeEvent);
                }
                break;
            }
            case SYSTEM_ALL_CALL: {
                final var m = (SystemAllCallMessage) message;
                unitIds.add(m.getCallerUnitId().getValue());

                final var decodeEvent = DecodeEvent.builder(m.getTimestamp())
                    .protocol(Protocol.EDACS)
                    .eventDescription("System All-Call")
                    .details(channelStatusText(m.getChannel(), "Call"))
                    .channel(channelDescriptor(m.getChannel()))
                    .identifiers(buildIdentifiers(m.getIdentifiers()))
                    .build();

                broadcast(decodeEvent);
                broadcastContinuation();

                if (m.getChannel().isProperChannelNumber()) {
                    trafficChannelManager.handleCall(m, decodeEvent);
                }
                break;
            }
            case LOGIN_ACKNOWLEDGE: {
                final var m = (LoginAcknowledgeMessage) message;

                talkgroupIds.add(m.getGroupId().getValue());
                unitIds.add(m.getUnitId().getValue());

                broadcast(
                    DecodeEvent.builder(m.getTimestamp())
                        .protocol(Protocol.EDACS)
                        .eventDescription("Login")
                        .identifiers(buildIdentifiers(m.getIdentifiers()))
                        .build()
                );
                broadcastContinuation();
                break;
            }
            case STATUS_REQUEST_OR_ACKNOWLEDGE: {
                final var m = (StatusRequestOrAcknowledgeMessage) message;
                unitIds.add(m.getUnitId().getValue());

                if (m.isUnitStatus()) {
                    broadcast(
                        DecodeEvent.builder(m.getTimestamp())
                            .protocol(Protocol.EDACS)
                            .eventDescription("Status")
                            .details("Unit status: " + m.getStatus())
                            .identifiers(buildIdentifiers(m.getIdentifiers()))
                            .build()
                    );
                }
                broadcastContinuation();
                break;
            }
            case CANCEL_DYNAMIC_REGROUP: {
                final var m = (CancelDynamicRegroupMessage) message;
                unitIds.add(m.getUnitId().getValue());
                broadcastContinuation();
                break;
            }
            case ASSIGNMENT_TO_AUXILIARY_CONTROL_CHANNEL: {
                // TODO
                log.trace("Received assignment to auxiliary CC: {}", message);
                final var m = (AssignmentToAuxiliaryControlChannelMessage) message;
                talkgroupIds.add(m.getGroupId().getValue());
                broadcastContinuation();
                break;
            }
            case CHANNEL_CONFIGURATION: {
                log.trace("Received channel configuration: {}", message);
                broadcastContinuation();
                break;
            }
            case INITIATE_TEST_CALL: {
                log.trace("Received test call initiation: {}", message);
                broadcastContinuation();
                break;
            }
            case PATCH: {
                // TODO
                log.trace("Received patch: {}", message);
                final var m = (PatchMessage) message;
                talkgroupIds.add(m.getGroupId().getValue());
                patches.put(m.getPatchId().getValue(), m.getGroupId().getValue());
                broadcastContinuation();
                break;
            }
            case UNKEY_OR_CHANNEL_DROP: {
                // TODO
                log.trace("Received unkey or channel drop: {}", message);
                final var m = (UnkeyOrDropChannelMessage) message;
                unitIds.add(m.getUnitId().getValue());
                broadcastContinuation();
                break;
            }
            case ADJACENT_SITE_INFORMATION: {
                log.trace("Received adjacent site information: {}", message);
                broadcastContinuation();
                break;
            }
            case DYNAMIC_REGROUP: {
                log.trace("Received dynamic regroup: {}", message);
                final var m = (DynamicRegroupMessage) message;
                talkgroupIds.add(m.getGroupId().getValue());
                unitIds.add(m.getUnitId().getValue());
                broadcastContinuation();
                break;
            }
        }
        previousMessage = (EDACSMessage) message;

        if (traceState == 1) {
            log.info("between test calls: {}", message);
        }
    }

    private boolean isRepeatedMessage(EDACSMessage m1, EDACSMessage m2) {
        if (m1 != null && m1.getMessageType() == m2.getMessageType()) {
            switch (m1.getMessageType()) {
                // Login acknowledge is sent in two consecutive outbound slots
                case LOGIN_ACKNOWLEDGE: {
                    final var p1 = (LoginAcknowledgeMessage) m1;
                    final var p2 = (LoginAcknowledgeMessage) m2;
                    return Objects.equals(p1.getGroupId().getValue(), p2.getGroupId().getValue())
                        && Objects.equals(p1.getUnitId().getValue(), p2.getUnitId().getValue());
                }
                // Test call acknowledge is sent in two consecutive outbound slots
                case INDIVIDUAL_CALL_CHANNEL_ASSIGNMENT: {
                    final var p1 = (IndividualCallChannelAssignmentMessage) m1;
                    final var p2 = (IndividualCallChannelAssignmentMessage) m2;
                    return p1.isTestCall() && p2.isTestCall()
                        && p1.getChannel().getNumber() == p2.getChannel().getNumber();
                }
                // Channel assignment messages are sent in two consecutive outbound slots
                // but they are handled by the Traffic Channel Manager
            }
        }
        return false;
    }

    private void broadcastContinuation() {
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
    }

    private String channelStatusText(ChannelNumber channel, String subject) {
        if (channel.isProperChannelNumber()) {
            return "Channel granted";
        } else {
            switch (channel.getStatus()) {
                // @formatter:off
                case CONVERT_TO_CALLEE: return "Convert to callee";
                case CALL_QUEUED: return subject + " queued";
                case SYSTEM_BUSY: return "System busy";
                case CALL_DENIED: return subject + " denied";
                default: return "Unknown status: " + channel.getNumber();
                // @formatter:on
            }
        }
    }

    private EDACSChannelDescriptor channelDescriptor(ChannelNumber channel) {
        if (channel.isProperChannelNumber()) {
            return new EDACSChannelDescriptor(channel.getNumber(), channelMap);
        } else {
            return null;
        }
    }

    private IdentifierCollection buildIdentifiers(Collection<Identifier> messageIdentifiers) {
        final var collection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        collection.remove(IdentifierClass.USER);
        collection.update(messageIdentifiers);
        return collection;
    }

    @Override
    public String getActivitySummary() {
        final var builder = new StringBuilder(500);

        builder.append("Activity Summary - Decoder: EDACS\n");
        builder.append(DIVIDER1);
        builder.append("Site: ");

        if (siteId != null) {
            builder.append(siteId).append("\n");
        } else {
            builder.append("Unknown\n");
        }

        builder.append(DIVIDER2)
            .append("Talkgroups: ");

        if (talkgroupIds.isEmpty()) {
            builder.append("None\n");
        } else {
            builder.append("\n");
            for (final var talkgroupId : talkgroupIds) {
                builder.append(talkgroupId).append("\n");
            }
        }

        builder.append(DIVIDER2)
            .append("Patches: ");

        if (patches.isEmpty()) {
            builder.append("None\n");
        } else {
            builder.append("\n");
            for (final var patchId : patches.keySet()) {
                builder.append(patchId).append(" -> ")
                    .append(Joiner.on(", ").join(patches.get(patchId))).append("\n");
            }
        }

        builder.append(DIVIDER2)
            .append("Units: ");

        if (unitIds.isEmpty()) {
            builder.append("None\n");
        } else {
            builder.append("\n");
            for (final var unitId : unitIds) {
                builder.append(unitId).append("\n");
            }
        }

        return builder.toString();
    }
}
