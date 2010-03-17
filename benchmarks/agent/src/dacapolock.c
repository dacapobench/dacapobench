#include "dacapolock.h"

/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
void enterCriticalSection(jrawMonitorID* lock)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(baseEnv,RawMonitorEnter)(baseEnv, *lock);
    if (error != JNI_OK) {
        fprintf(stderr, "cannot enter with raw monitor\n");
    }
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
void exitCriticalSection(jrawMonitorID* lock)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(baseEnv,RawMonitorExit)(baseEnv, *lock);
    if (error != JNI_OK) {
        fprintf(stderr, "cannot exit with raw monitor\n");
    }
}

