#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <stdlib.h>
#include <sys/time.h>

#include <jni.h>

#include "dacapolog.h"
#include "dacapooptions.h"
#include "dacapotag.h"
#include "dacapolock.h"

#include "dacapoallocation.h"
#include "dacapoexception.h"
#include "dacapomethod.h"
#include "dacapomonitor.h"
#include "dacapothread.h"
#include "dacapocallchain.h"

#include "zlib.h"

MonitorLockType       lockLog;
MonitorLockType       gcLock;

#define FILE_TYPE int

#define FILE_IS_CLOSED -1
#define FILE_FLAGS      (O_WRONLY | O_CREAT | O_LARGEFILE )
#define FILE_MODE       (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP)
#define GZ_FILE_MODE   "w"
#define GZ_BUFFER_SIZE (128*1024)

FILE_TYPE              logFile = FILE_IS_CLOSED;

gzFile                 gzLogFile;

jboolean			gzLog = FALSE;

#define CLOSE()       { \
  if (gzLog) gzclose(gzLogFile); \
  close(logFile); \
  logFile = FILE_IS_CLOSED; \
}
#define OPEN(f)        { \
  FILE_TYPE tmp = logFile; \
  gzFile    tmpgz = gzLogFile; \
  logFile = open(f,FILE_FLAGS,FILE_MODE); \
  if (gzLog) { \
    gzFile localtmpgz = gzdopen(logFile,GZ_FILE_MODE); \
    gzbuffer(localtmpgz, GZ_BUFFER_SIZE); \
    gzLogFile = localtmpgz; \
  } \
  if (tmp != FILE_IS_CLOSED) { \
    if (gzLog) gzclose(tmpgz); \
    close(tmp); \
  } \
}
    
#define FLUSH()

#define WRITE(b,e,n) { \
  if (gzLog) { logFileSequenceLength += gzwrite(gzLogFile,b,(e)*(n)); } \
  else { logFileSequenceLength += write(logFile,b,(e)*(n)); } \
}

jboolean			logState = FALSE;
jboolean            localInitDone = FALSE;
struct timeval      startTime;
jclass              log_java_class = NULL;
jmethodID           reportHeapID;
jfieldID            firstReportSinceForceGCID;
jfieldID            agentIntervalTimeID;

jfieldID            callChainCountID;
jfieldID            callChainFrequencyID;
jfieldID            callChainEnableID;

#define LOG_FILE_NAME_MAX 10000
#define LOG_FILE_EXT_MAX  10000
#define SEQUENCE_MAX        100
#define LOG_FILE_LIMIT    (1<<30)

char                baseLogFileName[LOG_FILE_NAME_MAX];
char                baseLogFileExt[LOG_FILE_EXT_MAX];
int                 fileNameSequence      = 0;
long                logFileSequenceLength = 0;
jboolean            check_limit = FALSE;

static void openLogFile() {
	char logFileName[LOG_FILE_NAME_MAX+SEQUENCE_MAX];
	int  fileSeq = fileNameSequence++;
	
	if (strlen(baseLogFileExt)==0) {
		if (gzLog)
			sprintf(logFileName,"%s-%d.gz",baseLogFileName,fileSeq);
		else
			sprintf(logFileName,"%s-%d",baseLogFileName,fileSeq);
	} else {
		if (gzLog)
			sprintf(logFileName,"%s-%d.%s.gz",baseLogFileName,fileSeq,baseLogFileExt);
		else
			sprintf(logFileName,"%s-%d.%s",baseLogFileName,fileSeq,baseLogFileExt);
	}
	
	OPEN(logFileName);
	
	logFileSequenceLength = 0;
}

_Bool logFileOpen() {
	return logFile != FILE_IS_CLOSED;
}

void  dacapo_log_stop() {
    if (logFile != FILE_IS_CLOSED)
    	CLOSE();
}

void setLogFileName(const char* log_file) {
	dacapo_log_stop();

	int ext = strlen(log_file);
	while (0<ext && log_file[ext]!='.') ext--;

	if (0<ext) {
		strncpy(baseLogFileName,log_file,(ext<(LOG_FILE_NAME_MAX-1))?ext:(LOG_FILE_NAME_MAX-1));
		strncpy(baseLogFileExt,log_file+ext+1,SEQUENCE_MAX-1);
		baseLogFileExt[SEQUENCE_MAX-1] = '\0';
	} else {
		strcpy(baseLogFileName,log_file);
		baseLogFileExt[0]='\0';
	}

	fileNameSequence      = 0;
	logFileSequenceLength = 0;
	
	openLogFile(log_file);
}

void callReportHeap(JNIEnv *env) {
	(*env)->CallStaticVoidMethod(env,log_java_class,reportHeapID);
}

void setReportHeap(JNIEnv *env, jboolean flag) {
    rawMonitorEnter(&gcLock);
	if (localInitDone)
		(*env)->SetStaticBooleanField(env,log_java_class,firstReportSinceForceGCID,flag);
    rawMonitorExit(&gcLock);
}

void setReportCallChain(JNIEnv *env, jlong frequency, jboolean enable) {
	if (localInitDone) {
		if (enable) {
			(*env)->SetStaticLongField(env,log_java_class,callChainFrequencyID,frequency);
			(*env)->SetStaticLongField(env,log_java_class,callChainCountID,(jlong)0);
		}
		(*env)->SetStaticBooleanField(env,log_java_class,callChainEnableID,enable);
	}
}

_Bool dacapo_log_init() {
	if (!rawMonitorInit(baseEnv,"agent data",&lockLog)) {
		/* JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "agent data", &(lockLog)) != JNI_OK) */
		return FALSE;
	}

	if (!rawMonitorInit(baseEnv,"gc lock",&gcLock)) {
	    /* JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "gc lock", &(gcLock)) != JNI_OK) { */
		return FALSE;
	}
	

	if (isSelected(OPT_LOG_FILE_LIMIT,NULL)) {
		check_limit = TRUE;
	}

	if (isSelected(OPT_LOG_FILE_GZIP,NULL)) {
		gzLog = TRUE;
	}
	
	/* make log file */
	char* tmpFile = NULL;
	if (isSelected(OPT_LOG_FILE,&tmpFile)) {
		setLogFileName(tmpFile);
		if (tmpFile != NULL) {
			free(tmpFile);
			tmpFile = NULL;
		}
	}
	
    gettimeofday(&startTime, NULL);
    
	return TRUE;
}


JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLocalInit
  (JNIEnv *env, jclass klass)
{
	reportHeapID              = (*env)->GetStaticMethodID(env,klass,"reportHeap","()V");
	firstReportSinceForceGCID = (*env)->GetStaticFieldID(env,klass,"firstReportSinceForceGC","Z");
	agentIntervalTimeID       = (*env)->GetStaticFieldID(env,klass,"agentIntervalTime","J");
	callChainCountID          = (*env)->GetStaticFieldID(env,klass,"callChainCount","J");
	callChainFrequencyID      = (*env)->GetStaticFieldID(env,klass,"callChainFrequency","J");
	callChainEnableID         = (*env)->GetStaticFieldID(env,klass,"callChainEnable","Z");

	log_java_class                 = (*env)->NewGlobalRef(env, klass);
	
	char tmp[1024];
	if (isSelected(OPT_INTERVAL,tmp)) {
		int value = atoi(tmp);
		
		if (value > 0)
			(*env)->SetStaticLongField(env,log_java_class,agentIntervalTimeID,(jlong)value);
	}
	
	localInitDone = !FALSE;
}

JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_wierd
  (JNIEnv *env, jclass klass)
{
	fprintf(stderr,"Agent_wierd[start]\n");
	callReportHeap(env);
	fprintf(stderr,"Agent_wierd[stop]\n");
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    available
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_dacapo_instrument_Agent_internalAvailable
  (JNIEnv *env, jclass klass)
{
    return !FALSE;
}

JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLogThread
  (JNIEnv *env, jclass klass, jobject thread)
{
	thread_agent_log(env, klass, thread);
}


/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    log
 * Signature: (Ljava/lang/Thread;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLog
  (JNIEnv *env, jclass klass, jobject thread, jstring e, jstring m)
{
    if (logState) {
		jniNativeInterface* jni_table = JNIFunctionTable();

	    jboolean iscopy_e;
	    jboolean iscopy_m;
	    jlong    thread_tag = 0;
	    jlong    thread_klass_tag = 0;
	    jclass   thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(env,thread);
	    jboolean new_thread_tag;

		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
		rawMonitorExit(&lockTag);

	    const char *c_e = JVMTI_FUNC_PTR(env,GetStringUTFChars)(env, e, &iscopy_e);
	    const char *c_m = JVMTI_FUNC_PTR(env,GetStringUTFChars)(env, m, &iscopy_m);

	    rawMonitorEnter(&lockLog);
	    log_field_string(c_e);
		log_field_current_time();
		
		log_thread(thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
		
	    log_field_string(c_m);
	    log_eol();
	    rawMonitorExit(&lockLog);

	    JVMTI_FUNC_PTR(env,ReleaseStringUTFChars)(env, e, c_e);
	    JVMTI_FUNC_PTR(env,ReleaseStringUTFChars)(env, m, c_m);
    }
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    setLogFileName
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalSetLogFileName
  (JNIEnv *env, jclass klass, jstring s)
{
    jboolean iscopy;
    const char *m = JVMTI_FUNC_PTR(env,GetStringUTFChars)(env, s, &iscopy);

    setLogFileName(m);

    JVMTI_FUNC_PTR(env,ReleaseStringUTFChars)(env, s, m);
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    start
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalStart
  (JNIEnv *env, jclass klass)
{
    if (!logState) {
	    logState = logFile != FILE_IS_CLOSED;
	    if (logState) {
		    rawMonitorEnter(&lockLog);
	    	log_field_string(LOG_PREFIX_START);
	    	log_field_current_time();
	    	log_eol();
		    rawMonitorExit(&lockLog);
	    	
	    	allocation_logon(env);
			exception_logon(env);
			method_logon(env);
			monitor_logon(env);
			thread_logon(env);
			call_chain_logon(env);
	    }
	}
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalStop
  (JNIEnv *env, jclass klass)
{
	jboolean tmp = logState;
    logState = FALSE;
    if (tmp) {
	    rawMonitorEnter(&lockLog);
    	log_field_string(LOG_PREFIX_STOP);
    	log_field_current_time();
    	log_eol();
	    rawMonitorExit(&lockLog);
	}
}



/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    logCallChain
 * Signature: (Ljava/lang/Thread;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLogCallChain
  (JNIEnv *env, jclass klass, jobject thread) {
  	log_call_chain(env, klass, thread);
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    writeHeapReport
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalHeapReport(JNIEnv *local_env, jclass klass, jobject thread, jlong used, jlong free, jlong total, jlong max) {
	jniNativeInterface* jni_table = JNIFunctionTable();

	jlong thread_tag = 0;
	jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(local_env,thread);
	jlong thread_klass_tag = 0;

	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
	rawMonitorExit(&lockTag);

	rawMonitorEnter(&lockLog);
	log_field_string(LOG_PREFIX_HEAP_REPORT);
	log_field_current_time();

	log_thread(thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
	
	log_field_jlong(used);
	log_field_jlong(free);
	log_field_jlong(total);
	log_field_jlong(max);
	log_eol();    
	rawMonitorExit(&lockLog);
	
    return;
}

/*
 */

static _Bool first_field     = TRUE;
static char  field_separator = ',';
static char  field_delimiter = '\"';
static char  end_of_line     = '\n';

/* */

static void write_field(const char* text, int text_length, _Bool use_delimiter) {
  if (logFile == FILE_IS_CLOSED) {
  	fprintf(stderr,"LOG_FILE IS CLOSED\n");
  	exit(10);
  }

  if (first_field) {
    first_field = FALSE;
  } else {
    WRITE(&field_separator,sizeof(char),1);
  }
  
  if (use_delimiter) {
    char temp_field[10240];
    int  temp_length = 0;

    temp_field[temp_length++] = field_delimiter;
  
	if (text!=NULL) {
	  int i;
	  for(i=0; i<text_length;++i) {
	    if (text[i]==field_delimiter)
	      temp_field[temp_length++] = field_delimiter;
        temp_field[temp_length++] = field_delimiter;
	  }
	}
    
    temp_field[temp_length++] = field_delimiter;

    WRITE(temp_field,sizeof(char),temp_length);
  } else {
    if(text!=NULL && 0<text_length) {
      WRITE(text,sizeof(char),text_length);
    }
  }
}

void log_field_string(const char* text) {
  if (text==NULL)
    write_field(NULL,0,TRUE);
  else {
    int text_length = 0;
    _Bool use_delimiter = FALSE;
    
    while(text[text_length]!='\0') {
       use_delimiter = text[text_length]==field_delimiter || text[text_length]==field_separator;
       ++text_length;
    }
    
    write_field(text,text_length,use_delimiter);
  }
}

void log_field_jboolean(jboolean v) {
  log_field_string(v?"true":"false");
}

void log_field_int(int v) {
  char tmp[32];
  sprintf(tmp,"%d",v);
  log_field_string(tmp);
}

void log_field_jlong(jlong v) {
  char tmp[64];
  sprintf(tmp,"%" FORMAT_JLONG,v);
  log_field_string(tmp);
}

void log_field_long(long v) {
  char tmp[64];
  sprintf(tmp,"%ld",v);
  log_field_string(tmp);
}

void log_field_pointer(const void* p) {
  char tmp[64];
  sprintf(tmp,"%" FORMAT_PTR, PTR_CAST(p));
  log_field_string(tmp);
}

void log_field_string_n(const char* text, int field_length) {
  int i;
  _Bool use_delimiter = FALSE;
  
  for(i=0; i<field_length && !use_delimiter; ++i) {
  	use_delimiter = text[i]==field_delimiter || text[i]==field_separator;
  }
  
  write_field(text,field_length,use_delimiter);
}

void log_thread(jthread thread, jlong thread_tag, jboolean thread_has_new_tag, jobject klass, jlong klass_tag, jboolean klass_has_new_tag) {
	log_field_jlong(thread_tag);

	if (thread_has_new_tag) {
		jniNativeInterface* jni_table = JNIFunctionTable();

		// get thread name.
		jvmtiThreadInfo info;
		JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
		log_field_string(info.name);
		if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
	} else {
		log_field_string(NULL);
	}
	
	log_class(klass, klass_tag, klass_has_new_tag);
}

void log_class(jobject klass, jlong klass_tag, jboolean klass_has_new_tag) {
	log_field_jlong(klass_tag);
	if (klass_has_new_tag) {
		jniNativeInterface* jni_table = JNIFunctionTable();

		char* signature = NULL;
		char* generic = NULL;
		
		JVMTI_FUNC_PTR(baseEnv,GetClassSignature)(baseEnv,klass,&signature,&generic);
		log_field_string(signature);
		
		if (signature!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature);
		if (generic!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic);
	} else {
		log_field_string(NULL);
	}
}

void log_field_time(struct timeval* tv) {
    jlong t = ((jlong)(tv->tv_sec - startTime.tv_sec)) * (jlong)1000000 + (tv->tv_usec - startTime.tv_usec);
    log_field_jlong(t);
}

void log_field_current_time() {
	struct timeval tv;
    gettimeofday(&tv, NULL);
    log_field_time(&tv);
}

void log_eol() {
  WRITE(&end_of_line,sizeof(char),1);
  first_field = TRUE;
  FLUSH();
  
  if (check_limit && LOG_FILE_LIMIT <= logFileSequenceLength) {
  	openLogFile();
  }  
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    agentThread
 * Signature: (Ljava/lang/Thread;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_agentThread(JNIEnv *env, jclass klass, jobject thread) {
	threads_states(env);
}


