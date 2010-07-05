#include "dacapolock.h"

jboolean rawMonitorInit(jvmtiEnv* baseEnv, const char* name, MonitorLockType* lock) {
#ifdef USE_JAVA_LOCKS
    return JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, name, lock)==JNI_OK;
#else
	pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
	/* pthread_mutex_t m = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP; */

    lock->lock       = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;
    lock->cond       = (pthread_cond_t)PTHREAD_COND_INITIALIZER;
    
    lock->wait_count = 0;
    return !FALSE;
#endif
}


/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
void rawMonitorEnter(MonitorLockType* lock)
{
#ifdef USE_JAVA_LOCKS
    if (JVMTI_FUNC_PTR(baseEnv,RawMonitorEnter)(baseEnv, *lock) != JNI_OK) {
        fprintf(stderr, "cannot enter with raw monitor\n");
    }
#else
	pthread_mutex_lock(&(lock->lock));
#endif
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
void rawMonitorExit(MonitorLockType* lock)
{
#ifdef USE_JAVA_LOCKS
    if (JVMTI_FUNC_PTR(baseEnv,RawMonitorExit)(baseEnv, *lock) != JNI_OK) {
        fprintf(stderr, "cannot exit with raw monitor\n");
    }
#else
	pthread_mutex_unlock(&(lock->lock));
#endif
}

jboolean rawMonitorWait(MonitorLockType* lock,jlong timeout)
{
#ifdef USE_JAVA_LOCKS
	jvmtiError error = JVMTI_FUNC_PTR(baseEnv,RawMonitorWait)(baseEnv,*lock,timeout);
	
	if (error != JNI_OK && error != JVMTI_ERROR_NOT_MONITOR_OWNER) {
		fprintf(stderr, "cannot wait with raw monitor\n");
	}

	return error == JNI_OK;
#else
	lock->wait_count++;
	pthread_cond_wait(&(lock->cond),&(lock->lock));
	lock->wait_count--;
	return !FALSE;
#endif
}

void rawMonitorNotify(MonitorLockType* lock)
{
#ifdef USE_JAVA_LOCKS
	if (JVMTI_FUNC_PTR(baseEnv,RawMonitorNotify)(baseEnv,*lock)!=JNI_OK) {
		fprintf(stderr, "cannot notify raw monitor\n");
	}
#else
	/* do not signal if nothing is waiting */
	if (0<lock->wait_count)
		pthread_cond_signal(&(lock->cond));
#endif
}
