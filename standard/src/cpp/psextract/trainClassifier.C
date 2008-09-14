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
#include <ctype.h>
#include <stdio.h>
#include <values.h>

#include "assocarr.h"
#include "cmd_line.h"
#include "discreterv.h"
#include "dym_arr.h"
#include "extractFeatures.h"
#include "genSamples.h"
#include "matrix.h"
#include "misc.h"
#include "readText.h"
#include "str.h"

#define ALLOW_DOWNSAMPLING 1

#define CVFRACTION (0.30)

#define DEFAULT_NROUNDS 1000 
// #define DEFAULT_AUTHOR_THRESHOLD (0.4925)
#define DEFAULT_AUTHOR_THRESHOLD (28)

//-------

// #define TITLE_START_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/titleStartClassifier.dat"
// #define TITLE_END_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/titleEndClassifier.dat"
// #define AUTHOR_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/authorClassifier.dat"
// #define ABSTRACT_START_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/abstractStartClassifier.dat"
// #define ABSTRACT_END_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/abstractEndClassifier.dat"
// 
// #define BADAUTHORWORDS_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/badAuthorWords.txt"
// #define BADAUTHORPHRASES_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/badAuthorPhrases.txt"
// #define DOMAINS_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/domains.txt"
// 
// #define TRAINING_DATA_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/trainingData"
// #define TESTING_DATA_FN "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/testData"
// #define TXT_FILES_DIR "/afs/cs.cmu.edu/project/learn-7/an2i/fetch/pipe/txt"

//-------

/*
#define TITLE_START_FN "titleStartClassifier.dat"
#define TITLE_END_FN "titleEndClassifier.dat"
#define AUTHOR_FN "authorClassifier.dat"
#define ABSTRACT_START_FN "abstractStartClassifier.dat"
#define ABSTRACT_END_FN "abstractEndClassifier.dat"

#define BADAUTHORWORDS_FN "badAuthorWords.txt"
#define BADAUTHORPHRASES_FN "badAuthorPhrases.txt"
#define DOMAINS_FN "domains.txt"

#define TRAINING_DATA_FN "trainingData"
#define TESTING_DATA_FN "testData"
#define TXT_FILES_DIR "pipe/txt"
*/

#define TITLE_START_FN "titleStartClassifier.dat"
#define TITLE_END_FN "titleEndClassifier.dat"
#define AUTHOR_FN "authorClassifier.dat"
#define ABSTRACT_START_FN "abstractStartClassifier.dat"
#define ABSTRACT_END_FN "abstractEndClassifier.dat"

#define BADAUTHORWORDS_FN "badAuthorWords.txt"
#define BADAUTHORPHRASES_FN "badAuthorPhrases.txt"
#define DOMAINS_FN "domains.txt"

#define TRAINING_DATA_FN "trainingData"
#define TESTING_DATA_FN "testData"
#define TXT_FILES_DIR "pipe/txt"


struct classifier_t
  {
  public: 
    virtual double score(Vector &features) = 0;
    virtual int predict(Vector &features) = 0;
    ~classifier_t(void) {}
  };

struct threshClassifier_t : public classifier_t
  {
  int nthresh;
  Vector weights;
  Vector thresholds;
  int *featureIndex;

  threshClassifier_t(int nthresh_) 
		: nthresh(nthresh_), weights(nthresh_), thresholds(nthresh_)
	{ featureIndex = new int[nthresh_]; }
  ~threshClassifier_t() { delete[] featureIndex; }

   double score(Vector &features) 
	{
	double score = 0.0;
	for (int ctr=0; ctr < nthresh; ctr++)
	    {
	    if (features[featureIndex[ctr]] > thresholds[ctr])
		    score += weights[ctr];
	    }
	return score;
	}
  int predict(Vector &features)
	{ return (score(features) > 0.5*weights.sum()); }
  };

struct linearClassifier_t : public classifier_t
  {
  Vector posWeights, negWeights;
  linearClassifier_t(void) : posWeights(FEATURE_VECTOR_LEN), negWeights(FEATURE_VECTOR_LEN) {};
  double score(Vector &features) 
		{ return dot_prod(posWeights,features) - dot_prod(negWeights,features); }
  int predict(Vector &features) { 
    error("Sorry, predict(...) not implemented for linear classifier."); 
    return 0; // never executed 
  }
};

// counts the number of positive instances in a certain sample of data
int nPositive(Dymarr<paperSample*> &ex)
  {
  int count=0;

  for (int ctr=0; ctr < ex.usedSize(); ctr++)
	if (ex[ctr]->target)
		count++;

  return count; 
 }

int nNegative(Dymarr<paperSample*> &ex)
  {
  return ex.usedSize() - nPositive(ex);
  }

// reads seeds from command line (if any), and initializes
// random number generator
void start_random_number_from_cline(cmd_line &c_line,
                                int *seed1p=NULL, int *seed2p=NULL)
  {
  const long DEFAULT_SEED1 = 98239,
             DEFAULT_SEED2 = 19281;

  int seed1, seed2;

  seed1 = c_line.get_flag_arg("seeds", DEFAULT_SEED1, 0);
  seed2 = c_line.get_flag_arg("seeds", DEFAULT_SEED2, 1);

  start_random_number(seed1, seed2);

  if (seed1p != NULL)
        *seed1p = seed1;
  if (seed2p != NULL)
        *seed2p = seed2;

  return;
  }

// if restrictSamples, then it will limit the number of samples
// taken to be a small number.
DocInfo **genTrainingData(cmd_line &c_line,
			int trainingDataType,
			Dymarr<paperSample*> &titleStartExample, 
			Dymarr<paperSample*> &titleEndExample, 
			Dymarr<paperSample*> &authorExample, 
			Dymarr<paperSample*> &abstractStartExample, 
			Dymarr<paperSample*> &abstractEndExample,
			int restrictSamples, int *nsamplesp,
			Dymarr<Paper*> &papers)
  {
  error("training not available in this distribution.");
  return NULL;
  }

// this counts the most frequent words and prints them out.
// Intended for use in part of a manual process for feature design.
void countFreqWords(void)
  {
  AssocArray<int, String> count(0);
  AssocArray<int, int> heightCount(0);
  Paper *paper;
  const int WORDS_TO_EXAMPLE = 100;
  String s;
  Dymarr<String> txtFiles(emptyString);

  findTxtFiles(TXT_FILES_DIR, txtFiles);

  // note: Documents 501 onwards are used for determining
  // "common words"
  for (int ctr=501; ctr < txtFiles.usedSize(); ctr++)
	{
	if (txtFiles[ctr] == "")
	    continue;
	fprintf(stderr, "Doing %s\n", txtFiles[ctr].as_char());
	paper = new_readText(txtFiles[ctr]);
	for (int dex=0; dex < min(WORDS_TO_EXAMPLE, paper->words.usedSize()); dex++)
	    {
	    s = toLower(paper->words[dex].s);
	    count[s]++;
	    heightCount[paper->words[dex].height]++;
	    }

	delete paper;
	}

//  String *allWords = count.new_all_keys();
//  int nwords = count.nele_val();
//  for (int ctr=0; ctr < nwords; ctr++)
//	printf("%d %s\n", count[allWords[ctr]], allWords[ctr].as_char());

  int *allHeights = heightCount.new_all_keys();
  int nheights = heightCount.nele_val();
  for (int ctr=0; ctr < nheights; ctr++)
	printf("%d %d \n", allHeights[ctr], heightCount[allHeights[ctr]]);
  delete[] allHeights;

  return;
  }


// prediction will be (posWeights * features - negWeights * features) .
// posWeights and negWeights are always intended to be positive.
//
// precond: sampleWeights sums to examples.usedSize();
void EG_trainWeakLearner(Vector &posWeights, Vector &negWeights, 
		Dymarr<paperSample*> &examples, const Vector &sampleWeights)
  {
  error("training not available in this distribution.");
  return;
  }

double trainStump(int stumpIndex, Dymarr<paperSample*> &examples, 
		const Vector &sampleWeights, const Vector &sqrtSampleWeights,
		double *varCoeff, double *constCoeff)
  {
  error("training not available in this distribution.");
  return 0;
  }

// prediction will be (posWeights * features - negWeights * features) .
// posWeights and negWeights are always intended to be positive.
//
// precond: sampleWeights sums to examples.usedSize();
void stump_trainWeakLearner(Vector &posWeights, Vector &negWeights, 
		Dymarr<paperSample*> &examples, const Vector &sampleWeights)
  {
  error("training not available in this distribution.");

  return;
  }


// prediction will be (posWeights * features - negWeights * features) .
// posWeights and negWeights are always intended to be positive.
//
// precond: sampleWeights sums to examples.usedSize();
//
// "round" is taken to be the number of this round of boosting,
//  bagging or whatever
void trainLinearClassifierFromScratch(int round,
		Vector &posWeights, Vector &negWeights, 
		Dymarr<paperSample*> &examples, Vector &sampleWeights)
  {
  error("training not available in this distribution.");

  return;
  }

void boost_trainLinearClassifierFromScratch(linearClassifier_t &classifier, 
			Dymarr<paperSample*> &examples,
			DocInfo **docInfo, int nTrainDocs,
			int nrounds)
  {
  error("training not available in this distribution.");
  return;
  }

// returns the empirical (weighted) error
double trainThreshClassifierFromScratch(int round,
		Dymarr<paperSample*> &examples, Vector &sampleWeights,
		Dymarr<double> *possibleThresholds,
		double *threshold, int *featureIndex)
  {
  error("training not available in this distribution.");
  return 0;
  }

Dymarr<double> *new_possibleThresholds(Dymarr<paperSample*> &examples)
  {
  error("training not available in this distribution.");

  return NULL;
  }

void boost_trainThreshClassifierFromScratch(threshClassifier_t &classifier, 
			Dymarr<paperSample*> &examples,
			DocInfo **docInfo, int nTrainDocs,
			int nrounds)
  {
  error("training not available in this distribution.");

  return;
  }

void trainAndSaveClassifier(cmd_line &c_line, 
			const char fn[], Dymarr<paperSample*> &examples,
			DocInfo **docInfo, int nTrainDocs, int nrounds)
  {
  error("training not available in this distribution.");
  }

void trainAndSaveClassifier(cmd_line &c_line, 
			Dymarr<paperSample*> &titleStartExample, 
			Dymarr<paperSample*> &titleEndExample, 
			Dymarr<paperSample*> &authorExample, 
			Dymarr<paperSample*> &abstractStartExample, 
			Dymarr<paperSample*> &abstractEndExample,
			DocInfo **docInfo, int nTrainDocs)
  {
  error("training not available in this distribution.");
  }

classifier_t *new_loadClassifier(const char fn[], cmd_line *c_line=NULL, int hobble= -1)
  {
  FILE *fp = safe_fopen(fn, "rt");
  int err;
  char buff[1024]; 
  classifier_t *retval;

  fscanf(fp, "%s", buff);
  if (!strcmp(buff, "linear"))
	{
	error("new_loadClassifier: saw hobble while loading linear classifier. Not possible.");
	linearClassifier_t *classifier = new linearClassifier_t;
	for (int i=0; i < FEATURE_VECTOR_LEN; i++)
	    {
	    err = fscanf(fp, "%lf", &classifier->posWeights[i]);
	    if (err != 1)
		error(("Error reading classifier from " + String(fn)).as_char());
	    }
	for (int i=0; i < FEATURE_VECTOR_LEN; i++)
	    {
	    err = fscanf(fp, "%lf", &classifier->negWeights[i]);
	    if (err != 1)
		error(("Error reading classifier from " + String(fn)).as_char());
	    }
	retval = classifier;
	}
  else if (!strcmp(buff, "thresh"))
	{
	double nthresh_d;
	int nthresh;
	fscanf(fp, "%lf", &nthresh_d);
	assert(nthresh_d == (int)nthresh_d);
	nthresh = (int)nthresh_d;

	if (c_line != NULL && c_line->get_flag_arg("hobble", (long) -1) != -1)
		{
		if (hobble != -1)
		    error("new_loadClassifier: internally told to hobble, but also by command line. Can't.");
		hobble = c_line->get_flag_arg("hobble", (long) -1);
		}
	if (hobble != -1)
		{
		static int warned = 0;
		if (!warned)
		    {
		    fprintf(stderr, "RESTRICTING CLASSIFIER TO SMALLER NUMBER OF ROUNDS.\n");
		    warned=1;
		    }
		assert(hobble <= nthresh);
		nthresh = hobble;
		}

	threshClassifier_t *classifier = new threshClassifier_t(nthresh);
	assert(classifier->nthresh == nthresh);

	for (int i=0; i < classifier->nthresh; i++)
	    {
	    err = fscanf(fp, "%lf%lf%d\n",
		&classifier->weights[i],
		&classifier->thresholds[i],
		&classifier->featureIndex[i]);
	    if (err != 3)
		error(("Error reading classifier from " + String(fn) + " (" + as_string(err) 
			+ "," + as_string(i) + ")").as_char());
	    }

	retval = classifier;
	}
  else
	error(("Unknown classifier type in file "+String(fn)).as_char());

  fclose(fp);

  return retval;
  }

// if file contains "Foo Bar baz" on a line, then 
// "Foo#bar#baz" will be added to the dictionary
// (i.e. spaces changed to #-signs.)
HashedSet<String> *new_phrasesDictionary(const char dictFn[])
  {
  HashedSet<String> *dict = new HashedSet<String>;
  FILE *fp;
  char buff[4096];
  String s;

  fp = safe_fopen(dictFn, "rt");
  while(fgets(buff, 4096, fp) != NULL)
    {
    int dex = strlen(buff)-1;
    while (dex >= 0 && isspace(buff[dex]))
	{ buff[dex] = 0; dex--; }

    if (dex == 0) break; 

    for (dex=0; buff[dex] != 0; dex++)
	{
	if (isspace(buff[dex]))
	    {
	    buff[dex] = '#';
	    if (dex != 0 && buff[dex-1] == '#')
		warn("new_phrasesDictionary: mutiple consecutive spaces?");
	    }
	}
    
    s = buff;
    if (dict->containsElement(s))
	    warn((String("new_dictionary") + dictFn 
			+ " contains duplicate word " + s).as_char());
    else
	    dict->insertElement(s);
    }
  fclose(fp);

  return dict;
  }


// sets the appropiate elemeent in selected[] to -1 
void rejectImpossibleAuthors(Dymarr<int> &authorStart, Dymarr<int> &authorEnd, 
			Paper *paper, int selected[])
  {
  static HashedSet<String> *badAuthorWords = NULL;
  static HashedSet<String> *badAuthorPhrases = NULL;
  static HashedSet<String> *domains = NULL;

  if (badAuthorWords == NULL)
	{
	assert(badAuthorPhrases == NULL);
	badAuthorWords = new_dictionary(BADAUTHORWORDS_FN);
	badAuthorPhrases = new_phrasesDictionary(BADAUTHORPHRASES_FN);
	}

  for (int a=0; a < authorStart.usedSize(); a++)
    {
    String s;
    for (int dex=authorStart[a]; dex < authorEnd[a]; dex++)
	{
        if (badAuthorWords->containsElement(paper->words[dex].s))
		selected[a] = -1;
	if (s.len() == 0)
	   s = paper->words[dex].s;
	else 
	   s += String("#") + paper->words[dex].s;
	}

    // printf("-->%s<--\n", s.as_char());
    if (badAuthorPhrases->containsElement(s))
	{
	// printf("REJECTED AUTHOR: %s\n", s.as_char());
	selected[a] = -1;
	}

    // mostly to eliminate email addresses
    word w = paper->words[authorEnd[a]-1];	 // last word in author name
    int cantBeEmailAddress=0;
    for (int dex=authorStart[a]; dex < authorEnd[a]-1; dex++)
	{
	separatorInfo sep = paper->separators[dex+1];
	if (sep.space || sep.bigSpace || sep.newline 
		     || (!sep.period && !sep.other) )
		{ cantBeEmailAddress=1; break; }
	}

    if (!cantBeEmailAddress)
	{
	if (domains == NULL)
	    { domains = new_dictionary(DOMAINS_FN); }
	if (domains->containsElement(paper->words[authorEnd[a]-1].s))
		selected[a] = -1;	// seems to be an email address
	}
    }

  return;
  }

// returns 1 only if it's "pretty sure" the 2 authors are the same
int isSameAuthor(int aStart1, int aEnd1, int aStart2, int aEnd2, Paper *paper)
  {
  int len1 = aEnd1 - aStart1;
  int len2 = aEnd2 - aStart2;

  assert(aEnd1 > 0 && aEnd2 > 0);

  // make the aStart1,aEnd1 represent the shorter name
  if (len1 > len2)
	{
	swap(aStart1, aStart2);
	swap(aEnd1, aEnd2);
	swap(len1, len2);
	}

  // if they have the same number of words, then all the words must match.
  //   (first 3 cases)
  // Or, if one is of length 2, then the firstnames and lastnames must match,
  //   and must be of length >1 (i.e. not initials). (last case)
  //
  // Note a length 3 may never match a length 4 author.
  if (len1 == 2 && len2 == 2)
	{
	return paper->words.peek(aStart1).s == paper->words.peek(aStart2).s
	    && paper->words.peek(aStart1+1).s == paper->words.peek(aStart2+1).s;
	}
  else if (len1 == 3 && len2 == 3)
	{
	return paper->words.peek(aStart1).s == paper->words.peek(aStart2).s
	    && paper->words.peek(aStart1+1).s == paper->words.peek(aStart2+1).s
	    && paper->words.peek(aStart1+2).s == paper->words.peek(aStart2+2).s;
	}
  else if (len1 == 4 && len2 == 4)
	{
	return paper->words.peek(aStart1).s == paper->words.peek(aStart2).s
	    && paper->words.peek(aStart1+1).s == paper->words.peek(aStart2+1).s
	    && paper->words.peek(aStart1+2).s == paper->words.peek(aStart2+2).s
	    && paper->words.peek(aStart1+3).s == paper->words.peek(aStart2+3).s;
	}
  else if (len1 == 2)
	{
	return paper->words.peek(aStart1).s == paper->words.peek(aStart2).s
	    && paper->words.peek(aStart1).s.len() > 1
	    && paper->words.peek(aEnd1-1).s == paper->words.peek(aEnd2-1).s
	    && paper->words.peek(aEnd1-1).s.len() > 1;
	}

  return 0;
  }

// orders the author entries from highest to lowest score, and
// selects them out in that order, but skipping any author that
// "intersects" a previoosly selected author.
//
// New: Also excludes anything that intersects the title. 
//   (which seemed to be a common error)
//
// precond: trimedAuthor{Start,End,Scores} are empty
void trimAuthors(Dymarr<int> &authorStart, Dymarr<int> &authorEnd, 
			Dymarr<double> &authorScores, 
		 Dymarr<int> &trimmedAuthorStart, Dymarr<int> &trimmedAuthorEnd,
			Dymarr<double> &trimmedAuthorScores,
			int titleStart, int titleEnd,
			Paper *paper)
  {
  HashedSet<int> thisPositionPrinted;
  int *selected = new int[authorStart.usedSize()];
  zero_array(selected,authorStart.usedSize());
		// 0 is unselected, 1 is selected, -1 is rejected

  rejectImpossibleAuthors(authorStart, authorEnd, paper, selected);

  for (int dex=0; dex < titleEnd; dex++)
	thisPositionPrinted.insertElement(dex);

  for (int a=0; a < authorStart.usedSize(); a++)
    {
    double maxUnselectedAuthorScore = -MAXDOUBLE;
    int currAuthor = -1;
    for (int dex=0; dex < authorStart.usedSize(); dex++)
	if (selected[dex] == 0 && maxUnselectedAuthorScore < authorScores[dex])
		{
		maxUnselectedAuthorScore = authorScores[dex];
		currAuthor = dex;
		}
    if (currAuthor == -1)
	break;

    int selectThisAuthor = 1;
    for (int dex=authorStart[currAuthor]; dex < authorEnd[currAuthor]; dex++)
	if (thisPositionPrinted.containsElement(dex))
		selectThisAuthor = 0;

    for (int preva=0; preva < authorStart.usedSize(); preva++)
	if ((selected[preva] == 1) && 
		isSameAuthor(authorStart[preva], authorEnd[preva], authorStart[currAuthor], authorEnd[currAuthor], paper))
	    selectThisAuthor = 0;

    if (selectThisAuthor)
	{     
	for (int dex=authorStart[currAuthor]; dex < authorEnd[currAuthor]; dex++)
	    thisPositionPrinted.insertElement(dex);
	selected[currAuthor] = 1;
	}
    else
	selected[currAuthor] = -1;
    }

  // now add them to the trimmed set
  assert(trimmedAuthorStart.usedSize() == 0);
  assert(trimmedAuthorEnd.usedSize() == 0);
  assert(trimmedAuthorScores.usedSize() == 0);
  for (int a=0; a < authorStart.usedSize(); a++)
    {
    assert(selected[a] == 1 || selected[a] == -1);
    if (selected[a] == -1)
	continue;

    trimmedAuthorStart.append(authorStart[a]);
    trimmedAuthorEnd.append(authorEnd[a]);
    trimmedAuthorScores.append(authorScores[a]);
    }

  delete[] selected;

  return;
  }

// orders the author entries from highest to lowest score, and
// selects them out in that order, but skipping any author that
// "intersects" a previoosly selected author.
//
// New: Also excludes anything that intersects the title. 
//   (which seemed to be a common error)
void old_fancyPrintAuthors(Dymarr<int> &authorStart, Dymarr<int> &authorEnd, 
			Dymarr<double> &authorScores, 
			int titleStart, int titleEnd,
			Paper *paper, const String &fn)
  {
  HashedSet<int> thisPositionPrinted;
  int *selected = new int[authorStart.usedSize()];
  zero_array(selected,authorStart.usedSize());
		// 0 is unselected, 1 is selected, -1 is rejected

  for (int dex=0; dex < titleEnd; dex++)
	thisPositionPrinted.insertElement(dex);

  for (int a=0; a < authorStart.usedSize(); a++)
    {
    double maxUnselectedAuthorScore = -MAXDOUBLE;
    int currAuthor = -1;
    for (int dex=0; dex < authorStart.usedSize(); dex++)
	if (selected[dex] == 0 && maxUnselectedAuthorScore < authorScores[dex])
		{
		maxUnselectedAuthorScore = authorScores[dex];
		currAuthor = dex;
		}
    assert(currAuthor != -1);

    int selectThisAuthor = 1;
    for (int dex=authorStart[currAuthor]; dex < authorEnd[currAuthor]; dex++)
	if (thisPositionPrinted.containsElement(dex))
		selectThisAuthor = 0;

    if (selectThisAuthor)
	{     
	for (int dex=authorStart[currAuthor]; dex < authorEnd[currAuthor]; dex++)
	    thisPositionPrinted.insertElement(dex);
	selected[currAuthor] = 1;
	}
    else
	selected[currAuthor] = -1;
    }

  // now, print them out
  int printedSomething = 0;
  for (int a=0; a < authorStart.usedSize(); a++)
    {
    assert(selected[a] == 1 || selected[a] == -1);
    if (selected[a] == -1)
	continue;
    if (printedSomething)
		printf(" ANDALSOSOMEONECALLED ");
		// printf("AND ");

    int yy;
    for (yy = authorStart[a]; yy < authorEnd[a] - 1; ++yy) {
      paper->words[yy].printOrig(stdout, 1);
    }
    paper->words[yy].printOrig(stdout, 0);


//    new_readText(fn, authorStart[a], authorEnd[a], stdout, 0);

//    for (int ctr=authorStart[a]; ctr < authorEnd[a]; ctr++)
//	printf("%s ", paper->words[ctr].s.as_char());
    printedSomething = 1;
    printf("(%f) ", authorScores[a]);
    }

  delete[] selected;

  return;
  }

void findTitleAndAbstractFields(Paper *paper, 
  		   classifier_t &titleStartClassifier, classifier_t &titleEndClassifier,
  		   classifier_t &abstractStartClassifier, classifier_t &abstractEndClassifier,
		   double *bestTitleStartScore_p, double *bestTitleEndScore_p,
		   int *bestTitleStart_p, int *bestTitleEnd_p,
	 	   double *bestAbstractStartScore_p, double *bestAbstractEndScore_p,
		   int *bestAbstractStart_p, int *bestAbstractEnd_p)
  {
  int bestTitleStart=0, bestTitleEnd=0;
  int bestAbstractStart=0, bestAbstractEnd=0;
  double bestTitleStartScore= -MAXDOUBLE, bestTitleEndScore = -MAXDOUBLE,  
	 bestAbstractStartScore= -MAXDOUBLE, bestAbstractEndScore = -MAXDOUBLE;
  Vector features(FEATURE_VECTOR_LEN);

  // scan for the best title and abstract starts
  for (int pos1=0; pos1 < MAX_PAPER_SCAN; pos1++)
	{
	extractFeatures(*paper, pos1, pos1, features);

	if (pos1 >= TITLESTART_SCAN_BEGIN && pos1 <= TITLESTART_SCAN_END)
	    {
	    double titleStartScore = titleStartClassifier.score(features);
	    if (titleStartScore > bestTitleStartScore)
		{
		bestTitleStartScore = titleStartScore;
		bestTitleStart = pos1;
		}
	    }

	if (pos1 >= ABSTRACTSTART_SCAN_BEGIN && pos1 <= ABSTRACTSTART_SCAN_END)
	    {
	    double abstractStartScore = abstractStartClassifier.score(features);
	    if (abstractStartScore > bestAbstractStartScore)
		{
		bestAbstractStartScore = abstractStartScore;
		bestAbstractStart = pos1;
		}
	    }
	}

  // not scan for the best title and abstract ends
  for (int pos1=0; pos1 < MAX_PAPER_SCAN; pos1++)
	{
	if (pos1 >= bestTitleStart + MIN_TITLE_LEN 
			&& pos1 <= bestTitleStart + MAX_TITLE_LEN)
	    {
	    extractFeatures(*paper, pos1, pos1, features);

	    double titleEndScore = titleEndClassifier.score(features);
	    if (titleEndScore > bestTitleEndScore)
		{
		bestTitleEndScore = titleEndScore;
		bestTitleEnd = pos1;
		}
	    }

	if (pos1 >= bestAbstractStart + MIN_ABSTRACT_LEN 
			&& pos1 <= bestAbstractStart + MAX_ABSTRACT_LEN
			&& pos1 <= ABSTRACTEND_SCAN_END)
	    {
	    extractFeatures(*paper, pos1, pos1, features);

	    double abstractEndScore = abstractEndClassifier.score(features);
	    if (abstractEndScore > bestAbstractEndScore)
		{
		bestAbstractEndScore = abstractEndScore;
		bestAbstractEnd = pos1;
		}
	    }
	}

  *bestTitleStartScore_p = bestTitleStartScore;
  *bestTitleEndScore_p = bestTitleEndScore;
  *bestTitleStart_p = bestTitleStart;
  *bestTitleEnd_p = bestTitleEnd;
  *bestAbstractStartScore_p = bestAbstractStartScore;
  *bestAbstractEndScore_p = bestAbstractEndScore;
  *bestAbstractStart_p = bestAbstractStart;
  *bestAbstractEnd_p = bestAbstractEnd;

  return;
  }

// precond: trimedAuthor{Start,End,Scores} are empty
void findAuthors(Paper *paper, classifier_t &authorClassifier, 
	int bestTitleStart, int bestTitleEnd, double authorThreshold,
	Dymarr<int> &authorStart, Dymarr<int> &authorEnd, 
	Dymarr<double> &authorScores,
	Dymarr<int> &trimmedAuthorStart, Dymarr<int> &trimmedAuthorEnd, 
	Dymarr<double> &trimmedAuthorScores)
  {
  int bestAuthorStart=0, bestAuthorEnd=0;	// single most likely author
  double bestAuthorScore= -MAXDOUBLE;
  Vector features(FEATURE_VECTOR_LEN);

  // scan for the author matches
  for (int pos1=AUTHOR_SCAN_BEGIN;  pos1 <= AUTHOR_SCAN_END; pos1++)
    for (int pos2=pos1+MIN_AUTHOR_LEN; pos2 <= pos1+MAX_AUTHOR_LEN; pos2++)
	{
	if (pos1 >= AUTHOR_SCAN_BEGIN && pos1 <= AUTHOR_SCAN_END)
	    {
	    extractFeatures(*paper, pos1, pos2, features);
	    double authorScore = authorClassifier.score(features);
	    if (authorScore > bestAuthorScore)
		{
		bestAuthorScore = authorScore;
		bestAuthorStart = pos1;
		bestAuthorEnd = pos2;
		}
	    if (authorScore > authorThreshold)
		{
		authorStart.append(pos1);
		authorEnd.append(pos2);
		authorScores.append(authorScore);
		}
	    }
	}

  // if we couldn't find a single plausible author, we'll just
  // take the most likely one
  if (authorStart.usedSize() == 0)
	{
	authorStart.append(bestAuthorStart);
	authorEnd.append(bestAuthorEnd);
	authorScores.append(bestAuthorScore);
	}

  trimAuthors(authorStart, authorEnd, authorScores, 
		trimmedAuthorStart, trimmedAuthorEnd, trimmedAuthorScores,
		bestTitleStart, bestTitleEnd, paper);

  return;
  }

int rejectPaper(Vector &scores, double *predProb_p)
  {
  double myScore = -12.98973 + 0.0296622 * scores[0] + 0.03615744 * scores[1] + 
	(-0.1105602) * scores[2] + 0.03168413 * scores[3] + 0.03856832 * scores[4] + 
	0.01731713* scores[5] + 0.005616888 * scores[6] + (-3.787901) * scores[7] + 
	3.625161* scores[8];

  double predProb = sigmoid(myScore);

//  fprintf(stderr, "Paper Scored %f\n", predProb);

  *predProb_p = predProb;

  if (predProb < 0.2)		// HERE
	return 1;	// reject
  else
	return 0;
  }

void extractInfo(FILE *outfp, const char *fn, int detailed, int concise,
			double authorThreshold, Vector &scores)
  {
  classifier_t *titleStartClassifier, *titleEndClassifier, *authorClassifier, 
		*abstractStartClassifier, *abstractEndClassifier;

  titleStartClassifier = new_loadClassifier(TITLE_START_FN);
  titleEndClassifier = new_loadClassifier(TITLE_END_FN);
  authorClassifier = new_loadClassifier(AUTHOR_FN);
  abstractStartClassifier = new_loadClassifier(ABSTRACT_START_FN);
  abstractEndClassifier = new_loadClassifier(ABSTRACT_END_FN);

  int bestTitleStart=0, bestTitleEnd=0;
  Dymarr<int> authorStart(-1), authorEnd(-1);
  Dymarr<int> trimmedAuthorStart(-1), trimmedAuthorEnd(-1);
  Dymarr<double> authorScores(-MAXDOUBLE);
  Dymarr<double> trimmedAuthorScores(-MAXDOUBLE);
  int bestAbstractStart=0, bestAbstractEnd=0;
  double bestTitleStartScore= -MAXDOUBLE, bestTitleEndScore = -MAXDOUBLE,  
	 bestAbstractStartScore= -MAXDOUBLE, bestAbstractEndScore = -MAXDOUBLE;

  Paper *paper;
  if (fn == NULL)    
    paper = new_readText(stdin);
  else
    paper = new_readText(fn);
  if (detailed)
	paper->print(outfp);
  int paperLen = paper->words.usedSize();

  findTitleAndAbstractFields(paper, 
			*titleStartClassifier, *titleEndClassifier,
			*abstractStartClassifier, *abstractEndClassifier,
			&bestTitleStartScore, &bestTitleEndScore,
			&bestTitleStart, &bestTitleEnd,
			&bestAbstractStartScore, &bestAbstractEndScore,
			&bestAbstractStart, &bestAbstractEnd);

  findAuthors(paper, *authorClassifier, bestTitleStart, bestTitleEnd, authorThreshold,
			authorStart, authorEnd, authorScores,
			trimmedAuthorStart, trimmedAuthorEnd, trimmedAuthorScores);

//  printf("%d %d\n", bestTitleEnd, bestTitleStart);
  scores.resize(9);
  scores[0] = bestTitleStartScore; 
  scores[1] = bestTitleEndScore; 
  scores[2] = bestTitleEnd-bestTitleStart;
  if (trimmedAuthorScores.usedSize() > 0)
	{
	scores[3] = max_in_arr(trimmedAuthorScores.allele(), trimmedAuthorScores.usedSize());
	scores[4] = mean_in_arr(trimmedAuthorScores.allele(), trimmedAuthorScores.usedSize());
	}
  else
	scores[3] = scores[4] = 0.0;
  scores[5] = bestAbstractStartScore; 
  scores[6] = bestAbstractEndScore;
  scores[7] = (paperLen > 0);
  scores[8] = (paperLen > 30);

  double paperPredProb;
//  if (rejectPaper(scores, &paperPredProb))
  if (trimmedAuthorScores.usedSize() == 0 || rejectPaper(scores, &paperPredProb))
	{
	fprintf(outfp, "REJECT (%f)\n", paperPredProb);
	return; 
	}

  if (concise)
	{
	for (int dex=0; dex < scores.dim(); dex++)
	    fprintf(outfp, "%f ", scores[dex]);
	fprintf(outfp,"\n%c", 1);
	}

  if (!concise)
    	fprintf(outfp, "TITLE (%f, %f): ", bestTitleStartScore, bestTitleEndScore);
    //fprintf(outfp, "TITLE: ", bestTitleStartScore, bestTitleEndScore);

  int yy1;
  for (yy1 = bestTitleStart; yy1 < bestTitleEnd - 1; ++yy1) {
    paper->words[yy1].printOrig(outfp, 1);
  }
  paper->words[yy1].printOrig(outfp, 0);

	 //new_readText(fn, bestTitleStart, bestTitleEnd, outfp, 0);
  fprintf(outfp, "\n");
  if (concise)
	putc(1, outfp);

  if (!concise)
    {
    fprintf(outfp, "AUTHORS (N): ");
    for (int a=0; a < authorStart.usedSize(); a++)
	{
	  int yy2;
	  for (yy2 = authorStart[a]; yy2 < authorEnd[a] - 1; ++yy2) {
	    paper->words[yy2].printOrig(outfp, 1);
	  }
	  paper->words[yy2].printOrig(outfp, 0);
	  
	  //new_readText(fn, authorStart[a], authorEnd[a], outfp, 0);
	if (a != authorStart.usedSize()-1)
	    fprintf(outfp, " ANDALSOSOMEONECALLED ");
	    // fprintf(outfp, "AND ");
        }
    fprintf(outfp, "\n");
    }

  if (!concise)
    fprintf(outfp, "AUTHORS (F): ");
  for (int a=0; a < trimmedAuthorStart.usedSize(); a++)
    {
      int yy3;
      for (yy3 = trimmedAuthorStart[a]; yy3 < trimmedAuthorEnd[a] - 1; ++yy3) {
	paper->words[yy3].printOrig(outfp, 1);
      }
      paper->words[yy3].printOrig(outfp, 0);
      //new_readText(fn, trimmedAuthorStart[a], trimmedAuthorEnd[a], outfp, 0);
    if (!concise)
	fprintf(outfp, " (%f) ", trimmedAuthorScores[a]);
    if (a != trimmedAuthorStart.usedSize()-1)
	fprintf(outfp, " ANDALSOSOMEONECALLED ");
	// fprintf(outfp, "AND ");
    }
  fprintf(outfp, "\n");
//  fancyPrintAuthors(authorStart, authorEnd, authorScores, 
//			bestTitleStart, bestTitleEnd, paper, fn);

  if (concise)
	putc(1, outfp);

  if (!concise)
	fprintf(outfp, "ABSTRACT (%f, %f): ", bestAbstractStartScore, bestAbstractEndScore);

  int yy4;
  for (yy4 = bestAbstractStart; yy4 < bestAbstractEnd - 1; ++yy4) {
    paper->words[yy4].printOrig(outfp, 1);
  }
  paper->words[yy4].printOrig(outfp, 0);

  //new_readText(fn, bestAbstractStart, bestAbstractEnd, outfp, 2);
  fprintf(outfp, ".\n");

  if (concise)
	{
	putc(1, outfp);
	putc('\n', outfp);
	}
  
  delete titleStartClassifier; delete titleEndClassifier; delete authorClassifier;
  delete abstractStartClassifier; delete abstractEndClassifier;
  delete paper;

  return;
  }

void extractInfo(cmd_line &c_line)
  {
  const char *fn = c_line.get_flag_arg("extract", (const char*)NULL);
  int detailed = c_line.exist_flag("detailed");
  int concise = c_line.exist_flag("concise");
  Vector scores(1);		// value ignored
  double authorThreshold = c_line.get_flag_arg("thresh", (double)DEFAULT_AUTHOR_THRESHOLD);

  c_line.assert_all_parms_used();

  if (fn == NULL) {
    extractInfo(stdout, NULL, detailed, concise, authorThreshold, scores);
    //    error("Expected -extract <filename> on command line.");
  } else {
    extractInfo(stdout, fn, detailed, concise, authorThreshold, scores);
  }

  return;
  }

void findTrimmedAuthors(Paper *paper, double authorThreshold,
			Dymarr<int> &trimmedAuthorStart,
			Dymarr<int> &trimmedAuthorEnd)
  {
  int bestTitleStart=0, bestTitleEnd=0;
  Dymarr<int> authorStart(-1), authorEnd(-1);
  Dymarr<double> authorScores(-MAXDOUBLE);
  Dymarr<double> trimmedAuthorScores(-MAXDOUBLE);
  int bestAbstractStart=0, bestAbstractEnd=0;
  double bestTitleStartScore= -MAXDOUBLE, bestTitleEndScore = -MAXDOUBLE,  
	 bestAbstractStartScore= -MAXDOUBLE, bestAbstractEndScore = -MAXDOUBLE;

  classifier_t *titleStartClassifier, *titleEndClassifier, *authorClassifier, 
		*abstractStartClassifier, *abstractEndClassifier;

  titleStartClassifier = new_loadClassifier(TITLE_START_FN);
  titleEndClassifier = new_loadClassifier(TITLE_END_FN);
  authorClassifier = new_loadClassifier(AUTHOR_FN);
  abstractStartClassifier = new_loadClassifier(ABSTRACT_START_FN);
  abstractEndClassifier = new_loadClassifier(ABSTRACT_END_FN);

  findTitleAndAbstractFields(paper, 
			*titleStartClassifier, *titleEndClassifier,
			*abstractStartClassifier, *abstractEndClassifier,
			&bestTitleStartScore, &bestTitleEndScore,
			&bestTitleStart, &bestTitleEnd,
			&bestAbstractStartScore, &bestAbstractEndScore,
			&bestAbstractStart, &bestAbstractEnd);

  findAuthors(paper, *authorClassifier, bestTitleStart, bestTitleEnd, authorThreshold,
			authorStart, authorEnd, authorScores,
			trimmedAuthorStart, trimmedAuthorEnd, trimmedAuthorScores);

  delete titleStartClassifier; delete titleEndClassifier; delete authorClassifier;
  delete abstractStartClassifier; delete abstractEndClassifier;

  return;
  }

void printLevel2AuthorInfo(Dymarr<paperSample*> &authorExample, 
			   double authorThreshold,
			   Dymarr<Paper*> &papers)
  {
  Dymarr<int> trimmedAuthorStart(-1);
  Dymarr<int> trimmedAuthorEnd(-1);
  Dymarr<Paper*> papersAuthorsUpdated(NULL);	// NULL when not updated,
						// else points back to the 
						// paper in question
  classifier_t *authorClassifier;
  authorClassifier = new_loadClassifier(AUTHOR_FN);

  for (int ctr=0; ctr < authorExample.usedSize(); ctr++)
    {
    int thisPaperDex = authorExample[ctr]->paperDex;
    if (papersAuthorsUpdated[thisPaperDex] == NULL)
      {
      int pdex=0;
      for (pdex=0; pdex < papers.usedSize(); pdex++)
	if (papers[pdex]->docNumber == thisPaperDex)
	   break;
      if (pdex >= papers.usedSize())
	{
	for (int dex=0; dex < min(papers.usedSize(),20); dex++)
	  printf("%d ", papers[dex]->docNumber);
	printf("-->%d\n", thisPaperDex);
	}
      assert(pdex < papers.usedSize());

      // printf("Adding author info for paper %d\n", pdex);

      trimmedAuthorStart.forget_all(); trimmedAuthorEnd.forget_all();
      findTrimmedAuthors(papers[pdex], authorThreshold, trimmedAuthorStart, trimmedAuthorEnd);
      for (int a=0; a < trimmedAuthorStart.usedSize(); a++)
	for (int dex=trimmedAuthorStart[a]; dex < trimmedAuthorEnd[a]; dex++)
	    {
	    assert(dex < papers[pdex]->words.usedsize());
	    papers[pdex]->words[dex].predAuthor = 1;
	    }
      setLineFeatures(*papers[pdex]);
      papersAuthorsUpdated[thisPaperDex] = papers[pdex];
      }
    Paper *thisPaper = papersAuthorsUpdated[thisPaperDex];
    assert(thisPaper != NULL);

    // okie dokie. This thingamagik has its lineFeatures set.
    double thisPredict = authorClassifier->score(authorExample[ctr]->features);
    int pos1 = authorExample[ctr]->pos1;
    double f1 = (double)thisPaper->words[pos1].sharePredAuthor
    				/ thisPaper->words[pos1].lineLen;
    int f2 = (thisPaper->words[pos1].sharePredAuthor > 0);
    printf("%f %f %d  %d\n", thisPredict, f1, f2, authorExample[ctr]->target);
    }

  delete authorClassifier; 

  return;
  }

void printInfo(cmd_line &c_line, Dymarr<paperSample*> &authorExample, 
				Dymarr<Paper*> &papers)
  {
  classifier_t *authorClassifier;
  double authorThreshold = c_line.get_flag_arg("thresh", (double)DEFAULT_AUTHOR_THRESHOLD);

  authorClassifier = new_loadClassifier(AUTHOR_FN, &c_line);

  if (c_line.get_flag_arg("printinfo", "") == String("errors"))
    {
    // show the false negatives we make on authors
    for (int ctr=0; ctr < authorExample.usedSize(); ctr++)
	{
	double thisPredict = authorClassifier->score(authorExample[ctr]->features);
	if (thisPredict < authorThreshold && authorExample[ctr]->target==1)
		{
		printf("Paper %d (%f)\n", authorExample[ctr]->paperDex, thisPredict);
		}
	}
    }
  else if (c_line.get_flag_arg("printinfo", "") == String("level2"))
    {
    printLevel2AuthorInfo(authorExample, authorThreshold, papers);
    }
  else
    {
    for (int ctr=0; ctr < authorExample.usedSize(); ctr++)
	printf("%d %f\n", authorExample[ctr]->target, 
		authorClassifier->score(authorExample[ctr]->features));
    }

  delete authorClassifier; 

  return;
  }

void printErrorRate(const char fn[], Dymarr<paperSample*> sample, int maxHobble)
  {
  classifier_t *c;

  for (int hobble=100; hobble <= maxHobble; hobble += 100)
	{
	c = new_loadClassifier(fn, NULL, hobble);
	printf("%d: ", hobble);
	int errors = 0;
	// cov = E[xy] - E[x]E[y]
	double xySum = 0, xSum=0, ySum=0;
	double xxSum=0, yySum=0;
	for (int s=0; s < sample.usedSize(); s++)
	    {
	    double score = c->score(sample[s]->features);
	    xySum += score * sample[s]->target;
	    xSum += score;
	    ySum += sample[s]->target;
	    xxSum += sqr(score);
	    yySum += sqr(sample[s]->target);
	    if (c->predict(sample[s]->features) != sample[s]->target)
		errors++;
	    }
	double varx = xxSum/sample.usedSize() - sqr(xSum/sample.usedSize());
	double vary = yySum/sample.usedSize() - sqr(ySum/sample.usedSize());
	double cov = xySum/sample.usedSize() - xSum*ySum/sqr(sample.usedSize());
	double corr = cov / sqrt(varx*vary);
	printf("%.1f(%f) ", 100 * (double)errors/sample.usedSize(),
		corr);
//	printf("%.1f(%f) ", 100 * (double)errors/sample.usedSize(),
///		xySum/sample.usedSize() - xSum*ySum/sqr(sample.usedSize()));

	delete c;
	}

  printf("\n");

  return;
  }

void printErrorRates(cmd_line &c_line, Dymarr<paperSample*> &titleStartExample, 
	Dymarr<paperSample*> &titleEndExample, Dymarr<paperSample*> &authorExample,
	Dymarr<paperSample*> &abstractStartExample, Dymarr<paperSample*> &abstractEndExample)
  {
  printf("TitleStart: ");
  printErrorRate(TITLE_START_FN, titleStartExample, 2500);

  printf("TitleEnd: ");
  printErrorRate(TITLE_END_FN, titleEndExample, 2500);

  printf("Author: ");
  printErrorRate(AUTHOR_FN, authorExample, 1500);

  printf("AbstractStart: ");
  printErrorRate(ABSTRACT_START_FN, abstractStartExample, 2500);

  printf("AbstractEnd: ");
  printErrorRate(ABSTRACT_END_FN, abstractEndExample, 2500);

  return;
  }


//-------------------------------------------------------

void testExtract(cmd_line &c_line, Dymarr<Paper*> &papers)
  {
  classifier_t *titleStartClassifier, *titleEndClassifier, *authorClassifier, 
		*abstractStartClassifier, *abstractEndClassifier;
  double authorThreshold = c_line.get_flag_arg("thresh", (double)DEFAULT_AUTHOR_THRESHOLD);

  for (int hobble=100; hobble <= 2500; hobble+=100)
	{
	titleStartClassifier = new_loadClassifier(TITLE_START_FN, NULL, hobble);
	titleEndClassifier = new_loadClassifier(TITLE_END_FN, NULL, hobble);
	// authorClassifier = new_loadClassifier(AUTHOR_FN, NULL, hobble);
	abstractStartClassifier = new_loadClassifier(ABSTRACT_START_FN, NULL, hobble);
	abstractEndClassifier = new_loadClassifier(ABSTRACT_END_FN, NULL, hobble);

	int titleStartError = 0, titleEndError = 0, abstractStartError = 0, abstractEndError = 0;
	int titleStartCount = 0, titleEndCount = 0, abstractStartCount = 0, abstractEndCount = 0;
	for (int ctr=0; ctr < papers.usedSize(); ctr++)
	    {
	    int bestTitleStart=0, bestTitleEnd=0;
	    Dymarr<int> authorStart(-1), authorEnd(-1);
	    Dymarr<int> trimmedAuthorStart(-1), trimmedAuthorEnd(-1);
	    Dymarr<double> authorScores(-MAXDOUBLE);
	    Dymarr<double> trimmedAuthorScores(-MAXDOUBLE);
	    int bestAbstractStart=0, bestAbstractEnd=0;
	    double bestTitleStartScore= -MAXDOUBLE, bestTitleEndScore = -MAXDOUBLE,  
		     bestAbstractStartScore= -MAXDOUBLE, bestAbstractEndScore = -MAXDOUBLE;

	    Paper *paper = papers[ctr];

	    findTitleAndAbstractFields(paper, 
			*titleStartClassifier, *titleEndClassifier,
			*abstractStartClassifier, *abstractEndClassifier,
			&bestTitleStartScore, &bestTitleEndScore,
			&bestTitleStart, &bestTitleEnd,
			&bestAbstractStartScore, &bestAbstractEndScore,
			&bestAbstractStart, &bestAbstractEnd);

	    // findAuthors(paper, *authorClassifier, bestTitleStart, bestTitleEnd, authorThreshold,
	    // 		authorStart, authorEnd, authorScores,
	    // 		trimmedAuthorStart, trimmedAuthorEnd, trimmedAuthorScores);

	    if (paper->titleStart != INVALID_LOCATION)
	        {
		if (bestTitleStart != paper->titleStart)
		    titleStartError++;
		titleStartCount++;
		}
	    if (paper->titleEnd != INVALID_LOCATION)
		{
		if (bestTitleEnd != paper->titleEnd)
		    titleEndError++;
		titleEndCount++;
		}
	    if (paper->abstractStart != INVALID_LOCATION)
		{
		if (bestAbstractStart != paper->abstractStart)
		    abstractStartError++;
		abstractStartCount++;
		}
	    if (paper->abstractEnd != INVALID_LOCATION)
		{
		if (bestAbstractEnd != paper->abstractEnd)
		    abstractEndError++;
		abstractEndCount++;
		}
	    }

	printf("%d Errors: %d/%d %d/%d %d/%d %d/%d \n", hobble, titleStartError, titleStartCount, titleEndError, titleEndCount,
		abstractStartError, abstractStartCount, abstractEndError, abstractEndCount);

	delete titleStartClassifier; delete titleEndClassifier; 
	// delete authorClassifier;
	delete abstractStartClassifier; delete abstractEndClassifier;
	}
	
  return;
  }

int main(int argc, char *argv[])
  {
  cmd_line c_line(argc, argv);

  start_random_number_from_cline(c_line);

  if (c_line.exist_flag("extract"))
	extractInfo(c_line);
  else
	error("Bad command line. (Could not find valid command on it.)");

  return 0;
  }

