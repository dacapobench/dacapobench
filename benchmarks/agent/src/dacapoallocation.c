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
	enterCriticalSection(&lockTag);
	log_field_string(LOG_PREFIX_GC);
	log_eol();
	exitCriticalSection(&lockTag);
	jint res = JVMTI_FUNC_PTR(baseEnv,ForceGarbageCollection)(baseEnv);
	setReportHeap(env);
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

}

void allocation_class(jvmtiEnv *env, JNIEnv *jnienv, jclass klass) {

}

/* Callback for JVMTI_EVENT_VM_OBJECT_ALLOC */
void JNICALL callbackVMObjectAlloc(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                jobject object, jclass object_klass, jlong size)
{
	if (jvmRunning && !jvmStopped) {
		jlong class_tag  = 0;
		jlong thread_tag = 0;

		gcCount += size;
		if (0<gcForceCycle && gcForceCycle<gcCount)
			forceGC(env);
		
		enterCriticalSection(&lockTag);
		jlong tag = setTag(object, size);
		jboolean class_has_new_tag = getTag(object_klass, &class_tag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		exitCriticalSection(&lockTag);

		/* trace allocation */
		enterCriticalSection(&lockLog);
		log_field_string(LOG_PREFIX_ALLOCATION);

		log_field_jlong(tag);
	    if (class_has_new_tag) {
			jniNativeInterface* jni_table;
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}

	    	LOG_CLASS(jni_table,baseEnv,object_klass);
	    } else 
	    	log_field_string(NULL);

		thread_log(env,thread,thread_tag,thread_has_new_tag);

		log_field_jlong(size);
		
		log_eol();
		
		exitCriticalSection(&lockLog);
	}
}

/* Callback for JVMTI_EVENT_OBJECT_FREE */
void JNICALL callbackObjectFree(jvmtiEnv *jvmti, jlong tag)
{
	if (jvmRunning && !jvmStopped) {
		/* trace free */
		log_field_string(LOG_PREFIX_FREE);
		log_field_jlong(tag);
		log_eol();
	}
}

