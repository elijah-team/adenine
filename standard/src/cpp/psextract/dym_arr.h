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


/*************************************************

  dym_arr.h - Dynamic arrays: automatically 
        expands when an out-of-bounds element
        is referenced.
        (all code is in header file.)

  Andrew Y. Ng, 1996

**************************************************/

#include <assert.h>
#include <stdio.h>
#include <math.h>

#ifndef _DYM_ARR_H
#define _DYM_ARR_H

const double DYMARR_GROWTH_FACTOR=1.5;
const int DYMARR_INIT_SIZE = 8;

template <class T>
class Dymarr
{
 public:
  T *arr;
  T init_val;
  int arrsize, lastreffed;
  inline void expand_array(int req_dex);

  //  public:
  inline int usedsize(void) const {return lastreffed+1;}
  inline int usedSize(void) const {return lastreffed+1;}
  inline T* allele(void) {return arr;}
  inline const T &peek(int dex) const     // does not change anything
    // including lastreffed
    {
      if (dex < arrsize) return arr[dex];
      else return init_val;
    }
  inline T &operator [](int dex)
    { 
      if (dex > lastreffed) lastreffed = dex;
      if (dex < arrsize) return arr[dex];
      expand_array(dex);
      return arr[dex];
    }
  inline void forget_all(void)
    {
      //for (int ctr=0; ctr < arrsize; ctr++)
      for (int ctr=0; ctr <= lastreffed; ctr++)
	arr[ctr] = init_val;
      lastreffed = -1;
    }
  inline void forgetAll(void) { forget_all(); }
  inline const Dymarr<T> &operator= (const Dymarr<T> &da);
  inline Dymarr(T init_val_) 
    :init_val(init_val_), lastreffed(-1), arrsize(DYMARR_INIT_SIZE)
    {
      arr = new T[DYMARR_INIT_SIZE];
      for (int ctr=0; ctr < DYMARR_INIT_SIZE; ctr++) 
	arr[ctr] = init_val;
    }
  inline Dymarr(void) : lastreffed(-1), arrsize(DYMARR_INIT_SIZE)
    {
      if (!g_dymarr_allowCreationWithoutDefaultValueInThisFile)
	{
	  fprintf(stderr, "ERROR (probably internal): Dymarr: tried to call "
		  "constructor without param when this feature not enabled.");
	  abort();
	}
      arr = new T[DYMARR_INIT_SIZE];
    }
  inline void append(const T val)
    { 
      (*this)[lastreffed+1] = val;
    }
  inline Dymarr(const Dymarr<T> &da);
  inline ~Dymarr();
};

static int g_dymarr_allowCreationWithoutDefaultValueInThisFile = 0;

template <class T>
inline Dymarr<T>::~Dymarr()
{
  delete[] arr;
}

template <class T>
inline void Dymarr<T>::expand_array(int req_dex)
{
  int newsize, ctr;
  T *newarr;

  newsize = (int)ceil(arrsize*DYMARR_GROWTH_FACTOR);
  if (newsize <= req_dex) 
    newsize = (int)ceil(req_dex*DYMARR_GROWTH_FACTOR);
  assert(req_dex < newsize);

  newarr = new T[newsize];
  for (ctr=0; ctr < arrsize; ctr++)
    newarr[ctr] = arr[ctr];
  for (; ctr < newsize; ctr++)
    newarr[ctr] = init_val;

  delete[] arr;
  arr = newarr;
  arrsize = newsize;

  return;
}

template <class T>
inline Dymarr<T>::Dymarr(const Dymarr<T> &da)
{
  arr = new T[da.arrsize];
  init_val = da.init_val;
  arrsize = da.arrsize;
  lastreffed = da.lastreffed;

  assert(arrsize > 0);
  for (int ctr=0; ctr < arrsize; ctr++)
    arr[ctr] = da.arr[ctr];
}

template <class T>
inline const Dymarr<T> &Dymarr<T>::operator= (const Dymarr<T> &da)
{
  delete[] arr;
  arr = new T[da.arrsize];
  init_val = da.init_val;
  arrsize = da.arrsize;
  lastreffed = da.lastreffed;

  assert(arrsize > 0);
  for (int ctr=0; ctr < arrsize; ctr++)
    arr[ctr] = da.arr[ctr];

  return *this;
}

/*
// Test code

void foo(Dymarr<int> da)
  {
  printf("----\n");
  for (int ctr=0; ctr < da.usedsize(); ctr++)
        printf("%d: %d\n", ctr, da[ctr]);

  return;
  }

int main(void)
  {
  Dymarr<int> arr(0);
  Dymarr<int> arr2(15);

  arr[1] = 1;
  arr[4] = 4;
  arr[9] = 9;

  arr2 = arr;
  arr2[12]=12;

  for (int ctr=0; ctr < arr.usedsize(); ctr++)
        printf("%d: %d\n", ctr, arr[ctr]);
  printf("----\n");
  for (ctr=0; ctr < arr2.usedsize(); ctr++)
        printf("%d: %d\n", ctr, arr2[ctr]);

  foo(arr2);

  return 0;
  }
*/

#endif          // _DYM_ARR_H
