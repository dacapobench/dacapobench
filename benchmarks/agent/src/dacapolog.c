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
	    fprintf(logFile,"%s:%" FORMAT_JLONG ":%s\n",c_e,thread_tag,c_m);
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
    logState = logFile != NULL;
    if (logState) {
    	fprintf(logFile,"START\n");
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
		fprintf(logFile,"STOP\n");
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
	if (thread_has_new_tag || object_has_new_tag) {
		jniNativeInterface* jni_table;
		if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
			fprintf(stderr, "failed to get JNI function table\n");
			exit(1);
		}

		fprintf(logFile,"ME:%" FORMAT_JLONG,thread_tag);
		if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,local_env,baseEnv,thread);
		fprintf(logFile,":%" FORMAT_JLONG,object_tag);
		if (object_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,local_env,baseEnv,object);

		// get class and get thread name.
		jvmtiThreadInfo info;
		JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
		fprintf(logFile,":%s\n",info.name);
		if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
	} else {
		fprintf(logFile,"ME:%" FORMAT_JLONG ":%" FORMAT_JLONG "\n",thread_tag,object_tag);
	}
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
	if (thread_has_new_tag || object_has_new_tag) {
		jniNativeInterface* jni_table;
		if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
			fprintf(stderr, "failed to get JNI function table\n");
			exit(1);
		}

		fprintf(logFile,"MX:%" FORMAT_JLONG,thread_tag);
		if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,local_env,baseEnv,thread);
		fprintf(logFile,":%" FORMAT_JLONG,object_tag);
		if (object_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,local_env,baseEnv,object);

		// get class and get thread name.
		jvmtiThreadInfo info;
		JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
		fprintf(logFile,":%s\n",info.name);
		if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
	} else {
		fprintf(logFile,"MX:%" FORMAT_JLONG ":%" FORMAT_JLONG "\n",thread_tag,object_tag);
	}
	exitCriticalSection(&lockLog);
}



