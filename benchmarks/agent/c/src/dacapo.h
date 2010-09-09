#ifndef DACAPO_H
#define DACAPO_H

/* Standard C functions used throughout. */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <stdarg.h>

#ifndef TRUE
#define TRUE (0==0)
#endif
#ifndef FALSE
#define FALSE (0!=0)
#endif

#ifdef __WORDSIZE
#if __WORDSIZE == 64
#define FORMAT_JLONG "ld"
#define FORMAT_JINT  "d"
#define FORMAT_PTR "lx"
#define PTR_CAST(a) ((long)(a))
#else
#define FORMAT_JLONG "lld"
#define FORMAT_JINT  "d"
#define FORMAT_PTR "lx"
#define PTR_CAST(a) ((long)(a))
#endif
#else
#define FORMAT_JLONG "lld"
#define FORMAT_JINT  "d"
#define FORMAT_PTR "llx"
#define PTR_CAST(a) ((long long)(a))
#endif

/* Macro to get JVM function pointer. */
#define JVM_FUNC_PTR(jni,f) (*((*(jni)).f))

/* Macro to get JVMTI function pointer. */
#define JVMTI_FUNC_PTR(env,f) (*((*(env))->f))

/* General JVM/Java functions, types and macros. */

#include <sys/types.h>
#include "jni.h"
#include "jvmti.h"

#define NONE "<none>"

#define DEFINE_CALLBACK(cb,c,s) \
    (cb)->c = callback##c; \
    { \
      int retVal = JVMTI_FUNC_PTR(baseEnv,SetEventNotificationMode)(baseEnv, JVMTI_ENABLE, s, (jthread)NULL); \
      if (retVal != JNI_OK) { \
        fprintf(stderr, "unable to register event callback %s\n", #c); \
        reportJVMTIError(stderr, retVal, NULL); \
        /* exit(1); */ \
      } \
    }

extern JavaVM*             jvm;
extern jvmtiEnv*           baseEnv;
extern jboolean            jvmRunning;
extern jboolean            jvmStopped;

jniNativeInterface* getJNIFunctionTable(char* file, int line);

#define JNIFunctionTable() getJNIFunctionTable(__FILE__,__LINE__)

void reportJVMTIError(FILE* fh, jvmtiError errorNumber, const char *str);

#endif
