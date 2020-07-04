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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.edacs48.message.EDACSMessage.EDACSMessageType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import static io.github.dsheirer.module.decode.edacs48.message.EDACSMessage.Fragment.firstFragment;
import static io.github.dsheirer.module.decode.edacs48.message.EDACSMessage.Fragment.secondFragment;
import static io.github.dsheirer.module.decode.edacs48.message.EDACSMessage.binary;

public class EDACSMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(EDACSMessageDecoder.class);

    // TODO: Introduce BitRange instead of bit arrays?
    private static final int[] MTA_BITS = {0, 1, 2};
    private static final int[] MTB_BITS = {3, 4, 5};
    private static final int[] MTD_BITS = {6, 7, 8, 9, 10};

    // These are only used by extended site options
    private static final int[] MTE_BITS = {11};
    private static final int[] MESSAGE_NO = {12, 13, 14};

    /**
     * Set of message types that consist of two in-frame binary messages.
     */
    private static final EnumSet<EDACSMessageType> WIDE_TYPES = EnumSet.of(
        EDACSMessageType.VOICE_CHANNEL_ASSIGNMENT,
        EDACSMessageType.DATA_CHANNEL_ASSIGNMENT,
        EDACSMessageType.INDIVIDUAL_CALL_CHANNEL_ASSIGNMENT,
        EDACSMessageType.SYSTEM_ALL_CALL,
        EDACSMessageType.DYNAMIC_REGROUP
    );

    private Listener<IMessage> messageListener;

    public void setMessageListener(Listener<IMessage> messageListener) {
        this.messageListener = messageListener;
    }

    public void removeMessageListener() {
        this.messageListener = null;
    }

    private void sendOut(EDACSMessage message) {
        if (messageListener != null) {
            messageListener.receive(message);
        }
    }

    public void decode(CorrectedBinaryMessage m1, CorrectedBinaryMessage m2) {
        final var type1 = extractType(m1);
        final var type2 = extractType(m2);

        if (isWideType(type1) && isWideType(type2)) {
            if (type1 == type2) {
                sendOut(decodeWideMessage(type1, m1, m2));
            } else {
                log.trace("Messages m1 {} and m2 {} have different wide types {} and {}",
                    m1, m2, type1, type2);
                sendOut(firstFragment(type1, m1));
                sendOut(secondFragment(type2, m2));
            }
        } else if (isWideType(type1)) {
            log.trace("Message {} has wide type {} but {} has narrow type {}",
                m1, type1, m2, type2);
            sendOut(firstFragment(type1, m1));
            sendOut(decodeNarrowMessage(type2, m2));
        } else if (isWideType(type2)) {
            log.trace("Message {} has narrow type {} but {} has wide type {}",
                m1, type1, m2, type2);
            sendOut(decodeNarrowMessage(type1, m1));
            sendOut(secondFragment(type2, m2));
        } else {
            sendOut(decodeNarrowMessage(type1, m1));
            sendOut(decodeNarrowMessage(type2, m2));
        }
    }

    private EDACSMessageType extractType(CorrectedBinaryMessage m) {
        final var mta = m.getInt(MTA_BITS);
        switch (mta) {
            // @formatter:off
            case 0b000:
            case 0b001:
            case 0b010:
            case 0b011: return EDACSMessageType.VOICE_CHANNEL_ASSIGNMENT;
            case 0b101: return EDACSMessageType.DATA_CHANNEL_ASSIGNMENT;
            case 0b110: return EDACSMessageType.LOGIN_ACKNOWLEDGE;
            // @formatter:on
            case 0b111: {
                final var mtb = m.getInt(MTB_BITS);
                switch (mtb) {
                    // @formatter:off
                    case 0b000: return EDACSMessageType.STATUS_REQUEST_OR_ACKNOWLEDGE;
                    case 0b001: return EDACSMessageType.INTERCONNECT_CHANNEL_ASSIGNMENT;
                    case 0b011: return EDACSMessageType.CHANNEL_UPDATE;
                    case 0b100: return EDACSMessageType.PATCH;
                    case 0b101: return EDACSMessageType.INDIVIDUAL_CALL_CHANNEL_ASSIGNMENT;
                    case 0b110: return EDACSMessageType.UNKEY_OR_CHANNEL_DROP;
                    // @formatter:on
                    case 0b111: {
                        final var mtd = m.getInt(MTD_BITS);
                        switch (mtd) {
                            // @formatter:off
                            case 0b00000: return EDACSMessageType.CANCEL_DYNAMIC_REGROUP;
                            case 0b00001: return EDACSMessageType.ADJACENT_SITE_INFORMATION;
                            case 0b00010: return getExtendedSiteOptionsSubtype(m);
                            case 0b00011: return EDACSMessageType.CHANNEL_CONFIGURATION;
                            case 0b00100: return EDACSMessageType.SYSTEM_DYNAMIC_REGROUP_PLAN_BITMAP;
                            case 0b00101: return EDACSMessageType.ASSIGNMENT_TO_AUXILIARY_CONTROL_CHANNEL;
                            case 0b00110: return EDACSMessageType.INITIATE_TEST_CALL;
                            case 0b00111: return EDACSMessageType.UNIT_ENABLE_OR_DISABLE;
                            case 0b01000:
                            case 0b01001:
                            case 0b01010:
                            case 0b01011: return EDACSMessageType.SITE_ID;
                            case 0b01111: return EDACSMessageType.SYSTEM_ALL_CALL;
                            case 0b10000: return EDACSMessageType.DYNAMIC_REGROUP;
                            // @formatter:on
                            default:
                                // All other values are reserved.
                                log.trace("Message {} has unknown type mta={} mtb={} mtd={}",
                                    m, binary(mta, 3), binary(mtb, 3), binary(mtd, 5));
                                return EDACSMessageType.UNKNOWN;
                        }
                    }
                    default:
                        // All other values are reserved.
                        log.trace("Message {} has unknown type mta={} mtb={}",
                            m, binary(mta, 3), binary(mtb, 3));
                        return EDACSMessageType.UNKNOWN;
                }
            }
            default:
                // All other values are reserved.
                log.trace("Message {} has unknown type mta={}",
                    m, binary(mta, 3));
                return EDACSMessageType.UNKNOWN;
        }
    }

    private EDACSMessageType getExtendedSiteOptionsSubtype(CorrectedBinaryMessage m) {
        final var mte = m.getInt(MTE_BITS);
        if (mte != 0) {
            log.trace("Extended site options message {} has unknown subtype mte={}",
                m, binary(mte, 1));
            return EDACSMessageType.UNKNOWN;
        }

        final var messageNo = m.getInt(MESSAGE_NO);
        if (messageNo != 0b000) {
            log.trace("Extended site options message {} has unknown message#={}",
                m, binary(messageNo, 3));
            return EDACSMessageType.UNKNOWN;
        }

        return EDACSMessageType.EXTENDED_SITE_ID;
    }

    private boolean isWideType(EDACSMessageType type) {
        return WIDE_TYPES.contains(type);
    }

    private EDACSMessage decodeWideMessage(EDACSMessageType type, CorrectedBinaryMessage m1,
        CorrectedBinaryMessage m2) {

        switch (type) {
            // @formatter:off
            case VOICE_CHANNEL_ASSIGNMENT: return new VoiceChannelAssignmentMessage(m1, m2);
            case DATA_CHANNEL_ASSIGNMENT: return new DataChannelAssignmentMessage(m1, m2);
            case INDIVIDUAL_CALL_CHANNEL_ASSIGNMENT: return new IndividualCallChannelAssignmentMessage(m1, m2);
            case SYSTEM_ALL_CALL: return new SystemAllCallMessage(m1, m2);
            case DYNAMIC_REGROUP: return new DynamicRegroupMessage(m1, m2);
            // @formatter:on
        }
        log.trace("Unhandled {} wide message {}, {}", type, m1, m2);
        return new EDACSMessage.Unknown(m1);
    }

    private EDACSMessage decodeNarrowMessage(EDACSMessageType type, CorrectedBinaryMessage m) {
        switch (type) {
            // @formatter:off
            case LOGIN_ACKNOWLEDGE: return new LoginAcknowledgeMessage(m);
            case STATUS_REQUEST_OR_ACKNOWLEDGE: return new StatusRequestOrAcknowledgeMessage(m);
            case INTERCONNECT_CHANNEL_ASSIGNMENT: return new InterconnectChannelAssignmentMessage(m);
            case CHANNEL_UPDATE: return new ChannelUpdateMessage(m);
            case PATCH: return new PatchMessage(m);
            case UNKEY_OR_CHANNEL_DROP: return new UnkeyOrDropChannelMessage(m);
            case CANCEL_DYNAMIC_REGROUP: return new CancelDynamicRegroupMessage(m);
            case ADJACENT_SITE_INFORMATION: return new AdjacentSiteInformationMessage(m);
            case EXTENDED_SITE_ID: return new ExtendedSiteIdMessage(m);
            case CHANNEL_CONFIGURATION: return new ChannelConfigurationMessage(m);
            case SYSTEM_DYNAMIC_REGROUP_PLAN_BITMAP: return new SystemDynamicRegroupPlanBitmapMessage(m);
            case ASSIGNMENT_TO_AUXILIARY_CONTROL_CHANNEL: return new AssignmentToAuxiliaryControlChannelMessage(m);
            case INITIATE_TEST_CALL: return new InitiateTestCallMessage(m);
            case UNIT_ENABLE_OR_DISABLE: return new UnitEnableOrDisableMessage(m);
            case SITE_ID: return new SiteIdMessage(m);
            // @formatter:on
        }
        log.trace("Unhandled {} narrow message {}", type, m);
        return new EDACSMessage.Unknown(m);
    }
}
