#include "dacapolock.h"

/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
void rawMonitorEnter(jrawMonitorID* lock)
{
    if (JVMTI_FUNC_PTR(baseEnv,RawMonitorEnter)(baseEnv, *lock) != JNI_OK) {
        fprintf(stderr, "cannot enter with raw monitor\n");
    }
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
void rawMonitorExit(jrawMonitorID* lock)
{
    if (JVMTI_FUNC_PTR(baseEnv,RawMonitorExit)(baseEnv, *lock) != JNI_OK) {
        fprintf(stderr, "cannot exit with raw monitor\n");
    }
}

jboolean rawMonitorWait(jrawMonitorID* lock,jlong timeout)
{
	jvmtiError error = JVMTI_FUNC_PTR(baseEnv,RawMonitorWait)(baseEnv,*lock,timeout);
	
	if (error != JNI_OK && error != JVMTI_ERROR_NOT_MONITOR_OWNER) {
		fprintf(stderr, "cannot wait with raw monitor\n");
	}
	
	return error == JNI_OK;
}

void rawMonitorNotify(jrawMonitorID* lock)
{
	if (JVMTI_FUNC_PTR(baseEnv,RawMonitorNotify)(baseEnv,*lock)!=JNI_OK) {
		fprintf(stderr, "cannot notify raw monitor\n");
	}
}
