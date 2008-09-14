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

#include <stdio.h>

#include "assocarr.h"
#include "dym_arr.h"
#include "matrix.h"
#include "misc.h"

#include "extractFeatures.h"
#include "genSamples.h"
#include "readText.h"

int isAuthor(int pos1, int pos2, Paper &paper)
  {
  assert(pos2 > pos1);
  for (int ctr=0; ctr < paper.nauthors; ctr++)
    if (paper.authorsStart[ctr] == pos1 && paper.authorsEnd[ctr] == pos2)
	return 1;

  return 0;
  }

void genSamples(int trainingSampleType,
		Paper &paper, int paperDex,
		Dymarr<paperSample*> &titleStartExample, 
		Dymarr<paperSample*> &titleEndExample, 
		Dymarr<paperSample*> &authorExample, 
		Dymarr<paperSample*> &abstractStartExample, 
		Dymarr<paperSample*> &abstractEndExample)
  {
  Vector features(FEATURE_VECTOR_LEN);

  for (int pos1=0; pos1 < MAX_PAPER_SCAN; pos1++)
    {
    // title start
    if ( (trainingSampleType == ALL_TRAININGDATA || trainingSampleType == TITLESTARTONLY_DATA)
	    && paper.titleStart != INVALID_LOCATION
	    && (pos1 >= TITLESTART_SCAN_BEGIN && pos1 <= TITLESTART_SCAN_END)
	    && (pos1 == paper.titleStart || bernoulli(TITLESTART_KEEPRATE)) )
	{
	extractFeatures(paper, pos1, pos1, features);
	titleStartExample.append(new paperSample(paperDex, pos1, features, pos1==paper.titleStart));
	}

    // title end 
    if ( (trainingSampleType == ALL_TRAININGDATA || trainingSampleType == TITLEENDONLY_DATA)
	    && paper.titleEnd != INVALID_LOCATION
	    && (pos1 >= TITLEEND_SCAN_BEGIN && pos1 <= TITLEEND_SCAN_END)
	    && (pos1 == paper.titleEnd || bernoulli(TITLEEND_KEEPRATE)) )
	{
	extractFeatures(paper, pos1, pos1, features);
	titleEndExample.append(new paperSample(paperDex, pos1, features, pos1==paper.titleEnd));
	}

    // authors 
    for (int pos2=pos1+MIN_AUTHOR_LEN; pos2 <= pos1+MAX_AUTHOR_LEN; pos2++)
      {
      if ( (trainingSampleType == ALL_TRAININGDATA || trainingSampleType == AUTHORONLY_DATA)
	    && paper.nauthors != 0
	    && (pos1 >= AUTHOR_SCAN_BEGIN && pos1 <= AUTHOR_SCAN_END)
	    && (isAuthor(pos1, pos2, paper) || bernoulli(AUTHOR_KEEPRATE)) )
	{
	extractFeatures(paper, pos1, pos2, features);
	authorExample.append(new paperSample(paperDex, pos1, features, isAuthor(pos1, pos2, paper)));
	}
      }

    // abstract start
    if ( (trainingSampleType == ALL_TRAININGDATA || trainingSampleType == ABSTRACTSTARTONLY_DATA)
	    && paper.abstractStart != INVALID_LOCATION
	    && (pos1 >= ABSTRACTSTART_SCAN_BEGIN && pos1 <= ABSTRACTSTART_SCAN_END)
	    && (pos1 == paper.abstractStart || bernoulli(ABSTRACTSTART_KEEPRATE)) )
	{
	extractFeatures(paper, pos1, pos1, features);
	abstractStartExample.append(new paperSample(paperDex, pos1, features, pos1==paper.abstractStart));
	}

    // abstract end
    if ( (trainingSampleType == ALL_TRAININGDATA || trainingSampleType == ABSTRACTENDONLY_DATA)
	    && paper.abstractEnd != INVALID_LOCATION
	    && (pos1 >= ABSTRACTEND_SCAN_BEGIN && pos1 <= ABSTRACTEND_SCAN_END)
	    && (pos1 == paper.abstractEnd || bernoulli(ABSTRACTEND_KEEPRATE)) )
	{
	extractFeatures(paper, pos1, pos1, features);
	abstractEndExample.append(new paperSample(paperDex, pos1, features, pos1==paper.abstractEnd));
	}
    }

  return;
  }

// precond: every paperSample in sample must have a paperDex that's in either
//     cvSet or trainingSet.
// This function will then set each paperSample's sample_type field to 
// either SAMPLE_TRAIN or SAMPLE_CV as appriopiate.
void setCVSample(Dymarr<paperSample*> sample, 
			HashedSet<int> &cvSet, HashedSet<int> &trainingSet)
  {
  for (int ctr=0; ctr < sample.usedsize(); ctr++)
	{
	int thisPaperNumber = sample[ctr]->paperDex;
	if (cvSet.containsElement(thisPaperNumber))
		sample[ctr]->sampleType = SAMPLE_CV;
	else
		{
		assert(trainingSet.containsElement(thisPaperNumber));
		sample[ctr]->sampleType = SAMPLE_TRAIN;
		}
	}

  return;
  }

void assignTrainTestSplit(Dymarr<paperSample*> &titleStartExample, 
			Dymarr<paperSample*> &titleEndExample, 
			Dymarr<paperSample*> &authorExample, 
			Dymarr<paperSample*> &abstractStartExample, 
			Dymarr<paperSample*> &abstractEndExample,
			DocInfo *samples[], int nsamples,
			double cvFraction)
  {
  int *isCVSample = new_select_frac_bits(nsamples, cvFraction);
  HashedSet<int> cvSet;
  HashedSet<int> trainingSet;

  for (int ctr=0; ctr < nsamples; ctr++)
	{
	if (isCVSample[ctr])
		cvSet.insertElement(samples[ctr]->docNumber);
	else
		trainingSet.insertElement(samples[ctr]->docNumber);
	}

  setCVSample(titleStartExample, cvSet, trainingSet);
  setCVSample(titleEndExample, cvSet, trainingSet);
  setCVSample(authorExample, cvSet, trainingSet);
  setCVSample(abstractStartExample, cvSet, trainingSet);
  setCVSample(abstractEndExample, cvSet, trainingSet);

  delete[] isCVSample;

  return;
  }

void assignTrainTestSplit(Dymarr<paperSample*> &examples, 
			DocInfo *samples[], int nsamples,
			double cvFraction)
  {
  int *isCVSample = new_select_frac_bits(nsamples, cvFraction);
  HashedSet<int> cvSet;
  HashedSet<int> trainingSet;

  for (int ctr=0; ctr < nsamples; ctr++)
	{
	if (isCVSample[ctr])
		cvSet.insertElement(samples[ctr]->docNumber);
	else
		trainingSet.insertElement(samples[ctr]->docNumber);
	}

  setCVSample(examples, cvSet, trainingSet);

  delete[] isCVSample;

  return;
  }

