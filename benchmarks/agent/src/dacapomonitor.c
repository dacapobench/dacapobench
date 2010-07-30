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

struct MonitorLockType_s monitorLock;

void monitor_init() {
	rawMonitorInit(baseEnv, "monitor lock", &monitorLock);
}

void monitor_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {
	if (isSelected(OPT_MONITOR,NULL)) {
		monitor_events = TRUE;
		capabilities->can_generate_monitor_events         = availableCapabilities->can_generate_monitor_events;
		capabilities->can_generate_field_access_events    = availableCapabilities->can_generate_monitor_events;
		if (isSelected(OPT_VOLATILE,NULL) && capabilities->can_generate_field_access_events)
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
		if (field_access_events)
			DEFINE_CALLBACK(callbacks,FieldAccess,JVMTI_EVENT_FIELD_ACCESS);
	}
}

void monitor_live(jvmtiEnv* jvmti, JNIEnv* env) {

}

void monitor_logon(JNIEnv* env) {

}

static void reportMethod(char* class_name, jlong class_tag, jmethodID method) {
	if (! logFileOpen()) return;
	
	char* name_ptr = NULL;
	char* signature_ptr  = NULL;
	char* generic_ptr = NULL;

	jint res = JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,method,&name_ptr,&signature_ptr,&generic_ptr);

	if (res!=JNI_OK) return;

	void* buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_METHOD_PREPARE);
	log_field_current_time(buffer);
	log_field_pointer(buffer, method);
	log_field_jlong(buffer, class_tag);
	log_field_string(buffer, name_ptr);
	log_field_string(buffer, signature_ptr);
	log_eol(buffer);

	if (name_ptr!=NULL)      JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name_ptr);
	if (signature_ptr!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature_ptr);
	if (generic_ptr!=NULL)   JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic_ptr);
}



void monitor_class(jvmtiEnv *env, JNIEnv *jnienv, jthread thread, jclass klass) {
	if (!field_access_events) return;

	jint       field_count = 0;
	jfieldID*  fields = NULL;
	          
	jint res = JVMTI_FUNC_PTR(env,GetClassFields)(env,klass,&field_count,&fields);

	if (res!=JNI_OK) return;

	jlong class_tag = 0;

	rawMonitorEnter(&lockTag);
	/* the class must have been tagged already */
	getTag(klass, &class_tag);
	rawMonitorExit(&lockTag);


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

			void* buffer = log_buffer_get();
			log_field_string(buffer, LOG_PREFIX_VOLATILE);
			log_field_current_time(buffer);
			log_field_jlong(buffer, class_tag);
			log_field_pointer(buffer, fields[i]);
			log_field_string(buffer, name_ptr);
			log_field_string(buffer, signature_ptr);
			log_eol(buffer);

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

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    reportMonitorEnter
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLogMonitorEnter
  (JNIEnv *local_env, jclass klass, jobject thread, jobject object)
{
	jniNativeInterface* jni_table = JNIFunctionTable();

	// jclass GetObjectClass(JNIEnv *env, jobject obj);
	jlong thread_tag = 0;
	jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(local_env,thread);
	jlong thread_klass_tag = 0;
	jlong object_tag = 0;
	jclass object_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(local_env,object);
	jlong object_klass_tag = 0;
	
	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
	jboolean object_has_new_tag = getTag(object, &object_tag);
	jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
	rawMonitorExit(&lockTag);

	rawMonitorEnter(&lockLog);
	void* buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_MONITOR_ACQUIRE);
	log_field_current_time(buffer);
	
	log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
	
	log_field_jlong(buffer, object_tag);
	log_class(buffer, object_klass, object_klass_tag, object_klass_has_new_tag);
	
	log_eol(buffer);
	rawMonitorExit(&lockLog);
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    reportMonitorExit
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLogMonitorExit
  (JNIEnv *local_env, jclass klass, jobject thread, jobject object)
{
	jniNativeInterface* jni_table = JNIFunctionTable();

	// jclass GetObjectClass(JNIEnv *env, jobject obj);
	jlong thread_tag = 0;
	jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(local_env,thread);
	jlong thread_klass_tag = 0;
	jlong object_tag = 0;
	jclass object_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(local_env,object);
	jlong object_klass_tag = 0;

	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
	jboolean object_has_new_tag = getTag(object, &object_tag);
	jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
	rawMonitorExit(&lockTag);

	rawMonitorEnter(&lockLog);
	void* buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_MONITOR_RELEASE);
	log_field_current_time(buffer);
	
	log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
	
	log_field_jlong(buffer, object_tag);
	log_class(buffer, object_klass, object_klass_tag, object_klass_has_new_tag);

	log_eol(buffer);
	rawMonitorExit(&lockLog);
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    reportMonitorExit
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLogMonitorNotify
  (JNIEnv *local_env, jclass klass, jobject thread, jobject object)
{
	jniNativeInterface* jni_table = JNIFunctionTable();

	// jclass GetObjectClass(JNIEnv *env, jobject obj);
	jlong thread_tag = 0;
	jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(local_env,thread);
	jlong thread_klass_tag = 0;
	jlong object_tag = 0;
	jclass object_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(local_env,object);
	jlong object_klass_tag = 0;

	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
	jboolean object_has_new_tag = getTag(object, &object_tag);
	jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
	rawMonitorExit(&lockTag);

	rawMonitorEnter(&lockLog);
	void* buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_MONITOR_NOTIFY);
	log_field_current_time(buffer);
	
	log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
	
	log_field_jlong(buffer, object_tag);
	log_class(buffer, object_klass, object_klass_tag, object_klass_has_new_tag);

	log_eol(buffer);
	rawMonitorExit(&lockLog);
}

void JNICALL callbackMonitorContendedEnter(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jobject object)
{
	if (logState) {
		jniNativeInterface* jni_table = JNIFunctionTable();
	
		// jclass GetObjectClass(JNIEnv *env, jobject obj);
		jlong thread_tag = 0;
		jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
		jlong thread_klass_tag = 0;
		jlong object_tag = 0;
		jclass object_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,object);
		jlong object_klass_tag = 0;
	
		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
		jboolean object_has_new_tag = getTag(object, &object_tag);
		jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
		rawMonitorExit(&lockTag);
	
		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_MONITOR_CONTENTED_ENTER);
		log_field_current_time(buffer);
		
		log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
		
		log_field_jlong(buffer, object_tag);
		log_class(buffer, object_klass, object_klass_tag, object_klass_has_new_tag);
	
		log_eol(buffer);
		rawMonitorExit(&lockLog);
	}
}

void JNICALL callbackMonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jobject object)
{
	if (logState) {
		jniNativeInterface* jni_table = JNIFunctionTable();
	
		// jclass GetObjectClass(JNIEnv *env, jobject obj);
		jlong thread_tag = 0;
		jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
		jlong thread_klass_tag = 0;
		jlong object_tag = 0;
		jclass object_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,object);
		jlong object_klass_tag = 0;
	
		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
		jboolean object_has_new_tag = getTag(object, &object_tag);
		jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
		rawMonitorExit(&lockTag);
	
		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_MONITOR_CONTENTED_ENTERED);
		log_field_current_time(buffer);
		
		log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
		
		log_field_jlong(buffer, object_tag);
		log_class(buffer, object_klass, object_klass_tag, object_klass_has_new_tag);
	
		log_eol(buffer);
		rawMonitorExit(&lockLog);
	}
}

void JNICALL callbackMonitorWait(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jobject object, jlong timeout)
{
	if (logState) {
		jniNativeInterface* jni_table = JNIFunctionTable();
	
		// jclass GetObjectClass(JNIEnv *env, jobject obj);
		jlong thread_tag = 0;
		jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
		jlong thread_klass_tag = 0;
		jlong object_tag = 0;
		jclass object_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,object);
		jlong object_klass_tag = 0;
	
		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
		jboolean object_has_new_tag = getTag(object, &object_tag);
		jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
		rawMonitorExit(&lockTag);
	
		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_MONITOR_WAIT);
		log_field_current_time(buffer);
		
		log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
		
		log_field_jlong(buffer, object_tag);
		log_class(buffer, object_klass, object_klass_tag, object_klass_has_new_tag);
		
		log_field_jlong(buffer, timeout);
	
		log_eol(buffer);
		rawMonitorExit(&lockLog);
	}
}

void JNICALL callbackMonitorWaited(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jobject object, jboolean timed_out)
{
	if (logState) {
		jniNativeInterface* jni_table = JNIFunctionTable();
	
		// jclass GetObjectClass(JNIEnv *env, jobject obj);
		jlong thread_tag = 0;
		jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
		jlong thread_klass_tag = 0;
		jlong object_tag = 0;
		jclass object_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,object);
		jlong object_klass_tag = 0;
	
		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
		jboolean object_has_new_tag = getTag(object, &object_tag);
		jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
		rawMonitorExit(&lockTag);
	
		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_MONITOR_WAITED);
		log_field_current_time(buffer);
		
		log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
		
		log_field_jlong(buffer, object_tag);
		log_class(buffer, object_klass, object_klass_tag, object_klass_has_new_tag);
		
		log_field_jboolean(buffer, timed_out);
	
		log_eol(buffer);
		rawMonitorExit(&lockLog);
	}
}

void JNICALL callbackFieldAccess(jvmtiEnv *jvmti_env,JNIEnv* jni_env,jthread thread,jmethodID method,jlocation location,jclass field_klass,jobject object,jfieldID field) {
	if (logState) {
		jniNativeInterface* jni_table = JNIFunctionTable();
		
		jlong thread_tag = 0;
		jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
		jlong thread_klass_tag = 0;
		jlong object_tag = 0;
		jclass object_klass = (object!=NULL)?JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,object):NULL;
		jlong object_klass_tag = 0;
		jlong class_tag  = 0;

		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
		jboolean object_has_new_tag = (object!=NULL)?getTag(object, &object_tag):FALSE;
		jboolean object_klass_has_new_tag = (object!=NULL)?getTag(object_klass, &object_klass_tag):FALSE;
		getTag(field_klass, &class_tag);
		rawMonitorExit(&lockTag);
		
		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_VOLATILE_ACCESS);

		log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);

		log_field_jlong(buffer, object_tag);
		log_class(buffer, object_klass, object_klass_tag, object_klass_has_new_tag);
		
		log_field_jlong(buffer, class_tag);
		log_field_pointer(buffer, field);
		
		log_eol(buffer);
		rawMonitorExit(&lockLog);
	}
}
