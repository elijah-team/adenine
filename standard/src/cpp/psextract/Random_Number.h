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
 *  Title: 	randomnumber
 *  Last Mod: 	Fri Mar 18 07:28:57 1988
 *  Author: 	Vincent Broman
 *		<broman@schroeder.nosc.mil>
 */  

void start_random_number(int seed_a, int seed_b);
double next_random_number(void);
    
/*  
 *  This package makes available Marsaglia's highly portable generator 
 *  of uniformly distributed pseudo-random numbers.
 *  
 *  The sequence of 24 bit pseudo-random numbers produced has a period 
 *  of about 2**144, and has passed stringent statistical tests 
 *  for randomness and independence.
 *  
 *  Supplying two seeds to start_random_number is required once
 *  at program startup before requesting any random numbers, like this:
 *      start_random_number(101, 202);
 *      r = next_random_number();
 *  The correspondence between pairs of seeds and generated sequences 
 *  of pseudo-random numbers is many-to-one.
 *  
 *  This package should compile and run identically on any 
 *  machine/compiler which supports >=16 bit integer arithmetic
 *  and >=24 bit floating point arithmetic.
 *  
 *  References:
 *      M G Harmon & T P Baker, ``An Ada Implementation of Marsaglia's
 *      "Universal" Random Number Generator'', Ada Letters, late 1987.
 *      
 *      G Marsaglia, ``Toward a universal random number generator'',
 *      to appear in the Journal of the American Statistical Association.
 *  
 *  George Marsaglia is at the Supercomputer Computations Research Institute
 *  at Florida State University.
 */  
