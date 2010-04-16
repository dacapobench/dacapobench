#ifndef DACAPO_CALL_CHAIN_H
#define DACAPO_CALL_CHAIN_H

#include "dacapo.h"

/*
 * Log format
 *
 */

void call_chain_init();
void call_chain_capabilities(const jvmtiCapabilities* availableCapabilities, jvmtiCapabilities* capabilities);
void call_chain_callbacks(const jvmtiCapabilities* capabilities, jvmtiEventCallbacks* callbacks);
void call_chain_logon(JNIEnv* env);

void log_call_chain(JNIEnv *env, jclass klass, jobject thread);

#endif
