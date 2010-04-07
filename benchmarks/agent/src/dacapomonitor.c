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
		log_field_string(LOG_PREFIX_MONITOR_CONTENTED_ENTER);
		log_field_time();
		
		jniNativeInterface* jni_table;
		if (thread_has_new_tag || object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}

		log_field_jlong(thread_tag);
		if (thread_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,thread);
		} else {
			log_field_string(NULL);
		}
		
		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
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
		log_field_string(LOG_PREFIX_MONITOR_CONTENTED_ENTERED);
		log_field_time();
		
		jniNativeInterface* jni_table;
		if (thread_has_new_tag || object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}

		log_field_jlong(thread_tag);
		if (thread_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,thread);
		} else {
			log_field_string(NULL);
		}
		
		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
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
		log_field_string(LOG_PREFIX_MONITOR_WAIT);
		log_field_time();
		
		jniNativeInterface* jni_table;
		if (thread_has_new_tag || object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}

		log_field_jlong(thread_tag);
		if (thread_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,thread);
		} else {
			log_field_string(NULL);
		}
		
		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
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
		log_field_string(LOG_PREFIX_MONITOR_WAITED);
		log_field_time();

		jniNativeInterface* jni_table;
		if (thread_has_new_tag || object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}
		
		log_field_jlong(thread_tag);
		if (thread_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,thread);
		} else {
			log_field_string(NULL);
		}
		
		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
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
}


