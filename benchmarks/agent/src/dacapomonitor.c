#include <classfile_constants.h>

#include "dacapolog.h"
#include "dacapotag.h"
#include "dacapooptions.h"
#include "dacapolock.h"
#include "dacapothread.h"
#include "dacapoagent.h"

#include "dacapomonitor.h"

static jboolean monitor_events = FALSE; 
static jboolean field_access_events = FALSE;

void monitor_init() {

}

void monitor_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {
	if (isSelected(OPT_MONITOR,NULL)) {
		monitor_events = TRUE;
		capabilities->can_generate_monitor_events         = availableCapabilities->can_generate_monitor_events;
		capabilities->can_generate_field_access_events    = availableCapabilities->can_generate_monitor_events;
		if (capabilities->can_generate_field_access_events)
			field_access_events = TRUE;
	}
}

void monitor_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks) {
	if (capabilities->can_generate_monitor_events && isSelected(OPT_MONITOR,NULL)) {
		DEFINE_CALLBACK(callbacks,MonitorContendedEnter,JVMTI_EVENT_MONITOR_CONTENDED_ENTER);
		DEFINE_CALLBACK(callbacks,MonitorContendedEntered,JVMTI_EVENT_MONITOR_CONTENDED_ENTERED);
		DEFINE_CALLBACK(callbacks,MonitorWait,JVMTI_EVENT_MONITOR_WAIT);
		DEFINE_CALLBACK(callbacks,MonitorWaited,JVMTI_EVENT_MONITOR_WAITED);
		DEFINE_CALLBACK(callbacks,ClassPrepare,JVMTI_EVENT_CLASS_PREPARE);
		DEFINE_CALLBACK(callbacks,FieldAccess,JVMTI_EVENT_FIELD_ACCESS);
	}
}

void monitor_live(jvmtiEnv* jvmti, JNIEnv* env) {

}

void monitor_logon(JNIEnv* env) {

}

static void reportMethod(char* class_name, jlong class_tag, jmethodID method) {
	if (logFile==NULL) return;
	
	char* name_ptr = NULL;
	char* signature_ptr  = NULL;
	char* generic_ptr = NULL;

	jint res = JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,method,&name_ptr,&signature_ptr,&generic_ptr);

	if (res!=JNI_OK) return;

	log_field_string(LOG_PREFIX_METHOD_PREPARE);
	log_field_pointer(method);
	log_field_jlong(class_tag);
	log_field_string(name_ptr);
	log_field_string(signature_ptr);
	log_eol();

	if (name_ptr!=NULL)      JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name_ptr);
	if (signature_ptr!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature_ptr);
	if (generic_ptr!=NULL)   JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic_ptr);
}



void monitor_class(jvmtiEnv *env, JNIEnv *jnienv, jclass klass) {
	if (!field_access_events) return;

	jint       field_count = 0;
	jfieldID*  fields = NULL;
	          
	jint res = JVMTI_FUNC_PTR(env,GetClassFields)(env,klass,&field_count,&fields);

	if (res!=JNI_OK) return;

	jlong class_tag = 0;

	enterCriticalSection(&lockTag);
	/* the class must have been tagged already */
	getTag(klass, &class_tag);
	exitCriticalSection(&lockTag);


	/*
	char* signature = NULL;
	char* generic   = NULL;

	res = JVMTI_FUNC_PTR(env,GetClassSignature)(env, klass, &signature, &generic);
	*/
	
	int i = 0;
	for(i=0;i<field_count;i++) {
		jint modifiers = 0;
		res =  JVMTI_FUNC_PTR(env,GetFieldModifiers)(env,klass,fields[i],&modifiers);
		
		if ((modifiers & JVM_ACC_VOLATILE)==JVM_ACC_VOLATILE) {
			res = JVMTI_FUNC_PTR(env,SetFieldAccessWatch)(env,klass,fields[i]);
			if (res != JNI_OK) {
				fprintf(stderr,"Unable to set watch point\n");
				exit(10);
			}
			
			char* name_ptr = NULL;
			char* signature_ptr  = NULL;
			char* generic_ptr = NULL;

			res = JVMTI_FUNC_PTR(env,GetFieldName)(env,klass,fields[i],&name_ptr,&signature_ptr,&generic_ptr);

			log_field_string(LOG_PREFIX_VOLATILE);
			log_field_jlong(class_tag);
			log_field_pointer(fields[i]);
			log_field_string(name_ptr);
			log_field_string(signature_ptr);
			log_eol();

			if (name_ptr!=NULL)      JVMTI_FUNC_PTR(env,Deallocate)(env,(unsigned char*)name_ptr);
			if (signature_ptr!=NULL) JVMTI_FUNC_PTR(env,Deallocate)(env,(unsigned char*)signature_ptr);
			if (generic_ptr!=NULL)   JVMTI_FUNC_PTR(env,Deallocate)(env,(unsigned char*)generic_ptr);
		}
	}
	
	JVMTI_FUNC_PTR(env,Deallocate)(env,(unsigned char*)fields);
	
	/*
	if (signature!=NULL) JVMTI_FUNC_PTR(env,Deallocate)(env,(unsigned char*)signature);
	if (generic!=NULL)   JVMTI_FUNC_PTR(env,Deallocate)(env,(unsigned char*)generic);
	*/
}

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
		
		thread_log(jni_env, thread, thread_tag, thread_has_new_tag);
		
		jniNativeInterface* jni_table;
		if (object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}

		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
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


		thread_log(jni_env, thread, thread_tag, thread_has_new_tag);
		
		jniNativeInterface* jni_table;
		if (object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}

		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
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

		thread_log(jni_env, thread, thread_tag, thread_has_new_tag);
		
		jniNativeInterface* jni_table;
		if (object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}

		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
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

		thread_log(jni_env, thread, thread_tag, thread_has_new_tag);

		jniNativeInterface* jni_table;
		if (object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}
		
		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
		} else {
			log_field_string(NULL);
		}
		
		log_eol();
		exitCriticalSection(&lockLog);
	}
}

void JNICALL callbackFieldAccess(jvmtiEnv *jvmti_env,JNIEnv* jni_env,jthread thread,jmethodID method,jlocation location,jclass field_klass,jobject object,jfieldID field) {
	if (logState) {
		jlong thread_tag = 0;
		jlong object_tag = 0;
		jlong class_tag  = 0;

		enterCriticalSection(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean object_has_new_tag = FALSE;
		if (object!=NULL) object_has_new_tag = getTag(object, &object_tag);
		getTag(field_klass, &class_tag);
		exitCriticalSection(&lockTag);
			
		enterCriticalSection(&lockLog);
		log_field_string(LOG_PREFIX_VOLATILE_ACCESS);

		thread_log(jni_env, thread, thread_tag, thread_has_new_tag);

		jniNativeInterface* jni_table;
		if (object_has_new_tag) {
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}
		}
		
		log_field_jlong(object_tag);
		if (object_has_new_tag) {
			LOG_OBJECT_CLASS(jni_table,jni_env,baseEnv,object);
		} else {
			log_field_string(NULL);
		}
		
		log_field_jlong(class_tag);
		log_field_pointer(field);
		
		log_eol();
		exitCriticalSection(&lockLog);
	}
}
