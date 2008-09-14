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

#ifndef _EXTRACT_FEATURES_H
#define _EXTRACT_FEATURES_H

#include "matrix.h"
#include "readText.h"

#define WORD_WITHIN_FEATUREINDEX_LEFT  22
#define WORD_WITHIN_FEATUREINDEX_MIDDLE 23
#define WORD_WITHIN_FEATUREINDEX_RIGHT 24
#define WORD_WITHIN_FEATUREINDEX_ALL 25

#define MAX_WORD_FEATURE_INDEX 26		// well, actually this is max+1

//----------------------------------------

#define SEP_INSIDE_AT_ENDS 22 
#define SEP_INSIDE_ALL 23

#define MAX_SEP_FEATURE_INDEX 24		// well, actually this is max+1

//----------------------------------------

#define FIRSTCAPNUM 0
#define ANYCAPNUM 1
#define SINGLEALPHAPLUSDOT 2
#define ISFIRST 3
#define ISMIDDLE 4
#define ISLAST 5
#define INDICT 6
#define HEIGHTGT95 7
#define HEIGHTGT110 8
#define HEIGHTGT120 9
#define HEIGHTGT130 10
#define HEIGHTGT140 11
#define HEIGHTGT150 12
#define HEIGHTGT175 13
#define NUMERICNUM 14		// should be consistent with N_BINARY_WORD_FEATURES
#define SINGLEALPHANODOT 15

#define N_BINARY_WORD_FEATURES 16
#define N_WORD_FEATURES (N_BINARY_WORD_FEATURES+N_SPECIAL_WORD_SETS)

//----------------------------------------

#define SPACE 0
#define BIGSPACE 1
#define PERIOD 2
#define COMMA 3
#define COMMAORSEMI 4
#define SEMIORPARENORQUOTE 5
#define NEWPARA 6
#define NEWLINE 7 
#define NLORWRAPNL 8 
#define FONTCHANGE 9 
#define FONTCHANGEORBS 10	// should be consistent with N_SEP_FEATURES

#define N_SEP_FEATURES 11

#define START_SEP_INDEX (N_WORD_FEATURES * MAX_WORD_FEATURE_INDEX)

//----------------------------------------

#define START_POS_INDEX (START_SEP_INDEX + N_SEP_FEATURES * MAX_SEP_FEATURE_INDEX)

#define N_POS_FEATURES 23

//----------------------------------------

#define FEATURE_VECTOR_LEN (START_POS_INDEX + N_POS_FEATURES)

//----------------------------------------

void extractFeatures(Paper &paper, 
		int pos1, int pos2, Vector &features);

#endif		// _EXTRACT_FEATURES_H

