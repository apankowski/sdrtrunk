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

public class CancelDynamicRegroupMessage extends EDACSMessage.Narrow {

    private static final int[] KNOB_SETTING_BITS = {11, 12, 13};
    private static final int[] LOGICAL_ID_BITS = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    private final UnitId unitId;
    private final int knobSetting;

    CancelDynamicRegroupMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.CANCEL_DYNAMIC_REGROUP, m);
        unitId = UnitId.to(m.getInt(LOGICAL_ID_BITS));
        knobSetting = m.getInt(KNOB_SETTING_BITS) + 1;
    }

    public UnitId getUnitId() {
        return unitId;
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
            .add("knobSetting", knobSetting)
            .toString();
    }
}
