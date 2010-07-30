#include "dacapolog.h"
#include "dacapotag.h"
#include "dacapooptions.h"
#include "dacapolock.h"
#include "dacapothread.h"

#include "dacapocallchain.h"

void call_chain_init() {

}

void call_chain_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {
}

void call_chain_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks) {
}

void call_chain_logon(JNIEnv* env) {
	if (jvmRunning && !jvmStopped) {
		char* arg = NULL;
		long frequency = 0;
		if (isSelected(OPT_CALL_CHAIN,&arg))
			frequency = atol(arg);

		free(arg);

		if (0 < frequency) {
			setReportCallChain(env, frequency, (jboolean)TRUE);
		} else {
			setReportCallChain(env, (jboolean)1, (jboolean)FALSE);
		}
	}
}

#define MAX_NUMBER_OF_FRAMES 64
#define START_FRAME 4

void log_call_chain(JNIEnv *jni_env, jclass klass, jobject thread) {
	void* buffer = NULL;
	jniNativeInterface* jni_table = JNIFunctionTable();

	jlong thread_tag = 0;
	jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
	jlong thread_klass_tag = 0;

	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
	rawMonitorExit(&lockTag);

	/* iterate through frames */
	jvmtiFrameInfo frames[MAX_NUMBER_OF_FRAMES];
	jint count = 0;
	jvmtiError err;

	err = JVMTI_FUNC_PTR(baseEnv,GetStackTrace)(baseEnv, thread, START_FRAME, MAX_NUMBER_OF_FRAMES,frames, &count);

	rawMonitorEnter(&lockLog);
	buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_CALL_CHAIN_START);
	log_thread(buffer, thread,thread_tag,thread_has_new_tag,thread_klass,thread_klass_tag,thread_klass_has_new_tag);
	log_eol(buffer);
	rawMonitorExit(&lockLog);

	int i;
	for(i=0; i<count; i++) {
		jlong class_tag = 0;

		jclass klass = NULL;
		jlong  klass_tag = 0;
		err = JVMTI_FUNC_PTR(baseEnv,GetMethodDeclaringClass)(baseEnv,frames[i].method,&klass);
		
		rawMonitorEnter(&lockTag);
		jboolean klass_has_new_tag = getTag(klass,&klass_tag);
		rawMonitorExit(&lockTag);

		rawMonitorEnter(&lockLog);
		buffer = log_buffer_get();		
		log_field_string(buffer, LOG_PREFIX_CALL_CHAIN_FRAME);
		log_field_jlong(buffer, thread_tag);
		log_field_jlong(buffer, (jlong)i);
		log_class(buffer, klass,klass_tag,klass_has_new_tag);

    	char* name_ptr = NULL;
    	char* signature_ptr  = NULL;
    	char* generic_ptr = NULL;

    	jint res = JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,frames[i].method,&name_ptr,&signature_ptr,&generic_ptr);

    	log_field_string(buffer, LOG_PREFIX_METHOD_PREPARE);
    	log_field_pointer(buffer, frames[i].method);
    	log_field_jlong(buffer, class_tag);
    	log_field_string(buffer, name_ptr);
    	log_field_string(buffer, signature_ptr);

    	if (name_ptr!=NULL)      JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name_ptr);
    	if (signature_ptr!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature_ptr);
    	if (generic_ptr!=NULL)   JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic_ptr);

    	log_eol(buffer);
		rawMonitorExit(&lockLog);		
	}

	/*
	 * Trace(jvmtiEnv* env,
            jthread thread,
            jint start_depth,
            jint max_frame_count,
            jvmtiFrameInfo* frame_buffer,
            jint* count_ptr)
	 *
	 *
	if (err == JVMTI_ERROR_NONE && count >= 1) {
	   char *methodName;
	   err = (*jvmti)->GetMethodName(jvmti, frames[0].method,
	                       &methodName, NULL);
	   if (err == JVMTI_ERROR_NONE) {
	      printf("Executing method: %s", methodName);
	   }
	}
	*/

	rawMonitorEnter(&lockLog);
	buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_CALL_CHAIN_STOP);
	log_field_jlong(buffer, thread_tag);
	log_eol(buffer);
	rawMonitorExit(&lockLog);
}




