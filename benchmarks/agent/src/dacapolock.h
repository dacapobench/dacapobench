#ifndef DACAPO_LOCK_H
#define DACAPO_LOCK_H

#include <pthread.h>

#include "dacapo.h"

/* #define USE_JAVA_LOCKS */

#ifdef USE_JAVA_LOCKS
typedef jrawMonitorID MonitorLockType;
#else
struct MonitorLockType_s {
    pthread_mutex_t     lock;
    pthread_cond_t      cond;
    int                 wait_count;
};
typedef struct MonitorLockType_s MonitorLockType;
#endif

jboolean rawMonitorInit(jvmtiEnv* baseEnv, const char* name, MonitorLockType* lock);

void     rawMonitorEnter(MonitorLockType* lock);
void     rawMonitorExit(MonitorLockType* lock);
jboolean rawMonitorWait(MonitorLockType* lock,jlong timeout);
void     rawMonitorNotify(MonitorLockType* lock);

#endif
