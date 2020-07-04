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

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Identifier of EDACS radio unit. Value of zero is reserved and has
 * context-dependent meaning, e.g. confirmation of a test call request.
 */
public class UnitId extends RadioIdentifier {

    private UnitId(Integer value, Role role) {
        super(value, role);
    }

    static UnitId from(int id) {
        return new UnitId(id, Role.FROM);
    }

    static UnitId to(int id) {
        return new UnitId(id, Role.TO);
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.EDACS;
    }

    @Override
    public String toString() {
        return "u" + super.toString();
    }

    boolean isZero() {
        return getValue() == 0;
    }
}
