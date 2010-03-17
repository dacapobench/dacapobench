#include "dacapoexception.h"

#include "dacapotag.h"
#include "dacapolog.h"
#include "dacapolock.h"

void JNICALL callbackException(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jlocation location, jobject exception, jmethodID catch_method, jlocation catch_location)
{
	if (logState ) {
		jlong thread_tag = 0;
		jlong exception_tag = 0;

		enterCriticalSection(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean exception_has_new_tag = getTag(exception, &exception_tag);
		exitCriticalSection(&lockTag);

		enterCriticalSection(&lockLog);
		if (thread_has_new_tag || exception_has_new_tag) {
			jniNativeInterface* jni_table;
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}

			fprintf(logFile,"X:%" FORMAT_JLONG,thread_tag);
			if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,thread);
			fprintf(logFile,":%" FORMAT_JLONG,exception_tag);
			if (exception_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,exception);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			fprintf(logFile,":%s",info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			fprintf(logFile,"X:%" FORMAT_JLONG ":%" FORMAT_JLONG "",thread_tag,exception_tag);
		}

		char* name = NULL;
		char* signature  = NULL;
		char* generic = NULL;
		jclass klass = NULL;
		char* class_signature = NULL;
		char* class_generic = NULL;

		jint res = JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,method,&name,&signature,&generic);

		if (res!=JNI_OK)
			fprintf(logFile,":<error>");
		else {
			if (JVMTI_FUNC_PTR(baseEnv,GetMethodDeclaringClass)(baseEnv,method,&klass)==JNI_OK &&
				JVMTI_FUNC_PTR(baseEnv,GetClassSignature)(baseEnv,klass,&class_signature,&class_generic)==JNI_OK)
				fprintf(logFile,":%" FORMAT_PTR "{%s.%s%s}",PTR_CAST(catch_method),class_signature,name,signature);
			else
				fprintf(logFile,":%" FORMAT_PTR "{%s.%s%s}",PTR_CAST(catch_method),"<class_name>",name,signature);
		}

		if (class_signature!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)class_signature); class_signature = NULL; }
		if (class_generic!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)class_generic); class_generic = NULL; }
		if (name!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name); name = NULL; }
		if (signature!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature); signature = NULL; }
		if (generic!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic); generic = NULL; }

		res = JVMTI_FUNC_PTR(baseEnv,GetMethodDeclaringClass)(baseEnv,method,&klass);
		if (res==JNI_OK) {
			if (catch_method==NULL) {
				fprintf(logFile,":NULL");
			} else {
			    res = JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,catch_method,&name,&signature,&generic);

				if (res!=JNI_OK)
					fprintf(logFile,":<error>");
				else {
					if (JVMTI_FUNC_PTR(baseEnv,GetMethodDeclaringClass)(baseEnv,catch_method,&klass)==JNI_OK &&
						JVMTI_FUNC_PTR(baseEnv,GetClassSignature)(baseEnv,klass,&class_signature,&class_generic)==JNI_OK)
						fprintf(logFile,":%" FORMAT_PTR "{%s.%s%s}",PTR_CAST(catch_method),class_signature,name,signature);
					else
						fprintf(logFile,":%" FORMAT_PTR "{%s.%s%s}",PTR_CAST(catch_method),"<class_name>",name,signature);

				}
			}
		} else
			fprintf(logFile,":<error>");
		fprintf(logFile,"\n");

		if (class_signature!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)class_signature); class_signature = NULL; }
		if (class_generic!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)class_generic); class_generic = NULL; }
		if (name!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name); name = NULL; }
		if (signature!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature); signature = NULL; }
		if (generic!=NULL) { JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic); generic = NULL; }

		exitCriticalSection(&lockLog);
	}
}

void JNICALL callbackExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jlocation location, jobject exception)
{
	if (logState ) {
		jlong thread_tag = 0;
		jlong exception_tag = 0;

		enterCriticalSection(&lockTag);
		jboolean thread_has_new_tag = getTag(thread, &thread_tag);
		jboolean exception_has_new_tag = getTag(exception, &exception_tag);
		exitCriticalSection(&lockTag);

		enterCriticalSection(&lockLog);
		if (thread_has_new_tag || exception_has_new_tag) {
			jniNativeInterface* jni_table;
			if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
				fprintf(stderr, "failed to get JNI function table\n");
				exit(1);
			}

			fprintf(logFile,"XC:%" FORMAT_JLONG,thread_tag);
			if (thread_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,thread);
			fprintf(logFile,":%" FORMAT_JLONG,exception_tag);
			if (exception_has_new_tag) LOG_OBJECT_CLASS(logFile,jni_table,jni_env,baseEnv,exception);

			// get class and get thread name.
			jvmtiThreadInfo info;
			JVMTI_FUNC_PTR(baseEnv,GetThreadInfo)(baseEnv, thread, &info);
			fprintf(logFile,":%s",info.name);
			if (info.name!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)info.name);
		} else {
			fprintf(logFile,"XC:%" FORMAT_JLONG ":%" FORMAT_JLONG ":",thread_tag,exception_tag);
		}
		char* name_ptr = NULL;
		char* signature_ptr  = NULL;
		char* generic_ptr = NULL;

		jint res = JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,method,&name_ptr,&signature_ptr,&generic_ptr);

		if (res!=JNI_OK)
			fprintf(logFile,"<error>\n");
		else {
			fprintf(logFile,":%" FORMAT_PTR ":%s.%s%s\n",PTR_CAST(method),"<class_name>",name_ptr,signature_ptr);

			if (name_ptr!=NULL)      JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name_ptr);
			if (signature_ptr!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature_ptr);
			if (generic_ptr!=NULL)   JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic_ptr);
		}

		exitCriticalSection(&lockLog);
	}
}



