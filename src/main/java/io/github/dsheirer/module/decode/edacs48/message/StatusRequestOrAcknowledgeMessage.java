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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StatusRequestOrAcknowledgeMessage extends EDACSMessage.Narrow {

    private static final Logger log = LoggerFactory.getLogger(StatusRequestOrAcknowledgeMessage.class);
    private static final int[] STATUS_BITS = {6, 7, 8, 9, 10, 11, 12, 13};
    private static final int[] LOGICAL_ID_BITS = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};

    enum ControllerStatus {
        STATUS_REQUEST,
    }

    private final UnitId unitId;
    private final int status;
    private final boolean isUnitStatus;
    private final ControllerStatus controllerStatus;

    StatusRequestOrAcknowledgeMessage(CorrectedBinaryMessage m) {
        super(EDACSMessageType.STATUS_REQUEST_OR_ACKNOWLEDGE, m);
        status = m.getInt(STATUS_BITS);
        isUnitStatus = status >= 1 && status <= 247;

        if (isUnitStatus) {
            unitId = UnitId.from(m.getInt(LOGICAL_ID_BITS));
            controllerStatus = null;
        } else {
            unitId = UnitId.to(m.getInt(LOGICAL_ID_BITS));
            controllerStatus = decodeControllerStatus(status);
        }
    }

    private static ControllerStatus decodeControllerStatus(int status) {
        if (status == 248) return ControllerStatus.STATUS_REQUEST;
        // Values 249-255 are reserved
        return null;
    }

    public UnitId getUnitId() {
        return unitId;
    }

    public int getStatus() {
        return status;
    }

    public boolean isUnitStatus() {
        return isUnitStatus;
    }

    public ControllerStatus getControllerStatus() {
        return controllerStatus;
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
            .add("status", status)
            .add("isUnitStatus", isUnitStatus)
            .add("controllerStatus", controllerStatus)
            .toString();
    }
}
