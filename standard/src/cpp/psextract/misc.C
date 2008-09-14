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

  misc.C - A collection of miscellaneous, 
           self-intuitive functions.

  Andrew Y. Ng, 1994-96

**************************************************/

#include <assert.h>
#include <ctype.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <time.h>
#include <values.h>

#include "misc.h"
#include "Random_Number.h"

void error(const char*msg)
  {
  fflush(stdout);
  fprintf(stderr,"\nERROR: %s\n\n", msg);
  fflush(stderr);

  exit(-1);
  }

void prob_internal_error(const char *msg)
  {
  fflush(stdout);
  fprintf(stderr,"\nPROBABLE INTERNAL ERROR: %s\n", msg);
  fprintf(stderr,"Sorry.\n");
  fflush(stderr);

  abort();
  }

void internal_error(const char *msg)
  {
  fflush(stdout);
  fprintf(stderr,"\nINTERNAL ERROR: %s\n", msg);
  fprintf(stderr,"Sorry.\n");
  fflush(stderr);

  abort();
  }

void warn(const char*msg)
  {
  fflush(stdout);
  fprintf(stderr,"\nWARNING: %s\n\n", msg);
  fflush(stderr);

  return;
  }

void internal_warn(const char*msg)
  {
  fflush(stdout);
  fprintf(stderr,"\nWARNING, POSSIBLE INTERNAL ERROR: %s\n\n", msg);
  fflush(stderr);

  return;
  }

void warn_once(const char *msg)
  {
  static int used_warnings=0;
  static char *warned[512];	// 512 is the max number of different warnings this
				// function will handle
  int ctr;

  for (ctr=0; ctr < used_warnings; ctr++)
	{
	if (!strcmp(warned[ctr], msg))
		return;		// warning had been displayed earlier
	}

  if (used_warnings == 512)
	error("Too many different warnings (> 512). (Is the programmer crazy???)");

  warned[used_warnings] = strdup(msg);
  if (warned[used_warnings] == NULL)
	error("Out of memory.");

  used_warnings++;
	
  warn(msg);

  return;
  }

void not_hang(void)
  {
  fflush(stdout);
  fprintf(stderr,"MESSAGE: I have not hung yet. %s\n", time_str());
  fflush(stderr);

  return;
  }

void *safe_malloc(size_t size)
  {
  void *temp;

  temp = malloc(size);
  if (temp == NULL)
	error("Out of memory.");

  return temp;
  }

void *safe_calloc(int n_elem, size_t size)
  {
  void *temp;
  
  temp = calloc(n_elem, size);
  if (temp == NULL)
	error("Out of memory.");

  return temp;
  }

void my_new_handler(void)
  {
//  error("new: failed to allocate memory.");
  fprintf(stderr,"\nERROR: new: failed to allocate memory.\n\n");
  abort();
  }

float **alloc_float_arr(int m, int n)
  {
  int ctr;
  float **p;

  p = (float**)safe_malloc(sizeof(float*)*(m+1));
  for (ctr=0; ctr < m; ctr++)
	p[ctr] = (float*)safe_malloc(sizeof(float)*n);
  p[m] = NULL;		// to help catch some run-off-array errors

  return p;
  }

double **alloc_double_arr(int m, int n)
  {
  int ctr;
  double **p;

  p = (double**)safe_malloc(sizeof(double*)*(m+1));
  for (ctr=0; ctr < m; ctr++)
	p[ctr] = (double*)safe_malloc(sizeof(double)*n);
  p[m] = NULL;		// to help catch some run-off-array errors

  return p;
  }

void free_float_arr(float **p, int m)
  {
  int ctr;

  for (ctr=0; ctr < m; ctr++)
	free(p[ctr]);
  free(p);

  return;
  }

void free_double_arr(double **p, int m)
  {
  int ctr;

  for (ctr=0; ctr < m; ctr++)
	free(p[ctr]);
  free(p);

  return;
  }

float round(float x)
  {
  return floor(x+0.5);
  }

float float_rand(float lowlim, float highlim)
  {
  float random;
 
  random = next_random_number();

  return random * (highlim-lowlim) + lowlim;
  }

double doub_rand(double lowlim, double highlim)
  {
  double random;
 
  random = next_random_number();

  return random * (highlim-lowlim) + lowlim;
  }

// returns a number between lowlim and highlim INCLUSIVE
int int_rand(int lowlim, int highlim)
  {
  int range;

  range = highlim - lowlim + 1;

  return (int)floor(next_random_number() * range) + lowlim;
  }

// given a list of probabilities that sum to 1, returns 
// a integer i, with P(i) = prob_list[i]
int nonunif_int_dev(double *prob_list, int len)
  {
  double rand, accum;
  int dex;

  rand = next_random_number();

  accum = 0.0;
  for (dex=0; dex < len; dex++)
	{
	accum += prob_list[dex];
	if (accum > 1.00000001)
		{
		internal_warn("nonunif_int_dev: prob_list seem to sum >1.\n");
		fprintf(stderr, "WARNING: (was %.10f)\n", accum);
		}
	if (accum >= rand)
		return dex;
	}

  if (!double_eq(accum,1.0))
	{
	internal_warn("nonunif_int_dev: prob_list does not seem to sum to 1.\n");
	fprintf(stderr, "WARNING: (was %.10f)\n", accum);
	}

  return len-1;
  }

// given an array arr[] of size nele, randomly sets nbits _distinct_
// elements of the array of them to 1, and the rest to 0.
void select_nbits(int *arr, int nele, int nbits)
  {
  int n_selected;
  int ctr, dex;

  assert(nbits >= 0 && nbits <= nele);

  if (nbits < nele/2)
        {
        // less than 1/2 '1's
        for (ctr=0; ctr < nele; ctr++)
                arr[ctr] = 0;
        n_selected = 0;
        while (n_selected < nbits)
                {
                while (arr[(dex=int_rand(0,nele-1))] == 1);
                arr[dex] = 1;
                n_selected++;
                }
        }
  else
        {
        // less than 1/2 '0's
        for (ctr=0; ctr < nele; ctr++)
                arr[ctr] = 1;
        n_selected = 0;
        while (n_selected < nele-nbits)
                {
                while (arr[(dex=int_rand(0,nele-1))] == 0);
                arr[dex] = 0;
                n_selected++;
                }
        }

  assert(sum_arr(arr,nele) == nbits);   // check we got exactly nbits set to 1....

  return;
  }

// (sort of) selects fract-fraction of nele bits, setting them to 1.
// if frac*nele is non-integral, it selects either ceil(frac*nele) bits
// of floor(frac*nele) bits, and gets the expected value of bits on
// to be frac*nele .
void select_frac_bits(int *arr, int nele, double frac)
  {
  double e_num_1;
  double temp;
  int num_1;

  e_num_1 = frac * nele;    // expected number of "1"s
  temp = fract(e_num_1);

  assert(temp >= 0.0 && temp <= 1);
  if (next_random_number() > temp)
        num_1 = (int)floor(e_num_1);
  else
        num_1 = (int)floor(e_num_1) + 1;

  select_nbits(arr, nele, num_1);

  for (int ctr=0; ctr < nele; ctr++)
        assert(arr[ctr] == 0 || arr[ctr] == 1);
  assert(sum_arr(arr, nele) == num_1);

  return;
  }

int *new_select_frac_bits(int nele, double frac)
  {
  int *arr;

  arr = new int[nele];
  assert(arr != NULL);
  select_frac_bits(arr, nele, frac);

  return arr;
  }

FILE *safe_fopen(const char *fn, const char *mode)
  {
  FILE *fp;

  // printf("Opening \"%s\"\n", fn);

  fp = fopen(fn, mode);

  if (fp == NULL)
	{
	fprintf(stderr,"Tried to open file \"%s\".\n", fn);
	error("Could not open file.");
	}

  return fp;
  }

int exist_file(const char *fn)
  {
  FILE *fp;

  fp = fopen(fn, "rt");

  if (fp != NULL)
	{
	fclose(fp);
	return 1;
	}

  return 0;
  }

void verify_exist_file(const char *fn)
  {
  if (!exist_file(fn))
	{
	fprintf(stderr,"ERROR: File %s not found. (Or access denied.)\n", fn);
	error("Program expected file to exist.");
	}

  return;
  }

void verify_not_exist_file(const char *fn)
  {
  if (exist_file(fn))
	{
	fprintf(stderr,"ERROR: File %s exists.\n", fn);
	error("Program expected file not to exist.");
	}

  return;
  }

double *new_doubles_from_file(FILE *fp, int *n_ele)
  {
  const double GROWTH_FACTOR = 2.0;
  int curr_arr_size=16, dex;
  double *p, *new_p, temp;

  p = new double[curr_arr_size];
  assert(p != NULL);

  dex=0;
  while (fscanf(fp, "%lf", &temp) == 1)
	{
	if (dex == curr_arr_size)
		{
		// expand array
		curr_arr_size = (int)ceil(curr_arr_size*GROWTH_FACTOR);
		new_p = new double[curr_arr_size];
		assert(new_p != NULL);
		bcopy((char*)p, (char*)new_p, sizeof(double)*dex);
		delete p;
		p = new_p;
		}

	p[dex] = temp;
	dex++;
	}

  *n_ele = dex;

  return p;
  }

double *new_doubles_from_file(const char *fn, int *n_ele)
  {
  FILE *fp = safe_fopen(fn, "rt");
  double *p = new_doubles_from_file(fp, n_ele);
  fclose(fp);
  return p;
  }

int get_int(const char *prompt)
  {
  int temp;

  printf(prompt);
  scanf("%d",&temp);
  
  return temp;
  }

float get_float(const char *prompt)
  {
  float temp;

  printf(prompt);
  scanf("%f",&temp);

  return temp;
  }

// From Numerical Recipes File GAMMLN.C
double gammln(double xx)
{
  error("gammln left out of distribution/Copyright reasons.");
  return 0; // never executed
}

double float_factorial(double n)
  {
  return exp(gammln(n+1));
  }

int factorial(int x)
  {
  int fact, ctr;

  ctr=1;
  fact=1;
  while (ctr <= x)
	{
	fact *= ctr;
	ctr++;
	}

  return fact;
  }

int choose(int m, int n)
  {
  // horrendously inefficient!
  return (long)factorial(m) / factorial(n) / factorial(m-n);
  }

double log_float_choose(double m, double n)
  {
  return log_double_choose(m,n);
  }

double log_double_choose(double m, double n)
  {
  double f;
  f = gammln(m+1) - gammln(n+1) - gammln(m-n+1);

  return f;
  }

double entropy(double f)
  {
  double h;
 
  if (f == 0.0 || f == 1.0)
	h = 0.0;
  else 
	{
	h = -f*log(f) - (1.0-f)*log(1.0-f);
	h /= log(2);
	}

  return h;
  }

float max_in_arr(float *x, int n_points)
  {
  float best;
  int ctr;

  best = -MAXFLOAT;
  for (ctr=0; ctr < n_points; ctr++)
	if (x[ctr] > best)
		best = x[ctr];

  return best;
  }

float min_in_arr(float *x, int n_points)
  {
  float best;
  int ctr;

  best = MAXFLOAT;
  for (ctr=0; ctr < n_points; ctr++)
	if (x[ctr] < best)
		best = x[ctr];

  return best;
  }

float max_fabs_in_arr(float *x, int n_points)
  {
  float best;
  int ctr;

  best = 0.0;
  for (ctr=0; ctr < n_points; ctr++)
	if (fabs(x[ctr]) > best)
		best = fabs(x[ctr]);

  return best;
  }

float min_fabs_in_arr(float *x, int n_points)
  {
  float best;
  int ctr;

  best = MAXFLOAT;
  for (ctr=0; ctr < n_points; ctr++)
	if (fabs(x[ctr]) < best)
		best = fabs(x[ctr]);

  return best;
  }

double max_in_arr(double *x, int n_points)
  {
  double best;
  int ctr;

  best = -MAXFLOAT;
  for (ctr=0; ctr < n_points; ctr++)
	if (x[ctr] > best)
		best = x[ctr];

  return best;
  }

double min_in_arr(double *x, int n_points)
  {
  double best;
  int ctr;

  best = MAXFLOAT;
  for (ctr=0; ctr < n_points; ctr++)
	if (x[ctr] < best)
		best = x[ctr];

  return best;
  }

double max_fabs_in_arr(double *x, int n_points)
  {
  double best;
  int ctr;

  best = 0.0;
  for (ctr=0; ctr < n_points; ctr++)
	if (fabs(x[ctr]) > best)
		best = fabs(x[ctr]);

  return best;
  }

double min_fabs_in_arr(double *x, int n_points)
  {
  double best;
  int ctr;

  best = MAXFLOAT;
  for (ctr=0; ctr < n_points; ctr++)
	if (fabs(x[ctr]) < best)
		best = fabs(x[ctr]);

  return best;
  }

int median(int a, int b, int c)
  {
  if (a < b)
	{
	if (b < c)
		return b;	// a<b, b<c
	else 
		{
		if (a < c)
			return c;	// a<b, b>=c, a<c 
		else
			return a;	// a<b, b>=c, c<=a
		}
	}
  else
	{
	if (b < c)
		{
		if (a < c)
			return a;	// a>=b, b<c, a<c
		else
			return c;	// a>=b, b<c, a>=c
		}
	else
		return b;		// a>=b, b>=c
	}
  }

double median(double a, double b, double c)
  {
  if (a < b)
	{
	if (b < c)
		return b;	// a<b, b<c
	else 
		{
		if (a < c)
			return c;	// a<b, b>=c, a<c 
		else
			return a;	// a<b, b>=c, c<=a
		}
	}
  else
	{
	if (b < c)
		{
		if (a < c)
			return a;	// a>=b, b<c, a<c
		else
			return c;	// a>=b, b<c, a>=c
		}
	else
		return b;		// a>=b, b>=c
	}
  }

int sum_arr(int *arr, int nele)
  {
  int ctr, sum;

  sum = 0;
  for (ctr=0; ctr < nele; ctr++)
	sum += arr[ctr];

  return sum;
  }

double sum_arr(double *arr, int nele)
  {
  int ctr;
  double sum;

  sum = 0.0;
  for (ctr=0; ctr < nele; ctr++)
	sum += arr[ctr];

  return sum;
  }

double mean_in_arr(int *arr, int nele)
  {
  return (double)sum_arr(arr,nele)/nele;
  }

double se_in_arr(int *arr, int nele)
  {
  double ex, exsqr;	// E[X] and E[X^2]
  double temp;
  int ctr;

  if (nele <= 1)
	return MAXFLOAT;

  ex = mean_in_arr(arr,nele);
  exsqr=0.0;
  for (ctr=0; ctr < nele; ctr++)
	exsqr += sqr(arr[ctr]);
  exsqr /= nele;

  temp = (exsqr - sqr(ex)) * (double)nele / (nele-1);
  if (temp < 0)
	{
	assert(double_eq(temp, 0));
	return 0;
	}
  else
	return sqrt(temp);
  }

double mean_in_arr(double *arr, int nele)
  {
  return (double)sum_arr(arr,nele)/nele;
  }

double se_in_arr(double *arr, int nele)
  {
  double ex, exsqr;	// E[X] and E[X^2]
  double temp;
  int ctr;

  if (nele <= 1)
	return MAXFLOAT;

  ex = mean_in_arr(arr,nele);
  exsqr=0.0;
  for (ctr=0; ctr < nele; ctr++)
	exsqr += sqr(arr[ctr]);
  exsqr /= nele;

  temp = (exsqr - sqr(ex)) * (double)nele / (nele-1);
  if (temp < 0)
	{
	assert(double_eq(temp, 0));
	return 0;
	}
  else
	return sqrt(temp);
  }

static int int_compare(const void *i1, const void *i2)
  {
  if (*(const int*)i1 > *(const int*)i2)
	return 1;
  else if (*(const int*)i1 == *(const int*)i2)
	return 0;
  else
	return -1;
  }

static int float_compare(const void *f1, const void *f2)
  {
  if (*(const float*)f1 > *(const float*)f2)
	return 1;
  else if (*(const float*)f1 == *(const float*)f2)
	return 0;
  else
	return -1;
  }

static int double_compare(const void *d1, const void *d2)
  {
  if (*(const double*)d1 > *(const double*)d2)
	return 1;
  else if (*(const double*)d1 == *(const double*)d2)
	return 0;
  else
	return -1;
  }

void qsort_int(int *arr, int n_ele)
  {
  qsort(arr, n_ele, sizeof(int), int_compare);

  return;
  }

void qsort_float(float *arr, int n_ele)
  {
  qsort(arr, n_ele, sizeof(float), float_compare);

  return;
  }

void qsort_double(double *arr, int n_ele)
  {
  qsort(arr, n_ele, sizeof(double), double_compare);

  return;
  }

void reorder_int_arr(int *arr, int n_ele)
  {
  int temp;
  int ctr, dex;

  for (ctr=0; ctr < n_ele; ctr++)
	{
	dex = int_rand(0, n_ele-1);

	// swap arr[ctr] and arr[dex]
	temp = arr[ctr];
	arr[ctr] = arr[dex];
	arr[dex] = temp;
	}

  return;
  }

void reorder_float_arr(float *arr, int n_ele)
  {
  float temp;
  int ctr, dex;

  for (ctr=0; ctr < n_ele; ctr++)
	{
	dex = int_rand(0, n_ele-1);

	// swap arr[ctr] and arr[dex]
	temp = arr[ctr];
	arr[ctr] = arr[dex];
	arr[dex] = temp;
	}

  return;
  }

void reorder_double_arr(double *arr, int n_ele)
  {
  double temp;
  int ctr, dex;

  for (ctr=0; ctr < n_ele; ctr++)
	{
	dex = int_rand(0, n_ele-1);

	// swap arr[ctr] and arr[dex]
	temp = arr[ctr];
	arr[ctr] = arr[dex];
	arr[dex] = temp;
	}

  return;
  }

// returns log(exp(a) - exp(b)
// Precond: b <= a. returns -MAXDOUBLE or -Inf if too close.
//
// Notes:
// log(exp(a) - exp(b)) = log(exp(a) * (1-exp(b-a)))
//                      =   a        + log(1-exp(b-a))
double logDiffExp(double a, double b)
  {
  assert(b<=a);
  double temp1 = exp(b-a);
  if (temp1 == 1.0)
	return -MAXDOUBLE;
  double temp2 = log1p(-temp1);
  if (isnan(temp2))
	return - MAXDOUBLE;

  assert(double_eq(a+temp2, a+log1p(-exp(b-a))));	// erm... whatever
  return a + temp2;
  }

// returns log(exp(a) + exp(b))
//
// Notes:
// log(exp(a) + exp(b)) = log(exp(a) * (1+exp(b-a)))
//                      = a + log(1+exp(b-a))
// if a > b, then b-a<0, exp(b-a)<1, and everything's okay.
// with a little help from log1p(...)
double logSumExp(double a, double b)
  {
  double temp;

  if (a < b)
	swap(a,b);
  assert(a >= b);

  return a + log1p(exp(b-a));
  }

// returns log(exp(arr[0]) + exp(arr[1]) + ... + exp(arr[nele-1])),
// calculated in a numerically stable way. (Or at least stabler than
// taking the exponents and the logs.
double logSumExp(double *arr, int nele)
  {
  double val1, val2;

  if (nele <= 4)
    {
    switch (nele)
	{
	case 0: error("logSumExp: nele = 0. Bad.");
	case 1: return arr[0];
	case 2: return logSumExp(arr[0], arr[1]);
	case 3: return logSumExp(logSumExp(arr[0],arr[1]),arr[2]);
	case 4: return logSumExp(logSumExp(arr[0],arr[1]),
				 logSumExp(arr[2],arr[3]));
	}
    }

  val1 = logSumExp(arr, nele/2);
  val2 = logSumExp(arr+nele/2, nele-(nele/2));

  return logSumExp(val1, val2);
  }

int is_num(float x)
  {
  float temp;
  char buff[256];
 
  sprintf(buff, "%f", x);
  if (sscanf(buff, "%f", &temp) == 1)
	return 1;
  else
	return 0;
  }

char *time_str(int strip_cr)
  {
  static char buff[128];
  char *time_str;
  int dex;
  time_t t;

  t = time(NULL);
  time_str = ctime(&t);

  if (!strip_cr)
	strcpy(buff,time_str);
  else
	{
	for (dex=0; time_str[dex] != 0 && time_str[dex] != '\n'; dex++)
		buff[dex] = time_str[dex];
	buff[dex] = 0;
	}

  return buff;
  }

int columns_in_file(FILE *fp, int dont_warn_if_not_at_start)
  {
  int num_fields, in_data;
  int c;
  long old_fp_pos;

  old_fp_pos = ftell(fp);

  if (old_fp_pos != 0 && !dont_warn_if_not_at_start)
	warn_once("columns_in_file() called with fp not at start of file");

  // use a FSA-like approach to find the number of data fields to the
  // next newline.
  num_fields = 0;
  in_data=0;
  while ((c=getc(fp)) != '\n' && c != EOF)
	{
	if (isspace(c))
		{
		in_data=0;
		continue;
		}

	if (in_data)
		continue;

	in_data=1;
	num_fields++;
	}

#ifdef SEEK_SET
  fseek(fp, old_fp_pos, SEEK_SET);	// for radish.research.att.com
#else
  fseek(fp, old_fp_pos, 0L);		// for *.andrew.cmu.edu 
#endif
  
  return num_fields;
  }

int peekChar(FILE *fp)
  {
  int c = fgetc(fp);
  int err;

  if (c == EOF)
	return c;

  err = ungetc(c, fp);
  if (err == EOF)
	error("peekChar: ungetc failed (but not EOF)\n");

  return c;
  }

// strip off trailing whitespace.
void strip_trailing_white(char *str)
  {
  int dex;

  dex = strlen(str)-1;
  while (dex >= 0 && isspace(str[dex]))
	str[dex--] = 0;

  return;
  }

// advances fp to the first non-whitespace character
void skip_whitespace(FILE *fp)
  {
  int c;

  while ((c = fgetc(fp)) != EOF && isspace(c));

  if (c != EOF)
	ungetc(c, fp);

  return;
  }

const char *get_hostname(void)
  {
  static char hostname[256];
  char *p;

  p = getenv("HOST");
  if (p == NULL)
	strcpy(hostname, "(Unknown Host)");
  else
	strcpy(hostname, p);

  return hostname;
  }

const char *get_userid(void)
  {
  static char userid[64];
  char *p;

  p = getenv("USER");
  if (p == NULL)
	strcpy(userid, "(Unknown User)");
  else
	strcpy(userid, p);

  return userid;
  }

// b1 and b2 should NOT overlap
void bswap(char *b1, char *b2, int length)
  {
  char temp[512];

  if (length > 512)
	internal_error("bswap(): My buffer's not big enough.");

  bcopy(b1, temp, length);
  bcopy(b2, b1, length);
  bcopy(temp, b2, length);

  return;
  }

// tests for approximate equality of 2 doubles.
// returns true if they are within delta of
// each other in absolute magnitude, or within
// epsilon in relative magnitude.
int double_eq(double d1, double d2, 
		double epsilon, double delta)
  {

  if (d1 == d2)
	return 1;

  // return 1 if within delta of each other
  if (d1 + delta >= d2 && d1 - delta <= d2)
	return 1;
  if (d2 + delta >= d1 && d2 - delta <= d1)
	return 1;

  if ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
	return 0;

  if (d1 > 0)	
	{
	// d1, d2 > 0
	return ( (d2 <= d1 * (1.0+epsilon))
		   && (d2 >= d1 * (1.0-epsilon)) )
	    || ( (d1 <= d2 * (1.0+epsilon))
		   && (d1 >= d2 * (1.0-epsilon)) );
	}
  else	
	{
	// d1, d2 < 0
	return ( (d2 >= d1 * (1.0+epsilon))
		   && (d2 <= d1 * (1.0-epsilon)) )
	    || ( (d1 >= d2 * (1.0+epsilon))
		   && (d1 <= d2 * (1.0-epsilon)) );
	}
  }

// tests for approximate less-than-or-equal-to for 2 doubles.
// see comments for double_eq for exaplanation of epsiln and delta
int double_leq(double d1, double d2, 
		double epsilon, double delta)
  {
  if (d1 <= d2)
	return 1;

  return double_eq(d1, d2);
  }

// tests for approximate less-than-or-equal-to for 2 doubles.
// see comments for double_eq for exaplanation of epsiln and delta
int double_geq(double d1, double d2, 
		double epsilon, double delta)
  {
  if (d1 >= d2)
	return 1;

  return double_eq(d1, d2);
  }

// these argmax and argmin functions all return -1 if nele is 0.
int argmax(int *arr, int nele)
  {
  int ctr;
  int best_dex, best;

  best_dex= -1;
  best = -MAXINT;

  for (ctr=0; ctr < nele; ctr++)
	if (best < arr[ctr])
		{
		best = arr[ctr];
		best_dex = ctr;
		}

  return best_dex;
  }

int argmax(float *arr, int nele)
  {
  int ctr;
  int best_dex;
  double best;

  best_dex= -1;
  best = -MAXFLOAT;

  for (ctr=0; ctr < nele; ctr++)
	if (best < arr[ctr])
		{
		best = arr[ctr];
		best_dex = ctr;
		}

  return best_dex;
  }

int argmax(double *arr, int nele)
  {
  int ctr;
  int best_dex;
  double best;

  best_dex= -1;
  best = -MAXDOUBLE;

  for (ctr=0; ctr < nele; ctr++)
	if (best < arr[ctr])
		{
		best = arr[ctr];
		best_dex = ctr;
		}

  return best_dex;
  }

int argmin(int *arr, int nele)
  {
  int ctr;
  int best_dex, best;

  best_dex= -1;
  best = MAXINT;

  for (ctr=0; ctr < nele; ctr++)
	if (best > arr[ctr])
		{
		best = arr[ctr];
		best_dex = ctr;
		}

  return best_dex;
  }

int argmin(float *arr, int nele)
  {
  int ctr;
  int best_dex;
  double best;

  best_dex= -1;
  best = MAXFLOAT;

  for (ctr=0; ctr < nele; ctr++)
	if (best > arr[ctr])
		{
		best = arr[ctr];
		best_dex = ctr;
		}

  return best_dex;
  }
 
int argmin(double *arr, int nele)
  {
  int ctr;
  int best_dex;
  double best;

  best_dex= -1;
  best = MAXDOUBLE;

  for (ctr=0; ctr < nele; ctr++)
	if (best > arr[ctr])
		{
		best = arr[ctr];
		best_dex = ctr;
		}

  return best_dex;
  }

// like strtod, but dies on failure
double safe_strtod(char *s)
  {
  char *ptr;
  double val;

  val = strtod(s, &ptr);
  if (ptr == s || ptr[0] != 0)
	{
	fprintf(stderr, "ERROR: safe_strtod: Saw %s\n", s);
	error("safe_strtod failed.");
	}

  return val;
  }

int safe_readInt(FILE *fp)
  {
  int i, err;
  err = fscanf(fp, "%d", &i);
  if (err != 1)
	error("safe_readInt failed.");
  return i;
  }

double safe_readDouble(FILE *fp)
  {
  double d;
  int err;
  err = fscanf(fp, "%lf", &d);
  if (err != 1)
	error("safe_readInt failed.");
  return d;
  }

void safe_readStr(FILE *fp, char str[])
  {
  int err;
  err = fscanf(fp, "%s", str);
  if (err != 1)	
	{
	fprintf(stderr, "DEBUGGING STRING> ");
	int c;
	for (int dex=0; dex < 100; dex++)
	    { 
	    while ((c=fgetc(fp)) != EOF) 
		fprintf(stderr,"%c", c); 
	    }
	error("safe_readStr failed.");
	}
  }

