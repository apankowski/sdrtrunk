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

class SystemDynamicRegroupPlanBitmapMessage extends EDACSMessage.Narrow {

    private static final int[] BANK_BITS = {11};
    private static final int[] RESIDENT_BITMAP_BITS = {12, 13, 14, 15, 16, 17, 18, 19};
    private static final int[] ACTIVE_BITMAP_BITS = {20, 21, 22, 23, 24, 25, 26, 27};

    private final int bank;
    private final int residentBitmap;
    private final int activeBitmap;

    SystemDynamicRegroupPlanBitmapMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.SYSTEM_DYNAMIC_REGROUP_PLAN_BITMAP, m);
        bank = m.getInt(BANK_BITS);
        residentBitmap = m.getInt(RESIDENT_BITMAP_BITS);
        activeBitmap = m.getInt(ACTIVE_BITMAP_BITS);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", getMessageType())
            .add("bank", bank)
            .add("residentBitmap", Integer.toBinaryString(residentBitmap))
            .add("activeBitmap", Integer.toBinaryString(activeBitmap))
            .toString();
    }
}
