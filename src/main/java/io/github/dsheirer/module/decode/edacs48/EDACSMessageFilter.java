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

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.edacs48.message.EDACSMessage;
import io.github.dsheirer.module.decode.edacs48.message.EDACSMessage.EDACSMessageType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Filter} allowing filtering by {@link EDACSMessageType EDACS message type}.
 */
public class EDACSMessageFilter extends Filter<IMessage> {

    private final Map<EDACSMessageType, FilterElement<EDACSMessageType>> filterMap =
        new EnumMap<>(EDACSMessageType.class);

    public EDACSMessageFilter() {
        super("EDACS Message Type Filter");
        for (final var type : EDACSMessageType.values()) {
            filterMap.put(type, new MessageTypeFilterElement(type));
        }
    }

    @Override
    public List<FilterElement<?>> getFilterElements() {
        return new ArrayList<>(filterMap.values());
    }

    @Override
    public boolean canProcess(IMessage message) {
        return message instanceof EDACSMessage;
    }

    @Override
    public boolean passes(IMessage message) {
        if (mEnabled && canProcess(message)) {
            final var messageType = ((EDACSMessage) message).getMessageType();
            return filterMap.get(messageType).isEnabled();
        }
        return false;
    }

    /**
     * {@link FilterElement Filter element} containing {@link EDACSMessageType EDACS message type}
     * as the actual element.
     */
    private static class MessageTypeFilterElement extends FilterElement<EDACSMessageType> {

        MessageTypeFilterElement(EDACSMessageType messageType) {
            super(messageType);
        }

        @Override
        public String getName() {
            return getElement().getDescription();
        }
    }
}
