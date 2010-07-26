#ifndef DACAPO_AGENT_H
#define DACAPO_AGENT_H

#include "dacapo.h"

void JNICALL callbackClassPrepare(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jclass klass);

void setLogState(jboolean logState);

#endif

