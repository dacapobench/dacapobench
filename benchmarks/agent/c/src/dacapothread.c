#include <sys/time.h>

#include "dacapo.h"

#include "dacapothread.h"

#include "dacapotag.h"
#include "dacapolock.h"
#include "dacapolog.h"

#include "dacapooptions.h"

struct thread_s {
    struct thread_s* next;
    jboolean  new_tag;
    jlong     tag;
    jthread   thread;
    jlong     thread_klass_tag;
    jclass    thread_klass;
    jboolean  thread_klass_has_new_tag;
    jboolean  start;
    jboolean  end;
};

struct thread_list_s {
	struct thread_list_s* next;
	jlong     tag;
	jthread   thread;
	jclass    thread_klass;
	jlong     thread_klass_tag;
	jlong     lastCPUTime;
	jlong     diffCPUTime;
};

MonitorLockType       lockThreadData;

struct thread_s *thread_head = NULL, *thread_tail = NULL;

struct thread_list_s *thread_list_head = NULL, *thread_list_tail = NULL;

static void logThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag, jclass thread_class, jlong thread_class_tag, jboolean thread_class_has_new_tag);
static void logThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag, jclass thread_class, jlong thread_class_tag, jboolean thread_class_has_new_tag);

void thread_init() {
	if (!rawMonitorInit(baseEnv,"thread data",&lockThreadData)) {
		/* JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "thread data", &(lockThreadData)) != JNI_OK) { */
		fprintf(stderr,"unable to create thread data lock\n");
		exit(10);
	}
}

void thread_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {
	if (isSelected(OPT_INTERVAL,NULL)) {
		capabilities->can_get_thread_cpu_time = availableCapabilities->can_get_thread_cpu_time;
	}
}

void thread_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks) {
	if (isSelected(OPT_THREAD,NULL)) {
		DEFINE_CALLBACK(callbacks,ThreadStart,JVMTI_EVENT_THREAD_START);
		DEFINE_CALLBACK(callbacks,ThreadEnd,JVMTI_EVENT_THREAD_END);
	}
}

void thread_live(jvmtiEnv* jvmti, JNIEnv* env) {
	/* fprintf(stderr,"thread_live %s\n",(logState?"true":"false")); */

	struct timeval tv;
    gettimeofday(&tv, NULL);

	rawMonitorEnter(&lockThreadData);
	
	struct thread_list_s* temp = thread_list_head;
	while (temp!=NULL) {
		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_THREAD_STATUS);
		log_field_time(buffer, &tv);
		log_field_jlong(buffer, temp->tag);
		log_eol(buffer);
		rawMonitorExit(&lockLog);
	}

	rawMonitorExit(&lockThreadData);
}

void thread_logon(JNIEnv* jnienv) {
	/* fprintf(stderr,"thread_logon %s\n",(logState?"true":"false")); */

	rawMonitorEnter(&lockThreadData);
	while (thread_head!=NULL) {
		struct thread_s* temp = thread_head;
		thread_head = thread_head->next;
		if (thread_head==NULL) thread_tail = NULL;

		if (temp!=NULL) {
			if (temp->start) logThreadStart(baseEnv, jnienv, temp->thread, temp->tag, temp->new_tag, temp->thread_klass, temp->thread_klass_tag, temp->thread_klass_has_new_tag);
			if (temp->end)   logThreadEnd(baseEnv, jnienv, temp->thread, temp->tag, temp->new_tag, temp->thread_klass, temp->thread_klass_tag, temp->thread_klass_has_new_tag);
			
			if (temp->end) {
				(*jnienv)->DeleteGlobalRef(jnienv,temp->thread);

				struct thread_list_s* found    = NULL;
				struct thread_list_s* previous = NULL;
				struct thread_list_s* check    = thread_list_head;
				
				while (found==NULL && check!=NULL) {
					if (check->tag == temp->tag)
						found = check;
					else 
						previous = check;
					check = check->next;
				}
				
				if (found!=NULL) {
					if (previous==NULL) {
						thread_list_head = found->next;
					} else {
						previous->next = found->next;
					}
					if (thread_list_tail==found) thread_list_tail = previous;

					(*jnienv)->DeleteGlobalRef(jnienv,found->thread);
					(*jnienv)->DeleteGlobalRef(jnienv,found->thread_klass);

					free(found);
				}
			} else {
				struct thread_list_s* tempList = (struct thread_list_s*)malloc(sizeof(struct thread_list_s));
				
				tempList->next        = NULL;

				tempList->thread       = temp->thread;
				tempList->tag          = temp->tag;
				tempList->thread_klass = temp->thread_klass;
				tempList->thread_klass_tag = temp->thread_klass_tag;

				tempList->lastCPUTime = 0;

				JVMTI_FUNC_PTR(baseEnv,GetThreadCpuTime)(baseEnv,temp->thread,&(tempList->lastCPUTime));
				
				if (thread_list_tail==NULL) {
					thread_list_head       = thread_list_tail = tempList;				
				} else {
					thread_list_tail->next = tempList;
					thread_list_tail       = tempList;				
				}
			}
		}
	}
	rawMonitorExit(&lockThreadData);
}

void thread_class(jvmtiEnv *env, JNIEnv *jnienv, jthread thread, jclass klass) {
	
}

void thread_agent_log(JNIEnv *env, jclass klass, jobject thread)
{

}

static void logThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag, jclass thread_klass, jlong thread_klass_tag, jboolean thread_klass_has_new_tag)
{
	rawMonitorEnter(&lockLog);
	void* buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_THREAD_START);
	log_field_current_time(buffer);

	log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
	
	log_eol(buffer);
	rawMonitorExit(&lockLog);
}

void JNICALL callbackThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
	/* fprintf(stderr,"callbackThreadStart %s\n",(logState?"true":"false")); */
	jniNativeInterface* jni_table = JNIFunctionTable();

	jlong thread_tag = 0;
	jlong thread_klass_tag = 0;
	jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
	
	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
	rawMonitorExit(&lockTag);

	if (logState) {
		/* log thread start */

		logThreadStart(jvmti_env, jni_env, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);
		
		/* add to the thread list */
		rawMonitorEnter(&lockThreadData);
		struct thread_list_s* found    = NULL;
		struct thread_list_s* check    = thread_list_head;
		
		/* try and find it in the list */
		while (found==NULL && check!=NULL) {
			if (check->tag == thread_tag)
				found = check;
			check = check->next;
		}
		
		/* add it if it is not there */
		if (found==NULL) {
			found = (struct thread_list_s*)malloc(sizeof(struct thread_list_s));
			
			found->thread                   = (*jni_env)->NewGlobalRef(jni_env,thread);
			found->tag                      = thread_tag;
			found->thread_klass             = (*jni_env)->NewGlobalRef(jni_env,thread_klass);
			found->thread_klass_tag         = thread_klass_tag;
			found->next          = NULL;
			found->lastCPUTime   = 0;
			
			if (thread_list_head == NULL)
				thread_list_head = thread_list_tail = found;
			else {
				thread_list_tail->next = found;
				thread_list_tail = found;
			}
		}
		rawMonitorExit(&lockThreadData); 
	} else {
		struct thread_s* new_thread = (struct thread_s*)malloc(sizeof(struct thread_s));
	
		new_thread->next    = NULL;

		new_thread->thread  = (*jni_env)->NewGlobalRef(jni_env,thread);
		new_thread->new_tag = thread_has_new_tag;
		new_thread->tag     = thread_tag;
		new_thread->thread_klass = (*jni_env)->NewGlobalRef(jni_env,thread_klass);
		new_thread->thread_klass_tag = thread_klass_tag;
		new_thread->thread_klass_has_new_tag = thread_klass_has_new_tag;
		   
		new_thread->start   = !FALSE;
		new_thread->end     = FALSE;

		rawMonitorEnter(&lockThreadData);
		if (thread_tail==NULL) {
			thread_head = thread_tail = new_thread;
		} else {
			thread_tail->next         = new_thread;
			thread_tail               = new_thread;
		}
		rawMonitorExit(&lockThreadData); 
	}
}

static void logThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag, jclass thread_klass, jlong thread_klass_tag, jboolean thread_klass_has_new_tag)
{
	rawMonitorEnter(&lockLog);
	void* buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_THREAD_STOP);
	log_field_current_time(buffer);

	log_thread(buffer, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);	
	
	log_eol(buffer);
	rawMonitorExit(&lockLog);
}

void JNICALL callbackThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
	/* fprintf(stderr,"callbackThreadEnd %s\n",(logState?"true":"false")); */

	jniNativeInterface* jni_table = JNIFunctionTable();

	jlong thread_tag = 0;
	jlong thread_klass_tag = 0;
	jclass thread_klass = JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,thread);
	
	rawMonitorEnter(&lockTag);
	jboolean thread_has_new_tag = getTag(thread, &thread_tag);
	jboolean thread_klass_has_new_tag = getTag(thread_klass, &thread_klass_tag);
	rawMonitorExit(&lockTag);

	if (logState ) {
		logThreadEnd(jvmti_env, jni_env, thread, thread_tag, thread_has_new_tag, thread_klass, thread_klass_tag, thread_klass_has_new_tag);

		rawMonitorEnter(&lockThreadData); 
		struct thread_list_s* found    = NULL;
		struct thread_list_s* previous = NULL;
		struct thread_list_s* check    = thread_list_head;
		
		while (found==NULL && check!=NULL) {
			if (check->tag == thread_tag)
				found = check;
			else 
				previous = check;
			check = check->next;
		}
		
		if (found!=NULL) {
			if (previous==NULL) {
				thread_list_head = found->next;
			} else {
				previous->next = found->next;
			}
			if (thread_list_tail==found) thread_list_tail = previous;
		
			(*jni_env)->DeleteGlobalRef(jni_env,found->thread);
			(*jni_env)->DeleteGlobalRef(jni_env,found->thread_klass);
			
			free(found);
		}
		rawMonitorExit(&lockThreadData); 
	} else {
		rawMonitorEnter(&lockThreadData);
		struct thread_s* found = NULL;
		if (!thread_has_new_tag) {
			struct thread_s* temp  = thread_head;
			
			while(found==NULL && temp!=NULL) {
				if (thread_tag==temp->tag) found = temp;
				temp = temp->next;
			}
		}
	
		if (found!=NULL) {
			found->end = !FALSE;
		} else {
			struct thread_s* new_thread = (struct thread_s*)malloc(sizeof(struct thread_s));
	
			new_thread->next   = NULL;

			new_thread->thread = (*jni_env)->NewGlobalRef(jni_env,thread);
			new_thread->new_tag = thread_has_new_tag;
			new_thread->tag    = thread_tag;

			new_thread->thread_klass = (*jni_env)->NewGlobalRef(jni_env,thread_klass);
			new_thread->thread_klass_tag = thread_klass_tag;
			new_thread->thread_klass_has_new_tag = thread_klass_has_new_tag;

			new_thread->start  = FALSE;
			new_thread->end    = !FALSE;

			if (thread_tail==NULL) {
				thread_head = thread_tail = new_thread;
			} else {
				thread_tail->next = new_thread;
				thread_tail       = new_thread;
			}
		} 
		rawMonitorExit(&lockThreadData);
	}
}

void threads_states(JNIEnv* env) {
	struct timeval tv;
    gettimeofday(&tv, NULL);

	jlong index = 0;
	rawMonitorEnter(&lockThreadData);
	struct thread_list_s *temp = thread_list_head;
	while (temp != NULL) {
		jlong thread_time = temp->lastCPUTime;
		JVMTI_FUNC_PTR(baseEnv,GetThreadCpuTime)(baseEnv,temp->thread,&thread_time);

		temp->diffCPUTime = thread_time - temp->lastCPUTime;
		temp->lastCPUTime = thread_time;
		temp = temp->next;
	}

	temp = thread_list_head;
	while (temp != NULL) {
		rawMonitorEnter(&lockLog);
		void* buffer = log_buffer_get();
		log_field_string(buffer, LOG_PREFIX_THREAD_TIME);
		log_field_time(buffer, &tv);
		log_field_jlong(buffer, index++);
		log_field_jlong(buffer, temp->tag);
		log_field_jlong(buffer, temp->diffCPUTime/1000); /* time in microseconds */
		log_eol(buffer);
		rawMonitorExit(&lockLog);
		temp = temp->next;
	}
	rawMonitorExit(&lockThreadData);
}
