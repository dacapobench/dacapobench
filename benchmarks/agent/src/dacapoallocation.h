#ifndef DACAPO_ALLOCATION_H
#define DACAPO_ALLOCATION_H

#include "dacapo.h"

/*
 * Log format
 *
 *   Object allocation:
 *     A:<object_tag>:<class_tag>[{<object_class>}]:<thread_tag>[{thread_class}]:<size>
 *
 *        object_tag
 *          When an object is allocated it is given a tag which is the value of the
 *          last object_tag + the size of the last object allocated.  Since a tag is
 *          a 64 bit number this will roughly equate to an allocation time since epoc.
 *          The first object allocation event assigns 4 as the object_tag, this is the
 *          notional start of the heap.
 *
 *        class_tag
 *          The class_tag is reported, or assigned if no tag has been associated with the class
 *          of the object being allocated an internal one is generated (internally generated
 *          tags are negative and do not represent an allocation time).
 *
 *        object_class
 *          This is the full class name of the object being allocated and is only reported
 *          if a class_tag had to be assigned (indiciated that this class had not been
 *          reported before).
 *
 *        thread_tag
 *          The thread_tag is reported, or assigned if no tag has been associated with the
 *          class of the thread that caused the allocation.
 *
 *        thread_class
 *          The class of the thread object is reported if a tag had to be assigned.
 *
 *        size
 *          The size, in bytes, of the object allocated.
 *
 *   Free:
 *     F:<object_tag>
 *
 *        object_tag
 *          The tag that was assigned to the object when it was allocated.  If this is
 *          zero then the allocation took place before the agent was running.
 */

void allocation_init();
void allocation_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities);
void allocation_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks);
void allocation_live(jvmtiEnv* jvmti, JNIEnv* env);
void allocation_logon(JNIEnv* env);
void allocation_class(jvmtiEnv *env, JNIEnv *jnienv, jthread thread, jclass klass);

void JNICALL callbackVMObjectAlloc(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                jobject object, jclass object_klass, jlong size);
void JNICALL callbackObjectFree(jvmtiEnv *jvmti, jlong tag);

#endif
