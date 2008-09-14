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

// Database.h

#ifndef __DATABASE_H__
#define __DATABASE_H__

#include "Platform.h"

class CTable;
class CDatabase;

#define SECTOR_SIZE (512)

struct CNodeRuntimeData
{
	CTable* m_pSubjectTable;
	CTable* m_pObjectTable;

	inline CTable* GetSubjectTable() { return m_pSubjectTable; }
	inline CTable* GetObjectTable() { return m_pObjectTable; }

	int m_subject;
	int m_predicate;
	int m_object;

	inline int& GetReifiedSubject()
	{
		return m_subject;
	}

	inline int& GetReifiedPredicate()
	{
		return m_predicate;
	}

	inline int& GetReifiedObject()
	{
		return m_object;
	}

	time_t m_timeLastUsed;
	char* m_pszData;

	int m_iNext;
};

struct CReadWriteLockAndData : public CReadWriteLock
{
	CNodeRuntimeData* m_pData;

#ifdef CLEAN_LOCKS
	CReadWriteLockAndData() : m_cUsers(0) {}
	int m_cUsers;
#endif
};

CReadWriteLockAndData* AllocReadWriteLockAndData();
void FreeReadWriteLockAndData(CReadWriteLockAndData* p);

struct CNode
{
	CReadWriteLockAndData* m_plockLoad;
	int m_iFile;
	int m_nHash;
	unsigned char m_otherData[2 + sizeof(short) / sizeof(unsigned char) * 2];

	short& GetSector() { return *(short*)(m_otherData + 2); }
	short& GetSectors() { return *((short*)(m_otherData + 2) + 1); }
	unsigned char& GetLoaded() { return m_otherData[0]; }
	unsigned char GetChanged() { return m_otherData[1] & 0x0f; }
	unsigned char GetHasTables() { return (m_otherData[1] & 0xf0) >> 4; }
	void SetChanged(unsigned char b = 1) { m_otherData[1] = (b & 0x0f) | (GetHasTables() << 4); }
	void SetHasTables(unsigned char b = 1) { m_otherData[1] = GetChanged() | ((b & 0x0f) << 4); }

	inline CTable* GetSubjectTableAutoCreate();
	inline CTable* GetObjectTableAutoCreate();

	inline char* GetString()
	{
		return GetData()->m_pszData;
	}
	
	inline CNodeRuntimeData* GetData() const { return m_plockLoad->m_pData; }

	void Create(const char* pszDatum, const char* pszBasePath);
	void Init(const char* pszBasePath);

	inline void Cleanup()
	{
		ASSERT(this != NULL);
	}

	bool StartUse(const char* pszBasePath, CDatabase* pdb);
	void EndUse(CDatabase* pdb);
	void AboutToUnload(CDatabase* pdb);
	short Unload(FILE* fp, int iFile, short iSector);
};

struct node_less : public binary_function <CNode*, CNode*, bool> 
{
	bool operator()(const CNode* _Left, const CNode* _Right) const
	{
		return _Left->GetData()->m_timeLastUsed < _Right->GetData()->m_timeLastUsed;
	}
};

class CTable
{
public:
	CMapIntToIntSet m_mapForward;

	CReadWriteLock m_lock;

	~CTable()
	{
		CMapIntToIntSet::iterator i;
		for (i = m_mapForward.begin(); i != m_mapForward.end(); i++)
			delete (*i).second;
	}

	inline CIntSet* GetForwardList(int i, bool bAutoCreate = true)
	{
		if (bAutoCreate)
			m_lock.StartWrite();
		else
			m_lock.StartRead();

		CIntSet* p;
		CMapIntToIntSet::iterator it = m_mapForward.find(i);
		if (it != m_mapForward.end())
		{
			p = (*it).second;
			if (bAutoCreate)
				m_lock.EndWrite();
			else
				m_lock.EndRead();
			return p;
		}
		if (!bAutoCreate)
		{
			m_lock.EndRead();
			return NULL;
		}
		p = new CIntSet;
		m_mapForward[i] = p;
		m_lock.EndWrite();
		return p;
	}

	inline bool Add(int subject, int object)
	{
		CIntSet* p1 = GetForwardList(subject);
		m_lock.StartWrite();
		if (!p1->Contains(object))
		{
			p1->AddTail(object);
			m_lock.EndWrite();
			return true;
		}
		else
		{
			m_lock.EndWrite();
			return false;
		}
	}

	inline bool Remove(int subject, int object)
	{
		CIntSet* p1 = GetForwardList(subject, false);
	
		m_lock.StartWrite();
		if (p1)
		{
			CIntSetIterator it = p1->find(object);
			if (it != p1->end())
			{
				p1->erase(it);

				m_lock.EndWrite();
				return true;
			}
			m_lock.EndWrite();
			return false;
		}
		else
		{
			m_lock.EndWrite();
			return false;
		}
	}

	inline bool Contains(int x, int y)
	{
		CIntSet* p1 = GetForwardList(x, false);
		if (p1 == NULL)
			return false;

		m_lock.StartRead();
		bool bRet = p1->Contains(y);
		m_lock.EndRead();
		return bRet;
	}
};

class CLiteralCallback
{
public:
	virtual void OnNewLiteral(const char* pszLiteral) = 0;
};

class CDatabase
{
public:
	CDatabase(const char* pszBasePath, CLiteralCallback* pLiteralCallback);
	void Shutdown();
	~CDatabase();

	bool m_bShuttingDown;
	bool m_bDefrag;
	bool m_bExport;
	CCounter m_cRunningThreads;
	CCounter* m_pcFiles;

	CCriticalSection m_csLocks;
	CCriticalSection m_csNodes;
	int m_cMaxNodesInMemory;
	deque<CNode*> m_listNodesInMemory;
	int* m_pbClean;

	inline void StartUsingNode(CNode* pnode, bool bMarkChanged = false)
	{
		if (pnode->StartUse(m_pszBasePath, this))
		{
			m_csNodes.Enter();
			m_listNodesInMemory.push_back(pnode);
			m_csNodes.Leave();
		}

		if (bMarkChanged)
			pnode->SetChanged();
	}

	inline CNode& GetNode(int i)
	{
		return m_apNodes[i >> 18][i & 262143];
	}

	inline void EndUsingNode(CNode* pnode)
	{
		pnode->EndUse(this);
	}

	void DebugPrintDatum(const CIntVector &datum, int cExistentials);
	
	const char *GetString(int i) 
	{
		if (i == 0) return "(i==0)";

		CNode& rnode = GetNode(i);
		StartUsingNode(&rnode);
		char* psz = rnode.GetString();
		EndUsingNode(&rnode);
		return psz;
	}

	CReadWriteLock m_lockIDs;
	hash_map<long, int> m_resourceIDs;
	inline int GetID(const char* lpsz, bool bAutoCreate = true)
	{
		assert(lpsz != NULL);
		if (!lpsz)
			return 0;

		if (bAutoCreate)
			m_lockIDs.StartWrite();
		else
			m_lockIDs.StartRead();
		int i;

		hash_map<long, int>::iterator it = m_resourceIDs.find(HashString(lpsz));
		if (it != m_resourceIDs.end())
		{
			i = (*it).second;
			
			do
			{
				CNode& rnode = GetNode(i);
				StartUsingNode(&rnode);
				int nCmp = strcmp(rnode.GetString(), lpsz);
				if (nCmp == 0)
				{
					EndUsingNode(&rnode);
					if (bAutoCreate)
						m_lockIDs.EndWrite();
					else
						m_lockIDs.EndRead();
					return i;
				}
				i = rnode.GetData()->m_iNext;
				EndUsingNode(&rnode);
			}
			while (i != 0);
		}

		if (!bAutoCreate)
		{
			m_lockIDs.EndRead();
			return 0;
		}

		m_lockNodes.StartWrite();
		if (0 == m_cNodeSpace)
			GrowDatabase();

		i = (*m_pcNodes) + 1;
#if defined(_DEBUG) || defined(LOGGING)
		if (i % 10000 == 0)
			printf(">> Cholesterol: %i nodes present\n" , i);
#endif
		CNode& rnode = GetNode(i);
		m_csLocks.Enter();
		rnode.Create(lpsz, m_pszBasePath);
		m_csLocks.Leave();
		assert(rnode.GetData()->m_pszData != NULL);
		m_csNodes.Enter();
		m_listNodesInMemory.push_back(&rnode);
		m_csNodes.Leave();
		++(*m_pcNodes);
		if (m_resourceIDs.find(rnode.m_nHash) == m_resourceIDs.end())
			m_resourceIDs[rnode.m_nHash] = i;
		else
		{
			CNode* pNode = &GetNode(m_resourceIDs[rnode.m_nHash]);
			StartUsingNode(pNode);
			while (pNode->GetData()->m_iNext != 0)
			{
				CNode* pOldNode = pNode;
				pNode = &GetNode(pNode->GetData()->m_iNext);
				EndUsingNode(pOldNode);
				StartUsingNode(pNode);
			}
			pNode->SetChanged();
			pNode->GetData()->m_iNext = i;
			EndUsingNode(pNode);
		}
		--m_cNodeSpace;
		m_lockIDs.EndWrite();
		m_lockNodes.EndWrite();
		return i;
	}

	void GrowDatabase(bool bWipe = true);

	bool Add(int s, int p, int o, int id);
	void Remove(int s, int p, int o, CIntVector* paRemoved);
	bool Contains(int s, int p, int o);
	int Extract(int s, int p, int o);
	bool Query(CIntVector& raQuery, int cExistentials, CIntVector* paExistingData, CIntVector* paExistingCounts, CIntVector* paResultData, CIntVector* paResultCounts);

	// Logging functionality
	void LoadFromLog(CLiteralCallback* pLiteralCallback);
	void WriteStatementToLog(char code, CNode* pSubject, CNode* pPredicate, CNode* pObject, CNode* pID);
	void FlushLog();
	void SetupLog();
	void WriteToLog(const void *buffer, int length);
	CCriticalSection m_csLog;
	char* m_pszLogFilename;
	int m_nCacheSize;
	int m_nCacheLoc;
	void* m_pCache;
	bool m_bReloading;

	CReadWriteLock m_lockNodes;
	vector<CMemoryMapping*> m_aMemoryMaps;
	CNode* m_apNodes[1024];
	int* m_pcNodes;
	int m_cNodeSpace;
	char* m_pszBasePath;
};

#endif // __DATABASE_H__

