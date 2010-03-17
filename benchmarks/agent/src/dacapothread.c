#include "dacapothread.h"

#include "dacapotag.h"
#include "dacapolock.h"
#include "dacapolog.h"

void JNICALL callbackThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
	if (logState ) {
		jlong thread_tag = 0;

		enterCriticalSection(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		exitCriticalSection(&lockTag);

		enterCriticalSection(&lockLog);
		if (thread_has_new_tag) {
			jniNativeInterface* jni_table;
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}

			fprintf(logFile,"TS:%" FORMAT_JLONG,thread_tag);
			if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,thread);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			fprintf(logFile,":%s\n",info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			fprintf(logFile,"TS:%" FORMAT_JLONG "\n",thread_tag);
		}
		exitCriticalSection(&lockLog);
	}
}

void JNICALL callbackThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
	if (logState ) {
		jlong thread_tag = 0;

		enterCriticalSection(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		exitCriticalSection(&lockTag);

		enterCriticalSection(&lockLog);
		if (thread_has_new_tag) {
			jniNativeInterface* jni_table;
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}

			fprintf(logFile,"TE:%" FORMAT_JLONG,thread_tag);
			if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,thread);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			fprintf(logFile,":%s\n",info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			fprintf(logFile,"TE:%" FORMAT_JLONG "\n",thread_tag);
		}
		exitCriticalSection(&lockLog);
	}
}


/* JVMTI_EVENT_METHOD_ENTRY */
void JNICALL callbackMethodEntry(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method) {
	if (logState ) {
		jlong thread_tag = 0;

		enterCriticalSection(&lockTag);
		getTag(thread, &thread_tag);
		exitCriticalSection(&lockTag);

		enterCriticalSection(&lockLog);
		fprintf(logFile,"mE:%" FORMAT_JLONG ":%" FORMAT_PTR "\n",thread_tag,PTR_CAST(method));
		exitCriticalSection(&lockLog);
	}
}

/* JVMTI_EVENT_METHOD_EXIT */
void JNICALL callbackMethodExit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jboolean was_popped_by_exception, jvalue return_value) {
	if (logState ) {
		/* record thread as exiting from method */
	}
}
