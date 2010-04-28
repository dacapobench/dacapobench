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
    jboolean  start;
    jboolean  end;
};

struct thread_list_s {
	struct thread_list_s* next;
	jlong     tag;
	jthread   thread;
};

jrawMonitorID       lockThreadData;

struct thread_s *thread_head = NULL, *thread_tail = NULL;

struct thread_list_s *thread_list_head = NULL, *thread_list_tail = NULL;

static void logThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag);
static void logThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag);

void thread_init() {
	if (JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "thread data", &(lockThreadData)) != JNI_OK) {
		fprintf(stderr,"unable to create thread data lock\n");
		exit(10);
	}
}

void thread_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities) {

}

void thread_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks) {
	if (isSelected(OPT_THREAD,NULL)) {
		DEFINE_CALLBACK(callbacks,ThreadStart,JVMTI_EVENT_THREAD_START);
		DEFINE_CALLBACK(callbacks,ThreadEnd,JVMTI_EVENT_THREAD_END);
	}
}

void thread_live(jvmtiEnv* jvmti, JNIEnv* env) {
	rawMonitorEnter(&lockThreadData);
	
	struct thread_list_s* temp = thread_list_head;
	while (temp!=NULL) {
		rawMonitorEnter(&lockLog);
		log_field_string(LOG_PREFIX_THREAD_STATUS);
		log_field_jlong(temp->tag);
		log_eol();
		rawMonitorExit(&lockLog);
	}

	rawMonitorExit(&lockThreadData);
}

void thread_logon(JNIEnv* jnienv) {
	/*
	rawMonitorEnter(&lockThreadData);
	while (thread_head!=NULL) {
		struct thread_s* temp = thread_head;
		if (thread_head==NULL) thread_tail = NULL;
		else thread_head = thread_head->next;

		if (temp!=NULL) {
			if (temp->start) logThreadStart(baseEnv, jnienv, temp->thread, temp->tag, temp->new_tag);
			if (temp->end)   logThreadEnd(baseEnv, jnienv, temp->thread, temp->tag, temp->new_tag);
			
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

					free(found);
				}
			} else {
				struct thread_list_s* tempList = (struct thread_list_s*)malloc(sizeof(struct thread_list_s));
				
				tempList->thread = temp->thread;
				tempList->next   = NULL;
				tempList->tag    = temp->tag;
				
				if (thread_list_tail==NULL) {
					thread_list_head = thread_list_tail = tempList;				
				} else {
					thread_list_tail->next = tempList;
					thread_list_tail       = tempList;				
				}
			}
		}
	}
	rawMonitorExit(&lockThreadData);
	*/
}

void thread_class(jvmtiEnv *env, JNIEnv *jnienv, jclass klass) {

}

void thread_log(JNIEnv* env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag) {
	jniNativeInterface* jni_table;
	log_field_jlong(thread_tag);
	if (thread_has_new_tag) {
		jniNativeInterface* jni_table;
		if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
			fprintf(stderr, "failed to get JNI function table\n");
			exit(1);
		}

		LOG_OBJECT_CLASS(jni_table,env,baseEnv,thread);

		// get class and get thread name.
		jvmtiThreadInfo info;
		JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
		log_field_string(info.name);
		if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
	} else {
		log_field_string(NULL);
		log_field_string(NULL);
	}
}

static void logThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag)
{
	rawMonitorEnter(&lockLog);
	log_field_string(LOG_PREFIX_THREAD_START);
	log_field_time();

	thread_log(jni_env, thread, thread_tag, thread_has_new_tag);
	
	log_eol();
	rawMonitorExit(&lockLog);
}

void JNICALL callbackThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
	if (logState) {
		/* log thread start */
		jlong thread_tag = 0;

		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		rawMonitorExit(&lockTag);

		logThreadStart(jvmti_env, jni_env, thread, thread_tag, thread_has_new_tag);
		
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
			
			found->thread = (*jni_env)->NewGlobalRef(jni_env,thread);
			found->tag    = thread_tag;
			found->next   = NULL;
			
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
	
		new_thread->next   = NULL;
		new_thread->thread = (*jni_env)->NewGlobalRef(jni_env,thread);
		   
		new_thread->start  = !FALSE;
		new_thread->end    = FALSE;

		rawMonitorEnter(&lockTag);
		new_thread->new_tag = getTag(thread, &(new_thread->tag));
		rawMonitorExit(&lockTag);

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

static void logThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jlong thread_tag, jboolean thread_has_new_tag)
{
	rawMonitorEnter(&lockLog);
	log_field_string(LOG_PREFIX_THREAD_STOP);
	log_field_time();
	
	thread_log(jni_env, thread, thread_tag, thread_has_new_tag);
	
	log_eol();
	rawMonitorExit(&lockLog);
}

void JNICALL callbackThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
	if (logState ) {
		jlong thread_tag = 0;
	
		rawMonitorEnter(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		rawMonitorExit(&lockTag);

		logThreadEnd(jvmti_env, jni_env, thread, thread_tag, thread_has_new_tag);

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
			
			free(found);
		}
		rawMonitorExit(&lockThreadData); 
	} else {
		jlong  thread_tag = 0;

		rawMonitorEnter(&lockTag);
		jboolean new_thread_tag = getTag(thread, &thread_tag);
		rawMonitorExit(&lockTag);

		rawMonitorEnter(&lockThreadData);
		struct thread_s* found = NULL;
		if (!new_thread_tag) {
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
			new_thread->new_tag = new_thread_tag;
			new_thread->tag    = thread_tag;
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
	return;
}
