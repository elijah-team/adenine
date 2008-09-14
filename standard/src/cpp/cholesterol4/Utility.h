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

// Utility.h

#ifndef __UTILITY_H__
#define __UTILITY_H__

#include "stdafx.h"

typedef unsigned long ub4;
typedef unsigned char ub1;

#define mix(a,b,c) \
{ \
  a -= b; a -= c; a ^= (c>>13); \
  b -= c; b -= a; b ^= (a<<8); \
  c -= a; c -= b; c ^= (b>>13); \
  a -= b; a -= c; a ^= (c>>12);  \
  b -= c; b -= a; b ^= (a<<16); \
  c -= a; c -= b; c ^= (b>>5); \
  a -= b; a -= c; a ^= (c>>3);  \
  b -= c; b -= a; b ^= (a<<10); \
  c -= a; c -= b; c ^= (b>>15); \
}

inline ub4 HashString(register ub1* k, register ub4 length, register ub4 initval = 0)
{
   register ub4 a,b,c,len;

   /* Set up the internal state */
   len = length;
   a = b = 0x9e3779b9;  /* the golden ratio; an arbitrary value */
   c = initval;           /* the previous hash value */

   /*---------------------------------------- handle most of the key */
   while (len >= 12)
   {
      a += (k[0] +((ub4)k[1]<<8) +((ub4)k[2]<<16) +((ub4)k[3]<<24));
      b += (k[4] +((ub4)k[5]<<8) +((ub4)k[6]<<16) +((ub4)k[7]<<24));
      c += (k[8] +((ub4)k[9]<<8) +((ub4)k[10]<<16)+((ub4)k[11]<<24));
      mix(a,b,c);
      k += 12; len -= 12;
   }

   /*------------------------------------- handle the last 11 bytes */
   c += length;
   switch(len)              /* all the case statements fall through */
   {
   case 11: c+=((ub4)k[10]<<24);
   case 10: c+=((ub4)k[9]<<16);
   case 9 : c+=((ub4)k[8]<<8);
      /* the first byte of c is reserved for the length */
   case 8 : b+=((ub4)k[7]<<24);
   case 7 : b+=((ub4)k[6]<<16);
   case 6 : b+=((ub4)k[5]<<8);
   case 5 : b+=k[4];
   case 4 : a+=((ub4)k[3]<<24);
   case 3 : a+=((ub4)k[2]<<16);
   case 2 : a+=((ub4)k[1]<<8);
   case 1 : a+=k[0];
     /* case 0: nothing left to add */
   }
   mix(a,b,c);
   /*-------------------------------------------- report the result */
   return c;
}

inline long HashString(const char* psz)
{
	return (long)HashString((ub1*)psz, (ub4)strlen(psz));
}

struct eqcchar
{
	bool operator()(const char *s1, const char *s2) const
	{
		assert(s1 != NULL);
		assert(s2 != NULL);
		return (strcmp(s1, s2) == 0);
	}
};


#include "ReadWriteLock.h"

typedef vector<int> CIntVector;

typedef set<int>::iterator CIntSetIterator;
class CIntSet : public set<int>
{
public:
	CReadWriteLock m_lock;

	int GetHead()
	{
		int result;
		m_lock.StartRead();
		iterator i = begin();
		if (i == end())
			result = 0;
		else
			result = *i;
		m_lock.EndRead();
		return result;
	}

	void AddTail(int i)
	{
		m_lock.StartWrite();
		insert(i);
		m_lock.EndWrite();
	}
	
	bool Contains(int i)
	{
		m_lock.StartRead();
		CIntSetIterator it = find(i);
		bool b = it != end();
		m_lock.EndRead();
		return b;
	}

	void RemoveAll()
	{
		m_lock.StartWrite();
		clear();
		m_lock.EndWrite();
	}
};


class CMapIntToIntSet : public hash_map<int, CIntSet*>
{
public:
	CMapIntToIntSet() : hash_map<int, CIntSet*>(32) {}
	void Read(FILE* fp)
	{
		int size;
		fread(&size, sizeof(int), 1, fp);
		for (int i = 0; i < size; i++)
		{
			int key;
			fread(&key, sizeof(int), 1, fp);

			int size2;
			fread(&size2, sizeof(int), 1, fp);

			CIntSet* plist = new CIntSet;
			for (int j = 0; j < size2; j++)
			{
				int datum;
				fread(&datum, sizeof(int), 1, fp);
				plist->AddTail(datum);
			}

			(*this)[key] = plist;
		}
	}

	void Write(FILE* fp)
	{
		int size = this->size();
		fwrite(&size, sizeof(int), 1, fp);
		for (iterator i = begin(); i != end(); i++)
		{
			int key = (*i).first;
			CIntSet* plist = (*i).second;

			fwrite(&key, sizeof(int), 1, fp);
			size = plist->size();
			fwrite(&size, sizeof(int), 1, fp);
			for (CIntSetIterator j = plist->begin(); j != plist->end(); j++)
			{
				int datum = *j;
				fwrite(&datum, sizeof(int), 1, fp);
			}
		}
	}
};


#ifdef _DEBUG
#define ASSERT_VALID(x) AssertValid()
#else
#define ASSERT_VALID(x)
#endif // _DEBUG

#ifdef WIN32
#define AfxIsValidAddress(a, b, c) (!IsBadReadPtr(a, b))
#else
#define AfxIsValidAddress(a, b, c)
#endif // WIN32

#define AFX_INLINE inline
#define ASSERT assert
#define AFXAPI


#endif // __UTILITY_H__
