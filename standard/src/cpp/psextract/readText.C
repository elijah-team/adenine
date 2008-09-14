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

#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>

#include "assocarr.h"
#include "dym_arr.h"
#include "readText.h"
#include "str.h"
#include "misc.h"

// #define FIRST_DIC_NAME "/l/d45/ang/store/firstnames.txt"
// #define LAST_DIC_NAME "/l/d45/ang/store/lastnames.txt"
// #define MIDDLE_DIC_NAME "/l/d45/ang/store/middlenames.txt"
// #define GENERAL_DIC_NAME "/usr/dict/words"
// #define SPECIAL_WORD_NAME "/l/d45/ang/store/wordFeatures.txt"

// #define FIRST_DIC_NAME "/afs/cs/project/learn-7/an2i/fetch/firstnames.txt"
// #define LAST_DIC_NAME "/afs/cs/project/learn-7/an2i/fetch/lastnames.txt"
// #define MIDDLE_DIC_NAME "/afs/cs/project/learn-7/an2i/fetch/middlenames.txt"
// #define GENERAL_DIC_NAME "/afs/cs/project/learn-7/an2i/fetch/dictionarywords.txt"
// #define SPECIAL_WORD_NAME "/afs/cs/project/learn-7/an2i/fetch/wordFeatures.txt"

#define FIRST_DIC_NAME "firstnames.txt"
#define LAST_DIC_NAME "lastnames.txt"
#define MIDDLE_DIC_NAME "middlenames.txt"
// #define GENERAL_DIC_NAME "/usr/dict/words"
#define GENERAL_DIC_NAME "dictionarywords.txt"
#define SPECIAL_WORD_NAME "wordFeatures.txt"

int g_showMatches = 0;		// if true, prints out the locations of 
				// where it finds titles, authors, etc in the
				// training examples

//-------------------------------------------------------
// Misc functions 

inline int isSeparatorChar(int c)
  {
//  return !isalnum(c) && (c != '-') && (c != '$');
  return !isalnum(c) && (c != '$');
  }

inline int isSeparatingPunctuation(int c)
  {
  return (c == '.') || (c == ',') || (c == ';') || (c == ':')
      || (c == '\"') || (c == '`');  // '\'';
  }

// advances fp to the first non-whitespace character,
// and all lines starting with "#". (Taken to be comment
// lines.)
// Assumes it was called starting at a new line.
//
// Meant to be used for reading in the training data.
void skipWhitespaceAndComments(FILE *fp)
  {
  int c, lastChar = '\n';

  while ((c = fgetc(fp)) != EOF)
    {
    if (c == '#' && lastChar == '\n')
	{
	while ((c=fgetc(fp)) != '\n' && c != EOF);
	if (c == EOF)
		break;
	}

    lastChar = c;
    if (!isspace(c))
	break;
    }

  if (c != EOF)
	ungetc(c, fp);

  // assert(!isspace(peekChar(fp)));

  return;
  }

HashedSet<String> *new_dictionary(const char dictFn[])
  {
  HashedSet<String> *dict = new HashedSet<String>;
  FILE *fp;
  char buff[4096];
  String s;

  fp = safe_fopen(dictFn, "rt");
  while (fscanf(fp, "%s", buff) == 1)
	{
	s = buff;
	if (dict->containsElement(s))
	    warn((String("new_dictionary") + dictFn + " contains duplicate word " + s).as_char());
	else
	    dict->insertElement(s);
	}
  fclose(fp);

  return dict;
  }

AssocArray<int,String> *new_specialWordSet(const char fn[])
  {
  FILE *fp = safe_fopen(fn, "rt");
  AssocArray<int,String> *aa = new AssocArray<int,String>(NOT_SPECIAL_WORD);
  char buff[4096];
  int setNumber, lastSetNumber;
  String s;

  for (;;)
    {
    int err = fscanf(fp, "%d%s", &setNumber, buff);
    if (err == EOF)
	break;
    if (err != 2)
	error("new_specialWordSet: error while reading file.");

    s = buff;
    s.setToLower();

    // printf("%d %s\n", setNumber, s.as_char());

    if ((*aa)[s] != NOT_SPECIAL_WORD)
	error(("new_specialWordSet: duplicate \"" + s + "\"").as_char());
    (*aa)[s] = setNumber;
    assert(setNumber >= 0 && setNumber < N_SPECIAL_WORD_SETS);
    }
  fclose(fp);

  return aa;
  }

// sets s, height, width, 
// firstCap, anyCap, singleAlphabet;
// isFirst, isMiddle, isLast, inDict;
void setWord(word &thisWord, const String &s, int height, int width)
  {
  static HashedSet<String> *firstDic = NULL;
  static HashedSet<String> *lastDic = NULL;
  static HashedSet<String> *middleDic = NULL;
  static HashedSet<String> *generalDic = NULL;
  static AssocArray<int,String> *specialWordSet = NULL;

  thisWord.height = height;
  thisWord.width = width;

  if (s[0] == 0)
	error("setWord: got empty string");

  if (firstDic == NULL)
	{
	assert(lastDic==NULL && middleDic==NULL 
		&& generalDic==NULL && specialWordSet == NULL);
	firstDic = new_dictionary(FIRST_DIC_NAME);
	lastDic = new_dictionary(LAST_DIC_NAME);
	middleDic = new_dictionary(MIDDLE_DIC_NAME);
	generalDic = new_dictionary(GENERAL_DIC_NAME);
	specialWordSet = new_specialWordSet(SPECIAL_WORD_NAME);
	}

  thisWord.orig = s;
  thisWord.s = toLower(s);
  thisWord.isFirst = firstDic->containsElement(thisWord.s);
  thisWord.isMiddle = middleDic->containsElement(thisWord.s);
  thisWord.isLast = lastDic->containsElement(thisWord.s);
  thisWord.inDict = generalDic->containsElement(s);	// s, not lowers for this one
  thisWord.specialWordNum = (*specialWordSet)[thisWord.s];

  thisWord.firstCap= 1;
  thisWord.anyCap = 0;
  thisWord.numeric = 1;

  if (s[1] == 0)
	thisWord.singleAlphabet = 1;
  else
	thisWord.singleAlphabet = 0;

  if (!isdigit(s[0]))
	thisWord.numeric = 0;

  if (!isupper(s[0]))
	thisWord.firstCap = 0;
  else
	thisWord.anyCap = 1;

  for (int dex=1; s[dex] != 0; dex++)
	{
	if (isupper(s[dex]))
	    { thisWord.firstCap=0; thisWord.anyCap = 1; }
	if (!isdigit(s[dex]))
	    thisWord.numeric = 0;
	}

  return;
  }

//-------------------------------------------------------
// from struct word

void word::addsep(char c) {
  seps += c;
}

void word::printOrig(FILE *fp, int sep) {
  if (sep) 
    fprintf(fp, "%s%s", orig.as_char(), seps.as_char());
  else
    fprintf(fp, "%s", orig.as_char());
}

void word::print(FILE *fp) 
  { 
  // printf("%d %d: %s\n", height, width, s.as_char()); 
  fprintf(fp,"fc:%d ac:%d sa:%d  if:%d im:%d il:%d id:%d nu:%d  spe:%2d %s\n", 
	  (int)firstCap, (int)anyCap, (int)singleAlphabet, (int)isFirst, (int)isMiddle, 
		(int)isLast, (int)inDict, (int)numeric, (int)specialWordNum,
				s.as_char()); 
  }
  
//-------------------------------------------------------
// from class Paper 

// returns true if phrase matches arr staring at startLoc
inline int matchHere(Dymarr<String> &phrase, Dymarr<word> &arr, int startLoc)
  {
  int dex1, dex2;

  if (phrase.usedsize() == 0)
	error("matchHere: given empty phrase");

  for (dex1=0, dex2 = startLoc; dex1 < phrase.usedsize(); dex1++, dex2++)
	{
	assert(phrase[dex1] != "");	// else we probably screwed up an 
					// access somewhere
	if (dex2 >= arr.usedsize())
		return 0;
	assert(arr[dex2].s != "");

	if (phrase[dex1] != arr[dex2].s)
		return 0;
	}

  return 1;
  }

// sets titleStart, etc and nauthors
void Paper::matchToDoc(DocInfo &doc)
  {
  int ctr;

  docNumber = doc.docNumber;

  // title 
  if (doc.title.usedsize() == 0)
	{
  	titleStart = INVALID_LOCATION;
  	titleEnd = INVALID_LOCATION;
	}
  else
	{
	for (ctr=0; ctr < words.usedsize(); ctr++)
	    if (matchHere(doc.title, words, ctr))
		{ 
		titleStart = ctr; 
		titleEnd = ctr + doc.title.usedsize();
		if (g_showMatches)
		    printf("TITLE %d %d\n", ctr, doc.title.usedsize());
		break; 
		}
        if (ctr == words.usedsize())
		{
		warn(("matchToDoc: Unable to find title for "
				+as_string(doc.docNumber)).as_char());
		titleStart = INVALID_LOCATION;
		titleEnd = INVALID_LOCATION;
		}
	}
	   
  // authors 
  nauthors = doc.nauthors;
  if (nauthors > 0)
    {
    authorsStart = new int[nauthors];
    authorsEnd = new int[nauthors];
    }
  for (int a=0; a < nauthors; a++)
    {
    assert(doc.authors[a].usedsize() > 0);
    for (ctr=0; ctr < words.usedsize(); ctr++)
	if (matchHere(doc.authors[a], words, ctr))
	    { 
	    authorsStart[a] = ctr; 
            authorsEnd[a] = ctr + doc.authors[a].usedsize(); 
	    if (g_showMatches)
		printf("AUTHOR %d %d\n", ctr, doc.authors[a].usedsize());
	    break; 
	    }
    if (ctr == words.usedsize())
	{
	warn(("matchToDoc: Unable to find authors for "
				+as_string(doc.docNumber)).as_char());
	authorsStart[a] = INVALID_LOCATION;
	authorsEnd[a] = INVALID_LOCATION;
	}
    }

  // abstractstart 
  if (doc.summaryStartType == SUMMARY_INTRO || doc.summaryStartType == SUMMARY_NONE)
  	abstractStart = INVALID_LOCATION;
  else
	{
	assert(doc.summaryStartType == SUMMARY_ABSTRACT);
	assert(doc.abstractStart.usedsize() > 0);
	for (ctr=0; ctr < words.usedsize(); ctr++)
	    if (matchHere(doc.abstractStart, words, ctr))
		{ 
		abstractStart = ctr; 
		if (g_showMatches && doc.summaryEndType == SUMMARY_ABSTRACT)
		    printf("ABSTRACTSTART %d \n", ctr);
		break; 
		}
    	if (ctr == words.usedsize())
		{
		warn(("matchToDoc: Unable to find abstractstart for "
				+as_string(doc.docNumber)).as_char());
		abstractStart = INVALID_LOCATION;
		}
	}

  // abstractend
  if (doc.summaryEndType == SUMMARY_INTRO || doc.summaryEndType == SUMMARY_NONE)
  	abstractEnd = INVALID_LOCATION;
  else
	{
	assert(doc.summaryEndType == SUMMARY_ABSTRACT);
	assert(doc.abstractEnd.usedsize() > 0);
	for (ctr=max(abstractStart,0); ctr < words.usedsize(); ctr++)
	    if (matchHere(doc.abstractEnd, words, ctr))
		{ 
		abstractEnd = ctr + doc.abstractEnd.usedsize();
		if (g_showMatches)
		    printf("ABSTRACTEND %d \n", ctr+ doc.abstractEnd.usedsize());
		break; 
		}
    	if (ctr == words.usedsize())
		{
		warn(("matchToDoc: Unable to find abstractend for "
				+as_string(doc.docNumber)).as_char());
		abstractEnd = INVALID_LOCATION;
		}
	}

  return;
  }


void separatorInfo::print(FILE *fp)
  { 
//  printf("Sep: %s%s%s%s%s%s%s%s%s%s%s%s%s (%d)\n",
//		bigSpace?"BigSpace ":"", 
//		newline?"NL ":"", 
//		comma?", ":"", 
//		period?". ":"", 
//		semicolon?"; ":"", 
//		colon?": ":"", 
//		paren?"( ":"", 
//		quote?"\" ":"", 
//		newPara?"newPara ":"", 
//		wrapNL?"wrapNL":"", 
//		otherPunct?"otherPunc ":"", 
//		other?"other ":"", 
//		fontChange?"fontChange ":"",
//		(int)fontChange );

  fprintf(fp,"fc:%d bs:%d nl:%d wnl:%d np:%d ,:%d .:%d \n",
		(int)fontChange, (int)bigSpace, (int)newline, 
			(int)wrapNL, (int)newPara, (int)comma, (int)period);
  }

void Paper::print(FILE *fp)
  {
//  for (int ctr=0; ctr < words.usedsize(); ctr++)
  for (int ctr=0; ctr < min(words.usedsize(), 200); ctr++)
	{
	separators[ctr].print(fp);

	if (ctr == titleStart)
		fprintf(fp, "TITLESTART->");
	if (ctr == titleEnd)
		fprintf(fp, "TITLEEND->");
	for (int a=0; a < nauthors; a++)
		if (ctr == authorsStart[a])
			fprintf(fp, "AUTHORSTART[%d]->", a);
	for (int a=0; a < nauthors; a++)
		if (ctr == authorsEnd[a])
			fprintf(fp, "AUTHOREND[%d]->", a);
	if (ctr == abstractStart)
		fprintf(fp, "ABSTRACTSTART->");
	if (ctr == abstractEnd)
		fprintf(fp, "ABSTRACTEND->");

	words[ctr].print(fp);
	}
 
  return;
  }

//-------------------------------------------------------
// For reading text

// font change, big-space, 
//
// Reads from '<' util '>'
int readTextCommand(FILE *fp, int *param1, int *param2)
  {
  char commandBuff[4096];
  int commandBuffDex = 0;
  int c, command;

  // read <<<
  c = fgetc(fp); assert(c == '<');

  // read command
  while ((c = fgetc(fp)) != ' ')
    {
    if (c == EOF)		// shouldn't happen, unless gs 
				// coredumped in the middle of a command
	return COMMAND_IGNORE;
    commandBuff[commandBuffDex++] = c;
    }
  commandBuff[commandBuffDex] = 0;

  if (!strcmp(commandBuff, "FONTCHANGE"))
	{
	fscanf(fp, "%d", param1);
	fscanf(fp, "%d", param2);
	fscanf(fp, "%s", commandBuff);	// font name; ignored
	command = COMMAND_FONTCHANGE;
	}
  else if (!strcmp(commandBuff, "BIGSPACE"))
	command = COMMAND_BIGSPACE;
  else if (!strcmp(commandBuff, "NEWPARA"))
	command = COMMAND_NEWPARA;
  else if (!strcmp(commandBuff, "WRAPAROUNDNEWLINE"))
	command = COMMAND_WRAPAROUNDNL;
  else
	error("Unknown command.");

  while ((c=getc(fp)) != '>' && c != EOF);

  return command;
  }

inline void setSepInfo(int c, separatorInfo &sepInfo)
  {
  switch (c)
	{
	case ' ':
	case '\t': sepInfo.space = 1; break;
	case '.': sepInfo.period = 1; break;
	case ',': sepInfo.comma = 1; break;
	case '\n': sepInfo.newline = 1; break;
	case ';': sepInfo.semicolon = 1; break;
	case ':': sepInfo.colon = 1; break;
	case '(':  
	case ')': sepInfo.paren = 1; break;
	case '\"': 
	case '`': sepInfo.quote = 1; break;
	case '?':
	case '!': sepInfo.otherPunct = 1; break;
	default: sepInfo.other = 1;
	}

  return;
  }

// sets share7, share14, share19, and lineLen
//
// NEW: Also sharePredAuthor
void setLineFeatures(Paper &paper)
  {
  const int MAX_SHARE_VAL = 127;	// largest signed char 
					// (actually can go up to 255 
					//   w/unsigned, but hey)
  int sharePredAuthor, share7, share14, share19;
  int lineLen;

  if (paper.words.usedSize() == 0)
	return;

  int lineStart=0;
  while (lineStart < paper.words.usedSize())
    {
    int lineEnd = lineStart+1;
    while (lineEnd < paper.words.usedSize()
		&& !paper.separators.peek(lineEnd).newline
		&& !paper.separators.peek(lineEnd).newPara
		&& !paper.separators.peek(lineEnd).wrapNL)
	lineEnd++;

    // we now have a line that starts at lineStart and ends at
    // lineEnd-1 
    assert(lineStart == 0
		|| paper.separators.peek(lineStart).newline
		|| paper.separators.peek(lineStart).newPara
		|| paper.separators.peek(lineStart).wrapNL);
    assert(lineEnd == paper.words.usedSize() 
		|| paper.separators.peek(lineEnd).newline
		|| paper.separators.peek(lineEnd).newPara
		|| paper.separators.peek(lineEnd).wrapNL);
    assert(lineStart < paper.words.usedSize());

    // count the occurrences of 14 and 19 on this line
    lineLen = lineEnd - lineStart;
    assert(lineLen > 0);
    sharePredAuthor = share7 = share14 = share19 = 0;
    for (int dex=lineStart; dex < lineEnd; dex++)
	{
	sharePredAuthor += (paper.words.peek(dex).predAuthor);
	share7 += (paper.words.peek(dex).specialWordNum == 7);
	share14 += (paper.words.peek(dex).specialWordNum == 14);
	share19 += (paper.words.peek(dex).specialWordNum == 19);
	}
    sharePredAuthor = min(sharePredAuthor, MAX_SHARE_VAL);
    share7 = min(share7, MAX_SHARE_VAL);
    share14 = min(share14, MAX_SHARE_VAL);
    share19 = min(share19, MAX_SHARE_VAL);

    // set the line-based attributes for the words on this line
    for (int dex=lineStart; dex < lineEnd; dex++)
	{
	paper.words[dex].sharePredAuthor = sharePredAuthor;
	paper.words[dex].share7 = share7;
	paper.words[dex].share14 = share14;
	paper.words[dex].share19 = share19;
	paper.words[dex].lineLen = lineLen;
	}

    lineStart = lineEnd;
    }

  return;
  }

// if printOnlyStart and printOnlyEnd are not -1. 
// then it reads and returns a paper.
// Otherwise, it reads the paper only to print
// from printOnlyStart to printOnlyEnd-1, and then
// returns NULL
Paper *new_readText(FILE *fp, int printOnlyStart, int printOnlyEnd, 
		FILE *outfp, int nNewParasAllowed)
  {
  Paper *paper = new Paper;
  int currWordDex=0;
  char currString[4096];
  int currStringDex=0;
  int readingWord;
  fontInfo currFontInfo, oldFontInfo;
		// oldFontInfo is what you started the current word with
  separatorInfo sepInfo;
  int c;
  int lastPrintedChar=0;	// used only for printOnly{Start,End}
  int justEncounteredNewPara;
  int nNewParasPrinted = 0;

  assert((printOnlyStart== -1) == (printOnlyEnd== -1));	// either both or neither
							// should be -1
  assert((printOnlyStart== -1) == (outfp == NULL));

  readingWord = 0;
  justEncounteredNewPara = 0;

  while ((c=fgetc(fp)) != EOF)
    {
    if (currWordDex >= MAX_PAPER_READ)
	break;

    if (printOnlyStart != -1 && currWordDex >= printOnlyEnd 
		&& !readingWord && !isSeparatorChar(c))
			// I think this means we're about to go 
			// on to the next word (past printOnlyEnd), 
			// which we want to skip completely
	{
	delete paper;
	return NULL;
	}

    if (currWordDex == printOnlyStart)
	justEncounteredNewPara = 0;
    if ( printOnlyStart != -1 && 
		(currWordDex > printOnlyStart
		 || (currWordDex == printOnlyStart && (readingWord || !isSeparatorChar(c)) )) )
	{
	// If we get here, it means we've either started reading the stuff we're
	// supposed to print, or the current character c is the very first character
	// of it.
	if ( ((currWordDex == printOnlyEnd-1 && readingWord)
	       || (currWordDex == printOnlyEnd && !readingWord)) 
		&& isSeparatorChar(c)
		&& !isspace(c) && c != '.' && c != '.' && c != '!' && c != '?'
		&& c != '(' && c != ')' && c != '[' && c != ']' && c != '{' && c != '}'
		&& c != '%' && c != '\'' && c != '`' && c != '*' && c != '&' && c != '#'
		&& c != '"')
	    {
	    1;		// nop: skip (there's an unusual separator character
			// just after the last word we're printing
	    }
	else if (c == '\n' && !isspace(lastPrintedChar))
	    {
	    putc(' ', outfp);
	    lastPrintedChar = ' ';
	    }
	else if (c != '\n' && c != '\001')
	    {
	    if (!isspace(c))
		{
		if (justEncounteredNewPara && nNewParasPrinted < nNewParasAllowed)
		    {
		    fprintf(outfp, "<p>\n");
		    nNewParasPrinted++;
		    }
		justEncounteredNewPara = 0;
		}

	    if (c == '<')
		fprintf(outfp, "&lt");
	    else if (c == '>')
		fprintf(outfp, "&gt");
	    else if (c != ' ' || lastPrintedChar != ' ')
		putc(c, outfp);
	    lastPrintedChar = c;
	    }
	
	}

    if (c == '\001')	// our escape character
	{
	int param1, param2;
	switch (readTextCommand(fp, &param1, &param2))
	    {
	    case COMMAND_FONTCHANGE: currFontInfo.height = param1;
	    			     currFontInfo.width = param2; 
					sepInfo.fontChange=1; break;
	    case COMMAND_BIGSPACE: assert(!readingWord); 
					sepInfo.bigSpace=1; break;
	    case COMMAND_NEWPARA: assert(!readingWord); 
					justEncounteredNewPara = 1; 
					sepInfo.newPara=1; break;
	    case COMMAND_WRAPAROUNDNL: assert(!readingWord);
					sepInfo.wrapNL=1; break;
	    case COMMAND_IGNORE: break;
	    default: internal_error("91453");
	    }
	}
    else if (isSeparatorChar(c))
	{
	setSepInfo(c, sepInfo);

	if (currStringDex != 0)
	    {
	    assert(readingWord);
	    currString[currStringDex] = 0;

	    setWord(paper->words[currWordDex], currString,
			    oldFontInfo.height, oldFontInfo.width);

	    currWordDex++;
	    currStringDex=0;
	    }
	
	if (currWordDex > 0) {
	    paper->words[currWordDex - 1].addsep(c);
	}

	readingWord = 0;
	}
    else  // is not separator
	{
	if (readingWord)
	    {
	    currString[currStringDex++] = c;
	    }
	else
	    {
	    // starting to read a new word 

	    assert(currStringDex == 0);
	    readingWord = 1;

	    oldFontInfo = currFontInfo;
	    // note state changes here
	    paper->separators[currWordDex] = sepInfo;
	    sepInfo.zeroIt();
	    currString[currStringDex++] = c;
	    }
	}
    }

  if (printOnlyStart != -1)
	{
	delete paper;
	return NULL;
	}

  setLineFeatures(*paper);

  return paper;
  }

Paper *new_readText(const String &fn, int printOnlyStart, int printOnlyEnd, FILE *outfp,
			int nNewParasAllowed)
  {
  FILE *fp = safe_fopen(fn.as_char(), "rt");
  Paper *paper = new_readText(fp, printOnlyStart, printOnlyEnd, outfp, nNewParasAllowed);
  fclose(fp);

  return paper;
  }
  
//-------------------------------------------------------
// For reading training samples

// i.e. if  startChars="<", endChars=">", sepChars=" ", then 
// reads "<FOO 123 BAR>  \n" into the 3 string sentence "FOO", "123", "BAR"
// (Note it does read to the EOL.)
// Precond: endChars and sepChars should be disjoint sets.
int readCommand(FILE *fp, Sentence &params, 
		char *startChars, char *sepChars, char *endChars, 
		int ignoreMultipleSeps=1)
  {
  int c;
  char buff[4096];
  int dex; 

  params.makeEmpty();

  // read and ignore startChars
  c = fgetc(fp);
  if (!strchr(startChars, c))
	error(("readCommand: did not see startChar (saw '" + String((char)c) + "'=" 
				+ as_string((int)c) + ")\n").as_char());

  dex=0;
  while (c != EOF && !strchr(endChars, c))
    {
    dex=0;
    while ((c = fgetc(fp)) != EOF && !strchr(endChars, c) && !strchr(sepChars, c))
	buff[dex++] = c;
    buff[dex] = 0;
    if (!ignoreMultipleSeps || dex > 0)
	params.append(buff);
    }

  if (c == EOF)
    error("readCommand: unexpected EOF");
  assert(strchr(endChars, c));

  while ((c=fgetc(fp)) != '\n')
	{
	if (!isspace(c))
	   error(("Read command: Garbage at end of command? ("
			+ String((char)c) + ")\n").as_char());
	}

  return params.nwordsVal();
  }	

// returns 1 if "unusual"
// reads in something like <ABSTRACTSTART> followed by 
// a chunk of text until the next line that starts with "<",
// and putting the text into "text". If it sees 
// "UNUSUAL" in the comman (i.e. <ABSTRACTSTART UNUSUAL>),
// it doesn't read any text, and just returns.
int readTextChunk(FILE *fp, String &command, Dymarr<String> &text)
  {
  Sentence params;
  int c, lastChar;
  char buff[4096];
  int index, wordsRead;
  int count;

  count = readCommand(fp, params, "<", " ", ">", 1);
  command = params[0];

  if (count >= 2)
	{
	if (params[1] != "UNUSUAL")
		error(("readTextChunk: Unusual Token" + params[0]).as_char());

	return 1;
	}
  else
	{
	lastChar = 0;
	index=wordsRead=0;
	// read the text chunk here 
	while (((c=fgetc(fp)) != EOF) && !(c == '<' && lastChar=='\n'))
	    {
	    if (isSeparatorChar(c))
		{
		if (index == 0)
			{ lastChar=c; continue; }
		buff[index]=0;
		text[wordsRead] = buff;
		text[wordsRead].setToLower();
		wordsRead++;
		index=0;
		// printf("%s ", buff);
		}
	    else
		buff[index++] = c;
	    lastChar=c;
	    }

	if (c == EOF)
	    error("readTextChunk: UnexpectedEOF");
	assert(c=='<');
	safe_ungetc(c, fp);
	}

  return 0;
  }

Dymarr<String>* new_readAuthors(FILE *fp, int *nauthors)
  {
  const int MAXAUTHORS = 20;
  int c;
  Dymarr<String> *authors = new Dymarr<String>[MAXAUTHORS](emptyString);
  char buff[4096]; 
  int index, wordsRead;

  *nauthors=0;
  while (((c = fgetc(fp)) != EOF) && c != '<')
    {
    safe_ungetc(c, fp);
    assert(*nauthors < MAXAUTHORS);

    index=wordsRead=0;
    while ((c=fgetc(fp)) != EOF)
        {
        if (isSeparatorChar(c))
	    {
	    if (index != 0)
		{
		buff[index]=0;
		authors[*nauthors][wordsRead] = buff;
		authors[*nauthors][wordsRead].setToLower();
		wordsRead++;
		index=0;
		}
	    }
	else
	    buff[index++] = c;
	if (c == '\n')
	    break;
	}
    if (wordsRead > 4)
	warn(("Authors with >4 words in name? " 
		+ authors[*nauthors][0] + " " 
		+ authors[*nauthors][1] + " " 
		+ authors[*nauthors][2] + " " 
		+ authors[*nauthors][3] + " " 
		+ authors[*nauthors][4] + "...\n").as_char());

    if (wordsRead == 0)
	error("new_readAuthors: Extra newline in training data?");
    (*nauthors)++;
    }
  if (c == EOF)
	error("Unexpected EOF");
  safe_ungetc(c, fp);

  Dymarr<String> *retAuthors = new Dymarr<String>[*nauthors](emptyString);
  for (int ctr=0; ctr < *nauthors; ctr++)
	retAuthors[ctr] = authors[ctr];

  delete[] authors;

  return retAuthors;
  }

// Precond: just read a "<PAPER xxx>", and paperNumber==xxx
// reads to <ENDPAPER xxx>.
DocInfo *new_readPaper(int paperNumber, FILE *fp)
  {
  DocInfo *doc = new DocInfo;
  int count, unusual;
  String command;
  Sentence params;

  doc->docNumber = paperNumber;

  // printf("Reading Doc %d\n", paperNumber);

  skipWhitespaceAndComments(fp);

  // title ----------------------------------------
  unusual = readTextChunk(fp, command, doc->title);
  assert(!unusual || doc->title.usedsize()==0);
  if (command != "TITLE")
	error(("readPaper: Expected TITLE, saw " + command).as_char());

  // authors ----------------------------------------
  count = readCommand(fp, params, "<", " ", ">", 1);
  if (params[0] != "AUTHORS")
	error(("readPaper: Unusual Token" + params[0]).as_char());
  if (count > 2)
	{
	if (params[1] != "UNUSUAL")
		error(("readPaper: Unusual Token" + params[0]).as_char());
	}
  else
	{
	doc->authors = new_readAuthors(fp, &doc->nauthors);
	}

  // abstractstart ----------------------------------------
  unusual = readTextChunk(fp, command, doc->abstractStart);
  assert(!unusual || doc->abstractStart.usedsize()==0);
  if (command == "ABSTRACTSTART")
	doc->summaryStartType = SUMMARY_ABSTRACT;
  else if (command == "INTROSTART")
	doc->summaryStartType = SUMMARY_INTRO;
  else
	error(("readPaper: In doc " + as_string(paperNumber) 
	      + " Expected {ABSTRACT,INTRO}START, saw " + command).as_char());
  if (unusual)
	doc->summaryStartType = SUMMARY_NONE;

  // abstractend ----------------------------------------
  unusual = readTextChunk(fp, command, doc->abstractEnd);
  assert(!unusual || doc->abstractEnd.usedsize()==0);
  if (command == "ABSTRACTEND")
	doc->summaryEndType = SUMMARY_ABSTRACT;
  else if (command == "INTROEND")
	doc->summaryEndType = SUMMARY_INTRO;
  else
	error(("readPaper: In doc " + as_string(paperNumber) 
	       + " Expected {ABSTRACT,INTRO}END, saw " + command).as_char());
  if (unusual)
	doc->summaryEndType = SUMMARY_NONE;

  // endpaper ----------------------------------------
  count = readCommand(fp, params, "<", " ", ">", 1);
  if (count != 2 || params[0] != "ENDPAPER")
	error(("Expected ENDPAPER ("+params[0]+")").as_char());
  int endPaperNumber = atoi(params[1].as_char());
  if (endPaperNumber != paperNumber)
	error(("Paper numbers don't match: " + as_string(paperNumber) 
				+ ", " + as_string(endPaperNumber)).as_char());

  return doc;
  }

/* <PAPER 051>
<TITLE>
Recognition Algorithms for the Loom Classifier
<AUTHORS>
Robert M. MacGregor 
David Brill
<ABSTRACTSTART>
Most of today's terminological representation systems implement hybrid
<ABSTRACTEND>
inference modes is likely 
to be more useful than one that offers only a single style of 
reasoning.
<ENDPAPER 051>
*/


// reads to EOF
DocInfo **new_readDocInfo(FILE *fp, int *nDocsReadp)
  {
  Dymarr<DocInfo*> docInfop(NULL);
  int nDocsRead; 
  Sentence st;
  int cmdlen;

  nDocsRead = 0;
  do
    {
    skipWhitespaceAndComments(fp);
    assert(peekChar(fp) == '<');
    cmdlen = readCommand(fp, st, "<", " ", ">", 1);
    if (st[0] == "PAPER")
	{
	assert(cmdlen==2); 
	int paperNumber = atoi(st[1].as_char()); 
	docInfop[nDocsRead] = new_readPaper(paperNumber, fp);	
	nDocsRead++;
	}
    else if (st[0] == "BAD-COREDUMP" || st[0] == "BAD-ERROR"
	     || st[0] == "REJECT-EMPTY" || st[0] == "REJECT-FOREIGN"
	     || st[0] == "REJECT-OTHER" || st[0] == "REJECT-BADPOSTSCRIPT"
	     || st[0] == "REJECT-UNUSUAL" || st[0] == "REJECT-REVERSED" 
	     || st[0] == "SKIP")
	{
	assert(cmdlen>= 2); int paperNumber = atoi(st[1].as_char());
	// printf("Skipping %d because of %s\n", paperNumber, st[0].as_char());
	}
    else
	error(("new_readDocInfo: Expected \"PAPER\". Bad Token: "
				+st[0]).as_char());
    skipWhitespaceAndComments(fp);
    }
  while (peekChar(fp) != EOF);

  // put things into a nice array for returning
  *nDocsReadp = nDocsRead;

  DocInfo **retval;
  retval = new DocInfo*[nDocsRead];
  for (int ctr=0; ctr < nDocsRead; ctr++)
	retval[ctr] = docInfop[ctr];

  return retval;
  }

DocInfo **new_readDocInfo(const char *fn, int *nDocsReadp)
  {
  FILE *fp = safe_fopen(fn, "rt");
  DocInfo **retval = new_readDocInfo(fp, nDocsReadp);
  fclose(fp);
  return retval;
  }

void printSample(DocInfo **samples, int nsamples)
  {
  printf("%d samples:\n", nsamples);

  for (int ctr=0; ctr < nsamples; ctr++)
    {
    printf("Paper %d ------------\n", samples[ctr]->docNumber);
    printf("TITLE: ");
    for (int dex=0; dex < samples[ctr]->title.usedsize(); dex++)
	printf("%s ", samples[ctr]->title[dex].as_char());
    printf("\nAUTHORS: ");
    for (int a=0; a < samples[ctr]->nauthors; a++)
	{
	for (int dex=0; dex < samples[ctr]->authors[a].usedsize(); dex++)
		printf("%s ", samples[ctr]->authors[a][dex].as_char());
	if (a != samples[ctr]->nauthors-1)
		printf("AND ");
	}
    printf("\nABSTRACTSTART: ");
    for (int dex=0; dex < samples[ctr]->abstractStart.usedsize(); dex++)
	printf("%s ", samples[ctr]->abstractStart[dex].as_char());
    printf("\nABSTRACTEND: ");
    for (int dex=0; dex < samples[ctr]->abstractEnd.usedsize(); dex++)
	printf("%s ", samples[ctr]->abstractEnd[dex].as_char());
    printf("\n");
    }

  return;
  }

// gets the training data filenames, returns then in a
// hashtable, indexed by the number of the training sample
void findTxtFiles(const char txtFilesDir[], Dymarr<String> &txtFiles)
  {
  DIR *dirp;
  struct dirent *direntp;
  char buff[10];
  int dex;

  txtFiles.forget_all();

  dirp = opendir(txtFilesDir);
  if (dirp == NULL)
	error("opendir failed");

  while ((direntp = readdir(dirp)) != NULL)
	{
	if (direntp->d_name == String(".") || direntp->d_name == String(".."))
		continue;
	for (dex=0; direntp->d_name[dex] != '-'; dex++)
	    {
	    buff[dex] = direntp->d_name[dex];
	    assert(dex < 8);
	    }
	buff[dex] = 0;
	if (dex == 0)
	    error(("Bad filename? \"" + String(direntp->d_name) + "\"\n").as_char());
	int paperNumber = atoi(buff);
	assert(txtFiles[paperNumber] == "");
	txtFiles[paperNumber] = String(txtFilesDir) + "/" + direntp->d_name;
        }

  closedir(dirp);

  return;
  }


