#include "dacapoallocation.h"

#include "dacapotag.h"
#include "dacapolog.h"
#include "dacapolock.h"

/* Callback for JVMTI_EVENT_VM_OBJECT_ALLOC */
void JNICALL callbackVMObjectAlloc(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                jobject object, jclass object_klass, jlong size)
{
	if (jvmRunning && !jvmStopped) {
		jlong class_tag  = 0;
		jlong thread_tag = 0;

		enterCriticalSection(&lockTag);
		jlong tag = setTag(object, size);
		jboolean class_has_new_tag = getTag(object_klass, &class_tag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		exitCriticalSection(&lockTag);

		/* trace allocation */
		enterCriticalSection(&lockLog);
		if (class_has_new_tag || thread_has_new_tag) {
		jniNativeInterface* jni_table;
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}

			fprintf(logFile, "A:%" FORMAT_JLONG ":%" FORMAT_JLONG,tag,class_tag);
		    if (class_has_new_tag) LOG_CLASS(logFile,jni_table,env,baseEnv,object_klass);
		    fprintf(logFile, ":%" FORMAT_JLONG,thread_tag);
		    if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,env,baseEnv,thread);
		    fprintf(logFile, ":%" FORMAT_JLONG "\n",size);
		} else {
			fprintf(logFile, "A:%" FORMAT_JLONG ":%" FORMAT_JLONG ":%" FORMAT_JLONG ":%" FORMAT_JLONG "\n",tag,class_tag,thread_tag,size);
		}
		exitCriticalSection(&lockLog);
	}
}

/* Callback for JVMTI_EVENT_OBJECT_FREE */
void JNICALL callbackObjectFree(jvmtiEnv *jvmti, jlong tag)
{
	if (logState ) {
		/* trace allocation */
		fprintf(logFile, "F:%" FORMAT_JLONG "\n",tag);
	}
}

