#ifndef DACAPO_AGENT_H
#define DACAPO_AGENT_H

#include "dacapo.h"

/* Agent library externals to export. */

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved);
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm);

void JNICALL callbackClassPrepare(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jclass klass);

void setLogState(jboolean logState);

#endif

