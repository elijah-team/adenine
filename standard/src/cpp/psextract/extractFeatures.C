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

#include <assert.h>
#include <stdio.h>

#include "matrix.h"
#include "misc.h"
#include "readText.h"

#include "extractFeatures.h"

#define FEATURES_ARE_AVERAGES 1		// TTD/HERE: try 0 as well and 
					// compare performance

void debug_printConstants(void)
  {
  printf("MAX_WORD_FEATURE_INDEX : %d\n", MAX_WORD_FEATURE_INDEX);
  printf("MAX_SEP_FEATURE_INDEX : %d\n", MAX_SEP_FEATURE_INDEX);
  printf("N_BINARY_WORD_FEATURES : %d\n", N_BINARY_WORD_FEATURES);
  printf("N_WORD_FEATURES : %d\n", N_WORD_FEATURES);
  printf("N_SEP_FEATURES : %d\n", N_SEP_FEATURES);
  printf("START_SEP_INDEX : %d\n", START_SEP_INDEX);
  printf("START_POS_INDEX : %d\n", START_POS_INDEX);
  printf("N_POS_FEATURES : %d\n", N_POS_FEATURES);
  printf("FEATURE_VECTOR_LEN : %d\n", FEATURE_VECTOR_LEN);
  }

//----------------------------------------

void new_posFeatureIndex(int ***posFeatureIndexp, int **nPosFeaturesp)
  {
  int **featureIndex = new int*[16];	// actually 15 will do, but hey
  for (int ctr= 0; ctr <= 15; ctr++)
	featureIndex[ctr] = new int[10];		// (10 could be smaller, but hey again.)
  int *nfeatures = new int[16];
  zero_array(nfeatures, 16);

  featureIndex[14][nfeatures[14]++] = 21;
  featureIndex[13][nfeatures[13]++] = 21;
  featureIndex[12][nfeatures[12]++] = 21;
  featureIndex[11][nfeatures[11]++] = 21;
  featureIndex[10][nfeatures[10]++] = 21;
  featureIndex[ 9][nfeatures[ 9]++] = 21;
  featureIndex[ 8][nfeatures[ 8]++] = 21;
  featureIndex[ 7][nfeatures[ 7]++] = 21;
  featureIndex[ 6][nfeatures[ 6]++] = 21;
  featureIndex[ 5][nfeatures[ 5]++] = 21;
  featureIndex[ 4][nfeatures[ 4]++] = 21;
  featureIndex[ 3][nfeatures[ 3]++] = 21;
  featureIndex[ 2][nfeatures[ 2]++] = 21;
  featureIndex[ 1][nfeatures[ 1]++] = 21;
  featureIndex[ 0][nfeatures[ 0]++] = 21;

  featureIndex[ 9][nfeatures[ 9]++] = 20;
  featureIndex[ 8][nfeatures[ 8]++] = 20;
  featureIndex[ 7][nfeatures[ 7]++] = 20;
  featureIndex[ 6][nfeatures[ 6]++] = 20;
  featureIndex[ 5][nfeatures[ 5]++] = 20;
  featureIndex[ 4][nfeatures[ 4]++] = 20;
  featureIndex[ 3][nfeatures[ 3]++] = 20;
  featureIndex[ 2][nfeatures[ 2]++] = 20;
  featureIndex[ 1][nfeatures[ 1]++] = 20;
  featureIndex[ 0][nfeatures[ 0]++] = 20;

  featureIndex[ 4][nfeatures[ 4]++] = 19;
  featureIndex[ 3][nfeatures[ 3]++] = 19;
  featureIndex[ 2][nfeatures[ 2]++] = 19;
  featureIndex[ 1][nfeatures[ 1]++] = 19;
  featureIndex[ 0][nfeatures[ 0]++] = 19;

  featureIndex[ 2][nfeatures[ 2]++] = 18;
  featureIndex[ 1][nfeatures[ 1]++] = 18;
  featureIndex[ 0][nfeatures[ 0]++] = 18;

  featureIndex[ 3][nfeatures[ 3]++] = 17;
  featureIndex[ 2][nfeatures[ 2]++] = 17;

  featureIndex[ 9][nfeatures[ 9]++] = 16;
  featureIndex[ 8][nfeatures[ 8]++] = 16;
  featureIndex[ 7][nfeatures[ 7]++] = 16;
  featureIndex[ 6][nfeatures[ 6]++] = 16;
  featureIndex[ 5][nfeatures[ 5]++] = 16;
  featureIndex[ 4][nfeatures[ 4]++] = 16;
  featureIndex[ 3][nfeatures[ 3]++] = 16;

  featureIndex[ 4][nfeatures[ 4]++] = 15;
  featureIndex[ 3][nfeatures[ 3]++] = 15;

  featureIndex[ 2][nfeatures[ 2]++] = 14;
  featureIndex[ 1][nfeatures[ 1]++] = 14;

  featureIndex[ 2][nfeatures[ 2]++] = 13;

  featureIndex[ 1][nfeatures[ 1]++] = 12;

  featureIndex[ 0][nfeatures[ 0]++] = 11;

  *posFeatureIndexp = featureIndex;
  *nPosFeaturesp = nfeatures;

  return;
  }

void new_negFeatureIndex(int ***negFeatureIndexp, int **nNegFeaturesp)
  {
  int **featureIndex = new int*[16];
  for (int ctr= 0; ctr <= 15; ctr++)
	featureIndex[ctr] = new int[10];		// (10 could be smaller, but hey.)
  int *nfeatures = new int[16];
  zero_array(nfeatures, 16);

  featureIndex[15][nfeatures[15]++] = 10;
  featureIndex[14][nfeatures[14]++] = 10;
  featureIndex[13][nfeatures[13]++] = 10;
  featureIndex[12][nfeatures[12]++] = 10;
  featureIndex[11][nfeatures[11]++] = 10;
  featureIndex[10][nfeatures[10]++] = 10;
  featureIndex[ 9][nfeatures[ 9]++] = 10;
  featureIndex[ 8][nfeatures[ 8]++] = 10;
  featureIndex[ 7][nfeatures[ 7]++] = 10;
  featureIndex[ 6][nfeatures[ 6]++] = 10;
  featureIndex[ 5][nfeatures[ 5]++] = 10;
  featureIndex[ 4][nfeatures[ 4]++] = 10;
  featureIndex[ 3][nfeatures[ 3]++] = 10;
  featureIndex[ 2][nfeatures[ 2]++] = 10;
  featureIndex[ 1][nfeatures[ 1]++] = 10;

  featureIndex[10][nfeatures[10]++] = 9;
  featureIndex[ 9][nfeatures[ 9]++] = 9;
  featureIndex[ 8][nfeatures[ 8]++] = 9;
  featureIndex[ 7][nfeatures[ 7]++] = 9;
  featureIndex[ 6][nfeatures[ 6]++] = 9;
  featureIndex[ 5][nfeatures[ 5]++] = 9;
  featureIndex[ 4][nfeatures[ 4]++] = 9;
  featureIndex[ 3][nfeatures[ 3]++] = 9;
  featureIndex[ 2][nfeatures[ 2]++] = 9;
  featureIndex[ 1][nfeatures[ 1]++] = 9;

  featureIndex[ 5][nfeatures[ 5]++] = 8;
  featureIndex[ 4][nfeatures[ 4]++] = 8;
  featureIndex[ 3][nfeatures[ 3]++] = 8;
  featureIndex[ 2][nfeatures[ 2]++] = 8;
  featureIndex[ 1][nfeatures[ 1]++] = 8;

  featureIndex[ 3][nfeatures[ 3]++] = 7;
  featureIndex[ 2][nfeatures[ 2]++] = 7;
  featureIndex[ 1][nfeatures[ 1]++] = 7;

  featureIndex[ 2][nfeatures[ 2]++] = 6;
  featureIndex[ 1][nfeatures[ 1]++] = 6;

  featureIndex[10][nfeatures[10]++] = 5;
  featureIndex[ 9][nfeatures[ 9]++] = 5;
  featureIndex[ 8][nfeatures[ 8]++] = 5;
  featureIndex[ 7][nfeatures[ 7]++] = 5;
  featureIndex[ 6][nfeatures[ 6]++] = 5;
  featureIndex[ 5][nfeatures[ 5]++] = 5;
  featureIndex[ 4][nfeatures[ 4]++] = 5;

  featureIndex[ 5][nfeatures[ 5]++] = 4;
  featureIndex[ 4][nfeatures[ 4]++] = 4;

  featureIndex[ 3][nfeatures[ 3]++] = 3;
  featureIndex[ 2][nfeatures[ 2]++] = 3;

  featureIndex[ 3][nfeatures[ 3]++] = 2;

  featureIndex[ 2][nfeatures[ 2]++] = 1;

  featureIndex[ 1][nfeatures[ 1]++] = 0;

  *negFeatureIndexp = featureIndex;
  *nNegFeaturesp = nfeatures;

  return;
  }

// we'll organize our features array as follows:
// 
// the first chunk are the word features. 
// Suppose we have W word features, and P different position features.
// Then the first W chunks of P elements per chunk will be the word 
// features, with each chunk of P representing a different position.
// 

// seenCount[i] is incremented once for each time features[i]
// "could" have been incremented.
// (The intention being to divide features by seenCount afterwards.)
void addWordFeatures(Vector &features, Vector &seenCount, 
			word &thisWord, 
			separatorInfo &afterSep,
			int featuresIndex[], int nfeatureIndices)
  {
  int onFeatures[N_WORD_FEATURES];
		// first N_BINARY_WORD_FEATURES are indexed by
		// the #defined indices above.
		// of the next N_SPECIAL_WORD_SETS, at most 1 bit should
		// be on for a particular word, corresponding to the 
		// special word in question

  zero_array(onFeatures, N_WORD_FEATURES);

  if (thisWord.firstCap)
	onFeatures[FIRSTCAPNUM] = 1;
  if (thisWord.anyCap)
	onFeatures[ANYCAPNUM] = 1;
  if (thisWord.singleAlphabet && afterSep.period)
	onFeatures[SINGLEALPHAPLUSDOT] = 1;
  if (thisWord.singleAlphabet && !afterSep.period)
	onFeatures[SINGLEALPHANODOT] = 1;
  if (thisWord.isFirst)
	onFeatures[ISFIRST] = 1;
  if (thisWord.isMiddle)
	onFeatures[ISMIDDLE] = 1;
  if (thisWord.isLast)
	onFeatures[ISLAST] = 1;
  if (thisWord.inDict)
	onFeatures[INDICT] = 1;
  if (thisWord.height > 95)
	onFeatures[HEIGHTGT95] = 1;
  if (thisWord.height > 110)
	onFeatures[HEIGHTGT110] = 1;
  if (thisWord.height > 120)
	onFeatures[HEIGHTGT120] = 1;
  if (thisWord.height > 130)
	onFeatures[HEIGHTGT130] = 1;
  if (thisWord.height > 140)
	onFeatures[HEIGHTGT140] = 1;
  if (thisWord.height > 150)
	onFeatures[HEIGHTGT150] = 1;
  if (thisWord.height > 175)
	onFeatures[HEIGHTGT175] = 1;
  if (thisWord.numeric)
	onFeatures[NUMERICNUM] = 1;
  if (thisWord.specialWordNum != NOT_SPECIAL_WORD)
	{
	assert(thisWord.specialWordNum >= 0 && thisWord.specialWordNum < N_SPECIAL_WORD_SETS);
	assert(onFeatures[N_BINARY_WORD_FEATURES] == 0);
	assert(onFeatures[N_BINARY_WORD_FEATURES+1] == 0);
	assert(onFeatures[N_BINARY_WORD_FEATURES+2] == 0);
	assert(onFeatures[N_BINARY_WORD_FEATURES+thisWord.specialWordNum] == 0);

	onFeatures[N_BINARY_WORD_FEATURES+thisWord.specialWordNum] = 1;
	}

  // now, time to stick it into the features vector
  assert(nfeatureIndices > 0);		// (because I designed the features that way.)
  for (int fctr=0; fctr < nfeatureIndices; fctr++)
    {
    int thisFeatureIndex = featuresIndex[fctr];
    for (int dex=0; dex < N_WORD_FEATURES; dex++)
	{
	features[thisFeatureIndex * N_WORD_FEATURES + dex] += onFeatures[dex];
	seenCount[thisFeatureIndex * N_WORD_FEATURES + dex] ++;
	assert(thisFeatureIndex * N_WORD_FEATURES + dex < START_SEP_INDEX);
	}
    }

  return;
  } 

// seenCount[i] is incremented once for each time features[i]
// "could" have been incremented.
// (The intention being to divide features by seenCount afterwards.)
void addSepFeatures(Vector &features, Vector &seenCount, 
			separatorInfo &thisSep, 
			int featuresIndex[], int nfeatureIndices)
  {
  int onFeatures[N_SEP_FEATURES];
		// first N_SEP_FEATURES are indexed by
		// the #defined indices above.

  zero_array(onFeatures, N_SEP_FEATURES);

  if (thisSep.space)
	onFeatures[SPACE] = 1;
  if (thisSep.space || thisSep.bigSpace)
	onFeatures[BIGSPACE] = 1;
  if (thisSep.period)
	onFeatures[PERIOD] = 1;
  if (thisSep.comma)
	onFeatures[COMMA] = 1;
  if (thisSep.comma || thisSep.semicolon || thisSep.colon)
	onFeatures[COMMAORSEMI] = 1;
  if (thisSep.semicolon || thisSep.colon || thisSep.paren || thisSep.quote)
	onFeatures[SEMIORPARENORQUOTE] = 1;
  if (thisSep.newPara)
	onFeatures[NEWPARA] = 1;
  if (thisSep.newline)
	onFeatures[NEWLINE] = 1;
  if (thisSep.newline || thisSep.wrapNL)
	onFeatures[NLORWRAPNL] = 1;
  if (thisSep.fontChange)
	onFeatures[FONTCHANGE] = 1;
  if (thisSep.fontChange || thisSep.bigSpace)
	onFeatures[FONTCHANGEORBS] = 1;

  // now, time to stick it into the features vector
  assert(nfeatureIndices > 0);		// (because I designed the features that way.)
  for (int fctr=0; fctr < nfeatureIndices; fctr++)
    {
    int thisFeatureIndex = featuresIndex[fctr];
    for (int dex=0; dex < N_SEP_FEATURES; dex++)
	{
	features[thisFeatureIndex * N_SEP_FEATURES + dex + START_SEP_INDEX] += onFeatures[dex];
	seenCount[thisFeatureIndex * N_SEP_FEATURES + dex + START_SEP_INDEX] ++;
	assert(thisFeatureIndex * N_SEP_FEATURES + dex + START_SEP_INDEX < START_POS_INDEX);
	}
    }

  return;
  } 

void extractFeatures(Paper &paper, 
		int pos1, int pos2, Vector &features)
  {
  assert(pos1 <= pos2);
  int nwords = paper.words.usedsize();
  static int **negFeatureIndex = NULL, **posFeatureIndex = NULL;
  static int *nNegFeatures = NULL, *nPosFeatures = NULL;

  features.assert_dim(FEATURE_VECTOR_LEN);
  features.make_zero();
  static Vector seenCount(FEATURE_VECTOR_LEN);
  seenCount.make_zero();
  assert(features[START_POS_INDEX] == 0);

  if (negFeatureIndex == NULL)
     { assert(nNegFeatures == NULL && posFeatureIndex == NULL && nPosFeatures == NULL); 
	new_negFeatureIndex(&negFeatureIndex, &nNegFeatures); 
	new_posFeatureIndex(&posFeatureIndex, &nPosFeatures); }

  //-----------------------------------------------------------------
  // WORD FEATURES
  assert(features[START_POS_INDEX] == 0);

  // extract negative word features
  for (int negPos=1; negPos <= 15; negPos++)
	{
	
	int pos = pos1 - negPos;
	if (pos < 0)
		continue;
	addWordFeatures(features, seenCount, 
			paper.words[pos], paper.separators[pos+1],
			negFeatureIndex[negPos], nNegFeatures[negPos]);
	}

  // middle word features
  if (pos1 != pos2)
	{
	if (pos2 == pos1+1)
		error("extractFeatures: pos2 shouldbe >pos1+1 or ==pos1. "
			"(i.e. no one-word fields.)");
	int leftFeatureIndices[] = {WORD_WITHIN_FEATUREINDEX_LEFT, 
					WORD_WITHIN_FEATUREINDEX_ALL};
	int midFeatureIndices[] = {WORD_WITHIN_FEATUREINDEX_MIDDLE, 
					WORD_WITHIN_FEATUREINDEX_ALL};
	int rightFeatureIndices[] = {WORD_WITHIN_FEATUREINDEX_RIGHT, 
					WORD_WITHIN_FEATUREINDEX_ALL};

	addWordFeatures(features, seenCount, 
			paper.words[pos1], paper.separators[pos1+1],
			leftFeatureIndices, 2);
	for (int withinPos=pos1+1; withinPos < pos2-1; withinPos++)
	    addWordFeatures(features, seenCount, 
				paper.words[withinPos], paper.separators[withinPos+1],
				midFeatureIndices, 2);
	addWordFeatures(features, seenCount, 
			paper.words[pos2-1], paper.separators[pos2], 
			rightFeatureIndices, 2);
	}

  // extract positive word features
  for (int posPos=0; posPos < 15; posPos++)
	{
	int pos = pos2 + posPos;
	if (pos > nwords)
		break;
	addWordFeatures(features, seenCount, paper.words[pos], paper.separators[pos+1],
			posFeatureIndex[posPos], nPosFeatures[posPos]);
	}

  //-----------------------------------------------------------------
  // SPACE FEATURES

  // extract negative sep features
  for (int negPos=0; negPos < 15; negPos++)	// note offby1 from word features
	{
	
	int pos = pos1 - negPos;
	if (pos < 0)
		continue;
	addSepFeatures(features, seenCount, paper.separators[pos],
			negFeatureIndex[negPos+1], nNegFeatures[negPos+1]);
	}

  // middle word features
  if (pos1 != pos2)
	{
	assert(pos2 > pos1+1);		// checked earlier to be true 

	int featureIndicesAtEnds[] = {SEP_INSIDE_AT_ENDS, SEP_INSIDE_ALL};
	int featureIndicesInside[] = {SEP_INSIDE_ALL};

	addSepFeatures(features, seenCount, paper.separators[pos1+1],
			featureIndicesAtEnds, 2);
	for (int withinPos=pos1+2; withinPos < pos2-1; withinPos++)
	    addSepFeatures(features, seenCount, paper.separators[withinPos], 
				featureIndicesInside, 1);
	if (pos2-1 != pos1+1)
	    addSepFeatures(features, seenCount, paper.separators[pos2-1],
		featureIndicesAtEnds, 2);
	}

  // extract positive sep features
  for (int posPos=0; posPos < 15; posPos++)
	{
	int pos = pos2 + posPos;
	if (pos > nwords)
		break;
	addSepFeatures(features, seenCount, paper.separators[pos],
			posFeatureIndex[posPos], nPosFeatures[posPos]);

	assert(features[START_POS_INDEX] == 0);
	}

  //-----------------------------------------------------------------
  // POS FEATURES
  // (also includes the "constant 1" feature)
  // (also, we don't bother with the seenCounts for these)

  assert(features[START_POS_INDEX] == 0);
  assert(features[START_POS_INDEX+1] == 0);
  assert(features[START_POS_INDEX+2] == 0);
  assert(features[START_POS_INDEX+3] == 0);

  int posFeatureCtr = 0;

  features[START_POS_INDEX+posFeatureCtr++] = (pos1<1);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1<2);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1<4);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1<8);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1<16);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1<32);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1<64);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1<128);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1<256);

  features[START_POS_INDEX+posFeatureCtr++] = (pos2-pos1 == 0);
  features[START_POS_INDEX+posFeatureCtr++] = (pos2-pos1 == 1);
  features[START_POS_INDEX+posFeatureCtr++] = (pos2-pos1 == 2);
  features[START_POS_INDEX+posFeatureCtr++] = (pos2-pos1 == 3);
  features[START_POS_INDEX+posFeatureCtr++] = (pos2-pos1 == 4);

  // these are the pos features relating to the line containing
  // paper.words[pos1];
  word pos1Word = paper.words.peek(pos1);
  if (pos1Word.lineLen <= 0)
	{ internal_warn("93580"); pos1Word.lineLen = 1; }
  features[START_POS_INDEX+posFeatureCtr++] = (pos1Word.share7 > 0);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1Word.share14 > 0);
  features[START_POS_INDEX+posFeatureCtr++] = (pos1Word.share19 > 0);
  features[START_POS_INDEX+posFeatureCtr++] 
				= (double)pos1Word.share7/ pos1Word.lineLen;
  features[START_POS_INDEX+posFeatureCtr++] 
				= (double)pos1Word.share14/ pos1Word.lineLen;
  features[START_POS_INDEX+posFeatureCtr++] 
				= (double)pos1Word.share19/ pos1Word.lineLen;
  features[START_POS_INDEX+posFeatureCtr++] = pos1Word.lineLen;
  features[START_POS_INDEX+posFeatureCtr++] = sqrt(pos1Word.lineLen);

  features[START_POS_INDEX+posFeatureCtr++] = 1;

//FEATURE_VECTOR_LEN : 1223


  assert(posFeatureCtr == N_POS_FEATURES); 
  assert(START_POS_INDEX + posFeatureCtr == FEATURE_VECTOR_LEN);
  
  //-----------------------------------------------------------------
  // CLEANUP

  if (FEATURES_ARE_AVERAGES)
    for (int ctr=0; ctr < features.dim(); ctr++)
	{
	if (seenCount[ctr] == 0)
		assert(features[ctr] == 0 || ctr >= START_POS_INDEX);
			// only position features can have non-zero values
			// while having seenCounts (as they are always
			// seen exactly once, so we don't bother with seenCounts 
			// for them.)
	else
		features[ctr] /= seenCount[ctr];
	}

  return;
  }

/*

Features:
DONE 1. Capitalization (lower, Firstcap, ANycaP)
DONE 2. Exact word match
DONE 3. In-dictionary
DONE 4. In-firstNames
DONE 4b. In-middleNames
DONE 5. In-lastNames
DONE 6. punctionation, bigspace, fontChange
DONE 7. position from start of doc
DONE 8. font size
SKIP 9. Single-character long
DONE 10. Single character with period after (i.e. "V." in "V. Vapnik")
DONE Numeric 

*/

