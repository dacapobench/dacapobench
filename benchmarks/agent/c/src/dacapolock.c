#include "dacapolock.h"

jboolean rawMonitorInit(jvmtiEnv* baseEnv, const char* name, MonitorLockType* lock) {
	pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
	/* pthread_mutex_t m = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP; */

    lock->lock       = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;
    lock->cond       = (pthread_cond_t)PTHREAD_COND_INITIALIZER;
    
    lock->wait_count = 0;
    return !FALSE;
}


/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
void rawMonitorEnter(MonitorLockType* lock)
{
	pthread_mutex_lock(&(lock->lock));
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
void rawMonitorExit(MonitorLockType* lock)
{
	pthread_mutex_unlock(&(lock->lock));
}

jboolean rawMonitorWait(MonitorLockType* lock,jlong timeout)
{
	lock->wait_count++;
	pthread_cond_wait(&(lock->cond),&(lock->lock));
	lock->wait_count--;
	return !FALSE;
}

void rawMonitorNotify(MonitorLockType* lock)
{
	/* do not signal if nothing is waiting */
	if (0<lock->wait_count)
		pthread_cond_signal(&(lock->cond));
}
