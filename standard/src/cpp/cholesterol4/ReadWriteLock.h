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

// ReadWriteLock.h

#ifndef _READ_WRITE_LOCK_H_
#define _READ_WRITE_LOCK_H_

#include "Platform.h"

class CReadWriteLock
{
public:
	CReadWriteLock()
	{
		Init();
	}

	~CReadWriteLock()
	{
		Cleanup();
	}

	inline void Init()
	{
		m_cs.Init();
		m_cReaders = 0;
		m_bWriter = false;
	}

	inline void Cleanup()
	{
		m_cs.Cleanup();
	}

	int m_cReaders;
	bool m_bWriter;
	CCriticalSection m_cs;

	inline void StartWrite()
	{
		do
		{
			m_cs.Enter();
			if ((m_cReaders == 0) && !m_bWriter)
			{
				m_bWriter = true;
				m_cs.Leave();
				return;
			}
			m_cs.Leave();
			Sleep(50);
		}
		while (true);
	}

	inline void EndWrite()
	{
		m_cs.Enter();
		m_bWriter = false;
		m_cs.Leave();
	}

	inline void EndWriteStartRead()
	{
		m_cs.Enter();
		m_bWriter = false;
		++m_cReaders;
		m_cs.Leave();
	}

	inline void EndReadStartWrite()
	{
		bool bReduce = true;
		while (true)
		{
			m_cs.Enter();
			if (bReduce)
			{
				--m_cReaders;
				bReduce = false;
			}
			if (m_cReaders == 0)
			{
				m_bWriter = true;
				m_cs.Leave();
				return;
			}
			m_cs.Leave();
		}
	}

	inline void StartRead()
	{
		do
		{
			m_cs.Enter();
			if (!m_bWriter)
			{
				++m_cReaders;
				m_cs.Leave();
				return;
			}
			m_cs.Leave();
			Sleep(50);
		}
		while (true);
	}

	inline void EndRead()
	{
		m_cs.Enter();
		--m_cReaders;
		assert(m_cReaders >= 0);
		m_cs.Leave();
	}
};

#endif
