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

#ifndef _RANDVAR_H
#define _RANDVAR_H

#include "matrix.h"

class DiscreteRV 
  {
  private:
	static const double INDEX_DENSITY = 2.0;
	Vector probs;		// vector of probabilities
	Vector inc_probs;	// will be inc_probs[i] = sum_{j=0}^i probs[j]
	int *indices;
	int n_indices;

	void init_indices(void);

  public:
	int generate(void) const;
	DiscreteRV(Vector &probs_);
	~DiscreteRV();
	double likelihood_at(int x) const 
		{ assert(x>=0&&x<probs.length()); return probs[x]; }
	int dimension(void) const { return probs.length(); }
  };

#endif 		// _RANDVAR_H

