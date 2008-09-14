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

// Database.cpp

#include "stdafx.h"
#include "Database.h"

#define FILE_SIZE 3000
#define INITIAL_DATABASE_SIZE (262144 * sizeof(CNode))
#define MAX_NODES_IN_MEMORY 30000
#define INITIAL_THREAD_COUNT 2
#define MAGIC_NUMBER 0xdace

#if defined(CLEAN_LOCKS) && defined(WIN32)
HANDLE g_hheap = ::HeapCreate(0, 0, 0);
#endif

CReadWriteLockAndData* AllocReadWriteLockAndData()
{
#if defined(CLEAN_LOCKS) && defined(WIN32)
	return (CReadWriteLockAndData*)::HeapAlloc(g_hheap, HEAP_ZERO_MEMORY, sizeof(CReadWriteLockAndData));
#else
	return new CReadWriteLockAndData;
#endif // WIN32
}

void FreeReadWriteLockAndData(CReadWriteLockAndData* p)
{
#if defined(CLEAN_LOCKS) && defined(WIN32)
	::HeapFree(g_hheap, 0, p);
#else
	delete p;
#endif // WIN32
}

inline CTable* CNode::GetSubjectTableAutoCreate()
{
	if (!m_plockLoad->m_pData->m_pSubjectTable)
	{
		SetHasTables();
		return (m_plockLoad->m_pData->m_pSubjectTable = new CTable);
	}
	return m_plockLoad->m_pData->m_pSubjectTable;
}

inline CTable* CNode::GetObjectTableAutoCreate()
{
	if (!m_plockLoad->m_pData->m_pObjectTable)
	{
		SetHasTables();
		return (m_plockLoad->m_pData->m_pObjectTable = new CTable);
	}
	return m_plockLoad->m_pData->m_pObjectTable;
}

void CNode::Create(const char* pszDatum, const char* pszBasePath)
{
	memset(this, 0, sizeof(CNode));
	m_plockLoad = AllocReadWriteLockAndData();
	m_plockLoad->Init();

#ifdef CLEAN_LOCKS
	m_plockLoad->m_cUsers = -1;
#endif

	m_iFile = -1;
	GetSector() = -1;
	GetSectors() = -1;

	GetLoaded() = 1;
	SetChanged();
	m_plockLoad->m_pData = new CNodeRuntimeData;
	time(&GetData()->m_timeLastUsed);
	m_plockLoad->m_pData->m_iNext = 0;

	m_nHash = HashString(pszDatum);
	GetData()->m_pszData = new char[strlen(pszDatum) + 1];
	strcpy(GetString(), pszDatum);
	GetData()->m_pSubjectTable = NULL;
	GetData()->m_pObjectTable = NULL;
	GetData()->GetReifiedSubject() = 0;
	GetData()->GetReifiedPredicate() = 0;
	GetData()->GetReifiedObject() = 0;
}

void CNode::Init(const char* pszBasePath)
{
	m_plockLoad = NULL;

	GetLoaded() = 0;
}

bool CNode::StartUse(const char* pszBasePath, CDatabase* pdb)
{
	pdb->m_csLocks.Enter();
	if (m_plockLoad == NULL)
	{
		m_plockLoad = AllocReadWriteLockAndData();
		m_plockLoad->Init();
	}

#ifdef CLEAN_LOCKS
	if (m_plockLoad->m_cUsers == -1)
		m_plockLoad->m_cUsers = 1;
	else
		++m_plockLoad->m_cUsers;
#endif

	pdb->m_csLocks.Leave();
	m_plockLoad->StartRead();

	if (GetLoaded())
	{
		if (GetLoaded() == 2)
		{
			m_plockLoad->EndReadStartWrite();
			GetLoaded() = 1;
			m_plockLoad->EndWriteStartRead();
		}
		return false;
	}

	m_plockLoad->EndReadStartWrite();
	if (GetLoaded())
	{
		if (GetLoaded() == 2)
			GetLoaded() = 1;
		m_plockLoad->EndWriteStartRead();
		return false;
	}

	m_plockLoad->m_pData = new CNodeRuntimeData;
	time(&GetData()->m_timeLastUsed);

	char achBuf[1024];
	assert(m_iFile >= 0);
	sprintf(achBuf, "%s%i", pszBasePath, m_iFile);
	
	FILE* fp = fopen(achBuf, "rb");
	assert(GetSector() >= 0);
	fseek(fp, GetSector() * SECTOR_SIZE, SEEK_CUR);
	int cch;
	fread(&cch, sizeof(int), 1, fp);
	GetData()->m_pszData = new char[cch + 1];
	fread(GetString(), sizeof(char), cch, fp);
	GetString()[cch] = 0;

	fread(&GetData()->m_iNext, sizeof(int), 1, fp);
	fread(&GetData()->GetReifiedSubject(), sizeof(int), 1, fp);
	fread(&GetData()->GetReifiedPredicate(), sizeof(int), 1, fp);
	fread(&GetData()->GetReifiedObject(), sizeof(int), 1, fp);	
	
	int size;
	fread(&size, sizeof(int), 1, fp);
	if (size > 0)
	{
		fseek(fp, -sizeof(int), SEEK_CUR);
		GetData()->m_pSubjectTable = new CTable;
		GetData()->GetSubjectTable()->m_mapForward.Read(fp);
	}
	else
		GetData()->m_pSubjectTable = NULL;

	fread(&size, sizeof(int), 1, fp);
	if (size > 0)
	{
		fseek(fp, -sizeof(int), SEEK_CUR);
		GetData()->m_pObjectTable = new CTable;
		GetData()->GetObjectTable()->m_mapForward.Read(fp);
	}
	else
		GetData()->m_pObjectTable = NULL;

	fclose(fp);
	
	GetLoaded() = 1;
	SetChanged(0);
	m_plockLoad->EndWriteStartRead();
	return true;
}

void CNode::EndUse(CDatabase* pdb)
{
#ifdef CLEAN_LOCKS
	pdb->m_csLocks.Enter();
#endif

	m_plockLoad->EndRead();

#ifdef CLEAN_LOCKS
	--m_plockLoad->m_cUsers;
	pdb->m_csLocks.Leave();
#endif
}

void CNode::AboutToUnload(CDatabase* pdb)
{
#ifdef CLEAN_LOCKS
	pdb->m_csLocks.Enter();
	++m_plockLoad->m_cUsers;
	pdb->m_csLocks.Leave();
#endif

	m_plockLoad->StartWrite();
	if (GetLoaded() == 1)
		GetLoaded() = 2;
	m_plockLoad->EndWrite();
}

short CNode::Unload(FILE* fp, int iFile, short iSector)
{
	m_plockLoad->StartWrite();

	if (!GetChanged())
	{
		if (GetData()->m_pSubjectTable)
		{
			delete GetData()->m_pSubjectTable;
			GetData()->m_pSubjectTable = NULL;
		}
		if (GetData()->m_pObjectTable)
		{
			delete GetData()->m_pObjectTable;
			GetData()->m_pObjectTable = NULL;
		}
		if (GetData()->m_pszData)
		{
			delete [] GetData()->m_pszData;
			GetData()->m_pszData = NULL;
		}
		delete m_plockLoad->m_pData;
		m_plockLoad->m_pData = NULL;
		GetLoaded() = 0;
		return 0;
	}

	if (GetLoaded() != 2)
		return -1;

	long l1 = ftell(fp);
	int cch = strlen(GetString());
	fwrite(&cch, sizeof(int), 1, fp);
	fwrite(GetString(), sizeof(char), cch, fp);

	fwrite(&GetData()->m_iNext, sizeof(int), 1, fp);
	fwrite(&GetData()->GetReifiedSubject(), sizeof(int), 1, fp);
	fwrite(&GetData()->GetReifiedPredicate(), sizeof(int), 1, fp);
	fwrite(&GetData()->GetReifiedObject(), sizeof(int), 1, fp);	

	if (!GetData()->m_pSubjectTable)
	{
		static int s_nZero = 0;
		fwrite(&s_nZero, sizeof(int), 1, fp);
	}
	else
		GetData()->GetSubjectTable()->m_mapForward.Write(fp);

	if (!GetData()->m_pObjectTable)
	{
		static int s_nZero = 0;
		fwrite(&s_nZero, sizeof(int), 1, fp);
	}
	else
		GetData()->GetObjectTable()->m_mapForward.Write(fp);

	GetSector() = iSector;
	m_iFile = iFile;

	long l2 = ftell(fp);
	int extra = (l2 - l1) % SECTOR_SIZE;
	GetSectors() = (l2 - l1) / SECTOR_SIZE;
	if (extra)
		++GetSectors();

	if (GetData()->m_pSubjectTable)
	{
		delete GetData()->m_pSubjectTable;
		GetData()->m_pSubjectTable = NULL;
	}
	if (GetData()->m_pObjectTable)
	{
		delete GetData()->m_pObjectTable;
		GetData()->m_pObjectTable = NULL;
	}
	if (GetData()->m_pszData)
	{
		delete [] GetData()->m_pszData;
		GetData()->m_pszData = NULL;
	}
	delete m_plockLoad->m_pData;
	m_plockLoad->m_pData = NULL;

	GetLoaded() = 0;
	SetChanged(0);

	return GetSectors();
}

THREADPROC WatcherThreadProc(void* pv)
{
	CDatabase* pThis = (CDatabase*)pv;

	char achBuf[1024];
	CNode* apNodes[FILE_SIZE];

	while (!pThis->m_bShuttingDown)
	{
		int c;
		pThis->m_csNodes.Enter();
		c = pThis->m_listNodesInMemory.size();
		int cMax = pThis->m_cMaxNodesInMemory;
		pThis->m_csNodes.Leave();
		if (c > cMax)
		{
			set<CNode*> listStopped;

			// Sort and pop off FILE_SIZE nodes
			pThis->m_csNodes.Enter();
			c = pThis->m_listNodesInMemory.size();
			int cPoppedNodes = min(FILE_SIZE, c);
			deque<CNode*>::iterator mid = pThis->m_listNodesInMemory.begin() + cPoppedNodes;
			nth_element(pThis->m_listNodesInMemory.begin(), mid, pThis->m_listNodesInMemory.end(), node_less());
			mid = pThis->m_listNodesInMemory.begin() + cPoppedNodes;
			int i;
			deque<CNode*>::iterator j;
			for (i = 0, j = pThis->m_listNodesInMemory.begin(); j != mid; i++, j++)
			{
				apNodes[i] = *j;
				apNodes[i]->AboutToUnload(pThis);
			}
			pThis->m_listNodesInMemory.erase(pThis->m_listNodesInMemory.begin(), mid);
			pThis->m_csNodes.Leave();

			// Open a new file
			int iFile = pThis->m_pcFiles->Increment();
			sprintf(achBuf, "%s%i", pThis->m_pszBasePath, iFile);
			FILE* fp = fopen(achBuf, "wb");
			short iSector = 0;
			int cActualNodes = 0;
			for (i = 0; i < cPoppedNodes; i++)
			{
				int cSectors = apNodes[i]->Unload(fp, iFile, iSector);
				if (cSectors == -1)
				{
					pThis->m_csNodes.Enter();
					if (apNodes[i]->GetLoaded())
					{
						apNodes[i]->GetLoaded() = 1;
						pThis->m_listNodesInMemory.push_back(apNodes[i]);
						listStopped.insert(apNodes[i]);
						apNodes[i]->m_plockLoad->EndWrite();
					}
					pThis->m_csNodes.Leave();
				}
				else
				{
					if (cSectors)
						++cActualNodes;
					iSector += cSectors;
					fseek(fp, iSector * SECTOR_SIZE, SEEK_SET);
				}
			}
			fclose(fp);
			for (i = 0; i < cPoppedNodes; i++)
			{
				if (listStopped.find(apNodes[i]) == listStopped.end())
					apNodes[i]->m_plockLoad->EndWrite();

#ifdef CLEAN_LOCKS
				pThis->m_csLocks.Enter();
				--apNodes[i]->m_plockLoad->m_cUsers;
				pThis->m_csLocks.Leave();
#endif
			}
#if defined(_DEBUG) || defined(LOGGING)
			printf(">> Cholesterol: tried to pop %i of %i but only popped %i\n", cPoppedNodes, c, cActualNodes);
#endif
			pThis->m_csNodes.Enter();
			c = pThis->m_listNodesInMemory.size();
			cMax = pThis->m_cMaxNodesInMemory;
			pThis->m_csNodes.Leave();
		}

		Sleep(300);
	}

	pThis->m_cRunningThreads.Decrement();

#ifdef LINUX
	return NULL;
#endif
}

#ifdef CLEAN_LOCKS
THREADPROC LockCleanerThreadProc(void* pv)
{
	CDatabase* pThis = (CDatabase*)pv;

	while (!pThis->m_bShuttingDown)
	{
		int c = *pThis->m_pcNodes;
//		printf(">> Cholesterol: cleaning locks\n");
		int cLocksCleaned = 0;
		int cLocksUncleaned = 0;
		for (int i = 1; i <= c; i++)
		{
			pThis->m_lockNodes.StartRead();
			CNode& rnode = pThis->GetNode(i);
			if (rnode.m_plockLoad != NULL)
			{
				pThis->m_csLocks.Enter();
				if ((rnode.m_plockLoad->m_cUsers == 0) && (rnode.m_plockLoad->m_pData == NULL))
				{
					FreeReadWriteLockAndData(rnode.m_plockLoad);
					rnode.m_plockLoad = NULL;
					++cLocksCleaned;
				}
				else
					++cLocksUncleaned;
				pThis->m_csLocks.Leave();
			}
			pThis->m_lockNodes.EndRead();

			if (pThis->m_bShuttingDown)
				break;
		}
//		printf(">> Cholesterol: %i locks cleaned; %i uncleaned\n", cLocksCleaned, cLocksUncleaned);

		pThis->FlushLog();
		Sleep(60000);
	}

	pThis->m_cRunningThreads.Decrement();

#ifdef LINUX
	return NULL;
#endif
}
#endif

CDatabase::CDatabase(const char* pszBasePath, CLiteralCallback* pLiteralCallback) : m_resourceIDs(65536)
{
	printf(">> Cholesterol: size of node %i bytes\n", sizeof(CNode));

	m_pszBasePath = strdup(pszBasePath);
	m_bDefrag = false;
	m_bExport = false;
	SetupLog();
	
	// Load the database
	char achBuf[1024];
	sprintf(achBuf, "%snodes", pszBasePath);
	CMemoryMapping& rmm = *(new CMemoryMapping);
	m_aMemoryMaps.push_back(&rmm);
	void* p = rmm.Map(achBuf, INITIAL_DATABASE_SIZE + sizeof(int) + sizeof(CCounter) + sizeof(bool) - sizeof(CNode));
	m_pcNodes = (int*)p;
	m_pcFiles = (CCounter*)(m_pcNodes + 1);
	m_pcFiles->Init();
	m_pbClean = (int*)(m_pcFiles + 1);
	m_apNodes[0] = (CNode*)(m_pbClean + 1);

	m_csNodes.Init();
	m_csLocks.Init();

	bool bReloadNeeded = false;
	//bool bDefrag = false;
	if (((*m_pbClean) & 0xffff) != MAGIC_NUMBER)
	{
		// Redo the database from the log
		rmm.Unmap();
		unlink(achBuf);
		p = rmm.Map(achBuf, INITIAL_DATABASE_SIZE + sizeof(int) + sizeof(CCounter) + sizeof(bool) - sizeof(CNode));
		m_pcNodes = (int*)p;
		m_pcFiles = (CCounter*)(m_pcNodes + 1);
		m_pcFiles->Init();
		m_pbClean = (int*)(m_pcFiles + 1);
		m_apNodes[0] = (CNode*)(m_pbClean + 1);

		// Node indices are 1-based
		--m_apNodes[0];

		m_cNodeSpace = 262143;

		bReloadNeeded = true;
	}
	else
	{
		//bDefrag = *m_pbClean > 0xffff;
		*m_pbClean = 0;

		// Count and file the nodes
		m_cNodeSpace = 262143;

		// Node indices are 1-based
		--m_apNodes[0];

		for (int i = 1; i <= *m_pcNodes; i++)
		{
			CNode& rnode = GetNode(i);
			rnode.Init(pszBasePath);
			if (m_resourceIDs.find(rnode.m_nHash) == m_resourceIDs.end())
				m_resourceIDs[rnode.m_nHash] = i;
			--m_cNodeSpace;
			if (m_cNodeSpace == 0)
				GrowDatabase(false);
		}

		printf(">> Cholesterol: loaded %i nodes\n", *m_pcNodes);
	}

	// Set up watcher threads
	m_bShuttingDown = false;
	m_cRunningThreads.Init();
	m_cRunningThreads.m_c = INITIAL_THREAD_COUNT;

	m_cMaxNodesInMemory = MAX_NODES_IN_MEMORY;

	for (int i = 0; i < m_cRunningThreads.m_c; i++)
		PlatformBeginThread(&WatcherThreadProc, this);

#ifdef CLEAN_LOCKS
	PlatformBeginThread(&LockCleanerThreadProc, this);
#endif // CLEAN_LOCKS

	if (bReloadNeeded)
	{
		m_cMaxNodesInMemory = MAX_NODES_IN_MEMORY * 5;
		LoadFromLog(pLiteralCallback);
		m_cMaxNodesInMemory = MAX_NODES_IN_MEMORY;
	}
}

CDatabase::~CDatabase()
{
	Shutdown();
	free(m_pszBasePath);
}

void CDatabase::Shutdown()
{
	m_lockNodes.StartWrite();

	// Shut down log
	FlushLog();

	if (m_bDefrag)
	{
		puts(">> Cholesterol: compacting...");

		// Write to a new log
		char* pszLogFilename = m_pszLogFilename;
		m_pszLogFilename = new char[strlen(m_pszBasePath) + 100];
		strcpy(m_pszLogFilename, m_pszBasePath);
		strcat(m_pszLogFilename, "log-new");

		// Write statements
		int cStatements = 0;
		for (int i = 1; i <= *m_pcNodes; i++)
		{
			CNode& rnodeStatement = GetNode(i);
			StartUsingNode(&rnodeStatement);
			if (rnodeStatement.GetData()->m_subject)
			{
				CNode& rnodeSubject = GetNode(rnodeStatement.GetData()->m_subject);
				StartUsingNode(&rnodeSubject);
				CTable* ptable = rnodeSubject.GetData()->GetSubjectTable();
				if (ptable != NULL)
				{
					CIntSet* plist = ptable->GetForwardList(rnodeStatement.GetData()->m_predicate, false);
					if (plist != NULL)
					{
						if (plist->Contains(rnodeStatement.GetData()->m_object))
						{
							++cStatements;
							WriteStatementToLog('a', &rnodeSubject, &GetNode(rnodeStatement.GetData()->m_predicate), &GetNode(rnodeStatement.GetData()->m_object), &rnodeStatement);
						}
					}
				}
				EndUsingNode(&rnodeSubject);
			}
			EndUsingNode(&rnodeStatement);
		}

		unlink(pszLogFilename);
		rename(m_pszLogFilename, pszLogFilename);
		delete [] m_pszLogFilename;
		m_pszLogFilename = pszLogFilename;
	}

	m_csLog.Cleanup();
	free(m_pCache);

	// Force a save of all nodes
	m_csNodes.Enter();
	m_cMaxNodesInMemory = 0;
	m_csNodes.Leave();

	while (true)
	{
		int c;
		m_csNodes.Enter();
		c = m_listNodesInMemory.size();
		m_csNodes.Leave();
		if (c == 0)
			break;
		Sleep(50);
	}

	// Wait for threads to finish
	m_bShuttingDown = true;
	while (m_cRunningThreads.m_c)
		Sleep(50);
	m_cRunningThreads.Cleanup();

	int cFiles = m_pcFiles->m_c;

	// Clean up nodes
	printf(">> Cholesterol: Clean up nodes...\n");
	for (int i = 1; i < (*m_pcNodes - 1); i++)
		GetNode(i).Cleanup();

	m_csNodes.Cleanup();
	m_csLocks.Cleanup();
	m_pcFiles->Cleanup();

	delete [] m_pszLogFilename;

	*m_pbClean = MAGIC_NUMBER | (m_bDefrag ? 0x10000 : 0);

	printf(">> Cholesterol: Delete memory mappings...\n");
	for (vector<CMemoryMapping*>::iterator j = m_aMemoryMaps.begin(); j != m_aMemoryMaps.end(); j++)
		delete (*j);
	if (m_bDefrag)
	{
		// Delete all extra files
		char achBuf[1024];
		sprintf(achBuf, "%snodes", m_pszBasePath);
		unlink(achBuf);

		int i;
		for (i = 1; i < m_aMemoryMaps.size(); i++)
		{
			sprintf(achBuf, "%snodes-%i", m_pszBasePath, i);
			unlink(achBuf);
		}

		for (i = 1; i <= cFiles; i++)
		{
			sprintf(achBuf, "%s%i", m_pszBasePath, i);
			unlink(achBuf);
		}
	}

	m_aMemoryMaps.clear();
	m_lockNodes.EndWrite();
}

void CDatabase::GrowDatabase(bool bWipe)
{
	// Open the next memory mapping
	char achBuf[1024];
	sprintf(achBuf, "%snodes-%i", m_pszBasePath, m_aMemoryMaps.size());
	if (bWipe)
		unlink(achBuf);
	CMemoryMapping* pmm = new CMemoryMapping;
	m_aMemoryMaps.push_back(pmm);
	void* p = pmm->Map(achBuf, INITIAL_DATABASE_SIZE);
	m_apNodes[m_aMemoryMaps.size() - 1] = (CNode*)p;
	m_cNodeSpace = 262144;
}

bool CDatabase::Add(int s, int p, int o, int id)
{
	m_lockNodes.StartRead();
	if (m_bShuttingDown)
	{
		m_lockNodes.EndRead();
		return false;
	}

	CNode& rnodeSubject = GetNode(s);
	StartUsingNode(&rnodeSubject);
	bool bRet = rnodeSubject.GetSubjectTableAutoCreate()->Add(p, o);
	if (bRet)
		rnodeSubject.SetChanged();
	EndUsingNode(&rnodeSubject);

	if (bRet)
	{
		CNode& rnodeObject = GetNode(o);

		StartUsingNode(&rnodeObject, true);
		rnodeObject.GetObjectTableAutoCreate()->Add(p, s);
		EndUsingNode(&rnodeObject);

		CNode& statement = GetNode(id);
		StartUsingNode(&statement, true);
		statement.GetData()->GetReifiedSubject() = s;
		statement.GetData()->GetReifiedPredicate() = p;
		statement.GetData()->GetReifiedObject() = o;
		EndUsingNode(&statement);

		WriteStatementToLog('a', &rnodeSubject, &GetNode(p), &rnodeObject, &statement);
	}

	m_lockNodes.EndRead();
	return bRet;
}

void CDatabase::Remove(int s, int p, int o, CIntVector* paRemoved)
{
	m_lockNodes.StartRead();
	if (m_bShuttingDown)
	{
		m_lockNodes.EndRead();
		return;
	}

	bool bSubjectSpecified = s > 0;
	bool bPredicateSpecified = p > 0;
	bool bObjectSpecified = o > 0;
	int cBlanks;

	if (bSubjectSpecified && bPredicateSpecified && bObjectSpecified)
	{
		// <> <> <>
		CNode& rnodeSubject = GetNode(s);
		if (rnodeSubject.GetHasTables())
		{
			StartUsingNode(&rnodeSubject);
			CTable* pTable = rnodeSubject.GetData()->GetSubjectTable();
			bool bRet = false;
			if (pTable)
				bRet = pTable->Remove(p, o);
			if (bRet)
				rnodeSubject.SetChanged();
			EndUsingNode(&rnodeSubject);

			if (bRet)
			{
				CNode& rnodeObject = GetNode(o);

				StartUsingNode(&rnodeObject, true);
				rnodeObject.GetObjectTableAutoCreate()->Remove(p, s);
				EndUsingNode(&rnodeObject);

				WriteStatementToLog('r', &rnodeSubject, &GetNode(p), &rnodeObject, NULL);
				if (paRemoved)
				{
					paRemoved->push_back(s);
					paRemoved->push_back(p);
					paRemoved->push_back(o);
				}
			}
		}
	}
	else if (!bSubjectSpecified && bPredicateSpecified && bObjectSpecified)
	{
		// ?x <> <>
		CNode& rnodeObject = GetNode(o);
		if (rnodeObject.GetHasTables())
		{
			StartUsingNode(&rnodeObject);
			CTable* pTable = rnodeObject.GetData()->GetObjectTable();
			if (pTable != NULL)
			{
				CIntSet* plist = pTable->GetForwardList(p, false);
				if (plist != NULL)
				{
					plist->m_lock.StartRead();
					set<int> x(*plist);
					plist->m_lock.EndRead();
					EndUsingNode(&rnodeObject);
					if (x.size() != 0)
					{
						for (set<int>::iterator i = x.begin(); i != x.end(); i++)
							Remove(*i, p, o, paRemoved);
					}
				}
				else
					EndUsingNode(&rnodeObject);
			}
			else
				EndUsingNode(&rnodeObject);
		}
	}
	else if (bSubjectSpecified && bPredicateSpecified && !bObjectSpecified)
	{
		// <> <> ?x
		CNode& rnodeSubject = GetNode(s);
		if (rnodeSubject.GetHasTables())
		{
			StartUsingNode(&rnodeSubject);
			CTable* pTable = rnodeSubject.GetData()->GetSubjectTable();
			if (pTable != NULL)
			{
				CIntSet* plist = pTable->GetForwardList(p, false);
				if (plist != NULL)
				{
					plist->m_lock.StartRead();
					set<int> x(*plist);
					plist->m_lock.EndRead();
					EndUsingNode(&rnodeSubject);
					if (x.size() != 0)
					{
						for (set<int>::iterator i = x.begin(); i != x.end(); i++)
							Remove(s, p, *i, paRemoved);
					}
				}
				else
					EndUsingNode(&rnodeSubject);
			}
			else
				EndUsingNode(&rnodeSubject);
		}
	}
	else if ((cBlanks = (!!bSubjectSpecified + !!bPredicateSpecified + !!bObjectSpecified)) >= 1)
	{
		CIntVector aQuery;

		if ((s >= -2) && (o >= -2) && (p >= -2))
		{
			aQuery.push_back(s);
			aQuery.push_back(p);
			aQuery.push_back(o);

			CIntVector aResultData, aResultCounts;
			
			cBlanks = 3 - cBlanks;
			aResultCounts.push_back(0);
			if (cBlanks == 2)
				aResultCounts.push_back(0);
			
			if (Query(aQuery, cBlanks, NULL, NULL, &aResultData, &aResultCounts)) 
			{
				for (int x = 0; x < aResultData.size(); x += cBlanks)
				{
					int a = aResultData[x];
					int s0 = (s == -1 ? a : (s == -2 ? aResultData[x + 1] : s));
					int p0 = (p == -1 ? a : (p == -2 ? aResultData[x + 1] : p));
					int o0 = (o == -1 ? a : (o == -2 ? aResultData[x + 1] : o));
					Remove(s0, p0, o0, paRemoved);
				}
			}
		}
	}

	m_lockNodes.EndRead();
}

bool CDatabase::Contains(int s, int p, int o)
{
	m_lockNodes.StartRead();
	if (m_bShuttingDown)
	{
		m_lockNodes.EndRead();
		return false;
	}

	bool bRet = false;

	CNode& rnodeSubject = GetNode(s);
	StartUsingNode(&rnodeSubject);
	CTable* pTable = rnodeSubject.GetData()->GetSubjectTable();
	if (pTable)
		bRet = pTable->Contains(p, o);
	EndUsingNode(&rnodeSubject);

	m_lockNodes.EndRead();
	return bRet;
}

int CDatabase::Extract(int s, int p, int o)
{
	if (s == 0)
	{
		if (p == 0 || o == 0)
			return 0;

		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			return false;
		}

		int result = 0;
		
		CNode& rnodeObject = GetNode(o);
		StartUsingNode(&rnodeObject);
		CTable* pTable = rnodeObject.GetData()->GetObjectTable();
		if (pTable != NULL)
		{
			CIntSet* plist = pTable->GetForwardList(p, false);
			if (plist != NULL)
				result = plist->GetHead();
		}
		EndUsingNode(&rnodeObject);

		m_lockNodes.EndRead();
		return result;
	}
	else if (p == 0)
	{
		if (o == 0 || s == 0)
			return 0;

		// TODO:
		return 0;
	}
	else if (o == 0)
	{
		if (p == 0 || s == 0)
			return 0;

		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			return false;
		}

		int result = 0;

		CNode& rnodeSubject = GetNode(s);
		StartUsingNode(&rnodeSubject);
		CTable* pTable = rnodeSubject.GetData()->GetSubjectTable();
		if (pTable != NULL)
		{
			CIntSet* plist = pTable->GetForwardList(p, false);
			if (plist != NULL)
				result = plist->GetHead();
		}
		EndUsingNode(&rnodeSubject);

		m_lockNodes.EndRead();
		return result;
	}
	else
		return 0;
}

static void MergeRows(CIntVector& raRow, int cExistentials, CIntVector* paExistingData, CIntVector* paResultData, CIntVector* paResultCounts)
{
	if (!paExistingData)
	{
		for (int j = 0; j < cExistentials; j++)
		{
			int x = raRow[j];
			if (x != 0)
				++(*paResultCounts)[j];
			paResultData->push_back(x);
		}
		return;
	}

	int c = paExistingData->size();
	int i;
	for (i = 0; i < c; i += cExistentials)
	{
		// Check for overlap
		bool bFailed = false;
		int j;
		for (j = 0; j < cExistentials; j++)
		{
			int x = raRow[j];
			int y = (*paExistingData)[i + j];
			if ((x != 0) && (y != 0) && (x != y))
			{
				bFailed = true; // No match
				break;
			}
		}

		if (bFailed)
			continue;

		// Simply fold together
		for (j = 0; j < cExistentials; j++)
		{
			int x = raRow[j];
			int y = (*paExistingData)[i + j];
			if (x != 0)
			{
				paResultData->push_back(x);
				++(*paResultCounts)[j];
			}
			else if (y != 0)
			{
				paResultData->push_back(y);
				++(*paResultCounts)[j];
			}
			else
				paResultData->push_back(0);
		}
	}
}

void CDatabase::DebugPrintDatum(const CIntVector &datum, int cExistentials)
{
	printf("datum: ");
	for (int x=0;x<cExistentials;x++)
	{
		int a = datum[x];
		string s = GetString(a); printf("[%s %d] ", s.c_str(), a);
	}
	printf("\n");
}

bool CDatabase::Query(CIntVector& raQuery, int cExistentials, CIntVector* paExistingData, CIntVector* paExistingCounts, CIntVector* paResultData0, CIntVector* paResultCounts0)
{
	int cQueryStatements = raQuery.size() / 3;
	if (cQueryStatements == 0)
	{
		printf(">> Cholesterol: query with no statements\n");
		return false;
	}

	if (cQueryStatements == 4)
	{
		int x = 0;
		x = x + 1;
	}

	int cExistingData;
	if (paExistingData == NULL) 
		cExistingData = -1;
	else
		cExistingData = paExistingData->size();

	if (cExistingData == 0)
		return true; // Overconstrained already

	// Find the best query line to work on
	int iBestScore = -1;
	int nBestScore = 0;

	CIntVector* paResultData;
	CIntVector* paResultCounts;
	
	if (cQueryStatements == 1)
	{
		iBestScore = 0;
		paResultData = paResultData0;
		paResultCounts = paResultCounts0;
	}
	else
	{
		int i;
		for (i = 0; i < cQueryStatements; i++)
		{
			int s = raQuery[i * 3];
			int p = raQuery[i * 3 + 1];
			int o = raQuery[i * 3 + 2];
			int nScore = s > 0 ? 10000 : (paExistingCounts && (*paExistingCounts)[-s - 1] ? 10000 / (*paExistingCounts)[-s - 1] : -1);
			nScore += p > 0 ? 10000 : (paExistingCounts && (*paExistingCounts)[-p - 1] ? 10000 / (*paExistingCounts)[-p - 1] : -1);
			nScore += o > 0 ? 10000 : (paExistingCounts && (*paExistingCounts)[-o - 1] ? 10000 / (*paExistingCounts)[-o - 1] : -1);

			if ((iBestScore == -1) || (nScore > nBestScore))
			{
				iBestScore = i;
				nBestScore = nScore;
			}
		}
		paResultData = new CIntVector;
		paResultCounts = new CIntVector;
		for (i = 0; i < cExistentials; i++)
			paResultCounts->push_back(0);
	}

	int s = raQuery[iBestScore * 3];
	int p = raQuery[iBestScore * 3 + 1];
	int o = raQuery[iBestScore * 3 + 2];

	bool bSubjectSpecified = s >= 0;
	bool bPredicateSpecified = p >= 0;
	bool bObjectSpecified = o >= 0;

	CIntVector datum;
	for (int i = 0; i < cExistentials; i++)
		datum.push_back(0);

	// Perform the query depending on the position of the existentials
	// ?x ?y ?z
	if (!bSubjectSpecified && !bPredicateSpecified && !bObjectSpecified)
	{
		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			if (cQueryStatements > 1)
			{
				delete paResultData;
				delete paResultCounts;
			}
			return false;
		}

		bool bExistingSCount = false;
		int s_lbound, s_ubound;
		if (paExistingCounts && ((*paExistingCounts)[-s - 1] == cExistingData / cExistentials))
		{
			s_lbound = 0;
			s_ubound = cExistingData / cExistentials;
			bExistingSCount = true;
		}
		else
		{
			s_lbound = 1;
			s_ubound = *m_pcNodes+1;
		}

		for (int i = s_lbound; i < s_ubound; i++)
		{
			int kk;
			if (bExistingSCount) kk = (*paExistingData)[i * cExistentials - s - 1];
			else kk = i;
			
			CNode& rnodeSubject = GetNode(kk);
			if (rnodeSubject.GetHasTables())
			{
				bool bDone = false;

				StartUsingNode(&rnodeSubject);

				CTable* ptable = rnodeSubject.GetData()->GetSubjectTable();

				if (ptable != NULL)
				{
					bool bDone = false;

					if (paExistingData)
					{
						int currPredicate = bExistingSCount ? (*paExistingData)[i * cExistentials - p - 1] : 0;
						if (!bExistingSCount && ((*paExistingCounts)[-p - 1] == cExistingData / cExistentials))
						{
							bDone = true;

							for (int j = 0; j < cExistingData / cExistentials; j++)
							{
								currPredicate = (*paExistingData)[j * cExistentials - p - 1];
								CIntSet* plist = ptable->GetForwardList(currPredicate, false);
								if (plist != NULL)
								{
									for (int k = 0; k < cExistentials; k++)
										datum[k] = (*paExistingData)[j * cExistentials + k];
									datum[-s - 1] = kk;
									plist->m_lock.StartRead();
									if (datum[-o - 1] == 0)
									{
										for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
										{
											datum[-o - 1] = *l;
											MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
										}
									}
									else
									{
										for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
										{
											if (datum[-o - 1] == *l)
												MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
										}
									}
									plist->m_lock.EndRead();
								}
							}
						} 
						else if (bExistingSCount && currPredicate)
						{
							bDone = true;

							CIntSet* plist = ptable->GetForwardList(currPredicate, false);
							if (plist != NULL)
							{
								for (int k = 0; k < cExistentials; k++)
									datum[k] = (*paExistingData)[i * cExistentials + k];
								plist->m_lock.StartRead();
								for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
								{
									datum[-o - 1] = *l;
									MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
								}
								plist->m_lock.EndRead();
							}
						}
						else
						{
							bDone = true;
							datum[-s - 1] = kk;

							ptable->m_lock.StartRead();
							for (CMapIntToIntSet::iterator j = ptable->m_mapForward.begin(); j != ptable->m_mapForward.end(); j++)
							{
								datum[-p - 1] = (*j).first;
								CIntSet* plist = (*j).second;
								plist->m_lock.StartRead();
								for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
								{
									datum[-o - 1] = *l;
									MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
								}
								plist->m_lock.EndRead();
							}
							ptable->m_lock.EndRead();
						}
					}
					
					if (!bDone)
					{
						// [no data to help - i.e. someone wants whole db; might as well call System.hang()
						ptable->m_lock.StartRead();
						for (CMapIntToIntSet::iterator j = ptable->m_mapForward.begin(); j != ptable->m_mapForward.end(); j++)
						{
							for (int k = 0; k < cExistentials; k++)
								datum[k] = (*paExistingData)[i * cExistentials + k];
							datum[-p - 1] = (*j).first;
							CIntSet* plist = (*j).second;
							plist->m_lock.StartRead();
							for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
							{
								datum[-o - 1] = *l;
								MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
							}
							plist->m_lock.EndRead();
						}
						ptable->m_lock.EndRead();
					}
				}

				EndUsingNode(&rnodeSubject);
			}
		}

		m_lockNodes.EndRead();
	}

	else if (!bSubjectSpecified && !bPredicateSpecified && bObjectSpecified)
	{
		// ?x ?y <>
		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			if (cQueryStatements > 1)
			{
				delete paResultData;
				delete paResultCounts;
			}
			return false;
		}

		CNode& rnodeObject = GetNode(o);
		if (rnodeObject.GetHasTables())
		{
			StartUsingNode(&rnodeObject);
			CTable* ptable = rnodeObject.GetData()->GetObjectTable();
			if (ptable != NULL)
			{
				bool bDone = false;

				if (paExistingData)
				{
					if ((*paExistingCounts)[-p - 1] == cExistingData / cExistentials)
					{
						bDone = true;

						for (int j = 0; j < cExistingData / cExistentials; j++)
						{
							CIntSet* plist = ptable->GetForwardList((*paExistingData)[j * cExistentials - p - 1], false);
							if (plist != NULL)
							{
								for (int k = 0; k < cExistentials; k++)
									datum[k] = (*paExistingData)[j * cExistentials + k];

								plist->m_lock.StartRead();
								if (datum[-s - 1] == 0)
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										datum[-s - 1] = *l;
										MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								else
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										if (datum[-s - 1] == *l)
											MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								plist->m_lock.EndRead();
							}
						}
					}
				}
				
				if (!bDone)
				{
					ptable->m_lock.StartRead();
					for (CMapIntToIntSet::iterator j = ptable->m_mapForward.begin(); j != ptable->m_mapForward.end(); j++)
					{
						datum[-p - 1] = (*j).first;
						CIntSet* plist = (*j).second;
						plist->m_lock.StartRead();
						for (CIntSetIterator k = plist->begin(); k != plist->end(); k++)
						{
							datum[-s - 1] = *k;
							MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
						}
						plist->m_lock.EndRead();
					}
					ptable->m_lock.EndRead();
				}
			}

			EndUsingNode(&rnodeObject);
		}

		m_lockNodes.EndRead();
	}
	else if (!bSubjectSpecified && bPredicateSpecified && !bObjectSpecified)
	{
		// ?x <> ?z
		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			if (cQueryStatements > 1)
			{
				delete paResultData;
				delete paResultCounts;
			}
			return false;
		}

		bool bDone = false;

		if (paExistingData)
		{
			// TODO: can optimize with a metric comparison
			if ((*paExistingCounts)[-s - 1] == cExistingData / cExistentials)
			{
				bDone = true;

				for (int j = 0; j < cExistingData / cExistentials; j++)
				{
					CNode& rnodeSubject = GetNode((*paExistingData)[j * cExistentials - s - 1]);
					if (rnodeSubject.GetHasTables())
					{
						StartUsingNode(&rnodeSubject);
						CTable* ptable = rnodeSubject.GetData()->GetSubjectTable();
						if (ptable != NULL)
						{
							CIntSet* plist = ptable->GetForwardList(p, false);
							if (plist != NULL)
							{
								for (int k = 0; k < cExistentials; k++)
									datum[k] = (*paExistingData)[j * cExistentials + k];
								plist->m_lock.StartRead();
								if (datum[-o - 1] == 0)
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										datum[-o - 1] = *l;
										MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								else
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										if (datum[-o - 1] == *l)
											MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								plist->m_lock.EndRead();
							}
						}
						EndUsingNode(&rnodeSubject);
					}
				}
			}
			else if ((*paExistingCounts)[-o - 1] == cExistingData / cExistentials)
			{
				bDone = true;

				for (int j = 0; j < cExistingData / cExistentials; j++)
				{
					CNode& rnodeObject = GetNode((*paExistingData)[j * cExistentials - o - 1]);
					if (rnodeObject.GetHasTables())
					{
						StartUsingNode(&rnodeObject);
						CTable* ptable = rnodeObject.GetData()->GetObjectTable();
						if (ptable != NULL)
						{
							CIntSet* plist = ptable->GetForwardList(p, false);
							if (plist != NULL)
							{
								for (int k = 0; k < cExistentials; k++)
									datum[k] = (*paExistingData)[j * cExistentials + k];
								plist->m_lock.StartRead();
								if (datum[-s - 1] == 0)
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										datum[-s - 1] = *l;
										MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								else
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										if (datum[-s - 1] == *l)
											MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								plist->m_lock.EndRead();
							}
						}
						EndUsingNode(&rnodeObject);
					}
				}
			}
			else
			{
				// Partial solution that skips over <blank> <> <blank>
				bDone = true;

				for (int j = 0; j < cExistingData / cExistentials; j++)
				{
					CNode& rnodeObject = GetNode((*paExistingData)[j * cExistentials - o - 1]);
					if (rnodeObject.GetHasTables())
					{
						StartUsingNode(&rnodeObject);
						CTable* ptable = rnodeObject.GetData()->GetObjectTable();
						if (ptable != NULL)
						{
							CIntSet* plist = ptable->GetForwardList(p, false);
							if (plist != NULL)
							{
								for (int k = 0; k < cExistentials; k++)
									datum[k] = (*paExistingData)[j * cExistentials + k];
								plist->m_lock.StartRead();
								if (datum[-s - 1] == 0)
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										datum[-s - 1] = *l;
										MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								else
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										if (datum[-s - 1] == *l)
											MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								plist->m_lock.EndRead();
							}
						}
						EndUsingNode(&rnodeObject);
					}
					else
					{
						CNode& rnodeSubject = GetNode((*paExistingData)[j * cExistentials - s - 1]);
						if (rnodeSubject.GetHasTables())
						{
							StartUsingNode(&rnodeSubject);
							CTable* ptable = rnodeSubject.GetData()->GetSubjectTable();
							if (ptable != NULL)
							{
								CIntSet* plist = ptable->GetForwardList(p, false);
								if (plist != NULL)
								{
									for (int k = 0; k < cExistentials; k++)
										datum[k] = (*paExistingData)[j * cExistentials + k];
									plist->m_lock.StartRead();
									if (datum[-o - 1] == 0)
									{
										for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
										{
											datum[-o - 1] = *l;
											MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
										}
									}
									else
									{
										for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
										{
											if (datum[-o - 1] == *l)
												MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
										}
									}
									plist->m_lock.EndRead();
								}
							}
							EndUsingNode(&rnodeSubject);
						}
					}
				}
			}
		}
		
		if (!bDone)
		{
			// This is just too inefficient...
			/*
			int iUpperBound = *m_pcNodes + 1;
			for (int i = 1; i < iUpperBound; i++)
			{
				CNode& rnodeSubject = GetNode(i);
				if (rnodeSubject.GetHasTables())
				{
					StartUsingNode(&rnodeSubject);

					CTable* ptable = rnodeSubject.GetData()->GetSubjectTable();
					if (ptable != NULL)
					{
						CIntSet* plist = ptable->GetForwardList(p, false);
						if (plist != NULL)
						{
							datum[-s - 1] = i;
							plist->m_lock.StartRead();
							for (CIntSetIterator k = plist->begin(); k != plist->end(); k++)
							{
								datum[-o - 1] = *k;
								MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
							}
							plist->m_lock.EndRead();
						}
					}

					EndUsingNode(&rnodeSubject);
				}
			}*/
		}

		m_lockNodes.EndRead();
	}
	else if (bSubjectSpecified && !bPredicateSpecified && !bObjectSpecified)
	{
		// <> ?y ?z
		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			if (cQueryStatements > 1)
			{
				delete paResultData;
				delete paResultCounts;
			}
			return false;
		}

		CNode& rnodeSubject = GetNode(s);
		if (rnodeSubject.GetHasTables())
		{
			StartUsingNode(&rnodeSubject);
			CTable* ptable = rnodeSubject.GetData()->GetSubjectTable();

			if (ptable != NULL)
			{
				bool bDone = false;

				if (paExistingData)
				{
					if ((*paExistingCounts)[-p - 1] == cExistingData / cExistentials)
					{
						bDone = true;

						for (int j = 0; j < cExistingData / cExistentials; j++)
						{
							CIntSet* plist = ptable->GetForwardList((*paExistingData)[j * cExistentials - p - 1], false);
							if (plist != NULL)
							{
								for (int k = 0; k < cExistentials; k++)
									datum[k] = (*paExistingData)[j * cExistentials + k];
								plist->m_lock.StartRead();
								if (datum[-o - 1] == 0)
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										datum[-o - 1] = *l;
										MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								else
								{
									for (CIntSetIterator l = plist->begin(); l != plist->end(); l++)
									{
										if (datum[-o - 1] == *l)
											MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
									}
								}
								plist->m_lock.EndRead();
							}
						}
					}
				}
				
				if (!bDone)
				{
					ptable->m_lock.StartRead();
					for (CMapIntToIntSet::iterator j = ptable->m_mapForward.begin(); j != ptable->m_mapForward.end(); j++)
					{
						datum[-p - 1] = (*j).first;
						CIntSet* plist = (*j).second;
						plist->m_lock.StartRead();
						for (CIntSetIterator k = plist->begin(); k != plist->end(); k++)
						{
							datum[-o - 1] = *k;
							MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
						}
						plist->m_lock.EndRead();
					}
					ptable->m_lock.EndRead();
				}
			}

			EndUsingNode(&rnodeSubject);
		}

		m_lockNodes.EndRead();
	}
	else if (!bSubjectSpecified && bPredicateSpecified && bObjectSpecified)
	{
		// ?x <> <>
		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			if (cQueryStatements > 1)
			{
				delete paResultData;
				delete paResultCounts;
			}
			return false;
		}
		
		// Find the most efficient table to use
		CNode& rnodeObject = GetNode(o);
		if (rnodeObject.GetHasTables())
		{
			StartUsingNode(&rnodeObject);
			CTable* pTable = rnodeObject.GetData()->GetObjectTable();
			if (pTable != NULL)
			{
				CIntSet* plist = pTable->GetForwardList(p, false);
				if (plist != NULL)
				{
					plist->m_lock.StartRead();
					for (CIntSetIterator j = plist->begin(); j != plist->end(); j++)
					{
						datum[-s - 1] = *j;
						MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
					}
					plist->m_lock.EndRead();
				}
			}
			EndUsingNode(&rnodeObject);
		}

		m_lockNodes.EndRead();
	}
	else if (bSubjectSpecified && !bPredicateSpecified && bObjectSpecified)
	{
		// <> ?y <>
		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			if (cQueryStatements > 1)
			{
				delete paResultData;
				delete paResultCounts;
			}
			return false;
		}
		
		CNode& rnodeSubject = GetNode(s);
		if (rnodeSubject.GetHasTables())
		{
			StartUsingNode(&rnodeSubject);
			CTable* pTable = rnodeSubject.GetData()->GetSubjectTable();
			if (pTable != NULL)
			{
				if (paExistingCounts && ((*paExistingCounts)[-p - 1] == cExistingData / cExistentials))
				{
					for (int j = 0; j < cExistingData / cExistentials; j++)
					{
						int i = (*paExistingData)[j * cExistentials - p - 1];
						CIntSet* plist = pTable->GetForwardList(i, false);
						if (plist != NULL && plist->Contains(o))
						{
							for (int k = 0; k < cExistentials; k++)
								datum[k] = (*paExistingData)[j * cExistentials + k];
							MergeRows(datum, cExistentials, NULL, paResultData, paResultCounts);
						}
					}
				}
				else
				{
					pTable->m_lock.StartRead();
					for (CMapIntToIntSet::iterator j = pTable->m_mapForward.begin(); j != pTable->m_mapForward.end(); j++)
					{
						CIntSet* plist = (*j).second;
						plist->m_lock.StartRead();
						if (plist->Contains(o))
						{
							datum[-p - 1] = (*j).first;
							MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
						}
						plist->m_lock.EndRead();
					}
					pTable->m_lock.EndRead();
				}
			}		
			
			EndUsingNode(&rnodeSubject);
		}

		m_lockNodes.EndRead();
	}
	else if (bSubjectSpecified && bPredicateSpecified && !bObjectSpecified)
	{
		// <> <> ?z
		m_lockNodes.StartRead();
		if (m_bShuttingDown)
		{
			m_lockNodes.EndRead();
			if (cQueryStatements > 1)
			{
				delete paResultData;
				delete paResultCounts;
			}
			return false;
		}
		
		// Find the most efficient table to use
		CNode& rnodeSubject = GetNode(s);
		StartUsingNode(&rnodeSubject);
		CTable* pTable = rnodeSubject.GetData()->GetSubjectTable();
		if (pTable != NULL)
		{
			CIntSet* plist = pTable->GetForwardList(p, false);
			if (plist != NULL)
			{
				plist->m_lock.StartRead();
				for (CIntSetIterator j = plist->begin(); j != plist->end(); j++)
				{
					datum[-o - 1] = *j;
					MergeRows(datum, cExistentials, paExistingData, paResultData, paResultCounts);
				}
				plist->m_lock.EndRead();
			}
		}
		EndUsingNode(&rnodeSubject);
		m_lockNodes.EndRead();
	}
	else
	{
		puts(">> Cholesterol: no existentials in statement");
		return false; // If there are no existentials, there cannot be a solution
	}

	if (cQueryStatements == 1)
		return true; // We are done

	// Perform remaining lines of query
	CIntVector aRemainingQuery = raQuery;
	aRemainingQuery.erase(aRemainingQuery.begin() + iBestScore * 3, aRemainingQuery.begin() + (iBestScore + 1) * 3);
	bool bResult = Query(aRemainingQuery, cExistentials, paResultData, paResultCounts, paResultData0, paResultCounts0);
	delete paResultData;
	delete paResultCounts;

	return bResult;
}

