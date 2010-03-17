#include "dacapomonitor.h"

#include "dacapolog.h"
#include "dacapotag.h"
#include "dacapolock.h"

void JNICALL callbackMonitorContendedEnter(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jobject object)
{
	if (logState ) {
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

			fprintf(logFile,"MCE:%" FORMAT_JLONG,thread_tag);
			if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,thread);
			fprintf(logFile,":%" FORMAT_JLONG,object_tag);
			if (object_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,object);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			fprintf(logFile,":%s\n",info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			fprintf(logFile,"MCE:%" FORMAT_JLONG ":%" FORMAT_JLONG "\n",thread_tag,object_tag);
		}
		exitCriticalSection(&lockLog);
	}
}

void JNICALL callbackMonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jobject object)
{
	if (logState ) {
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

			fprintf(logFile,"MCe:%" FORMAT_JLONG,thread_tag);
			if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,thread);
			fprintf(logFile,":%" FORMAT_JLONG,object_tag);
			if (object_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,object);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			fprintf(logFile,":%s\n",info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			fprintf(logFile,"MCe:%" FORMAT_JLONG ":%" FORMAT_JLONG "\n",thread_tag,object_tag);
		}
		exitCriticalSection(&lockLog);
	}
}

void JNICALL callbackMonitorWait(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jobject object, jlong timeout)
{
	if (logState ) {
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

			fprintf(logFile,"MW:%" FORMAT_JLONG,thread_tag);
			if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,thread);
			fprintf(logFile,":%" FORMAT_JLONG,object_tag);
			if (object_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,object);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			fprintf(logFile,":%s\n",info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			fprintf(logFile,"MW:%" FORMAT_JLONG ":%" FORMAT_JLONG "\n",thread_tag,object_tag);
		}
		exitCriticalSection(&lockLog);
	}
}

void JNICALL callbackMonitorWaited(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jobject object, jboolean timed_out)
{
	if (logState ) {
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

			fprintf(logFile,"Mw:%" FORMAT_JLONG,thread_tag);
			if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,thread);
			fprintf(logFile,":%" FORMAT_JLONG,object_tag);
			if (object_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,object);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			fprintf(logFile,":%s\n",info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			fprintf(logFile,"Mw:%" FORMAT_JLONG ":%" FORMAT_JLONG "\n",thread_tag,object_tag);
		}
		exitCriticalSection(&lockLog);
	}
}


