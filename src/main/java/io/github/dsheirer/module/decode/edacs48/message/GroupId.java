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
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Identifier of EDACS talkgroup.
 */
public class GroupId extends TalkgroupIdentifier {

    GroupId(Integer value, Role role) {
        super(value, role);
    }

    static GroupId to(int id) {
        return new GroupId(id, Role.TO);
    }

    static GroupId patch(int id) {
        return new GroupId(id, Role.BROADCAST);
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.EDACS;
    }

    @Override
    public String toString() {
        return "g" + super.toString();
    }
}
