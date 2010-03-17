#include "dacapotag.h"

jrawMonitorID       lockTag;
jlong               objectNumber =  4;
jlong               internalTag  = 0;

_Bool dacapo_tag_init() {
    return JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "tag data", &(lockTag))==JNI_OK;
}

jboolean getTag(jobject object, jlong* tag) {
	jint res = JVMTI_FUNC_PTR(baseEnv,GetTag)(baseEnv,object,tag);
	if (*tag == 0) {
		*tag = --internalTag;
		JVMTI_FUNC_PTR(baseEnv,SetTag)(baseEnv,object,*tag);
		jlong tmp_tag = 0;
		JVMTI_FUNC_PTR(baseEnv,GetTag)(baseEnv,object,&tmp_tag);
		if (tmp_tag != *tag) {
		    fprintf(stderr, "unable to set tag %" FORMAT_JLONG "\n",*tag);
		    exit(1);
		}
		return !FALSE;
	} else
		return FALSE;
}

jlong setTag(jobject object, jlong size) {
	jlong tmp_objectNumber = objectNumber;
	JVMTI_FUNC_PTR(baseEnv,SetTag)(baseEnv, object, tmp_objectNumber);
	jlong tmp_tag = 0;
	JVMTI_FUNC_PTR(baseEnv,GetTag)(baseEnv,object,&tmp_tag);
	if (tmp_tag != tmp_objectNumber) {
	    fprintf(stderr, "unable to set tag %" FORMAT_JLONG "\n",tmp_objectNumber);
	    exit(1);
	}
	objectNumber += size;
	return tmp_objectNumber;
}
