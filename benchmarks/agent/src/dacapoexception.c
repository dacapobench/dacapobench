#include "dacapolog.h"
#include "dacapotag.h"
#include "dacapooptions.h"
#include "dacapolock.h"
#include "dacapothread.h"

#include "dacapoexception.h"

#include "dacapolog.h"
#include "dacapotag.h"
#include "dacapooptions.h"
#include "dacapolock.h"
#include "dacapothread.h"

void exception_init() {

}

void exception_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {
    if (isSelected(OPT_EXCEPTION,NULL)) {
    	capabilities->can_generate_exception_events = availableCapabilities->can_generate_exception_events;
    }
}

void exception_live(jvmtiEnv* jvmti, JNIEnv* env) {

}

void exception_logon(JNIEnv* env) {

}

void exception_class(jvmtiEnv *env, JNIEnv *jnienv, jthread thread, jclass klass) {

}

void exception_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks) {
	if (isSelected(OPT_EXCEPTION,NULL)) {
		DEFINE_CALLBACK(callbacks,Exception,JVMTI_EVENT_EXCEPTION);
		DEFINE_CALLBACK(callbacks,ExceptionCatch,JVMTI_EVENT_EXCEPTION_CATCH);
	}
}

void JNICALL callbackException(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jlocation location, jobject exception, jmethodID catch_method, jlocation catch_location)
{
	if (logState) {
		jniNativeInterface* jni_table = JNIFunctionTable();

		jlong thread_tag = 0;
		jlong exception_tag = 0;
		jlong klass_tag = 0;
		jlong exception_klass_tag = 0;

		jclass klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
		jobject exception_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,exception);
		
		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean exception_has_new_tag = getTag(exception, &exception_tag);
		jboolean klass_has_new_tag = getTag(klass, &klass_tag);
		jboolean exception_klass_has_new_tag = getTag(exception_klass, &exception_klass_tag);
		rawMonitorExit(&lockTag);

		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_EXCEPTION);

		log_thread(buffer, thread, thread_tag, thread_has_new_tag, klass, klass_tag, klass_has_new_tag);

		log_field_jlong(buffer, exception_tag);
		
		log_class(buffer, exception_klass, exception_klass_tag, exception_klass_has_new_tag);
		
		char* name = NULL;
		char* signature  = NULL;
		char* generic = NULL;
		char* class_signature = NULL;
		char* class_generic = NULL;

		log_field_pointer(buffer, catch_method);

		JVMTI_FUNC_PTR(baseEnv,GetMethodDeclaringClass)(baseEnv,method,&klass)==JNI_OK &&
		JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,method,&name,&signature,&generic)==JNI_OK &&
		JVMTI_FUNC_PTR(baseEnv,GetClassSignature)(baseEnv,klass,&class_signature,&class_generic)==JNI_OK;

		log_field_string(buffer, class_signature);
		log_field_string(buffer, name);
		log_field_string(buffer, signature);

		if (class_signature!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)class_signature); class_signature = NULL; }
		if (class_generic!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)class_generic); class_generic = NULL; }
		if (name!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name); name = NULL; }
		if (signature!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature); signature = NULL; }
		if (generic!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic); generic = NULL; }

		JVMTI_FUNC_PTR(baseEnv,GetMethodDeclaringClass)(baseEnv,catch_method,&klass)==JNI_OK &&
		JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,catch_method,&name,&signature,&generic)==JNI_OK &&
		JVMTI_FUNC_PTR(baseEnv,GetClassSignature)(baseEnv,klass,&class_signature,&class_generic)==JNI_OK;
						
		log_field_string(buffer, class_signature);
		log_field_string(buffer, name);
		log_field_string(buffer, signature);

		if (class_signature!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)class_signature); class_signature = NULL; }
		if (class_generic!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)class_generic); class_generic = NULL; }
		if (name!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name); name = NULL; }
		if (signature!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature); signature = NULL; }
		if (generic!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic); generic = NULL; }
		
		log_eol(buffer);
		rawMonitorExit(&lockLog);
	}
}

void JNICALL callbackExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jlocation location, jobject exception)
{
	if (logState ) {
		jniNativeInterface* jni_table = JNIFunctionTable();

		jclass klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
		jclass exception_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,exception);

		jlong thread_tag = 0;
		jlong exception_tag = 0;
		jlong klass_tag = 0;
		jlong exception_klass_tag = 0;

		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean exception_has_new_tag = getTag(exception, &exception_tag);
		jboolean klass_has_new_tag = getTag(klass, &klass_tag);
		jboolean exception_klass_has_new_tag = getTag(exception_klass, &exception_klass_tag);
		rawMonitorExit(&lockTag);

		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_EXCEPTION_CATCH);

		log_thread(buffer, thread, thread_tag, thread_has_new_tag, klass, klass_tag, klass_has_new_tag);

		log_field_jlong(buffer, exception_tag);
		
		log_class(buffer, exception_klass, exception_klass_tag, exception_klass_has_new_tag);

		char* name_ptr = NULL;
		char* signature_ptr  = NULL;
		char* generic_ptr = NULL;

		JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,method,&name_ptr,&signature_ptr,&generic_ptr);

		log_field_string(buffer, "<class_name>");
		log_field_string(buffer, name_ptr);
		log_field_string(buffer, signature_ptr);

		if (name_ptr!=NULL)      JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name_ptr);
		if (signature_ptr!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature_ptr);
		if (generic_ptr!=NULL)   JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic_ptr);
	
		log_eol(buffer);
		rawMonitorExit(&lockLog);
	}
}



