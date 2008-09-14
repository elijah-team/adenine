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

/*********************************************

  matrix.h - header for matrix.C, which is code
	  for doing Matrices and Vectors. 

  Andrew Y. Ng, 1996

*********************************************/

#ifndef _MATRIX_H
#define _MATRIX_H

#include <assert.h>
#include "misc.h"

class Matrix;
void mat_error(const char*msg);

class Vector
  {
  private:
	double *val;
	int n;

  public:
	inline int dim(void) const {return n;}
	inline int n_ele(void) const {return n;}
	inline int length(void) const {return n;}
	inline operator double*() const {return val;}
	inline double *ref(int dex) {return &val[dex];}
	inline void dangerousCoerceLength(int newlen) { n = newlen; }
	void resize(int n_, int initialize=1, double init_val=0);
			// destroys previous data
	void assert_dim(int n_) const;
	operator Matrix() const;
	void initialize_from_arr(const double *p);
	Vector subVector(int first, int lastp1) const;  // sub-vector including
						  // val[first]..val[lastp1-1]
	void setSubVector(Vector &v, int first, int lastp1);  
	double sumSubVector(int first, int lastp1) const;  // sums val[first]..val[lastp1-1]
	inline double sum(void) const { return sum_arr(val, n); }
	void normalize_sum_to(double desiredSum);

	void enlarge(int new_size, int init_new_ele=1, double init_val=0.0);

	Vector &operator+=(const Vector &vec);
	Vector &operator-=(const Vector &vec);
	Vector &operator*=(double d);
	Vector &operator/=(double d);

	void make_zero(void) {for(int ctr=0; ctr < n; ctr++)
				val[ctr]=0;}
	void randomize(double lowLim, double highLim);
	inline int isZero(void) const {
			for (int i=0; i < n; i++) if (val[i]!=0) return 0;
			return 1; }
	void make_const_vec(double x) {for(int ctr=0; ctr < n; ctr++)
						val[ctr]=x;}
	Vector &operator=(const Vector &vec);
	void print(FILE *fp=NULL, const char *separator=NULL, int print_cr=1) const;
	Vector(const Vector &vec);
	Vector(int n_, int initialize=1, double init_val=0);
	~Vector();
  };

// mat[i][j] is the element in the i-th row and j-th column,
//  using zero-indexing
class Matrix	
  {
  private:
	double **val;	// elements are all stored in one 
			// contiguous linear array
	int m, n;

	void init_structure(int m_, int n_);
	double **new_one_indices(void) const;

  public:
	inline int nrow(void) const {return m;} 
	inline int nrows(void) const {return m;} 
	inline int ncol(void) const {return n;}
	inline int ncols(void) const {return n;}
	inline double *ref(int row, int col) {return &val[row][col];}
	double setval(int row, int col, double d);
	void assert_dim(int m_, int n_) const;
	inline int is_square(void) const {return m==n;}
	int is_symmetric(double epsilon=1.0e-6, double delta=1.0e-10);
	inline operator double**() const {return val;}
	inline operator double() const 
		{if (m!=1||n!=1) 
		     mat_error("Cannot convert non-1x1 matrix to double");
		 return **val;}

	Matrix &operator+=(const Matrix &mat);
	Matrix &operator-=(const Matrix &mat);
	Matrix &operator*=(double d);
	Matrix &operator/=(double d);

	void transpose_me(void);

	int svd_decomp(Matrix &u, Vector &diag, Matrix &v) const;
	Vector linSolve(Vector &y, int canDestroyMe, int printDebug=1);
	int invert_me(double *abs_detp=NULL); // (Uses SVD)
			// returns 1 if ill-conditioned or singular. 
			// if abs_detp is non-NULL, then puts the 
			// ABSOLUTE VALUE of the determinent of the 
			// ORIGINAL matrix there.
	Matrix find_inverse(int *ill_conditioned=NULL, double 
					*abs_detp=NULL);
			// similar behavior to invert_me(..)
	Matrix find_transpose(void);
	Matrix find_AtransposeWA(const Vector &w); // w=diag matrix
	Matrix find_AtransposeA(void);
	Matrix find_AAtranspose(void);
	Matrix find_mat_sqrt(void); // precond: *this is SYMMETRIC
			// returns A, such that A-transpose * A == *this

	void make_zero(void) {for(int i=0; i < nrow(); i++)
				for (int j=0; j < ncol(); j++)
					val[i][j] = 0; }
	void make_identity(void) {assert(nrow()==ncol()); make_zero();
				      for(int i=0; i < nrow(); i++)
					val[i][i] = 1.0; }
	void randomize(double lowLim, double highLim);
	void resize(int m_, int n_, int initialize=1, double init_val=0);
			// destroys all old elements,
	inline void resize_and_assign_to(const Matrix &m) 
		{resize(m.nrow(),m.ncol(),0); *this = m; }

	void initialize_from_arr(const double *p);
	void initialize_from_file(FILE *fp);
	void initialize_from_file(const char *fn);
	void print(FILE *fp=NULL) const;
	Vector extract_row(int row);
	Vector extract_col(int col);
	void set_row(const double dat[], int row);
	void set_col(const double dat[], int col);
	void set_row(const Vector &v, int row);
	void set_col(const Vector &v, int col);
	Matrix &operator=(const Matrix &mat);

	Matrix find_det_deriv(void);	// i,j-th element is the
					// deriv of the determinant wrt
					// i,j element of the orig matrix
	double determinant(void);

	Matrix(const Matrix &mat);
	Matrix(int m_, int n_, int initialize=1, double init_val=0);
	~Matrix();
  };

Vector operator*(double d, const Vector &vec);
Vector operator*(const Vector &vec, double d);
Vector operator-(const Vector &vec);
Vector operator-(const Vector &vec1, const Vector &vec2);
Vector operator+(const Vector &vec1, const Vector &vec2);
Vector operator/(const Vector &vec, double d);

Matrix operator*(const Matrix &mat1, const Matrix &mat2);
Matrix operator*(double d, const Matrix &mat);
Matrix operator*(const Matrix &mat, double d);
Matrix operator/(const Matrix &mat, double d);
Matrix operator-(const Matrix &mat);
Matrix operator-(const Matrix &mat1, const Matrix &mat2);

Matrix operator+(const Matrix &mat1, const Matrix &mat2);

Vector operator*(const Matrix &mat, const Vector &vec);
Matrix operator*(const Matrix &mat1, const Matrix &mat2);

double dot_prod(const Vector &vec1, const Vector &vec2);

// these return true if all elements are withing 
// delta of each other in absolute value, or within 1+epsilon
// in relative value
int mat_approx_equal(const Matrix &m1, const Matrix &m2, 
		double epsilon=1.0e-6, double delta=1.0e-10);
int vect_approx_equal(const Vector &v1, const Vector &v2, 
		double epsilon=1.0e-6, double delta=1.0e-10);

double euclideanDistance(const Vector &v1, const Vector &v2);
double squaredEuclideanDistance(const Vector &v1, const Vector &v2);

#endif		// _MATRIX_H
