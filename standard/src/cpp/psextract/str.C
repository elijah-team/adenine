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

#include <ctype.h>
#include <stdio.h>
#include <assert.h>

//#include "dym_arr.h"
#include "str.h"

void String::setToLower(void)
  {
  for (int dex=0; str[dex] != 0; dex++)
	str[dex] = tolower(str[dex]);
  return;
  }

void String::setToUpper(void)
  {
  for (int dex=0; str[dex] != 0; dex++)
	str[dex] = toupper(str[dex]);
  return;
  }

String toLower(const String &s) 
  {
  int len = s.len();
  String lowers(s.len()+1);
  
  for (int ctr=0; ctr < len; ctr++)
	lowers[ctr] = tolower(s[ctr]);
  lowers[len] = 0;

  return lowers;
  }

String toUpper(const String &s) 
  {
  int len = s.len();
  String upper(s.len()+1);
  
  for (int ctr=0; ctr < len; ctr++)
	upper[ctr] = toupper(s[ctr]);
  upper[len] = 0;

  return upper;
  }

Sentence::Sentence(void) 
  {
  const int INIT_SIZE = 6;

  arr = new String*[INIT_SIZE];
  nwords = 0;
  arrSize = INIT_SIZE;

  }

Sentence::~Sentence(void)
  {
  for (int ctr=0; ctr < nwords; ctr++)
	delete arr[ctr];
  delete[] arr;
  }

void Sentence::makeEmpty(void)
  {
  for (int ctr=0; ctr < nwords; ctr++)
	delete arr[ctr];
  nwords = 0;
  }

void Sentence::append(const String &s)
  {
  if (nwords == arrSize)
	expandArray();
  assert(nwords < arrSize);

  arr[nwords++] = new String(s);

  return;
  }

void Sentence::expandArray(void) 
  {
  const double EXPAND_FACTOR = 1.5;
  int newSize = int(EXPAND_FACTOR * arrSize);
  String **newArr = new String*[newSize];

  for (int ctr=0; ctr < newSize; ctr++)
	newArr[ctr] = arr[ctr];

  delete[] arr;
  arr = newArr;
  arrSize = newSize;

  return;
  }

Sentence::Sentence(const Sentence &st) 
  {
  arr = new String*[st.arrSize];
  for (int ctr=0; ctr < st.nwords; ctr++)
	arr[ctr] = new String(st[ctr]);
  nwords = st.nwords;
  arrSize = st.arrSize;
  }
/*
Sentence &Sentence::operator = (const Sentence &st) 
  {
  for (int ctr=0; ctr < nwords; ctr++)
	delete arr[ctr];
  delete[] arr;

  arr = new String*[st.arrSize];
  for (int ctr=0; ctr < st.nwords; ctr++)
	arr[ctr] = new String(st[ctr]);
  nwords = st.nwords;
  arrSize = st.arrSize;
  }
*/
