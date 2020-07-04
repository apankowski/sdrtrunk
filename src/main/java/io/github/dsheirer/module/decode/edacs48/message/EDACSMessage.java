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
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public abstract class EDACSMessage extends Message {

    private static final Logger log = LoggerFactory.getLogger(EDACSMessage.class);

    public enum EDACSMessageType {
        UNKNOWN("Unknown"),
        VOICE_CHANNEL_ASSIGNMENT("Group Voice Channel Assignment"),
        DATA_CHANNEL_ASSIGNMENT("Data Call Channel Assignment"),
        LOGIN_ACKNOWLEDGE("Login Acknowledge"),
        STATUS_REQUEST_OR_ACKNOWLEDGE("Status Request / Message Acknowledge"),
        // AKA Special Call Channel Assignment
        INTERCONNECT_CHANNEL_ASSIGNMENT("Interconnect Channel Assignment"),
        CHANNEL_UPDATE("Channel Update"),
        PATCH("System Assigned ID"),
        INDIVIDUAL_CALL_CHANNEL_ASSIGNMENT("Individual Call Channel Assignment"),
        UNKEY_OR_CHANNEL_DROP("Console Unkey / Channel Drop"),
        CANCEL_DYNAMIC_REGROUP("Cancel Dynamic Regroup"),
        ADJACENT_SITE_INFORMATION("Adjacent Site Control Channel"),
        EXTENDED_SITE_ID("Extended Site ID"),
        // I couldn't find anywhere structure of below message type
        CHANNEL_CONFIGURATION("Intrasite Channel / Frequency Notification"),
        SYSTEM_DYNAMIC_REGROUP_PLAN_BITMAP("System Dynamic Regroup Plan Bitmap"),
        ASSIGNMENT_TO_AUXILIARY_CONTROL_CHANNEL("Assignment To Auxiliary Control Channel"),
        INITIATE_TEST_CALL("Initiate Test Call Command"),
        UNIT_ENABLE_OR_DISABLE("Unit Enable / Disable"),
        SITE_ID("Site ID"),
        SYSTEM_ALL_CALL("System All-Call"),
        DYNAMIC_REGROUP("Dynamic Regroup"),
        ;

        private final String description;

        EDACSMessageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    protected final EDACSMessageType messageType;
    private List<Identifier> identifiers;

    EDACSMessage(EDACSMessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.EDACS;
    }

    @Override
    public String toString() {
        return messageType.name();
    }

    public EDACSMessageType getMessageType() {
        return messageType;
    }

    @Override
    public List<Identifier> getIdentifiers() {
        if (identifiers == null) {
            identifiers = gatherIdentifiers();
        }
        return identifiers;
    }

    protected List<Identifier> gatherIdentifiers() {
        return Collections.emptyList();
    }

    protected static String binary(int bits, int n) {
        return StringUtils.leftPad(Integer.toBinaryString(bits), n, '0');
    }

    protected static String hexadecimal(int value, int n) {
        return StringUtils.leftPad(Integer.toHexString(value).toUpperCase(), n, '0');
    }

    protected static void reserved(BinaryMessage message, int start, int end) {
        final var bits = message.getInt(start, end);
        if (bits != 0) {
            log.trace("Bits {}..{} of message {} are reserved and should all be zero, but were {}",
                start, end, message, binary(bits, end - start + 1));
        }
    }

    static class Narrow extends EDACSMessage {

        protected final CorrectedBinaryMessage message;

        Narrow(EDACSMessageType messageType, CorrectedBinaryMessage message) {
            super(messageType);
            this.message = message;
        }

        @Override
        public boolean isValid() {
            return message.getCRC().passes();
        }
    }

    final static class Unknown extends Narrow {

        Unknown(CorrectedBinaryMessage message) {
            super(EDACSMessageType.UNKNOWN, message);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("type", messageType)
                .add("message", message)
                .toString();
        }
    }

    static class Wide extends EDACSMessage {

        protected final CorrectedBinaryMessage m1;
        protected final CorrectedBinaryMessage m2;

        Wide(EDACSMessageType messageType, CorrectedBinaryMessage m1, CorrectedBinaryMessage m2) {
            super(messageType);
            this.m1 = m1;
            this.m2 = m2;
        }

        @Override
        public boolean isValid() {
            return m1.getCRC().passes() && m2.getCRC().passes();
        }
    }

    final static class Fragment extends Wide {

        private Fragment(EDACSMessageType messageType, CorrectedBinaryMessage m1, CorrectedBinaryMessage m2) {
            super(messageType, m1, m2);
            if ((m1 == null) == (m2 == null)) {
                throw new IllegalArgumentException("Exactly one of m1 and m2 must be null");
            }
        }

        static Fragment firstFragment(EDACSMessageType messageType, CorrectedBinaryMessage m1) {
            return new Fragment(messageType, m1, null);
        }

        static Fragment secondFragment(EDACSMessageType messageType, CorrectedBinaryMessage m2) {
            return new Fragment(messageType, null, m2);
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("type", messageType)
                .add("m1", m1)
                .add("m2", m2)
                .toString();
        }
    }
}
