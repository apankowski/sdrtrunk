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
package io.github.dsheirer.edac;

/**
 * Berlekamp-Massey decoder for primitive 63-bit RS/BCH block codes using
 * primitive polynomial <code>x<sup>6</sup> + x + 1</code> that is typically
 * used as the generator of Galois Field 2<sup>6</sup>.
 */
class BerlekampMassey_63 extends BerlekampMassey {

    protected static final int CODEWORD_LENGTH = 63;

    BerlekampMassey_63(int tt) {
        super(6, tt, 0b1000011);
    }
}
