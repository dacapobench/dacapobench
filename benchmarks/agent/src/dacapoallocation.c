#include "dacapolog.h"
#include "dacapotag.h"
#include "dacapooptions.h"
#include "dacapolock.h"
#include "dacapothread.h"

#include "dacapoallocation.h"

static long gcForceCycle = 0;
static long gcCount = 0;

void allocation_init() {

}

void allocation_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {
	if (isSelected(OPT_ALLOCATE,NULL)) {
		capabilities->can_generate_vm_object_alloc_events = availableCapabilities->can_generate_vm_object_alloc_events;
		capabilities->can_generate_object_free_events     = availableCapabilities->can_generate_object_free_events;
	}
}

void allocation_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks) {
	if (isSelected(OPT_ALLOCATE,NULL)) {
		if (capabilities->can_generate_vm_object_alloc_events) DEFINE_CALLBACK(callbacks,VMObjectAlloc,JVMTI_EVENT_VM_OBJECT_ALLOC);
		if (capabilities->can_generate_object_free_events) DEFINE_CALLBACK(callbacks,ObjectFree,JVMTI_EVENT_OBJECT_FREE);
	}
	char arg[1024];
	if (isSelected(OPT_GC,arg)) {
	    gcForceCycle = atol(arg);
	}
}

static jint forceGC(JNIEnv *env) {
	gcCount = 0;
	rawMonitorEnter(&lockLog);
	log_field_string(LOG_PREFIX_GC);
	log_field_current_time();
	log_eol();
	rawMonitorExit(&lockLog);
	jint res = JVMTI_FUNC_PTR(baseEnv,ForceGarbageCollection)(baseEnv);
	setReportHeap(env,!FALSE);
	return res;
}

void allocation_logon(JNIEnv* env) {
	if (jvmRunning && !jvmStopped && 0<gcForceCycle) {
		if (forceGC(env) != JNI_OK) {
			fprintf(stderr,"force gc failed\n");
		}
	}
}

void allocation_live(jvmtiEnv* jvmti, JNIEnv* env) {
	// at this point we should enumerate all the objects in the heap.
	// how do we do this sensibly? we do not have an allocation order here, so
	// pointer distances at this point are meaningless.
}

void allocation_class(jvmtiEnv *env, JNIEnv *jnienv, jthread thread, jclass klass) {

}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    internalLogPointerChange
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalLogPointerChange
  (JNIEnv *jni_env, jclass agent_klass, jthread thread, jobject after, jobject object, jobject before)
{
	if (jvmRunning && !jvmStopped) {
		jniNativeInterface* jni_table = JNIFunctionTable();

		jlong thread_tag = 0;
		jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
		jlong thread_klass_tag = 0;
		jlong before_tag = 0;
		jclass before_klass = (before!=NULL)?JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,before):NULL;
		jlong before_klass_tag = 0;
		jlong object_tag = 0;
		jclass object_klass = (object!=NULL)?JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,object):NULL;
		jlong object_klass_tag = 0;
		jlong after_tag  = 0;
		jclass after_klass = (after!=NULL)?JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,after):NULL;
		jlong after_klass_tag  = 0;

		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
		jboolean before_has_new_tag = getTag(before, &before_tag);
		jboolean before_klass_has_new_tag = getTag(before_klass, &before_klass_tag);
		jboolean object_has_new_tag = getTag(object, &object_tag);
		jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
		jboolean after_has_new_tag  = getTag(after,  &after_tag);
		jboolean after_klass_has_new_tag  = getTag(after_klass,  &after_klass_tag);
		rawMonitorExit(&lockTag);
		
		/* trace allocation */
		rawMonitorEnter(&lockLog);
		log_field_string(LOG_PREFIX_POINTER);
		log_field_current_time();
		
		log_thread(thread,thread_tag,thread_has_new_tag,thread_klass,thread_klass_tag,thread_klass_has_new_tag);
		
		log_field_jlong(object_tag);
		log_class(object_klass,object_klass_tag,object_klass_has_new_tag);
		
		log_field_jlong(before_tag);
		log_class(before_klass,before_klass_tag,before_klass_has_new_tag);
		
		log_field_jlong(after_tag);
		log_class(after_klass,after_klass_tag,after_klass_has_new_tag);
		
		log_eol();
		rawMonitorExit(&lockLog);
	}
}

/*
 * Class:     org_dacapo_instrument_Agent
 * Method:    internalAllocReport
 * Signature: (Ljava/lang/Thread;Ljava/lang/Object;Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_org_dacapo_instrument_Agent_internalAllocReport
  (JNIEnv *jni_env, jclass agent_klass, jthread thread, jobject object, jclass klass)
{
	jlong size = 0;
	
	JVMTI_FUNC_PTR(baseEnv,GetObjectSize)(baseEnv,object,&size);

	callbackVMObjectAlloc(baseEnv, jni_env, thread, object, klass, size);	
}

/* Callback for JVMTI_EVENT_VM_OBJECT_ALLOC */
void JNICALL callbackVMObjectAlloc(jvmtiEnv *jvmti, JNIEnv *jni_env, jthread thread,
                jobject object, jclass object_klass, jlong size)
{
	if (jvmRunning && !jvmStopped) {
		jniNativeInterface* jni_table = JNIFunctionTable();

		jlong object_klass_tag  = 0;
		jlong thread_tag = 0;
		jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,object);
		jlong thread_klass_tag = 0;

		gcCount += size;
		if (0<gcForceCycle && gcForceCycle<gcCount)
			forceGC(jni_env);
		
		rawMonitorEnter(&lockTag);
		jlong tag = setTag(object, size);
		jboolean object_klass_has_new_tag = getTag(object_klass, &object_klass_tag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
		rawMonitorExit(&lockTag);

		/* trace allocation */
		rawMonitorEnter(&lockLog);
		log_field_string(LOG_PREFIX_ALLOCATION);
		log_field_current_time();

		log_thread(thread,thread_tag,thread_has_new_tag,object_klass,object_klass_tag,object_klass_has_new_tag);

		log_field_jlong(tag);
		log_class(object_klass,object_klass_tag,object_klass_has_new_tag);

		log_field_jlong(size);
		
		log_eol();
		rawMonitorExit(&lockLog);
	}
}

/* Callback for JVMTI_EVENT_OBJECT_FREE */
void JNICALL callbackObjectFree(jvmtiEnv *jvmti, jlong tag)
{
	if (jvmRunning && !jvmStopped) {
		/* trace free */
		rawMonitorEnter(&lockLog);
		log_field_string(LOG_PREFIX_FREE);
		log_field_current_time();
		log_field_jlong(tag);
		log_eol();
		rawMonitorExit(&lockLog);
	}
}

