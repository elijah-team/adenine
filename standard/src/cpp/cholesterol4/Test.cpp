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

// TODO actual database testing

#include <stdio.h>
#include "Utility.h"
#include "Database.h"

int main()
{
	/*
	{
		CMemoryMapping mem;
		char *p = (char*) mem.Map("file.txt", 1000);
		if (p == NULL) {
			printf("p == NULL\n");
			return 0 ;
		}
		
		for (int x=0;x<1000;x++)
			p[x] = (x % ('z'-'a')) + 'a';
	}
	
	{
		CMemoryMapping mem;
		char *p = (char*) mem.Map("file.txt", 1000);
		if (p == NULL) {
			printf("p == NULL\n");
			return 0 ;
		}
		
		int c = 0;
		for (int x=0;x<1000;x++)
			if (p[x] == (x % ('z'-'a')) + 'a') c++;
		ASSERT(c == 1000);
	}
	*/
	
	{
		CDatabase db("test");
		
		db.Add(db.GetID("mark"),    db.GetID("likes"), db.GetID("melanie"), db.GetID("1"));
		db.Add(db.GetID("melanie"), db.GetID("likes"), db.GetID("joe"),     db.GetID("2"));
		db.Add(db.GetID("melanie"), db.GetID("likes"), db.GetID("zebras"),  db.GetID("3"));
		db.Add(db.GetID("rufus"),   db.GetID("likes"), db.GetID("lions"),   db.GetID("4"));
		db.Add(db.GetID("joe"),   db.GetID("likes"), db.GetID("paris"),   db.GetID("4"));
		db.Add(db.GetID("zebras"),   db.GetID("likes"), db.GetID("grass"),   db.GetID("4"));
		db.Add(db.GetID("zebras"),   db.GetID("likes"), db.GetID("tofu"),   db.GetID("4"));
		
		CIntVector aQuery;

		aQuery.push_back(db.GetID("melanie"));
		aQuery.push_back(-2);
		aQuery.push_back(-1);
		
		aQuery.push_back(-1);
		aQuery.push_back(-2);
		aQuery.push_back(-3);
		
		 /*aQuery.push_back(-1);
		 aQuery.push_back(db.GetID("likes"));
		 aQuery.push_back(-2);*/
		
		const int cExistentials = 3;

		CIntVector aResultData, aResultCounts;
		
		int x;
		for (x=0;x<cExistentials;x++)
		{
			aResultCounts.push_back(0);
		}
		
		if (db.Query(aQuery, cExistentials, NULL, NULL, &aResultData, &aResultCounts)) {
			printf("****size = %d %d\n\n", aResultData.size(), aResultCounts.size());
		
			printf("\n\n\nQuery Results:\n");
			{
				x = 0;
				CIntVector::iterator it = aResultData.begin();
				while (it != aResultData.end())
				{
					int a = *it;
					
					string s = db.GetString(a); printf("[%s %d]", s.c_str(), a);
					//printf("%d", a);
					
					it++;
					x++;

					if (x % cExistentials == 0)
					{
						printf("\n");
					}
					else
					{
						printf(", ");
					}			
				}
			}

			{
				printf("\nResult counts: ");
				CIntVector::iterator it = aResultCounts.begin();
				while (it != aResultCounts.end())
				{
					int a = *it;
					printf("%d ", a);
					it++;
				}
				printf("\n\n\n");
			}
		}
		else
		{
			printf("FAILED query\n");
		}
		
	}
	
	return 0;
}

