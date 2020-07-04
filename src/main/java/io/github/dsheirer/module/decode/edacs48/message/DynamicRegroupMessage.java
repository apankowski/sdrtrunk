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

public class DynamicRegroupMessage extends EDACSMessage.Wide {

    private static final int[] FLEET_BITS_BITS = {11, 12, 13};
    private static final int[] LOGICAL_ID_BITS = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    private static final int[] PLAN_NUMBER_BITS = {7, 8, 9, 10};
    private static final int[] REGROUP_TYPE_BITS = {11, 12};
    private static final int[] KNOB_SETTING_BITS = {13, 14, 15};
    private static final int[] GROUP_ID_BITS = {17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    enum RegroupType {
        FORCED_SELECT_NO_DESELECT,
        FORCED_SELECT_OPTIONAL_DESELECT,
        RESERVED,
        OPTIONAL_SELECT
    }

    private final GroupId groupId;
    private final UnitId unitId;
    private final int planNumber;
    private final RegroupType regroupType;
    private final int knobSetting;
    private final int fleetBits;

    DynamicRegroupMessage(CorrectedBinaryMessage m1, CorrectedBinaryMessage m2) {
        super(EDACSMessageType.DYNAMIC_REGROUP, m1, m2);
        fleetBits = m1.getInt(FLEET_BITS_BITS);
        unitId = UnitId.to(m1.getInt(LOGICAL_ID_BITS));

        planNumber = m2.getInt(PLAN_NUMBER_BITS);
        regroupType = decodeRegroupType(m2.getInt(REGROUP_TYPE_BITS));
        knobSetting = m2.getInt(KNOB_SETTING_BITS) + 1;
        reserved(m2, 16, 16);
        groupId = GroupId.to(m2.getInt(GROUP_ID_BITS));
    }

    private static RegroupType decodeRegroupType(int bits) {
        switch (bits) {
            // @formatter:off
            case 0b00: return RegroupType.FORCED_SELECT_NO_DESELECT;
            case 0b01: return RegroupType.FORCED_SELECT_OPTIONAL_DESELECT;
            case 0b10: return RegroupType.RESERVED;
            case 0b11: return RegroupType.OPTIONAL_SELECT;
            // @formatter:on
        }
        throw new IllegalArgumentException("Illegal regroup type bits " + binary(bits, 2));
    }

    public GroupId getGroupId() {
        return groupId;
    }

    // TODO: What's the meaning of unit ID for this message?
    public UnitId getUnitId() {
        return unitId;
    }

    @Override
    protected List<Identifier> gatherIdentifiers() {
        return List.of(groupId, unitId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", getMessageType())
            .add("groupId", groupId)
            .add("unitId", unitId)
            .add("planNumber", planNumber)
            .add("regroupType", regroupType)
            .add("knobSetting", knobSetting)
            .add("fleetBits", fleetBits)
            .toString();
    }
}
