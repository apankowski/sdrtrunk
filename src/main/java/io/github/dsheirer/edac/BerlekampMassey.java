/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Berlekamp-Massey decoder for primitive RS/BCH block codes
 */
class BerlekampMassey
{
    private static final Logger mLog = LoggerFactory.getLogger( BerlekampMassey.class );

    /* Galois field size GF( 2 ** MM ) */
    private final int MM;

    /* Codeword Length: NN = 2 ** MM - 1 */
    private final int NN;

    /* Hamming distance between codewords: NN - KK + 1 = 2 * TT + 1 */
    private final int KK;

    /* Maximum number of errors that can be corrected */
    private final int TT;

    private final int[] alphaExp;
    private final int[] alphaLog;

    BerlekampMassey(int mm, int tt, int primitivePoly)
    {
        MM = mm;
        TT = tt;
        NN = (1 << MM) - 1;
        KK = NN - 2 * TT;

        alphaExp = new int[ NN + 1 ];
        alphaLog = new int[ NN + 1 ];

        generateGaloisField(primitivePoly);
    }

    /**
     * Generates the Galois Field.
     * <p>
     * Generates a GF( 2**mm ) from the irreducible polynomial
     * p(X) in pp[0]..pp[mm]
     * <p>
     * Lookup tables:
     * index_of[] = polynomial form
     * alpha_to[] = contains j=alpha**i;
     * <p>
     * Polynomial form -> Index form  index_of[j=alpha**i] = i
     * <p>
     * alpha_to = 2 is the primitive element of GF( 2**mm )
     *
     * @param primitivePoly
     */
    private void generateGaloisField(int primitivePoly) {
        final var largestPowerInPrimitivePoly = Integer.highestOneBit(primitivePoly);
        if (largestPowerInPrimitivePoly != 1 << MM) {
            throw new IllegalArgumentException("Generator polynomial of Galois field(2^" + MM + ") " +
                    "must be of degree " + MM);
        }

        for (var i = 0; i < NN; i++) {
            var residuePoly = i == 0 ? 1 : alphaExp[i - 1] << 1;
            if (residuePoly >= largestPowerInPrimitivePoly) residuePoly ^= primitivePoly;
            alphaExp[i] = residuePoly;
            alphaLog[residuePoly] = i;
        }

        alphaLog[0] = -1;
    }

    /**
     * Decodes
     * @param input
     * @param output
     * @return
     */
    /* assume we have received bits grouped into mm-bit symbols in recd[i],
    i=0..(nn-1),  and recd[i] is polynomial form.
    We first compute the 2*tt syndromes by substituting alpha**i into rec(X) and
    evaluating, storing the syndromes in s[i], i=1..2tt (leave s[0] zero) .
    Then we use the Berlekamp iteration to find the error location polynomial
    elp[i].   If the degree of the elp is >tt, we cannot correct all the errors
    and hence just put out the information symbols uncorrected. If the degree of
    elp is <=tt, we substitute alpha**i , i=1..n into the elp to get the roots,
    hence the inverse roots, the error location numbers. If the number of errors
    located does not equal the degree of the elp, we have more than tt errors
    and cannot correct them.  Otherwise, we then solve for the error value at
    the error location and correct the error.  The procedure is that found in
    Lin and Costello. For the cases where the number of errors is known to be too
    large to correct, the information symbols as received are output (the
    advantage of systematic encoding is that hopefully some of the information
    symbols will be okay and that if we are in luck, the errors are in the
    parity part of the transmitted codeword).  Of course, these insoluble cases
    can be returned as error flags to the calling routine if desired.   */
    public boolean decode( final int[] input, int[] output ) //input, output
    {
        int u, q;
        int[][] elp = new int[ NN - KK + 2 ][ NN - KK];
        int[] d = new int[ NN - KK + 2 ];
        int[] l = new int[ NN - KK + 2 ];
        int[] u_lu = new int[ NN - KK + 2 ];
        int[] s = new int[ NN - KK + 1 ];
        int count = 0;
        boolean syn_error = false;
        int[] root = new int[TT];
        int[] loc = new int[TT];
        int[] z = new int[ TT + 1 ];
        int[] err = new int[NN];
        int[] reg = new int[ TT + 1 ];

        boolean irrecoverable_error = false;

        /* put recd[i] into index form (ie as powers of alpha) */
        for(int i = 0; i < NN; i++ )
        {
            output[ i ] = alphaLog[ input[ i ] ];
        }

        /* first form the syndromes */
        for(int i = 1; i <= NN - KK; i++ )
        {
            s[ i ] = 0;

            for(int j = 0; j < NN; j++ )
            {
                if( output[ j ] != -1 )
                {
                    /* recd[j] in index form */
                    s[ i ] ^= alphaExp[ ( output[ j ] + i * j ) % NN];
                }
            }

            /* convert syndrome from polynomial form to index form  */
            if( s[ i ] != 0 )
            {
                /* set flag if non-zero syndrome => error */
                syn_error = true;
            }

            s[ i ] = alphaLog[ s[ i ] ];
        }

        if( syn_error ) /* if errors, try and correct */
        {
            /* compute the error location polynomial via the Berlekamp iterative algorithm,
             following the terminology of Lin and Costello :   d[u] is the 'mu'th
             discrepancy, where u='mu'+1 and 'mu' (the Greek letter!) is the step number
             ranging from -1 to 2*tt (see L&C),  l[u] is the
             degree of the elp at that step, and u_l[u] is the difference between the
             step number and the degree of the elp.
             */

            /* initialise table entries */
            d[ 0 ] = 0; /* index form */
            d[ 1 ] = s[ 1 ]; /* index form */
            elp[ 0 ][ 0 ] = 0; /* index form */
            elp[ 1 ][ 0 ] = 1; /* polynomial form */

            for(int i = 1; i < NN - KK; i++ )
            {
                elp[ 0 ][ i ] = -1; /* index form */
                elp[ 1 ][ i ] = 0; /* polynomial form */
            }

            l[ 0 ] = 0;
            l[ 1 ] = 0;
            u_lu[ 0 ] = -1;
            u_lu[ 1 ] = 0;
            u = 0;

            do
            {
                u++;

                if( d[ u ] == -1 )
                {
                    l[ u + 1 ] = l[ u ];

                    for( int i = 0; i <= l[ u ]; i++ )
                    {
                        elp[ u + 1 ][ i ] = elp[ u ][ i ];
                        elp[ u ][ i ] = alphaLog[ elp[ u ][ i ] ];
                    }
                }
                else
                    /* search for words with greatest u_lu[q] for which d[q]!=0 */
                {
                    q = u - 1;

                    while( ( d[ q ] == -1 ) && ( q > 0 ) )
                    {
                        q--;
                    }

                    /* have found first non-zero d[q]  */
                    if( q > 0 )
                    {
                        int j = q;

                        do
                        {
                            j--;

                            if( ( d[ j ] != -1 ) && ( u_lu[ q ] < u_lu[ j ] ) )
                            {
                                q = j;
                            }
                        }
                        while( j > 0 );
                    };

                    /* have now found q such that d[u]!=0 and u_lu[q] is maximum */
                    /* store degree of new elp polynomial */
                    l[u + 1] = FastMath.max(l[u], l[q] + u - q);

                    /* form new elp(x) */
                    for(int i = 0; i < NN - KK; i++ )
                    {
                        elp[ u + 1 ][ i ] = 0;
                    }

                    for( int i = 0; i <= l[q]; i++ )
                    {
                        if( elp[ q ][ i ] != -1 )
                        {
                            elp[ u + 1 ][ i + u - q ] =
                                    alphaExp[ ( d[ u ] + NN - d[ q ]
                                            + elp[ q ][ i ]) % NN];
                        }
                    }
                    for( int i = 0; i <= l[u]; i++ )
                    {
                        elp[ u + 1 ][ i ] ^= elp[ u ][ i ];
                        elp[ u ][ i ] = alphaLog[ elp[ u ][ i ] ]; /*convert old elp value to index*/
                    }
                }

                u_lu[ u + 1 ] = u - l[ u + 1 ];

                /* form (u+1)th discrepancy */
                if( u < NN - KK) /* no discrepancy computed on last iteration */
                {
                    if ( s[ u + 1 ] != -1 )
                    {
                        d[ u + 1 ] = alphaExp[ s[ u + 1 ] ];
                    }
                    else
                    {
                        d[ u + 1 ] = 0;
                    }
                    for( int i = 1; i <= l[ u + 1 ]; i++ )
                    {
                        if( ( s[ u + 1 - i ] != -1 ) && ( elp[ u + 1 ][ i]  != 0 ) )
                        {
                            d[ u + 1 ] ^= alphaExp[ ( s[ u + 1 - i ]
                                    + alphaLog[ elp[ u + 1 ][ i ] ] ) % NN];
                        }
                    }

                    d[ u + 1 ] = alphaLog[ d[ u + 1 ] ]; /* put d[u+1] into index form */
                }
            }
            while( ( u < NN - KK) && ( l[ u + 1 ] <= TT) );

            u++;

            if( l[ u ] <= TT) /* can correct error */
            {
                /* put elp into index form */
                for( int i = 0; i <= l[u]; i++ )
                {
                    elp[ u ][ i ] = alphaLog[ elp[ u ][ i ] ];
                }

                /* find roots of the error location polynomial */
                if (l[u] >= 0) System.arraycopy(elp[u], 1, reg, 1, l[u]);

                count = 0;

                for(int i = 1; i <= NN; i++ )
                {
                    q = 1;

                    for( int j = 1; j <= l[u]; j++ )
                    {
                        if( reg[ j ] != -1 )
                        {
                            reg[ j ] = ( reg[ j ] + j ) % NN;
                            q ^= alphaExp[ reg[ j ] ];
                        };
                    }

                    if( q == 0 ) /* store root and error location number indices */
                    {
                        root[ count ] = i;
                        loc[ count ] = NN - i;
                        count++;
                    };
                };

                if( count == l[ u ] ) /* no. roots = degree of elp hence <= tt errors */
                {
                    /* form polynomial z(x) */
                    for( int i = 1; i <= l[ u ]; i++ ) /* Z[0] = 1 always - do not need */
                    {
                        if( ( s[ i ] != -1 ) && ( elp[ u ][ i ] != -1 ) )
                        {
                            z[ i ] = alphaExp[ s[ i ] ] ^ alphaExp[ elp[ u ][ i ] ];
                        }
                        else if( ( s[ i ] != -1 ) && ( elp[ u ][ i ] == -1 ) )
                        {
                            z[ i ] = alphaExp[ s[ i ] ];
                        }
                        else if( ( s[ i ] == -1 ) && ( elp[ u ][ i ] != -1 ) )
                        {
                            z[ i ] = alphaExp[ elp[ u ][ i ] ];
                        }
                        else
                        {
                            z[ i ] = 0;
                        }

                        for( int j = 1; j < i; j++ )
                        {
                            if( ( s[ j ] != -1 ) && ( elp[ u ][ i - j ] != -1 ) )
                            {
                                z[ i ] ^= alphaExp[ ( elp[ u ][ i - j ] + s[ j ] ) % NN];
                            }
                        }

                        z[ i ] = alphaLog[ z[ i ] ]; /* put into index form */
                    };

                    /* evaluate errors at locations given by error location numbers loc[i] */
                    for(int i = 0; i < NN; i++ )
                    {
                        err[ i ] = 0;

                        if( output[ i ] != -1 ) /* convert recd[] to polynomial form */
                        {
                            output[ i ] = alphaExp[ output[ i ] ];
                        }
                        else
                        {
                            output[ i ] = 0;
                        }
                    }

                    for( int i = 0; i < l[ u ]; i++ ) /* compute numerator of error term first */
                    {
                        err[ loc[ i ] ] = 1; /* accounts for z[0] */

                        for( int j = 1; j <= l[ u ]; j++ )
                        {
                            if( z[ j ] != -1 )
                            {
                                err[ loc[ i ] ] ^= alphaExp[ ( z[ j ] + j * root[ i ] ) % NN];
                            }
                        }

                        if( err[ loc[ i ] ] != 0 )
                        {
                            err[ loc[ i ] ] = alphaLog[ err[ loc[ i ] ] ];

                            q = 0; /* form denominator of error term */

                            for (int j = 0; j < l[u]; j++)
                            {
                                if (j != i)
                                {
                                    q += alphaLog[1 ^ alphaExp[(loc[j] + root[i]) % NN]];
                                }
                            }

                            q = q % NN;
                            err[loc[i]] = alphaExp[(err[loc[i]] - q + NN) % NN];
                            output[loc[i]] ^= err[loc[i]]; /*recd[i] must be in polynomial form */
                        }
                    }
                }
                else
                {
                    /* no. roots != degree of elp => >tt errors and cannot solve */
                    irrecoverable_error = true;
                }

            }
            else
            {
                /* elp has degree >tt hence cannot solve */
                irrecoverable_error = true;
            }
        }
        else
        {
            /* no non-zero syndromes => no errors: output received codeword */
            for (int i = 0; i < NN; i++)
            {
                if (output[i] != -1) /* convert recd[] to polynomial form */
                {
                    output[i] = alphaExp[output[i]];
                }
                else
                {
                    output[i] = 0;
                }
            }
        }

        if( irrecoverable_error )
        {
            for (int i = 0; i < NN; i++) /* could return error flag if desired */
            {
                if (output[i] != -1) /* convert recd[] to polynomial form */
                {
                    output[i] = alphaExp[output[i]];
                }
                else
                {
                    output[i] = 0; /* just output received codeword as is */
                }
            }
        }

        return irrecoverable_error;
    }
}
