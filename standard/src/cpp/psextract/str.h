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

  str.h - Implementation of String object. 
	(All code in header file.)

  Andrew Y. Ng, 1996

**************************************************/

#ifndef _STR_H
#define _STR_H

// Notes:
//
// "+" has been overloaded for concatenation.

#include <stdio.h>
#include <string.h>

class String
  {
  private:
	char *str;

  public:
	inline int len(void) const {return strlen(str);}
	inline String(void) {str = new char[1]; str[0] = 0;}
	inline String(const char c) {str = new char[2]; str[0]=c; str[1]=0; }
	inline String(const char *s) {str = new char[strlen(s)+1]; 
					strcpy(str,s);}
	inline String(const String &s) {str = new char[s.len()+1];
					strcpy(str,s.as_char()); }
	inline String(int l) { str = new char[l]; str[0] = 0; }
	inline ~String() { delete[] str; }
	inline String &operator = (const String &s) { 
				char *old_str = str;
				str = new char[s.len()+1]; 
				strcpy(str,s.str);
				delete[] old_str;
				return *this; }
	inline String &operator = (const char *s) { delete [] str;
				str = new char[strlen(s)+1]; 
					strcpy(str,s);
					return *this; }
	inline int operator == (const char *s) const 
				{ return !strcmp(str,s); }
	inline int operator == (const String &s) const 
				{ return !strcmp(str,s.as_char()); }
	inline int operator != (const String &s) const 
				{ return (strcmp(str,s.as_char())!=0); }
	inline int operator != (const char *s) const 
				{ return (strcmp(str,s)!=0); }
	inline char *as_char(void) const {return str;}
	inline char &operator [](int index) const { return str[index]; }

	void setToUpper(void);
	void setToLower(void);

	inline void print(FILE *fp=NULL)
		{ if (fp==NULL) fp = stdout; 
		  fprintf(fp, str); }
	inline String &operator += (const String &s)
			{ char *old_str=str;
			  str = new char[len()+s.len()+1];
			  strcpy(str, old_str);
			  strcat(str, s.as_char());
			  delete[] old_str;
			  return *this; 
			}
  };

class Sentence
  {
  private:
	String **arr;
	int nwords;
	int arrSize;

  public:
	Sentence(const Sentence &st);
	Sentence(void);
	~Sentence(void);

	int nwordsVal(void) {return nwords;}

	void makeEmpty(void);
	void append(const String &s);
	void append(const char *s) { append(String(s)); };
				// not the most efficient way, but hey...
	void expandArray(void);
	Sentence &operator = (const char *st);

	inline String& operator[](int index) const
		{ assert(index < nwords); return *(arr[index]); }
  };

inline String operator + (const String &s1, const String &s2)
  {
  String s(s1.len()+s2.len()+1);

  strcpy(s.as_char(), s1.as_char());
  strcat(s.as_char(), s2.as_char());

  return s;
  }

inline bool operator== (const char *s1, const String &s2)
  { return s2==s1; }

inline String as_string(int x)
  {
  char buff[128];
  sprintf(buff, "%d", x);
  String s(buff);

  return s;
  }

String toLower(const String &s);
String toUpper(const String &s);

/*
// test code
int main(void)
  {
  String s1("String 1"), s2;
  String s3;

  s2 = (s1 + String("Foo"));
  s3 = "String 3";
  s3 += s1;

  printf("%s\n", s1.as_char());
  printf("%s\n", s2.as_char());
  printf("%s\n", s3.as_char());

  return 0;
  }
*/

#endif 		// _STR_H

