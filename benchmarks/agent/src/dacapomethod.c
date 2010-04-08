#include "dacapomethod.h"

#include "dacapotag.h"
#include "dacapolock.h"
#include "dacapolog.h"

#include "dacapooptions.h"

void method_init() {

}

void method_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {
    if (isSelected(OPT_METHOD_EVENTS,NULL)) {
	    capabilities->can_generate_method_entry_events    = availableCapabilities->can_generate_method_entry_events;
    	capabilities->can_generate_method_exit_events     = availableCapabilities->can_generate_method_exit_events;
	}
}

void method_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks) {
    if (isSelected(OPT_METHOD_EVENTS,NULL)) {
	    if (capabilities->can_generate_method_entry_events) DEFINE_CALLBACK(callbacks,MethodEntry,JVMTI_EVENT_METHOD_ENTRY);
		if (capabilities->can_generate_method_exit_events)  DEFINE_CALLBACK(callbacks,MethodExit,JVMTI_EVENT_METHOD_EXIT);
	}
}

/* JVMTI_EVENT_METHOD_ENTRY */
void JNICALL callbackMethodEntry(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method) {
	if (logState) {
		jlong thread_tag = 0;

		enterCriticalSection(&lockTag);
		getTag(thread, &thread_tag);
		exitCriticalSection(&lockTag);

		enterCriticalSection(&lockLog);
		log_field_string(LOG_PREFIX_METHOD_ENTER);
		log_field_jlong(thread_tag);
		log_field_pointer(method);
		log_eol();
		exitCriticalSection(&lockLog);
	}
}

/* JVMTI_EVENT_METHOD_EXIT */
void JNICALL callbackMethodExit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jboolean was_popped_by_exception, jvalue return_value) {
	if (logState) {
		/* record thread as exiting from method */
	}
}
