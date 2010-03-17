#ifndef DACAPO_ALLOCATION_H
#define DACAPO_ALLOCATION_H

#include "dacapo.h"

void JNICALL callbackVMObjectAlloc(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                jobject object, jclass object_klass, jlong size);
void JNICALL callbackObjectFree(jvmtiEnv *jvmti, jlong tag);

#endif
