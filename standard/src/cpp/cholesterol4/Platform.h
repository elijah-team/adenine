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

// Platform.h

#ifndef __PLATFORM_H__
#define __PLATFORM_H__

#ifdef LINUX
#include <pthread.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#endif

#ifdef WIN32
#include <process.h>

inline unsigned long PlatformBeginThread(void (__cdecl* start_routine)(void*) , void * arg)
#define THREADPROC void __cdecl
#endif

#ifdef LINUX
inline pthread_t PlatformBeginThread(void *(*start_routine)(void *) , void * arg)
#define THREADPROC void*
#endif

{
#ifdef WIN32
	return _beginthread(start_routine, 0, arg);
#endif
	
#ifdef LINUX
	pthread_t pt;
	int r = pthread_create(&pt, NULL, start_routine, arg);
	// r == 0 is success
	return pt;
#endif
	
}

struct CCriticalSection
{
#ifdef WIN32
	CRITICAL_SECTION m_cs;
	
	inline void Enter()
	{
		::EnterCriticalSection(&m_cs);
	}
	
	inline void Leave()
	{
		::LeaveCriticalSection(&m_cs);
	}
	
	inline void Init()
	{
		::InitializeCriticalSection(&m_cs);
	}
	
	inline void Cleanup()
	{
		::DeleteCriticalSection(&m_cs);
		
	}
	
#elif defined LINUX
	pthread_mutex_t m_mutex;
	
	inline void Enter()
	{
		pthread_mutex_lock(&m_mutex);
	}
	
	inline void Leave()
	{
		pthread_mutex_unlock(&m_mutex);
	}
	
	inline void Init()
	{
		pthread_mutex_init(&m_mutex, NULL);
	}
	
	inline void Cleanup()
	{
		pthread_mutex_destroy(&m_mutex);
	}
#endif
};

class CCSLock
{
public:
	CCriticalSection& m_rcs;
	inline CCSLock(CCriticalSection& rcs) : m_rcs(rcs) { m_rcs.Enter(); }
	inline ~CCSLock() { m_rcs.Leave(); }
};

#ifdef LINUX
inline void Sleep(int msec)
{
	struct timespec tv;
	tv.tv_sec = 0;
	// millisecond is 10^-3
	// nanosecond is 10^-9
	tv.tv_nsec = msec * 1000000;
	nanosleep(&tv, NULL);
}
#endif // LINUX

struct CCounter
{
	long m_c;
	
#ifdef WIN32
	inline void Init()
	{
	}
	
	inline void Cleanup()
	{
	}
	
	// WARNING: only works on NT4+ and Win98+
	inline long Increment()
	{
		return ::InterlockedIncrement(&m_c);
	}
	
	inline long Decrement()
	{
		return ::InterlockedDecrement(&m_c);
	}
	
	inline void Zero()
	{
		::InterlockedExchange(&m_c, 0);
	}
	
	inline long Increment(long c)
	{
		return ::InterlockedExchangeAdd(&m_c, c);
	}
	
	inline long Decrement(long c)
	{
		return ::InterlockedExchangeAdd(&m_c, -c);
	}
	
#else
	CCriticalSection m_cs;
	
	inline void Init()
	{
		m_cs.Init();
	}
	
	inline void Cleanup()
	{
		m_cs.Cleanup();
	}
	
	inline long Increment()
	{
		long r;
		m_cs.Enter();
		r = ++m_c;
		m_cs.Leave();
		return r;
	}
	
	inline long Decrement()
	{
		long r;
		m_cs.Enter();
		r = --m_c;
		m_cs.Leave();
		return r;
	}
	
	inline long Increment(long c)
	{
		long r;
		m_cs.Enter();
		m_c += c;
		r = m_c;
		m_cs.Leave();
		return r;
	}
	
	inline long Decrement(long c)
	{
		long r;
		m_cs.Enter();
		m_c -= c;
		r = m_c;
		m_cs.Leave();
		return r;
	}
	
	inline void Zero()
	{
		m_cs.Enter();
		m_c = 0;
		m_cs.Leave();
	}
#endif // WIN32
};

class CMemoryMapping
{
#ifdef WIN32
public:
	CMemoryMapping()
	{
		m_hFileMapping = NULL;
		m_hFile = NULL;
		m_lpvMapping = NULL;
	}
	
	~CMemoryMapping()
	{
		Unmap();
	}
	
	void* Map(const char* pszFilename, long cbInitial)
	{
		if (m_lpvMapping != NULL)
			return m_lpvMapping;
		
		m_pszFilename = strdup(pszFilename);
		
		m_hFile = ::CreateFile(m_pszFilename, GENERIC_WRITE | GENERIC_READ, 0, NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
		DWORD dw = 0;
		bool bNew = false;
		if ((m_cb = ::GetFileSize(m_hFile, &dw)) == 0 && dw == 0)
		{
			::SetFilePointer(m_hFile, cbInitial, NULL, FILE_BEGIN); 
			::SetEndOfFile(m_hFile);
			m_cb = cbInitial;
			bNew = true;
		}
		
		m_hFileMapping = ::CreateFileMapping(m_hFile, NULL, PAGE_READWRITE, 0, 0, NULL);
		m_lpvMapping = ::MapViewOfFile(m_hFileMapping, FILE_MAP_ALL_ACCESS, 0, 0, 0);
		
		if (bNew)
			memset(m_lpvMapping, 0, cbInitial);
		
		return m_lpvMapping;
	}
	
	void Flush()
	{
		if (m_lpvMapping != NULL)
			::FlushViewOfFile(m_lpvMapping, 0);
	}
	
	void Unmap()
	{
		if (m_hFileMapping != NULL)
		{
			::UnmapViewOfFile(m_lpvMapping);
			::CloseHandle(m_hFileMapping);
			::CloseHandle(m_hFile);
			m_hFileMapping = NULL;
			m_hFile = NULL;
			m_lpvMapping = NULL;
			
			free(m_pszFilename);
		}
	}
	
	void* Grow(int size)
	{
		// TODO:
		return NULL;
	}
	
	long GetSize()
	{
		return m_cb;
	}
	
protected:
	char* m_pszFilename;
	HANDLE m_hFileMapping;
	HANDLE m_hFile;
	LPVOID m_lpvMapping;
	long m_cb;
#endif // WIN32
	
	
#ifdef LINUX
public:
	CMemoryMapping()
	{
		m_lpvMapping = NULL;
		m_cb = 0;
	}
	
	~CMemoryMapping()
	{
		Unmap();
	}
	
	void* Map(const char* pszFilename, long cbInitial)
	{
		if (m_lpvMapping !=  NULL)
			return m_lpvMapping;
		
		m_pszFilename = strdup(pszFilename);
		
		m_fd = open(m_pszFilename, O_CREAT|O_RDWR, S_IRWXU);
		if (m_fd == -1) {
			printf("********** Can't open file %s\n", m_pszFilename);
			return NULL;
		}
		
		off_t offset;
		
		struct stat st;
		int r = stat(m_pszFilename, &st);
		if (r == -1) {
			offset = 0;
		}
		else {
			offset = st.st_size;
		}
		
		bool bNew = false;
		if (offset == 0) {
			int r = ftruncate(m_fd, cbInitial);
			if (r != 0) {
				// we need to write cbInitial bytes to fd
				const int bufsize = 1024;
				char buf[bufsize];
				memset(buf,0,bufsize);
				
				int nWritten = 0;
				while (nWritten < cbInitial) {
					int towrite = (cbInitial - nWritten) % bufsize;
					ssize_t s = write(m_fd, buf, towrite);
					nWritten += s;
				}
			}
			
			bNew = true;
			m_cb = cbInitial;
		}
		else {
			m_cb = offset;
		}
		
		m_lpvMapping = mmap(0, m_cb, PROT_READ|PROT_WRITE, MAP_SHARED, m_fd, 0);
		if ((int)m_lpvMapping == -1) {
			close(m_fd);
			return NULL;
		}
		
		if (bNew)
		{
			//memset(m_lpvMapping, 0, m_cb);
		}
		
		return m_lpvMapping;
	}
	
	void Flush()
	{
		if (m_lpvMapping != NULL)
		{
			msync(m_lpvMapping, m_cb, MS_SYNC);
		}
	}
	
	void Unmap()
	{
		if (m_lpvMapping != NULL)
		{
			munmap(m_lpvMapping, m_cb);
			close(m_fd);
		}
		
		m_fd = -1;
		m_lpvMapping = NULL;
		m_cb = 0;
		
		if (m_pszFilename != NULL) {
			delete [] m_pszFilename;
			m_pszFilename = NULL;
		}
	}
	
	void* Grow(int size)
	{
		// TODO:
		return NULL;
	}
	
	long GetSize()
	{
		return m_cb;
	}
	
 protected:
	 
	 int m_fd;
	 char* m_pszFilename;
	 void* m_lpvMapping;
	 long m_cb;
	 
#endif
};

#endif // __PLATFORM_H__

