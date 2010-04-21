#include "dacapothread.h"

#include "dacapotag.h"
#include "dacapolock.h"
#include "dacapolog.h"

#include "dacapooptions.h"

void thread_init() {

}

void thread_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {

}

void thread_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks) {
	if (isSelected(OPT_THREAD,NULL)) {
		DEFINE_CALLBACK(callbacks,ThreadStart,JVMTI_EVENT_THREAD_START);
		DEFINE_CALLBACK(callbacks,ThreadEnd,JVMTI_EVENT_THREAD_END);
	}
}

void thread_live(jvmtiEnv* jvmti, JNIEnv* env) {
	
}

void thread_logon(JNIEnv* env) {

}

void thread_class(jvmtiEnv *env, JNIEnv *jnienv, jclass klass) {

}

void thread_log(JNIEnv* env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag) {
	jniNativeInterface* jni_table;
	log_field_jlong(thread_tag);
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
}

void JNICALL callbackThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
	if (logState) {
		jlong thread_tag = 0;

		enterCriticalSection(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		exitCriticalSection(&lockTag);

		enterCriticalSection(&lockLog);
		log_field_string(LOG_PREFIX_THREAD_START);
		log_field_time();
	
		thread_log(jni_env, thread, thread_tag, thread_has_new_tag);
		
		log_eol();
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
		log_field_string(LOG_PREFIX_THREAD_STOP);
		log_field_time();
		
		thread_log(jni_env, thread, thread_tag, thread_has_new_tag);
		
		log_eol();
		exitCriticalSection(&lockLog);
	}
}

