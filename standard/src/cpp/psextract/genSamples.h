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

#ifndef _GENSAMPLES_H
#define _GENSAMPLES_H

#include "dym_arr.h"
#include "readText.h"
#include "matrix.h"

struct paperSample
  {
  Vector features;
  int pos1;
  int paperDex;
  int target;
  int sampleType;		// SAMPLE_TRAIN or SAMPLE_CV
  paperSample(int paperDex_, int pos1_, const Vector &features_, int target_)
	: paperDex(paperDex_), pos1(pos1_), features(features_), target(target_) {}
  };

enum {SAMPLE_TRAIN, SAMPLE_CV};

// give a paper (and it's paperDex), 
void genSamples(int trainingSampleType,
		Paper &paper, int paperDex,
		Dymarr<paperSample*> &titleStartExample, 
		Dymarr<paperSample*> &titleEndExample, 
		Dymarr<paperSample*> &authorExample, 
		Dymarr<paperSample*> &abstractStartExample, 
		Dymarr<paperSample*> &abstractEndExample);
void assignTrainTestSplit(Dymarr<paperSample*> &examples, 
			DocInfo *samples[], int nsamples,
			double cvFraction);

// "trainingSampleType" values
enum {ALL_TRAININGDATA, 
	TITLESTARTONLY_DATA, TITLEENDONLY_DATA, AUTHORONLY_DATA, 
	ABSTRACTSTARTONLY_DATA, ABSTRACTENDONLY_DATA};

void assignTrainTestSplit(Dymarr<paperSample*> &titleStartExample, 
			Dymarr<paperSample*> &titleEndExample, 
			Dymarr<paperSample*> &authorExample, 
			Dymarr<paperSample*> &abstractStartExample, 
			Dymarr<paperSample*> &abstractEndExample,
			DocInfo *samples[], int nsamples,
			double cvFraction);


#define TITLESTART_SCAN_BEGIN 0 
#define TITLESTART_SCAN_END 40
#define TITLEEND_SCAN_BEGIN (TITLESTART_SCAN_BEGIN+MIN_TITLE_LEN)
#define TITLEEND_SCAN_END (TITLESTART_SCAN_END+MAX_TITLE_LEN)

#define AUTHOR_SCAN_BEGIN 0
#define AUTHOR_SCAN_END 60		// 70? 80?

#define ABSTRACTSTART_SCAN_BEGIN 0
#define ABSTRACTSTART_SCAN_END 150 
#define ABSTRACTEND_SCAN_BEGIN 50	
#define ABSTRACTEND_SCAN_END 350	// 300?

#define MAX_PAPER_SCAN 400		// should be the max of all the 
					// scan thingy's above

#define MIN_TITLE_LEN 2
// #define MAX_TITLE_LEN 16	//HERE
#define MAX_TITLE_LEN 18
#define MIN_AUTHOR_LEN 2
#define MAX_AUTHOR_LEN 4
#define MIN_ABSTRACT_LEN 10
#define MAX_ABSTRACT_LEN 400		// will probably be ignored
					// so long as it's >ABSTRACTEND_SCAN_END

#define TITLESTART_KEEPRATE (0.06*2.4)	// HERE
#define TITLEEND_KEEPRATE (0.05*2.4)
// #define AUTHOR_KEEPRATE (0.030*3)
#define AUTHOR_KEEPRATE (0.030*3.2)
#define ABSTRACTSTART_KEEPRATE (0.0175*2.4)
#define ABSTRACTEND_KEEPRATE (0.0082*2.4)

#endif		// _GENSAMPLES_H

