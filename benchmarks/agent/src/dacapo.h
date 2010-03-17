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
#define FORMAT_PTR "lx"
#define PTR_CAST(a) ((long)(a))
#else
#define FORMAT_JLONG "lld"
#define FORMAT_PTR "lx"
#define PTR_CAST(a) ((long)(a))
#endif
#else
#define FORMAT_JLONG "lld"
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

#define LOG_CLASS(log,jni_table,jni_env,jvmti_env,klass) \
        {\
			char* signature = NULL;\
			char* generic = NULL;\
			\
			JVMTI_FUNC_PTR(baseEnv,GetClassSignature)(jvmti_env,klass,&signature,&generic);\
			fprintf(log, "{%s}", signature);\
			\
			if (signature!=NULL) JVMTI_FUNC_PTR(jvmti_env,Deallocate)(jvmti_env,(unsigned char*)signature);\
			if (generic!=NULL) JVMTI_FUNC_PTR(jvmti_env,Deallocate)(jvmti_env,(unsigned char*)generic);\
		}

#define LOG_OBJECT_CLASS(log,jni_table,jni_env,jvmti_env,object) LOG_CLASS(log,jni_tble,jni_env,jvmti_env,JVM_FUNC_PTR(jni_table,GetObjectClass)(jni_env,object))

extern JavaVM*             jvm;
extern jvmtiEnv*           baseEnv;
extern jboolean            jvmRunning;
extern jboolean            jvmStopped;


#endif
