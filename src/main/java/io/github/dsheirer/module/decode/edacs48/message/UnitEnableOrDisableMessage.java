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

class UnitEnableOrDisableMessage extends EDACSMessage.Narrow {

    private static final int[] UNIT_QUALIFIER_BITS = {12, 13};
    private static final int[] LOGICAL_ID_BITS = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    enum UnitQualifier {
        STOLEN_TEMPORARY_DISABLE,
        STOLEN_CORRUPT_PERSONALITY,
        REVOKE_LOGICAL_ID,
        REENABLE,
    }

    private final UnitQualifier unitQualifier;
    private final UnitId unitId;

    UnitEnableOrDisableMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.UNIT_ENABLE_OR_DISABLE, m);
        reserved(m, 11, 11);
        unitQualifier = decodeUnitQualifier(m.getInt(UNIT_QUALIFIER_BITS));
        unitId = UnitId.to(m.getInt(LOGICAL_ID_BITS));
    }

    private static UnitQualifier decodeUnitQualifier(int bits) {
        switch (bits) {
            // @formatter:off
            case 0b00: return UnitQualifier.STOLEN_TEMPORARY_DISABLE;
            case 0b01: return UnitQualifier.STOLEN_CORRUPT_PERSONALITY;
            case 0b10: return UnitQualifier.REVOKE_LOGICAL_ID;
            case 0b11: return UnitQualifier.REENABLE;
            // @formatter:on
        }
        throw new IllegalArgumentException("Illegal unit qualifier bits " + binary(bits, 2));
    }

    @Override
    protected List<Identifier> gatherIdentifiers() {
        return List.of(unitId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", getMessageType())
            .add("unitId", unitId)
            .add("unitQualifier", unitQualifier)
            .toString();
    }
}
