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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PolarityAutoSelectorTest {

    final PolarityAutoSelector polarityAutoSelector = new PolarityAutoSelector();
    final List<CorrectedBinaryMessage> capturedFrames = new ArrayList<>(20);

    @BeforeEach
    void beforeEach() {
        polarityAutoSelector.reset();
        capturedFrames.clear();
        polarityAutoSelector.setFrameListener(capturedFrames::add);
    }

    void test() {
        // TODO
        //polarityAutoSelector.process();
    }
}
