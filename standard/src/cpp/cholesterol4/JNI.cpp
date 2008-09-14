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

// JNI.cpp

#include <jni.h>

#include "stdafx.h"
#include "Database.h"

#include "edu_mit_lcs_haystack_server_rdfstore_CholesterolRDFStoreService.h"

//#define FULL_LOGGING 1
#ifdef FULL_LOGGING
FILE* g_fpLog;
CCriticalSection g_csLog;
#endif // FULL_LOGGING

class CJavaStringWrapper
{
public:
	const char* m_psz;
	JNIEnv* m_env;
	jstring m_str;

	CJavaStringWrapper(JNIEnv* env, jstring str)
	{
		jboolean b = JNI_FALSE;
		m_env = env;
		m_str = str;
		m_psz = env->GetStringUTFChars(str, &b);
	}
	
	~CJavaStringWrapper()
	{
		m_env->ReleaseStringUTFChars(m_str, m_psz);

		JNIEnv * env = m_env;
	}

	operator const char*()
	{
		return m_psz;
	}
};

struct CState
{
	inline static CState* GetState(JNIEnv* env, jobject o)
	{
		jclass clsCholesterol = env->FindClass("edu/mit/lcs/haystack/server/rdfstore/Cholesterol3RDFStoreService");
		jfieldID fidDatabase = env->GetFieldID(clsCholesterol, "m_database", "I");

		CState *p = (CState*)env->GetIntField(o, fidDatabase);
		return p;
	}

	CState(JNIEnv* env, jobject o, const char* pszFilename);

	~CState()
	{
		m_lock.Cleanup();
	}

	void Cleanup()
	{
		m_lock.StartWrite();
		delete m_pDatabase;
		m_pDatabase = NULL;
		m_lock.EndWrite();
	}

	inline jobject CreateHashSet(JNIEnv* env)
	{
		return env->NewObject(m_clsHashSet, m_mtHashSet);
	}

	inline void AddToCollection(JNIEnv* env, jobject collection, jobject datum)
	{
		env->CallObjectMethod(collection, m_mtAdd, datum);
	}

	inline void IndexLiteral(JNIEnv* env, jobject cholesterol, jobject datum)
	{
		env->CallObjectMethod(cholesterol, m_mtIndexLiteral, datum);
	}

	inline void AddToRemoveQueue(JNIEnv* env, jobject cholesterol, CNode& rnodeSubject, CNode& rnodePredicate, CNode& rnodeObject)
	{
		m_pDatabase->StartUsingNode(&rnodeSubject);
		jstring s = env->NewStringUTF(rnodeSubject.GetString());
		m_pDatabase->EndUsingNode(&rnodeSubject);

		m_pDatabase->StartUsingNode(&rnodePredicate);
		jstring p = env->NewStringUTF(rnodePredicate.GetString());
		m_pDatabase->EndUsingNode(&rnodePredicate);

		m_pDatabase->StartUsingNode(&rnodeObject);
		jstring o = env->NewStringUTF(rnodeObject.GetString());
		m_pDatabase->EndUsingNode(&rnodeObject);

		env->CallVoidMethod(cholesterol, m_mtAddToRemoveQueue, s, p, o);
	}

	inline jstring ToString(JNIEnv* env, jobject o)
	{
		return (jstring)env->CallObjectMethod(o, m_mtToString);
	}

	inline jobject CreateRDFNode(JNIEnv* env, CNode& rnode)
	{
		m_pDatabase->StartUsingNode(&rnode);
		char* psz = rnode.GetString();
		string str(psz + 1, strlen(psz) - 2);
		m_pDatabase->EndUsingNode(&rnode);
		jstring jstr = env->NewStringUTF(str.c_str());
		if (psz[0] == '<') {
			return env->NewObject(m_clsResource, m_mtResource, jstr);
		} else {
			return env->NewObject(m_clsLiteral, m_mtLiteral, jstr);
		}
	}

	inline jobject CreateStatement(JNIEnv* env, jobject s, jobject p, jobject o)
	{
		return env->NewObject(m_clsStatement, m_mtStatement, s, p, o);
	}

	inline jobject GetSubject(JNIEnv* env, jobject statement)
	{
		return env->CallNonvirtualObjectMethod(statement, m_clsStatement, m_mtGetSubject);
	}

	inline jobject GetPredicate(JNIEnv* env, jobject statement)
	{
		return env->CallNonvirtualObjectMethod(statement, m_clsStatement, m_mtGetPredicate);
	}

	inline jobject GetObject(JNIEnv* env, jobject statement)
	{
		return env->CallNonvirtualObjectMethod(statement, m_clsStatement, m_mtGetObject);
	}

	CDatabase* m_pDatabase;
	CReadWriteLock m_lock;
	jclass m_clsObject;
	jclass m_clsResource;
	jclass m_clsLiteral;
	jclass m_clsStatement;
	jclass m_clsRDFNode;
	jclass m_clsHashSet;
	jmethodID m_mtToString;
	jmethodID m_mtResource;
	jmethodID m_mtLiteral;
	jmethodID m_mtStatement;
	jmethodID m_mtGetSubject;
	jmethodID m_mtGetPredicate;
	jmethodID m_mtGetObject;
	jmethodID m_mtEquals;
	jmethodID m_mtAdd;
	jmethodID m_mtHashSet;
	jmethodID m_mtAddToRemoveQueue;
	jmethodID m_mtIndexLiteral;
};

class CStateBasedLiteralCallback : public CLiteralCallback
{
public:
	CStateBasedLiteralCallback(CState* pState, JNIEnv* env, jobject o) : m_pState(pState), m_env(env), m_o(o) {}
	CState* m_pState;
	JNIEnv* m_env;
	jobject m_o;
	virtual void OnNewLiteral(const char* pszLiteral);
};

void CStateBasedLiteralCallback::OnNewLiteral(const char* pszLiteral)
{
	m_pState->IndexLiteral(m_env, m_o, m_env->NewStringUTF(pszLiteral));
}

CState::CState(JNIEnv* env, jobject o, const char* pszFilename)
{
	jclass clsCholesterol = env->FindClass("edu/mit/lcs/haystack/server/rdfstore/Cholesterol3RDFStoreService");
	jfieldID fidDatabase = env->GetFieldID(clsCholesterol, "m_database", "I");

	m_clsHashSet = (jclass)env->NewGlobalRef((jobject)env->FindClass("java/util/HashSet"));
	m_clsObject = (jclass)env->NewGlobalRef((jobject)env->FindClass("java/lang/Object"));
	m_clsResource = (jclass)env->NewGlobalRef((jobject)env->FindClass("edu/mit/lcs/haystack/rdf/Resource"));
	m_clsLiteral = (jclass)env->NewGlobalRef((jobject)env->FindClass("edu/mit/lcs/haystack/rdf/Literal"));
	m_clsRDFNode = (jclass)env->NewGlobalRef((jobject)env->FindClass("edu/mit/lcs/haystack/rdf/RDFNode"));
	m_clsStatement = (jclass)env->NewGlobalRef((jobject)env->FindClass("edu/mit/lcs/haystack/rdf/Statement"));
	m_mtToString = env->GetMethodID(m_clsObject, "toString", "()Ljava/lang/String;");
	m_mtResource = env->GetMethodID(m_clsResource, "<init>", "(Ljava/lang/String;)V");
	m_mtLiteral = env->GetMethodID(m_clsLiteral, "<init>", "(Ljava/lang/String;)V");
	m_mtStatement = env->GetMethodID(m_clsStatement, "<init>", "(Ledu/mit/lcs/haystack/rdf/Resource;Ledu/mit/lcs/haystack/rdf/Resource;Ledu/mit/lcs/haystack/rdf/RDFNode;)V");
	m_mtGetSubject = env->GetMethodID(m_clsStatement, "getSubject", "()Ledu/mit/lcs/haystack/rdf/Resource;");
	m_mtGetPredicate = env->GetMethodID(m_clsStatement, "getPredicate", "()Ledu/mit/lcs/haystack/rdf/Resource;");
	m_mtGetObject = env->GetMethodID(m_clsStatement, "getObject", "()Ledu/mit/lcs/haystack/rdf/RDFNode;");
	m_mtEquals = env->GetMethodID(m_clsObject, "equals", "(Ljava/lang/Object;)Z");
	m_mtHashSet = env->GetMethodID(m_clsHashSet, "<init>", "()V");
	m_mtAdd = env->GetMethodID(m_clsHashSet, "add", "(Ljava/lang/Object;)Z");
	m_mtAddToRemoveQueue = env->GetMethodID(clsCholesterol, "addToRemoveQueue", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	m_mtIndexLiteral = env->GetMethodID(clsCholesterol, "indexLiteral", "(Ljava/lang/String;)V");
	
	CStateBasedLiteralCallback literalCallback(this, env, o);

	m_pDatabase = new CDatabase(pszFilename, &literalCallback);
	env->SetIntField(o, fidDatabase, (jint)this);

	m_lock.Init();
}

class CUseState
{
public:
	CUseState(CState* pState) : m_pState(pState)
	{
		m_pState->m_lock.StartRead();
	}

	~CUseState()
	{
		m_pState->m_lock.EndRead();
	}

	CState* m_pState;
};

/*
#include <crtdbg.h>
extern _CrtMemState g_state;
*/

extern "C" {

JNIEXPORT void JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_doNativeInit
  (JNIEnv * env, jobject obj, jstring filename)
{
	new CState(env, obj, CJavaStringWrapper(env, filename));

#ifdef FULL_LOGGING
	g_fpLog = fopen("c:\\cholesterollog.sql", "wt");
	fprintf(g_fpLog, "create table rdf (SUBJECT TEXT, PREDICATE TEXT, OBJECT TEXT, ID TEXT)\n");
	g_csLog.Init();
#endif // FULL_LOGGING
}

#ifdef FULL_LOGGING
}
string FilterCRLF(const char* ach)
{
	string s;
	while (*ach)
	{
		switch (*ach)
		{
		case '\n':
			s+=("\\n");
			break;
		case '\r':
			s+=("\\r");
			break;
		case '\'':
			s+=("\\\'");
			break;
		case '\"':
			s+=("\\\"");
			break;
		default:
			s+=(*ach);
		}

		++ach;
	}
	return s;
}
extern "C" {

#define FILTER(x) (FilterCRLF(x).c_str())

void generateCondition(int r1, int pos, char* field, string* existentialMap, CDatabase* pdb) {
	string var = r1 > 0 ? "" : existentialMap[-r1 - 1];
	char achPosition[20];
	sprintf(achPosition, "x%i.%s", pos, field);
	if (!stricmp(var.c_str(), achPosition)) {
		return;
	}

	fprintf(g_fpLog, " AND %s=", achPosition);

	if (var.length() == 0) {
		CNode& rnode = pdb->GetNode(r1);
		pdb->StartUsingNode(&rnode);
		fprintf(g_fpLog, "\'%s\'", FILTER(rnode.GetString()));
		pdb->EndUsingNode(&rnode);
	} else {
		fprintf(g_fpLog, var.c_str());
	}
}
#endif // FULL_LOGGING

JNIEXPORT void JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_doNativeKill
  (JNIEnv * env, jobject obj)
{
	CState::GetState(env, obj)->Cleanup();

#ifdef FULL_LOGGING
	fclose(g_fpLog);
	g_csLog.Cleanup();
#endif // FULL_LOGGING

	// TODO: clean up memory leak

	/*{
		_CrtMemState state2, stateDiff;
		_CrtMemCheckpoint(&state2);
		_CrtMemDifference(&stateDiff, &g_state, &state2);
		_CrtMemDumpStatistics(&stateDiff);
		_CrtMemDumpAllObjectsSince(&stateDiff);
	}*/
}

JNIEXPORT void JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_setDefrag
  (JNIEnv * env, jobject obj)
{
	CState::GetState(env, obj)->m_pDatabase->m_bDefrag = true;
}

JNIEXPORT jint JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_add
  (JNIEnv * env, jobject obj, jstring s, jstring p, jstring o, jstring id)
{
	if (!s || !p || !o || !id)
		return JNI_FALSE;

	CState* pState = CState::GetState(env, obj);
	CUseState us(pState);
	CDatabase* pdb = pState->m_pDatabase;
	if (pdb == NULL)
		return JNI_FALSE;

	CJavaStringWrapper s2(env, s);
	CJavaStringWrapper p2(env, p);
	CJavaStringWrapper o2(env, o);
	CJavaStringWrapper id2(env, id);

	int oid = pdb->GetID(o2, false);
	int newLiteral = 0;
	if (oid == 0 && o2[0] == '\"')
		newLiteral = 32;
	oid = pdb->GetID(o2);

#ifdef FULL_LOGGING
	g_csLog.Enter();
	fprintf(g_fpLog, "insert into rdf values (\'%s\', \'%s\', \'%s\', \'%s\')\n", FILTER(s2), FILTER(p2), FILTER(o2), FILTER(id2));
	g_csLog.Leave();
#endif // FULL_LOGGING

	return pdb->Add(pdb->GetID(s2), pdb->GetID(p2), oid, pdb->GetID(id2)) ? (1 | newLiteral) : (0 | newLiteral);
}

JNIEXPORT jboolean JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_contains
  (JNIEnv * env, jobject obj, jstring s, jstring p, jstring o)
{
	if (!s || !p || !o)
		return JNI_FALSE;

	CState* pState = CState::GetState(env, obj);
	CUseState us(pState);
	CDatabase* pdb = pState->m_pDatabase;
	if (pdb == NULL)
		return JNI_FALSE;

	CJavaStringWrapper s2(env, s);
	CJavaStringWrapper p2(env, p);
	CJavaStringWrapper o2(env, o);

#ifdef FULL_LOGGING
	g_csLog.Enter();
	fprintf(g_fpLog, "select ID from rdf where SUBJECT=\'%s\' and PREDICATE=\'%s\' and OBJECT=\'%s\'\n", FILTER(s2), FILTER(p2), FILTER(o2));
	g_csLog.Leave();
#endif // FULL_LOGGING

	return pdb->Contains(pdb->GetID(s2), pdb->GetID(p2), pdb->GetID(o2)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_getStatement
	(JNIEnv * env, jobject obj, jobject id)
{
	if (!id)
		return NULL;

	CState* pState = CState::GetState(env, obj);
	CUseState us(pState);
	CDatabase* pdb = pState->m_pDatabase;
	if (pdb == NULL)
		return NULL;

	jstring id2 = (jstring)env->CallObjectMethod(id, pState->m_mtToString);
	int iStatement = pdb->GetID(CJavaStringWrapper(env, id2), false);
	if (iStatement == 0)
		return NULL;

	CNode& rnodeStatement = pdb->GetNode(iStatement);
	pdb->StartUsingNode(&rnodeStatement);
	int iSubject = rnodeStatement.GetData()->GetReifiedSubject(), iPredicate = rnodeStatement.GetData()->GetReifiedPredicate(), iObject = rnodeStatement.GetData()->GetReifiedObject();
	pdb->EndUsingNode(&rnodeStatement);
	if (!iSubject || !iPredicate || !iObject)
		return NULL;

	return pState->CreateStatement(env, pState->CreateRDFNode(env, pdb->GetNode(iSubject)),
		pState->CreateRDFNode(env, pdb->GetNode(iPredicate)),
		pState->CreateRDFNode(env, pdb->GetNode(iObject)));
}

JNIEXPORT jobject JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_extract
  (JNIEnv * env, jobject obj, jobject s, jobject p, jobject o)
{
	// Only one argument may be null
	if ((!!s + !!p + !!o) != 2)
		return NULL;

	CState* pState = CState::GetState(env, obj);
	CUseState us(pState);
	CDatabase* pdb = pState->m_pDatabase;
	if (pdb == NULL)
		return NULL;

	int s3 = 0, p3 = 0, o3 = 0;
	jstring s2, p2, o2;

	if (s)
	{
		s2 = pState->ToString(env, s);
		s3 = pdb->GetID(CJavaStringWrapper(env, s2), false);
		if (!s3)
			return NULL;
	}
	if (p)
	{
		p2 = pState->ToString(env, p);
		p3 = pdb->GetID(CJavaStringWrapper(env, p2), false);
		if (!p3)
			return NULL;
	}
	if (o)
	{
		o2 = pState->ToString(env, o);
		o3 = pdb->GetID(CJavaStringWrapper(env, o2), false);
		if (!o3)
			return NULL;
	}

#ifdef FULL_LOGGING
	g_csLog.Enter();
	if (s && p)
		fprintf(g_fpLog, "select OBJECT from rdf where SUBJECT=\'%s\' and PREDICATE=\'%s\' length 1\n", FILTER(CJavaStringWrapper(env, s2)), FILTER(CJavaStringWrapper(env, p2)));
	else if (s && o)
		fprintf(g_fpLog, "select PREDICATE from rdf where SUBJECT=\'%s\' and OBJECT=\'%s\' length 1\n", FILTER(CJavaStringWrapper(env, s2)), FILTER(CJavaStringWrapper(env, o2)));
	else
		fprintf(g_fpLog, "select SUBJECT from rdf where PREDICATE=\'%s\' and OBJECT=\'%s\' length 1\n", FILTER(CJavaStringWrapper(env, p2)), FILTER(CJavaStringWrapper(env, o2)));
	g_csLog.Leave();
#endif // FULL_LOGGING

	int iResult = pdb->Extract(s3, p3, o3);
	if (!iResult)
		return NULL;

	return pState->CreateRDFNode(env, pdb->GetNode(iResult));
}

inline jobject DoQuery(JNIEnv * env, jobject obj, jobjectArray query, jobjectArray variables, jobjectArray existentials, jobjectArray hints, bool bExtract = false, int* piSize = NULL)
{
	CState* pState = CState::GetState(env, obj);
	CUseState us(pState);
	CDatabase* pdb = pState->m_pDatabase;
	if (pdb == NULL)
		return NULL;

	if (existentials == NULL || variables == NULL || query == NULL)
	{
		puts(">> Cholesterol: null parameter passed in");
		return NULL;
	}

	jsize c = env->GetArrayLength(query);
	jsize cVariables = env->GetArrayLength(variables);

	int i;
	
	// Convert existentials
	jsize cExistentials = env->GetArrayLength(existentials);
	int* pExistentials = new int[cExistentials];
	for (i = 0; i < cExistentials; i++)
	{
		jobject x = env->GetObjectArrayElement(existentials, i);
		if (!x)
		{
			puts(">> Cholesterol: null existential passed in");
			delete [] pExistentials;
			return NULL;
		}
		CJavaStringWrapper js(env, (jstring)pState->ToString(env, x));
		pExistentials[i] = pdb->GetID(js);
	}

	// Map variables over to existential indices
	int* pVariableIndices = new int[cVariables];
	for (i = 0; i < cVariables; i++)
	{
		pVariableIndices[i] = -1;
		jobject x = env->GetObjectArrayElement(variables, i);
		if (!x)
		{
			delete [] pExistentials;
			delete [] pVariableIndices;
			puts(">> Cholesterol: null variable passed in");
			return NULL;
		}

		int iv = pdb->GetID(CJavaStringWrapper(env, pState->ToString(env, x)));

		for (int j = 0; j < cExistentials; j++)
		{
			if (iv == pExistentials[j])
				pVariableIndices[i] = j;
		}
		if (pVariableIndices[i] == -1)
		{
			// A variable that wasn't listed in the existentials
			// Add it in
			pExistentials[cExistentials] = iv;
			pVariableIndices[i] = cExistentials;
			++cExistentials;
		}
	}

	// Translate query
	CIntVector aQuery;
	for (i = 0; i < c; i++)
	{
		jobject statement = env->GetObjectArrayElement(query, i);
		if (statement == NULL)
		{
			delete [] pExistentials;
			delete [] pVariableIndices;
			puts(">> Cholesterol: null statement passed in");
			return NULL;
		}

		jobject subject = pState->GetSubject(env, statement);
		jobject predicate = pState->GetPredicate(env, statement);
		jobject object = pState->GetObject(env, statement);
		if (subject == NULL || predicate == NULL || object == NULL)
		{
			delete [] pExistentials;
			delete [] pVariableIndices;
//			puts(">> Cholesterol: null subject, predicate, or object passed in");
			return NULL;
		}

		jstring s = pState->ToString(env, subject);
		jstring p = pState->ToString(env, predicate);
		jstring o = pState->ToString(env, object);
		if (s == NULL || p == NULL || o == NULL)
		{
			delete [] pExistentials;
			delete [] pVariableIndices;
			puts(">> Cholesterol: null string resolved from subject, predicate, or object passed in");
			return NULL;
		}

		int is = pdb->GetID(CJavaStringWrapper(env, s));
		int ip = pdb->GetID(CJavaStringWrapper(env, p));
		int io = pdb->GetID(CJavaStringWrapper(env, o));

		// Check for existentials
		for (int j = 0; j < cExistentials; j++)
		{
			if (pExistentials[j] == is)
				is = -j - 1;
			if (pExistentials[j] == ip)
				ip = -j - 1;
			if (pExistentials[j] == io)
				io = -j - 1;
		}
		
		aQuery.push_back(is);
		aQuery.push_back(ip);
		aQuery.push_back(io);
	}

#ifdef FULL_LOGGING
	// Create a map of existentials to their first reference
	string* existentialMap = new string[cExistentials];

	char achBuf[10];
	//for each statement s
	for (int j = 0; j < c; j++) {
		//if the subject of the statement is not in the existentialMap yet,
		//and it is one of the existantials, then add it to the map
		//as a second argument, say subject of which statement it is
		int n;
		if ((n = aQuery[j * 3]) < 0)
			existentialMap[-n - 1] = string("x") + itoa(j, achBuf, 10) + string(".SUBJECT");
		if ((n = aQuery[j * 3 + 1]) < 0)
			existentialMap[-n - 1] = string("x") + itoa(j, achBuf, 10) + string(".PREDICATE");
		if ((n = aQuery[j * 3 + 2]) < 0)
			existentialMap[-n - 1] = string("x") + itoa(j, achBuf, 10) + string(".OBJECT");
	}

	// Construct query
	fprintf(g_fpLog, "SELECT ");

	bool comma = false;
	for (j = 0; j < cVariables; j++) {
		//go over all variables, and get what part of which statement each of
	  //them is, if one of the variables is not part of any statement, then
	  //end up throwing an exception
		string s = existentialMap[pVariableIndices[j]];

		if (comma) {
			fprintf(g_fpLog, ", ");
		} else {
			comma = true;
		}
		//will say something like "SELECT x1.SUBJECT, x1.SUBJECTTYPE ..."
		//so if want to select a subject, then put the subject of the statement
		//into the variables array

		fprintf(g_fpLog, s.c_str());
	}
	//will say "...FROM Statements x1, Statements x2...", for however many
	//Statements were passed in
	fprintf(g_fpLog, " from ");
	for (j = 0; j < c; j++) {
		if (j > 0) {
			fprintf(g_fpLog, ", ");
		}
		fprintf(g_fpLog, "rdf x%i", j);
	}

	fprintf(g_fpLog, " where 1=1 ");
	//now for each statement
	for (int i2 = 0; i2 < c; i2++) {
		//for each part generate a condition, based on that part of the statement itself,
  //what it is, and the elements of the existantial map
		generateCondition(aQuery[i2 * 3], i2, "SUBJECT", existentialMap, pdb);
		generateCondition(aQuery[i2 * 3 + 1], i2, "PREDICATE", existentialMap, pdb);
		generateCondition(aQuery[i2 * 3 + 2], i2, "OBJECT", existentialMap, pdb);
	}

	if (bExtract)
		fprintf(g_fpLog, " LENGTH 1\n");
	else
		fprintf(g_fpLog, "\n");

#endif // FULL_LOGGING

	// Translate hints
	CIntVector* paExistingData = NULL;
	CIntVector* paExistingCounts = NULL;
	if (hints != NULL)
	{
		jsize cHints = env->GetArrayLength(hints);
		if (cHints != cExistentials)
		{
			delete [] pExistentials;
			delete [] pVariableIndices;
			printf(">> Cholesterol: %i hints but %i existentials\n", cHints, cExistentials);
			return NULL;
		}

		paExistingData = new CIntVector;
		paExistingCounts = new CIntVector;

		CIntVector aArrayLengths;
		vector<jobjectArray> aArrays;

		int cNonNull = 0;

		for (i = 0; i < cExistentials; i++)
		{
			paExistingCounts->push_back(0);

			jobjectArray arr = (jobjectArray)env->GetObjectArrayElement(hints, i);
			aArrays.push_back(arr);
			if (arr != NULL)
			{
				jsize cElements = env->GetArrayLength(arr);
				aArrayLengths.push_back(cElements);
				++cNonNull;
			}
			else
				aArrayLengths.push_back(0);
		}

		if (cNonNull > 0)
		{
			i = 0;
			bool b;
			do
			{
				b = false;
				for (int j = 0; j < cExistentials; j++)
				{
					if (i < aArrayLengths[j])
					{
						b = true;
						jobject subject = env->GetObjectArrayElement(aArrays[j], i);
						if (subject == NULL)
						{
							delete paExistingData;
							delete paExistingCounts;
							delete [] pExistentials;
							delete [] pVariableIndices;
							printf(">> Cholesterol: null found in hints\n", cHints, cExistentials);
							return NULL;
						}

						jstring s = pState->ToString(env, subject);
						paExistingData->push_back(pdb->GetID(CJavaStringWrapper(env, s)));
						++(*paExistingCounts)[j];
					}
					else
						paExistingData->push_back(0);
				}
				++i;
			}
			while (b);
			for (int j = 0; j < cExistentials; j++)
				paExistingData->pop_back();
		}
		else
		{
			delete paExistingData;
			delete paExistingCounts;
			paExistingData = NULL;
			paExistingCounts = NULL;
		}
	}

	// Perform query
	CIntVector aResultData, aResultCounts;
	for (i = 0; i < cExistentials; i++)
		aResultCounts.push_back(0);
	if (!pdb->Query(aQuery, cExistentials, paExistingData, paExistingCounts, &aResultData, &aResultCounts))
	{
		delete [] pExistentials;
		delete [] pVariableIndices;
		if (paExistingData)
			delete paExistingData;
		if (paExistingCounts)
			delete paExistingCounts;
		return NULL;
	}

	// Write out results
	jobject results = NULL;
	int c2 = aResultData.size() / cExistentials;

	if (bExtract)
	{
		if (c2 > 0)
		{
			int i = 0;
			jobjectArray datum = (jobjectArray)env->NewObjectArray(cVariables, pState->m_clsRDFNode, NULL);
			for (int j = 0; j < cVariables; j++)
			{
				if (aResultData[i * cExistentials + pVariableIndices[j]])
				{
					int n = aResultData[i * cExistentials + pVariableIndices[j]];
					if (n > 0)
					{
						jobject x = pState->CreateRDFNode(env, pdb->GetNode(n));
						env->SetObjectArrayElement(datum, j, x);
					}
					else
					{
						env->SetObjectArrayElement(datum, j, NULL);
					}
				}
			}

			results = datum;
		}
	}
	else
	{
		if (piSize)
			*piSize = c2;
		else
		{
			results = pState->CreateHashSet(env);
			for (i = 0; i < c2; i++)
			{
				jobjectArray datum = (jobjectArray)env->NewObjectArray(cVariables, pState->m_clsRDFNode, NULL);
				for (int j = 0; j < cVariables; j++)
				{
					if (aResultData[i * cExistentials + pVariableIndices[j]])
					{
						int n = aResultData[i * cExistentials + pVariableIndices[j]];
						if (n > 0)
						{
							jobject x = pState->CreateRDFNode(env, pdb->GetNode(n));
							env->SetObjectArrayElement(datum, j, x);
						}
						else
						{
							env->SetObjectArrayElement(datum, j, NULL);
						}
					}
				}
				pState->AddToCollection(env, results, datum);
			}
		}
	}

	delete [] pExistentials;
	delete [] pVariableIndices;
	if (paExistingData)
		delete paExistingData;
	if (paExistingCounts)
		delete paExistingCounts;
	return results;
}

JNIEXPORT jobject JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_query
  (JNIEnv * env, jobject obj, jobjectArray query, jobjectArray variables, jobjectArray existentials)
{
	return DoQuery(env, obj, query, variables, existentials, NULL);
}

JNIEXPORT jobject JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_queryMulti
  (JNIEnv * env, jobject obj, jobjectArray query, jobjectArray variables, jobjectArray existentials, jobjectArray hints)
{
	return DoQuery(env, obj, query, variables, existentials, hints);
}

JNIEXPORT jobject JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_queryExtract
  (JNIEnv * env, jobject obj, jobjectArray query, jobjectArray variables, jobjectArray existentials)
{
	return DoQuery(env, obj, query, variables, existentials, NULL, true);
}

JNIEXPORT jint JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_querySize
  (JNIEnv * env, jobject obj, jobjectArray query, jobjectArray variables, jobjectArray existentials)
{
	int n = 0;
	DoQuery(env, obj, query, variables, existentials, NULL, false, &n);
	return n;
}

JNIEXPORT void JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_remove
  (JNIEnv * env, jobject obj, jobject statement, jobjectArray existentials)
{
	CState* pState = CState::GetState(env, obj);
	CUseState us(pState);
	CDatabase* pdb = pState->m_pDatabase;
	if (pdb == NULL)
		return;

	// Convert existentials
	jsize cExistentials = env->GetArrayLength(existentials);
	int* pExistentials = new int[cExistentials];
	for (int i = 0; i < cExistentials; i++)
	{
		CJavaStringWrapper js(env, (jstring)pState->ToString(env, env->GetObjectArrayElement(existentials, i)));
		pExistentials[i] = pdb->GetID(js);
	}

	// Map over statement
	jobject subject = pState->GetSubject(env, statement);
	jobject predicate = pState->GetPredicate(env, statement);
	jobject object = pState->GetObject(env, statement);
	jstring s = pState->ToString(env, subject);
	jstring p = pState->ToString(env, predicate);
	jstring o = pState->ToString(env, object);
	if (s == NULL || p == NULL || o == NULL)
	{
		delete [] pExistentials;
		return;
	}

	int is = pdb->GetID(CJavaStringWrapper(env, s));
	int ip = pdb->GetID(CJavaStringWrapper(env, p));
	int io = pdb->GetID(CJavaStringWrapper(env, o));

	int j;
	// Check for existentials
	for (j = 0; j < cExistentials; j++)
	{
		if (pExistentials[j] == is)
			is = -j - 1;
		if (pExistentials[j] == ip)
			ip = -j - 1;
		if (pExistentials[j] == io)
			io = -j - 1;
	}

#ifdef FULL_LOGGING
	g_csLog.Enter();
	fprintf(g_fpLog, "delete from rdf where ");
	bool bAnd = false;
	if (s)
	{
		fprintf(g_fpLog, "SUBJECT=\'%s\' ", FILTER(CJavaStringWrapper(env, s)));
		bAnd = true;
	}
	if (p)
	{
		if (bAnd)
			fprintf(g_fpLog, " AND ");
		fprintf(g_fpLog, "PREDICATE=\'%s\' ", FILTER(CJavaStringWrapper(env, p)));
		bAnd = true;
	}
	if (o)
	{
		if (bAnd)
			fprintf(g_fpLog, " AND ");
		fprintf(g_fpLog, "OBJECT=\'%s\' ", FILTER(CJavaStringWrapper(env, o)));
	}
	fprintf(g_fpLog, "\n");
	g_csLog.Leave();
#endif // FULL_LOGGING

	CIntVector aRemoved;
	pdb->Remove(is, ip, io, &aRemoved);

	for (j = 0; j < aRemoved.size(); j += 3)
		pState->AddToRemoveQueue(env, obj, pdb->GetNode(aRemoved[j]), pdb->GetNode(aRemoved[j + 1]), pdb->GetNode(aRemoved[j + 2]));

	delete [] pExistentials;
}

JNIEXPORT jboolean JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_addAuthored
  (JNIEnv * env, jobject obj, jstring s, jstring p, jstring o, jstring id, jobjectArray authorArray)
{
	return JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_getAuthors
  (JNIEnv * env, jobject obj, jobject statementId)
{
	return NULL;
}

JNIEXPORT jobjectArray JNICALL Java_edu_mit_lcs_haystack_server_rdfstore_Cholesterol3RDFStoreService_getAuthoredStatementIDs
  (JNIEnv * env, jobject obj, jobject author)
{
	return NULL;
}

}

