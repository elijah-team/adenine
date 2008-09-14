/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */

/*
 *  Title: 	random_number
 *  Last Mod: 	Fri Mar 18 08:52:13 1988
 *  Author: 	Vincent Broman
 *		<broman@schroeder.nosc.mil>
 */

#include "Random_Number.h"

#define P        179
#define PM1  (P -  1)
#define Q    (P - 10)

#define STATE_SIZE     97
#define MANTISSA_SIZE  24

#define RANDOM_REALS  16777216.0
#define INIT_C          362436.0
#define INIT_CD        7654321.0
#define INIT_CM       16777213.0

static  unsigned int  ni;
static  unsigned int  nj;
static  double        u[STATE_SIZE];
static  double        c, cd, cm;

static unsigned int collapse(int anyint, unsigned int size);

static unsigned int collapse(
  int anyint,
  unsigned int size
){
/*
 * return a value between 0 and size-1 inclusive.
 * this value will be anyint itself if possible, 
 * otherwise another value in the required interval.
 */

    if (anyint < 0) anyint = -(anyint / 2);
    while (anyint >= size) anyint /= 2;
    return anyint;
}


void start_random_number(
  int seed_a,
  int seed_b
){
/*
 * This procedure initialises the state table u for a lagged 
 * Fibonacci sequence generator, filling it with random bits 
 * from a small multiplicative congruential sequence.
 * The auxilliaries c, ni, and nj are also initialized.
 * The seeds are transformed into an initial state in such a way that
 * identical results are guaranteed across a wide variety of machines.
 */

    double       s, bit;
    unsigned int ii, jj, kk, mm;
    unsigned int ll;
    unsigned int sd;
    unsigned int elt, bit_number;

    sd = collapse(seed_a, PM1 * PM1);
    ii = 1 + sd / PM1;
    jj = 1 + sd % PM1;
    sd = collapse(seed_b, PM1 * Q);
    kk = 1 + sd / PM1;
    ll = sd % Q;
    if (ii == 1   &&   jj == 1   &&   kk == 1)  ii = 2;

    ni  = STATE_SIZE - 1;
    nj  = STATE_SIZE / 3;
    c   = INIT_C;
    c  /= RANDOM_REALS;		/* compiler might mung the division itself */
    cd = INIT_CD;
    cd /= RANDOM_REALS;
    cm  = INIT_CM;
    cm /= RANDOM_REALS;

    for (elt = 0; elt < STATE_SIZE; elt += 1) {
	s   = 0.0;
	bit = 1.0 / RANDOM_REALS;
	for (bit_number = 0; bit_number < MANTISSA_SIZE; bit_number += 1) {
	    mm = (((ii * jj) % P) * kk) % P;
	    ii = jj;
	    jj = kk;
	    kk = mm;
	    ll = (53 * ll + 1) % Q;
	    if (((ll * mm) % 64) >= 32) s += bit;
	    bit += bit;
	}
	u[elt] = s;
    }
}
    
double next_random_number(void)
{
/*
 * Return a uniformly distributed pseudo random number
 * in the range 0.0 .. 1.0-2**(-24) inclusive.
 * There are 2**24 possible return values.
 * Side-effects the non-local variables: u, c, ni, nj.
 */

    double uni;
        
    if (u[ni] < u[nj]) uni = u[ni] + (1.0 - u[nj]);
        else           uni = u[ni] - u[nj];

    u[ni] = uni;

    if (ni > 0) ni -= 1;
        else    ni  = STATE_SIZE - 1;

    if (nj > 0) nj -= 1;
        else    nj  = STATE_SIZE - 1;

    if (c < cd) c = c + (cm - cd);
        else    c = c - cd;

    if (uni < c) return (uni + (1.0 - c));
        else     return (uni - c);
}
