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

jrawMonitorID       lockLog;
jrawMonitorID       agentLock;
jrawMonitorID       gcLock;

FILE*               logFile = NULL;
jboolean			logState = FALSE;
jboolean            localInitDone = FALSE;
struct timeval      startTime;
jclass              log_class = NULL;
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

static FILE* openLogFile() {
	char logFileName[LOG_FILE_NAME_MAX+SEQUENCE_MAX];
	int  fileSeq = fileNameSequence++;
	
	if (strlen(baseLogFileExt)==0)
		sprintf(logFileName,"%s-%d",baseLogFileName,fileSeq);
	else
		sprintf(logFileName,"%s-%d.%s",baseLogFileName,fileSeq,baseLogFileExt);
	
	FILE* tmp = fopen(logFileName,"w"); 
	if (logFile!=NULL) {
		fclose(logFile);
	}

	logFileSequenceLength = 0;
	logFile = tmp;
	
	return logFile; 
}

void setLogFileName(const char* log_file) {
	if (logFile!=NULL) {
		fclose(logFile);
		logFile = NULL;
	}

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
	logFile               = openLogFile(log_file);
}

void callReportHeap(JNIEnv *env) {
	(*env)->CallStaticVoidMethod(env,log_class,reportHeapID);
}

void setReportHeap(JNIEnv *env, jboolean flag) {
    rawMonitorEnter(&gcLock);
	if (localInitDone)
		(*env)->SetStaticBooleanField(env,log_class,firstReportSinceForceGCID,flag);
    rawMonitorExit(&gcLock);
}

void setReportCallChain(JNIEnv *env, jlong frequency, jboolean enable) {
	if (localInitDone) {
		if (enable) {
			(*env)->SetStaticLongField(env,log_class,callChainFrequencyID,frequency);
			(*env)->SetStaticLongField(env,log_class,callChainCountID,(jlong)0);
		}
		(*env)->SetStaticBooleanField(env,log_class,callChainEnableID,enable);
	}
}

_Bool dacapo_log_init() {
	if (JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "agent data", &(lockLog)) != JNI_OK)
		return FALSE;

	if (JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "agent lock", &(agentLock)) != JNI_OK) {
		return FALSE;
	}

	if (JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "gc lock", &(gcLock)) != JNI_OK) {
		return FALSE;
	}
	

	if (isSelected(OPT_LOG_FILE_LIMIT,NULL)) {
		check_limit = TRUE;
	}

	/* make log file */
	char tmpFile[10240];
	if (isSelected(OPT_LOG_FILE,tmpFile)) {
		setLogFileName(tmpFile);
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

	log_class                 = (*env)->NewGlobalRef(env, klass);
	
	char tmp[1024];
	if (isSelected(OPT_INTERVAL,tmp)) {
		int value = atoi(tmp);
		
		if (value > 0)
			(*env)->SetStaticLongField(env,log_class,agentIntervalTimeID,(jlong)value);
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

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    log
 * Signature: (Ljava/lang/Thread;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLog
  (JNIEnv *env, jclass klass, jobject thread, jstring e, jstring m)
{
    if (logState) {
	    jboolean iscopy_e;
	    jboolean iscopy_m;
	    jlong    thread_tag = 0;
	    jboolean new_thread_tag;

		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		rawMonitorExit(&lockTag);

	    const char *c_e = JVMTI_FUNC_PTR(env,GetStringUTFChars)(env, e, &iscopy_e);
	    const char *c_m = JVMTI_FUNC_PTR(env,GetStringUTFChars)(env, m, &iscopy_m);

	    rawMonitorEnter(&lockLog);
	    log_field_string(c_e);
		log_field_current_time();
	    
		jniNativeInterface* jni_table;
		if (thread_has_new_tag) {
			jniNativeInterface* jni_table;
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}

			LOG_OBJECT_CLASS(jni_table,env,baseEnv,thread);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			log_field_string(info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			log_field_string(NULL);
			log_field_string(NULL);
		}
		
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
	    logState = logFile != NULL;
	    if (logState) {
		    rawMonitorEnter(&lockLog);
	    	log_field_string(LOG_PREFIX_START);
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
    	log_field_string(LOG_PREFIX_STOP);
    	log_eol();
	}
}


/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    reportMonitorEnter
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLogMonitorEnter
  (JNIEnv *local_env, jclass klass, jobject thread, jobject object)
{
	// jclass GetObjectClass(JNIEnv *env, jobject obj);
	jlong thread_tag = 0;
	jlong object_tag = 0;

	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean object_has_new_tag = getTag(object, &object_tag);
	rawMonitorExit(&lockTag);

	rawMonitorEnter(&lockLog);
	log_field_string(LOG_PREFIX_MONITOR_AQUIRE);
	log_field_current_time();
	
	jniNativeInterface* jni_table;
	if (thread_has_new_tag || object_has_new_tag) {
		if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
			fprintf(stderr, "failed to get JNI function table\n");
			exit(1);
		}
	}

	log_field_jlong(thread_tag);
	if (thread_has_new_tag) {
		LOG_OBJECT_CLASS(jni_table,local_env,baseEnv,thread);
	} else {
		log_field_string(NULL);
	}
	
	log_field_jlong(object_tag);
	if (object_has_new_tag) {
		LOG_OBJECT_CLASS(jni_table,local_env,baseEnv,object);
	} else {
		log_field_string(NULL);
	}

	if (thread_has_new_tag) {
		// get class and get thread name.
		jvmtiThreadInfo info;
		JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
		log_field_string(info.name);
		if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
	} else {
		log_field_string(NULL);
	}
	
	log_eol();
	rawMonitorExit(&lockLog);
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    reportMonitorExit
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLogMonitorExit
  (JNIEnv *local_env, jclass klass, jobject thread, jobject object)
{
	// jclass GetObjectClass(JNIEnv *env, jobject obj);
	jlong thread_tag = 0;
	jlong object_tag = 0;

	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean object_has_new_tag = getTag(object, &object_tag);
	rawMonitorExit(&lockTag);

	rawMonitorEnter(&lockLog);
	log_field_string(LOG_PREFIX_MONITOR_RELEASE);
	log_field_current_time();
	
	jniNativeInterface* jni_table;
	if (thread_has_new_tag || object_has_new_tag) {
		if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
			fprintf(stderr, "failed to get JNI function table\n");
			exit(1);
		}
	}

	log_field_jlong(thread_tag);
	if (thread_has_new_tag) {
		LOG_OBJECT_CLASS(jni_table,local_env,baseEnv,thread);
	} else {
		log_field_string(NULL);
	}
	
	log_field_jlong(object_tag);
	if (object_has_new_tag) {
		LOG_OBJECT_CLASS(jni_table,local_env,baseEnv,object);
	} else {
		log_field_string(NULL);
	}

	if (thread_has_new_tag) {
		// get class and get thread name.
		jvmtiThreadInfo info;
		JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
		log_field_string(info.name);
		if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
	} else {
		log_field_string(NULL);
	}
	
	log_eol();
	rawMonitorExit(&lockLog);
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
	jlong thread_tag = 0;

	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	rawMonitorExit(&lockTag);

	rawMonitorEnter(&lockLog);
	log_field_string(LOG_PREFIX_HEAP_REPORT);
	log_field_current_time();

	thread_log(local_env, thread, thread_tag, thread_has_new_tag);
	
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
  if (first_field)
    first_field = FALSE;
  else
    fwrite(&field_separator,sizeof(char),1,logFile);
  
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

    fwrite(temp_field,sizeof(char),temp_length,logFile);
    
    logFileSequenceLength += temp_length;
  } else {
    if(text!=NULL && 0<text_length) {
      fwrite(text,sizeof(char),text_length,logFile);
    
	  logFileSequenceLength += text_length;
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

void log_field_time(struct timeval* tv) {
    long t = (tv->tv_sec - startTime.tv_sec) * 1000000 + (tv->tv_usec - startTime.tv_usec);
    log_field_long(t);
}

void log_field_current_time() {
	struct timeval tv;
    gettimeofday(&tv, NULL);
    log_field_time(&tv);
}

void log_eol() {
  fwrite(&end_of_line,sizeof(char),1,logFile);
  first_field = TRUE;
  fflush(logFile);
  
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


