#include "dacapolog.h"
#include "dacapooptions.h"
#include "dacapotag.h"
#include "dacapolock.h"

jrawMonitorID       lockLog;
FILE*               logFile = NULL;
jboolean			logState = FALSE;

void setLogFileName(const char* log_file) {
	if (logFile!=NULL) {
		fclose(logFile);
		logFile = NULL;
	}
	logFile = fopen(log_file,"w");
}

_Bool dacapo_log_init() {
	if (JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "agent data", &(lockLog)) != JNI_OK)
		return FALSE;

	/* make log file */
	char tmpFile[10240];
	if (isSelected(OPT_LOG_FILE,tmpFile)) {
		setLogFileName(tmpFile);
	}
	return TRUE;
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    available
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_dacapo_instrument_Agent_available
  (JNIEnv *env, jclass klass)
{
    return !FALSE;
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    log
 * Signature: (Ljava/lang/Thread;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_log
  (JNIEnv *env, jclass klass, jobject thread, jstring e, jstring m)
{
    if (logState) {
	    jboolean iscopy_e;
	    jboolean iscopy_m;
	    jlong    thread_tag = 0;

	    enterCriticalSection(&lockTag);
		getTag(thread, &thread_tag);
		exitCriticalSection(&lockTag);

	    const char *c_e = JVMTI_FUNC_PTR(env,GetStringUTFChars)(env, e, &iscopy_e);
	    const char *c_m = JVMTI_FUNC_PTR(env,GetStringUTFChars)(env, m, &iscopy_m);

	    enterCriticalSection(&lockLog);
	    log_field_string(c_e);
	    log_field_jlong(thread_tag);
	    log_field_string(c_m);
	    log_eol();
	    exitCriticalSection(&lockLog);

	    JVMTI_FUNC_PTR(env,ReleaseStringUTFChars)(env, e, c_e);
	    JVMTI_FUNC_PTR(env,ReleaseStringUTFChars)(env, m, c_m);
    }
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    setLogFileName
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_setLogFileName
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
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_start
  (JNIEnv *env, jclass klass)
{
    if (!logState) {
	    logState = logFile != NULL;
	    if (logState) {
	    	log_field_string("START");
	    	log_eol();
	    }
	}
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_stop
  (JNIEnv *env, jclass klass)
{
	jboolean tmp = logState;
    logState = FALSE;
    if (tmp) {
    	log_field_string("STOP");
    	log_eol();
	}
}


/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    reportMonitorEnter
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_logMonitorEnter
  (JNIEnv *local_env, jclass klass, jobject thread, jobject object)
{
	// jclass GetObjectClass(JNIEnv *env, jobject obj);
	jlong thread_tag = 0;
	jlong object_tag = 0;

	enterCriticalSection(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean object_has_new_tag = getTag(object, &object_tag);
	exitCriticalSection(&lockTag);

	enterCriticalSection(&lockLog);
	log_field_string(LOG_PREFIX_MONITOR_AQUIRE);
	
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
	exitCriticalSection(&lockLog);
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    reportMonitorExit
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_logMonitorExit
  (JNIEnv *local_env, jclass klass, jobject thread, jobject object)
{
	// jclass GetObjectClass(JNIEnv *env, jobject obj);
	jlong thread_tag = 0;
	jlong object_tag = 0;

	enterCriticalSection(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean object_has_new_tag = getTag(object, &object_tag);
	exitCriticalSection(&lockTag);

	enterCriticalSection(&lockLog);
	log_field_string(LOG_PREFIX_MONITOR_RELEASE);
	
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
	exitCriticalSection(&lockLog);
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
  } else {
    if(text!=NULL && 0<text_length)
      fwrite(text,sizeof(char),text_length,logFile);
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

void log_eol() {
  fwrite(&end_of_line,sizeof(char),1,logFile);
  first_field = TRUE;
  fflush(logFile);
}


