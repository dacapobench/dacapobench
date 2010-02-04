/* Agent for use with DaCapo benchmark suite.
 * Currently this is just a skeleton.
 */

#include "stdlib.h"

#include "dacapoagent.h"

#include "jni.h"
#include "jvmti.h"

#ifndef FALSE
#define FALSE 0
#endif
#ifndef TRUE
#define TRUE 0
#endif

/* Macro to get JVM function pointer. */
#define JVM_FUNC_PTR(env,f) (*((*(env))->f))

/* Macro to get JVMTI function pointer. */
#define JVMTI_FUNC_PTR(env,f) (*((*(env))->f))

/* C macros to create strings from tokens */
#define _STRING(s) #s
#define STRING(s) _STRING(s)

/* C macros for version numbers */
#define MAJOR_VERSION(v) (int)(( v & JVMTI_VERSION_MASK_MAJOR ) >> JVMTI_VERSION_SHIFT_MAJOR)
#define MINOR_VERSION(v) (int)(( v & JVMTI_VERSION_MASK_MINOR ) >> JVMTI_VERSION_SHIFT_MINOR)
#define MICRO_VERSION(v) (int)(( v & JVMTI_VERSION_MASK_MICRO ) >> JVMTI_VERSION_SHIFT_MICRO)

/* ------------------------------------------------------------------- */
/* Cached data */
JavaVM*             jvm = NULL;
jvmtiEnv*           env = NULL;
jint                jvmtiVersion = 0;
jvmtiCapabilities   availableCapabilities;
jvmtiCapabilities   capabilities;
jvmtiEventCallbacks callbacks;
jrawMonitorID       lock;
jboolean            jvmRunning = FALSE;
jboolean            jvmStopped = FALSE;
char                agentOptions[128];

/* ------------------------------------------------------------------- */
extern void processCapabilities();
extern void processOptions(char*);
static void JNICALL callbackClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv* env,
                jclass class_being_redefined, jobject loader,
                const char* name, jobject protection_domain,
                jint class_data_len, const unsigned char* class_data,
                jint* new_class_data_len,
                unsigned char** new_class_data);
static void JNICALL callbackVMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread);
static void JNICALL callbackVMDeath(jvmtiEnv *jvmti, JNIEnv *env);

/* Agent_OnLoad: This is called immediately after the shared library is 
 *   loaded. This is the first code executed.
 */
JNIEXPORT jint JNICALL 
Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
    jint res;
    jint jvmtiCompileTimeMajorVersion;
    jint jvmtiCompileTimeMinorVersion;
    jint jvmtiCompileTimeMicroVersion;

    (void)memset(&capabilities,0, sizeof(capabilities));
    (void)memset(&availableCapabilities,0, sizeof(availableCapabilities));
    (void)memset(&callbacks,0, sizeof(callbacks));
    (void)memset(&lock,0, sizeof(lock));

    strncpy(agentOptions,options,sizeof(agentOptions)-1);
    agentOptions[sizeof(agentOptions)-1] = '\0';

	jvm = vm;
    res = JVM_FUNC_PTR(jvm,GetEnv)(jvm, (void **)&env, JVMTI_VERSION_1);
    if (res != JNI_OK) {
    	fprintf(stderr, "Unable to access JVMTI Version 1 (0x%x),"
                " is your JDK a 5.0 or newer version?"
                " JNIEnv's GetEnv() returned %d\n", JVMTI_VERSION_1, res);

        exit(1); /* Kill entire process, no core dump */
    }

    res = JVMTI_FUNC_PTR(env,GetVersionNumber)(env, &jvmtiVersion);
    if (res != JNI_OK) {
    	fprintf(stderr, "Failed to retrive JVMTI runtime version number\n");
    	exit(1);
    }

    /* Insure that compile time and runtime versions are compatible */
    if (MAJOR_VERSION(JVMTI_VERSION)!=MAJOR_VERSION(jvmtiVersion) ||
        MINOR_VERSION(JVMTI_VERSION)>MINOR_VERSION(jvmtiVersion)) {
		fprintf(stderr, "compile (v%d.%d.%d) and runtime (v%d.%d.%d) JVMTI_VERSIONs are not compatible\n",
				MAJOR_VERSION(JVMTI_VERSION),
				MINOR_VERSION(JVMTI_VERSION),
				MICRO_VERSION(JVMTI_VERSION),
				MAJOR_VERSION(jvmtiVersion),
				MINOR_VERSION(jvmtiVersion),
				MICRO_VERSION(jvmtiVersion));

		exit(1);
    }

    /* Create a raw monitor in the agent for critical sections. */
    res = JVMTI_FUNC_PTR(env,CreateRawMonitor)(env, "agent data", &(lock));
    if (res != JNI_OK) {
    	fprintf(stderr, "failed to create raw monitor\n");
    	exit(1);
    }

    /* get capabilities */
    processCapabilities();

    /* process options */
    processOptions(options);

    return JNI_OK;
}

/* Agent_OnUnload: This is called immediately before the shared library is 
 *   unloaded. This is the last code executed.
 */
JNIEXPORT void JNICALL 
Agent_OnUnload(JavaVM *vm)
{
}

/* ------------------------------------------------------------------- */
extern void reportCapabilities();

void processCapabilities() {
	jint res;

	res = JVMTI_FUNC_PTR(env,GetPotentialCapabilities)(env,&availableCapabilities);

	if (res != JNI_OK) {
		fprintf(stderr, "unable to obtain potential capabilities\n");
		exit(1);
	}

#ifdef DEBUG
	reportCapabilities(&availableCapabilities);
#endif // DEBUG
}

#define DEFINE_CALLBACK(c,s) \
	callbacks.c = callback##c; \
	if (JVMTI_FUNC_PTR(env,SetEventNotificationMode)(env, JVMTI_ENABLE, s, (jthread)NULL) != JNI_OK) { \
	  fprintf(stderr, "unable to register event callback %s\n", #c); \
	  exit(1); \
	}

void defineCallbacks() {
	jint res;

	DEFINE_CALLBACK(VMInit,JVMTI_EVENT_VM_INIT);
	DEFINE_CALLBACK(VMDeath,JVMTI_EVENT_VM_DEATH);
	DEFINE_CALLBACK(ClassFileLoadHook,JVMTI_EVENT_CLASS_FILE_LOAD_HOOK);

    res = JVMTI_FUNC_PTR(env,SetEventCallbacks)(env, &callbacks, (jint)sizeof(callbacks));

    if (res != JNI_OK) {
    	fprintf(stderr, "unable to set event call backs\n");
    	exit(1);
    }
}

void processOptions(char* options) {
	jint res;

#ifdef DEBUG
    if (options != NULL) {
    	fprintf(stderr,"options=\"%s\"\n",options);
    } else {
    	fprintf(stderr,"options=NULL\n");
    }
#endif /* DEBUG */

    defineCallbacks();
}

char* intToboolean(int b) {
	return (b)?"true":"false";
}

void reportCapabilities(jvmtiCapabilities* capabilities) {
	fprintf(stderr,"capabilities->can_tag_objects=%s\n",intToboolean(capabilities->can_tag_objects));
	fprintf(stderr,"capabilities->can_generate_field_modification_events=%s\n",intToboolean(capabilities->can_generate_field_modification_events));
	fprintf(stderr,"capabilities->can_generate_field_access_events=%s\n",intToboolean(capabilities->can_generate_field_access_events));
	fprintf(stderr,"capabilities->can_get_bytecodes=%s\n",intToboolean(capabilities->can_get_bytecodes));
	fprintf(stderr,"capabilities->can_get_synthetic_attribute=%s\n",intToboolean(capabilities->can_get_synthetic_attribute));
	fprintf(stderr,"capabilities->can_get_owned_monitor_info=%s\n",intToboolean(capabilities->can_get_owned_monitor_info));
	fprintf(stderr,"capabilities->can_get_current_contended_monitor=%s\n",intToboolean(capabilities->can_get_current_contended_monitor));
	fprintf(stderr,"capabilities->can_get_monitor_info=%s\n",intToboolean(capabilities->can_get_monitor_info));
	fprintf(stderr,"capabilities->can_pop_frame=%s\n",intToboolean(capabilities->can_pop_frame));
	fprintf(stderr,"capabilities->can_redefine_classes=%s\n",intToboolean(capabilities->can_redefine_classes));
	fprintf(stderr,"capabilities->can_signal_thread=%s\n",intToboolean(capabilities->can_signal_thread));
	fprintf(stderr,"capabilities->can_get_source_file_name=%s\n",intToboolean(capabilities->can_get_source_file_name));
	fprintf(stderr,"capabilities->can_get_line_numbers=%s\n",intToboolean(capabilities->can_get_line_numbers));
	fprintf(stderr,"capabilities->can_get_source_debug_extension=%s\n",intToboolean(capabilities->can_get_source_debug_extension));
	fprintf(stderr,"capabilities->can_access_local_variables=%s\n",intToboolean(capabilities->can_access_local_variables));
	fprintf(stderr,"capabilities->can_maintain_original_method_order=%s\n",intToboolean(capabilities->can_maintain_original_method_order));
	fprintf(stderr,"capabilities->can_generate_single_step_events=%s\n",intToboolean(capabilities->can_generate_single_step_events));
	fprintf(stderr,"capabilities->can_generate_exception_events=%s\n",intToboolean(capabilities->can_generate_exception_events));
	fprintf(stderr,"capabilities->can_generate_frame_pop_events=%s\n",intToboolean(capabilities->can_generate_frame_pop_events));
	fprintf(stderr,"capabilities->can_generate_breakpoint_events=%s\n",intToboolean(capabilities->can_generate_breakpoint_events));
	fprintf(stderr,"capabilities->can_suspend=%s\n",intToboolean(capabilities->can_suspend));
	fprintf(stderr,"capabilities->can_redefine_any_class=%s\n",intToboolean(capabilities->can_redefine_any_class));
	fprintf(stderr,"capabilities->can_get_current_thread_cpu_time=%s\n",intToboolean(capabilities->can_get_current_thread_cpu_time));
	fprintf(stderr,"capabilities->can_get_thread_cpu_time=%s\n",intToboolean(capabilities->can_get_thread_cpu_time));
	fprintf(stderr,"capabilities->can_generate_method_entry_events=%s\n",intToboolean(capabilities->can_generate_method_entry_events));
	fprintf(stderr,"capabilities->can_generate_method_exit_events=%s\n",intToboolean(capabilities->can_generate_method_exit_events));
	fprintf(stderr,"capabilities->can_generate_all_class_hook_events=%s\n",intToboolean(capabilities->can_generate_all_class_hook_events));
	fprintf(stderr,"capabilities->can_generate_compiled_method_load_events=%s\n",intToboolean(capabilities->can_generate_compiled_method_load_events));
	fprintf(stderr,"capabilities->can_generate_monitor_events=%s\n",intToboolean(capabilities->can_generate_monitor_events));
	fprintf(stderr,"capabilities->can_generate_vm_object_alloc_events=%s\n",intToboolean(capabilities->can_generate_vm_object_alloc_events));
	fprintf(stderr,"capabilities->can_generate_native_method_bind_events=%s\n",intToboolean(capabilities->can_generate_native_method_bind_events));
	fprintf(stderr,"capabilities->can_generate_garbage_collection_events=%s\n",intToboolean(capabilities->can_generate_garbage_collection_events));
	fprintf(stderr,"capabilities->can_generate_object_free_events=%s\n",intToboolean(capabilities->can_generate_object_free_events));
}

/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
static void
enterCriticalSection(jvmtiEnv *jvmti)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(jvmti,RawMonitorEnter)(jvmti, lock);
    if (error != JNI_OK) {
    	fprintf(stderr, "cannot enter with raw monitor\n");
    }
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
static void
exitCriticalSection(jvmtiEnv *jvmti)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(jvmti,RawMonitorExit)(jvmti, lock);
    if (error != JNI_OK) {
    	fprintf(stderr, "cannot exit with raw monitor\n");
    }
}

void readClassData(char* infile, unsigned char** new_class_data, jint* new_class_data_len) {
	new_class_data     = 0;
	new_class_data_len = 0;
}

void writeClassData(char* outfile, const unsigned char* class_data, jint class_data_len) {
	return;
}

void generateFileName(char* file_name, int max_file_name_len) {
	strcpy(file_name,"file_name");
}

/* packages to be excluded from transformation */
#define ASM_PACKAGE_NAME     "org/objectweb/asm/"
#define DACAPO_PACKAGE_NAME  "org/dacapo/transform/"

static void JNICALL
callbackClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv* env,
                jclass class_being_redefined, jobject loader,
                const char* name, jobject protection_domain,
                jint class_data_len, const unsigned char* class_data,
                jint* new_class_data_len,
                unsigned char** new_class_data)
{
	if (! jvmStopped) {
		if (strncmp(DACAPO_PACKAGE_NAME,name,strlen(DACAPO_PACKAGE_NAME))!=0) {
			enterCriticalSection(jvmti);

			*new_class_data     = NULL;
			*new_class_data_len = 0;

			fprintf(stderr, "callbackClassFileLoadHook(...):\"%s\"\n",(name==NULL)?"NULL":name);
			char command[1024];
			char outfile[256];
			char infile [256];

			generateFileName(outfile, sizeof(outfile));
			generateFileName(infile, sizeof(infile));

			writeClassData(outfile,class_data,class_data_len);

			sprintf(command, "java -classpath dist/agent.jar:asm-3.2/lib/asm-3.2.jar org.dacapo.transform.DumpClass '%s' \"'%s'\"",(name!=NULL?name:"NULL"),agentOptions);
			if (system(command) == 0) {
				readClassData(infile,new_class_data,new_class_data_len);
			}

			exitCriticalSection(jvmti);
		}
	}
}

/* JVMTI_EVENT_VM_INIT */
static void JNICALL
callbackVMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread)
{
	jvmRunning = TRUE;
}

/* JVMTI_EVENT_VM_DEATH */
static void JNICALL
callbackVMDeath(jvmtiEnv *jvmti, JNIEnv *env)
{
	jvmStopped = TRUE;
}


