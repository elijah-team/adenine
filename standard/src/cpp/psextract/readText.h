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

#ifndef _READTEXT_H
#define _READTEXT_H

#include "assocarr.h"
#include "dym_arr.h"
#include "str.h"

struct word
  {
  String s, orig, seps;
  int height, width;
  char firstCap, anyCap, singleAlphabet;
  char isFirst, isMiddle, isLast, inDict, numeric;
  char predAuthor;		// whether this was predicted to be part of 
				// an author's name.
  unsigned char sharePredAuthor;
  unsigned char share7, share14, share19;
  int lineLen;
  int specialWordNum;

  word(void) : s(""), orig(""), seps(""), height(0), width(0),
		firstCap(0), anyCap(0), singleAlphabet(0),
		isFirst(0), isMiddle(0), isLast(0), inDict(0), numeric(0), predAuthor(0),
		sharePredAuthor(0), share7(0), share14(0), share19(0), lineLen(1),
		specialWordNum(0) {}
  void print(FILE *fp);
  void printOrig(FILE *fp, int sep);
  void addsep(char c);
  };

#define NOT_SPECIAL_WORD (-1)
#define N_SPECIAL_WORD_SETS (20)

struct fontInfo
  {
  int height, width;
  fontInfo(void) : height(0), width(0) {};
  };

struct separatorInfo
  {
  char space, bigSpace, newline; 
  char comma, period, semicolon, colon, paren, quote, newPara, wrapNL, otherPunct, other;
  char fontChange;

  void zeroIt(void) 
    { space = bigSpace = newline = comma = period = semicolon = colon = paren 
		= quote = newPara = wrapNL = otherPunct = other = fontChange = 0; }
  void print(FILE *fp);
  separatorInfo(void) { zeroIt(); }
  };

const word emptyWord;
const String emptyString;
const separatorInfo emptySeparatorInfo;

// When combined with the raw training data, this basically becomes
// 1 "sample" of training data.
struct DocInfo
  {
  Dymarr<String> title;
  Dymarr<String> *authors;
  Dymarr<String> abstractStart;
  Dymarr<String> abstractEnd;
  int nauthors;
  int summaryStartType, summaryEndType; 
  int docNumber;
  DocInfo(void) :title(emptyString), abstractStart(emptyString),
				abstractEnd(emptyString) {} 
  };
enum {SUMMARY_ABSTRACT, SUMMARY_INTRO, SUMMARY_NONE};

enum {COMMAND_FONTCHANGE, COMMAND_BIGSPACE, COMMAND_NEWPARA, COMMAND_WRAPAROUNDNL,
		COMMAND_IGNORE};

#define INVALID_LOCATION (-1)

struct Paper
  {
  Dymarr<word> words;
  Dymarr<separatorInfo> separators;
		// separators[17] is the separator info 
		// between word[16] and word[17]

  int titleStart;		    // all of these might take on the value 
  int titleEnd; 		    // "INVALID_LOCATION" otherwise, *Start should index 
  int *authorsStart;		    // into the first word of the item in question. 
  int *authorsEnd;      	    // *End should index into the element just
  int abstractStart, abstractEnd;   // AFTER the last word of the item in question.

  int nauthors;

  int docNumber;		// set by matchToDoc

  Paper(void) : words(emptyWord), separators(emptySeparatorInfo), 
			titleStart(INVALID_LOCATION),
			authorsStart(NULL), authorsEnd(NULL),
			abstractStart(INVALID_LOCATION), abstractEnd(INVALID_LOCATION),
			nauthors(0), docNumber(-1) {}
  ~Paper() { if (authorsStart) delete[] authorsStart; 
	     if (authorsEnd) delete[] authorsEnd; }

  void matchToDoc(DocInfo &doc);	// sets titleStart, etc and nauthors
  void print(FILE *fp);
  };


void setLineFeatures(Paper &paper);
void printSample(DocInfo **samples, int nsamples);
DocInfo **new_readDocInfo(const char *fn, int *nDocsReadp);
void findTxtFiles(const char txtFilesDir[], Dymarr<String> &txtFiles);
Paper *new_readText(FILE *fp, int printOnlyStart= -1, int printOnlyEnd= -1, 
			FILE *outfp = NULL, int nNewParasAllowed=0);
Paper *new_readText(const String &fn, int printOnlyStart= -1, int printOnlyEnd= -1, 
			FILE *outfp = NULL, int nNewParasAllowed=0);
HashedSet<String> *new_dictionary(const char dictFn[]);

#define MAX_PAPER_READ 750		// should be a bit bigger than
					// the MAX_PAPER_SCAN of genSamples.h

#endif		// _READTEXT_H

