/* Agent for use with DaCapo benchmark suite.
 * Currently this is just a skeleton.
 */

#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/wait.h>
#include <unistd.h>

#include <pthread.h>

#include "jni.h"
#include "jvmti.h"

/*
#include "dacapoagent.h"

*/

#include "dacapo.h"
#include "dacapoagent.h"
#include "dacapooptions.h"
#include "dacapoexception.h"
#include "dacapolog.h"
#include "dacapotag.h"
#include "dacapomonitor.h"
#include "dacapothread.h"
#include "dacapoallocation.h"
#include "dacapolock.h"
#include "dacapomethod.h"
#include "dacapocallchain.h"

/* C macros to create strings from tokens */
#define _STRING(s) #s
#define STRING(s) _STRING(s)

/* C macros for version numbers */
#define MAJOR_VERSION(v) (int)(((v) & JVMTI_VERSION_MASK_MAJOR ) >> JVMTI_VERSION_SHIFT_MAJOR)
#define MINOR_VERSION(v) (int)(((v) & JVMTI_VERSION_MASK_MINOR ) >> JVMTI_VERSION_SHIFT_MINOR)
#define MICRO_VERSION(v) (int)(((v) & JVMTI_VERSION_MASK_MICRO ) >> JVMTI_VERSION_SHIFT_MICRO)

#define FILE_SEPARATOR_CHAR '/'

#define STORE_CLASS_FILE_BASE "DACAPO_STORE"
#define DEFAULT_STORE_CLASS_FILE_BASE "store"

#define LOG_FILE_NAME "DACAPO_JVMTI_LOG_FILE"
#define DEFAULT_LOG_FILE_NAME "dacapo-jvmti.log"

#define JAR_LIST "agent.jar:asm-3.3.jar:asm-commons-3.3.jar"
#define JAR_BASE "dist"

struct class_list_s {
    struct class_list_s* next;
    jclass               klass;
} *class_list_head = NULL, *class_list_tail = NULL;

/* ------------------------------------------------------------------- */
/* Cached data */
JavaVM*             jvm = NULL;
jvmtiEnv*           baseEnv = NULL;
jint                jvmtiVersion = 0;
jvmtiCapabilities   availableCapabilities;
jvmtiCapabilities   capabilities;
jvmtiEventCallbacks callbacks;

MonitorLockType     agentLock;
MonitorLockType     lockClass;

jboolean            jvmRunning = FALSE;
jboolean            jvmStopped = FALSE;
char*               agentOptions = NULL; // [12800];
char*               jarList = JAR_LIST;
char*               jarBase = JAR_BASE;
char*               jarSet = NULL;
jboolean            requiresTransform = FALSE;
jboolean            storeClassFiles = FALSE;
jboolean            instrumentClasses = FALSE;
jboolean            methodCalls = FALSE;
jboolean            loadClasses = FALSE;
jboolean            storeClassFilesTXed = FALSE;
char*               storeClassFileBase = NULL; // [8192];
jboolean            breakOnLoad = FALSE;

char*               breakOnLoadClass = NULL;

struct exclude_list_s {
	struct exclude_list_s* next;
	char*                  name;
	int                    length;
} *exclude_package_list_head = NULL, *exclude_package_list_tail = NULL, *exclude_class_list_head = NULL, *exclude_class_list_tail = NULL;

/* ------------------------------------------------------------------- */
static void processCapabilities();
static void processOptions();
static void agent_thread_main(void* arg);

static jniNativeInterface* jni_table = NULL;

jniNativeInterface* getJNIFunctionTable(char* file, int line) {
	if (jni_table==NULL) {
		if (JVMTI_FUNC_PTR(baseEnv,GetJNIFunctionTable)(baseEnv,&jni_table) != JNI_OK) {
			fprintf(stderr, "failed to get JNI function table: %s:%d\n",file,line);
			exit(1);
		}
	}
	return jni_table;
}

static void JNICALL callbackClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv* env,
                jclass class_being_redefined, jobject loader,
                const char* name, jobject protection_domain,
                jint class_data_len, const unsigned char* class_data,
                jint* new_class_data_len,
                unsigned char** new_class_data);
static void JNICALL callbackVMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread);
static void JNICALL callbackVMDeath(jvmtiEnv *jvmti, JNIEnv *env);

static void processClassPrepare(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jclass klass);

static void makePath(const char* name);
static void reportCapabilities(FILE* fh, jvmtiCapabilities* capabilities);

void reportJVMTIError(FILE* fh, jvmtiError errorNumber, const char *str)
{
    char       *errorString;
    
    errorString = NULL;
    (void)(*baseEnv)->GetErrorName(baseEnv, errorNumber, &errorString);
    
    if (str == NULL) str  = "";
    fprintf(fh,"JVMTI Error (%d:\"%s\"): %s\n", errorNumber, errorString, str);
}

#define MAX_EXCLUDE_LIST_LENGTH 10240

static void agent_exclude_list()
{
	char* temp = NULL;
	
	if (isSelected(OPT_EXCLUDE_CLASSES,&temp)) {
		int start=0, next=0;
		
		while (temp[next] != '\0') {
			while (temp[next] != '\0' && temp[next] != ';') next++;
			struct exclude_list_s *item = (struct exclude_list_s *)malloc(sizeof(struct exclude_list_s));
			
			item->next = NULL;
			item->name = (char*)malloc(sizeof(char)*(next-start+1));
			item->length = next-start;
			strncpy(item->name,temp+start,item->length);
			item->name[item->length] = '\0';

			if (exclude_class_list_tail==NULL)
				exclude_class_list_head = exclude_class_list_tail = item;
			else {
				exclude_class_list_tail->next = item;
				exclude_class_list_tail       = item;
			}
			
			if (temp[next] != '\0')
				start = ++next;
		}
	}
	
	if (temp != NULL) {
		free(temp);
		temp = NULL;
	}
	
	if (isSelected(OPT_EXCLUDE_PACKAGES,&temp)) {
		int start=0, next=0;
		
		while (temp[next] != '\0') {
			while (temp[next] != '\0' && temp[next] != ';') next++;
			struct exclude_list_s *item = (struct exclude_list_s *)malloc(sizeof(struct exclude_list_s));
			
			item->next = NULL;
			item->name = (char*)malloc(sizeof(char)*(next-start+1));
			item->length = next-start;
			strncpy(item->name,temp+start,item->length);
			item->name[item->length] = '\0';

			if (exclude_package_list_tail == NULL)
				exclude_package_list_head = exclude_package_list_tail = item;
			else {
				exclude_package_list_tail->next = item;
				exclude_package_list_tail       = item;
			}
			
			if (temp[next] != '\0')
				start = ++next;
		}
	}
	
	if (temp != NULL) {
		free(temp);
		temp = NULL;
	}
}

static void agent_ext_java()
{
	char* temp = NULL;
	const char* newJarBase = jarBase;
	
	if (isSelected(OPT_BASE,&temp)) 
		newJarBase = (temp != NULL)?temp:jarBase;
		
	int newJarBaseLength = strlen(newJarBase);
	int jarCount      = 1;		
	int i             = 0;
	int jarListLength = strlen(jarList);
	
	for(i = 0; i < jarListLength; i++) {
		if (jarList[i] == ':') jarCount++;
	}
	
	int slash = newJarBase[newJarBaseLength-1]=='/'?0:1;
	int jarSetSize = (newJarBaseLength + slash) * jarCount + jarListLength; 
		
	jarSet = (char*)malloc(sizeof(char)*(jarSetSize + 1));
	
	char* jarSetPtr = jarSet;
	int   j = -1;

	for(i = 0; i < jarCount; i++) {
		strcpy(jarSetPtr,newJarBase);
		jarSetPtr += newJarBaseLength;
		if (slash==1) *(jarSetPtr++) = '/';
		int k = j+1;
		while (jarList[k]!=':' && jarList[k]!='\0') {
			*(jarSetPtr++) = jarList[k++];
		}
		if ((i + 1) < jarCount) *(jarSetPtr++) = ':';
		j = k;
	}
	jarSet[jarSetSize] = '\0';
	
	if (temp != NULL) {
		free(temp);
		temp = NULL;
	}
}


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
    (void)memset(&lockLog,0, sizeof(lockLog));
    (void)memset(&lockTag,0, sizeof(lockTag));
    (void)memset(&lockClass,0, sizeof(lockClass));

    if (options!=NULL && strlen(options)!=0) {
    	agentOptions = (char*)malloc(strlen(options)+1);
        strcpy(agentOptions,options);
    }

    jvm = vm;
    res = JVMTI_FUNC_PTR(jvm,GetEnv)(jvm, (void **)&baseEnv, JVMTI_VERSION_1);
    if (res != JNI_OK) {
        fprintf(stderr, "Unable to access JVMTI Version 1 (0x%x),"
                " is your JDK a 5.0 or newer version?"
                " JNIEnv's GetEnv() returned %d\n", JVMTI_VERSION_1, res);

        exit(1); /* Kill entire process, no core dump */
    }

    res = JVMTI_FUNC_PTR(baseEnv,GetVersionNumber)(baseEnv, &jvmtiVersion);
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

    /* need to examine options and determine if class transforms are required,
       if they are not then we should avoid this as writing and reading is 
       expensive.
    */
    makeOptionListFromFile(options);

    /* Create a raw monitor in the agent for critical sections. */
    if (!dacapo_log_init()) {
        fprintf(stderr, "failed to intialize log\n");
        exit(1);
    }
    if(!dacapo_tag_init()) {
        fprintf(stderr, "failed to initialize tag\n");
        exit(1);
    }
    
    if (!rawMonitorInit(baseEnv, "agent data", &lockClass)) {
    	/* JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "agent data", &(lockClass))!=JNI_OK) { */
        fprintf(stderr, "failed to create raw monitor\n");
        exit(1);
    }

	if (!rawMonitorInit(baseEnv,"agent lock",&agentLock)) {
		/* JVMTI_FUNC_PTR(baseEnv,CreateRawMonitor)(baseEnv, "agent lock", &(agentLock)) != JNI_OK) { */
		return FALSE;
	}

	agent_ext_java();

	agent_exclude_list();
	allocation_init();
	exception_init();
	method_init();
	monitor_init();
	thread_init();
	call_chain_init();

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
	dacapo_log_stop();
}

/* ------------------------------------------------------------------- */

static void processCapabilities() {
    jint res;

    res = JVMTI_FUNC_PTR(baseEnv,GetPotentialCapabilities)(baseEnv,&availableCapabilities);

    if (res != JNI_OK) {
        fprintf(stderr, "unable to obtain potential capabilities\n");
        exit(1);
    }

#ifdef DEBUG
    reportCapabilities(stderr,&availableCapabilities);
#endif // DEBUG
}

static void defineCallbacks() {
    jint res;
    jvmtiCapabilities tmp;
    
    (void)memset(&tmp,0, sizeof(tmp));
    
	method_capabilities(&availableCapabilities, &capabilities);
	monitor_capabilities(&availableCapabilities, &capabilities);
	allocation_capabilities(&availableCapabilities, &capabilities);
    exception_capabilities(&availableCapabilities, &capabilities);
    call_chain_capabilities(&availableCapabilities, &capabilities);
    thread_capabilities(&availableCapabilities, &capabilities);

	capabilities.can_tag_objects                     = availableCapabilities.can_tag_objects;
    
    res = JVMTI_FUNC_PTR(baseEnv,AddCapabilities)(baseEnv, &capabilities);
    
	DEFINE_CALLBACK(&callbacks,VMInit,JVMTI_EVENT_VM_INIT);
    DEFINE_CALLBACK(&callbacks,VMDeath,JVMTI_EVENT_VM_DEATH);
    DEFINE_CALLBACK(&callbacks,ClassFileLoadHook,JVMTI_EVENT_CLASS_FILE_LOAD_HOOK);
	DEFINE_CALLBACK(&callbacks,ClassPrepare,JVMTI_EVENT_CLASS_PREPARE);
    
	monitor_callbacks(&capabilities, &callbacks);
	allocation_callbacks(&capabilities, &callbacks);
	exception_callbacks(&capabilities, &callbacks);
	thread_callbacks(&capabilities, &callbacks);
	method_callbacks(&capabilities, &callbacks);
	call_chain_callbacks(&capabilities, &callbacks);
	
    res = JVMTI_FUNC_PTR(baseEnv,SetEventCallbacks)(baseEnv, &callbacks, (jint)sizeof(callbacks));

    if (res != JNI_OK) {
        fprintf(stderr, "unable to set event call backs\n");
        exit(1);
    }
}

static void processOptions() {
    jint res;

	storeClassFiles = isSelected(OPT_STORE_CLASS_FILE,NULL);
	storeClassFilesTXed = storeClassFiles && hasArgument(OPT_STORE_CLASS_FILE);
	
	loadClasses = isSelected(OPT_LOAD_CLASSES,NULL);
	methodCalls = isSelected(OPT_METHOD_EVENTS,NULL);
	breakOnLoad = isSelected(OPT_BREAK,&breakOnLoadClass);
	
	instrumentClasses =
		isSelected(OPT_CLINIT,NULL) ||
		isSelected(OPT_LOG_START,NULL) ||
		isSelected(OPT_BREAK,NULL) ||
		isSelected(OPT_MONITOR,NULL) ||
		isSelected(OPT_METHOD_INSTR,NULL) ||
		isSelected(OPT_ALLOCATE,NULL);
	
	char* storeDir = NULL;
	if (isSelected(OPT_STORE_DIRECTORY,&storeDir)) {
		storeClassFileBase = storeDir;
	} else {
		int agentOptionsLength = strlen(agentOptions);
		int slashLength = (agentOptionsLength<=0 || agentOptions[agentOptionsLength-1]=='/')?0:1;
		storeClassFileBase = (char*)malloc(sizeof(char)*(agentOptionsLength+slashLength+strlen(DEFAULT_STORE_CLASS_FILE_BASE)+1));
		strcpy(storeClassFileBase,agentOptions);
		if (slashLength!=0) storeClassFileBase[agentOptionsLength++] = FILE_SEPARATOR_CHAR;
		strcpy(storeClassFileBase+agentOptionsLength,DEFAULT_STORE_CLASS_FILE_BASE);
	}
	
    defineCallbacks();
}

static char* intToboolean(int b) {
    return (b)?"true":"false";
}

static void reportCapabilities(FILE* fh, jvmtiCapabilities* capabilities) {
	jvmtiCapabilities tmp;
    if (capabilities == NULL) {
	    jint res;
	
	    res = JVMTI_FUNC_PTR(baseEnv,GetPotentialCapabilities)(baseEnv,&tmp);
	
	    if (res != JNI_OK) {
	        fprintf(stderr, "unable to obtain potential capabilities\n");
	        exit(1);
	    }
	    
	    capabilities = &tmp;
	}

    fprintf(fh,"I:capabilities->can_tag_objects=%s\n",intToboolean(capabilities->can_tag_objects));
    fprintf(fh,"I:capabilities->can_generate_field_modification_events=%s\n",intToboolean(capabilities->can_generate_field_modification_events));
    fprintf(fh,"I:capabilities->can_generate_field_access_events=%s\n",intToboolean(capabilities->can_generate_field_access_events));
    fprintf(fh,"I:capabilities->can_get_bytecodes=%s\n",intToboolean(capabilities->can_get_bytecodes));
    fprintf(fh,"I:capabilities->can_get_synthetic_attribute=%s\n",intToboolean(capabilities->can_get_synthetic_attribute));
    fprintf(fh,"I:capabilities->can_get_owned_monitor_info=%s\n",intToboolean(capabilities->can_get_owned_monitor_info));
    fprintf(fh,"I:capabilities->can_get_current_contended_monitor=%s\n",intToboolean(capabilities->can_get_current_contended_monitor));
    fprintf(fh,"I:capabilities->can_get_monitor_info=%s\n",intToboolean(capabilities->can_get_monitor_info));
    fprintf(fh,"I:capabilities->can_pop_frame=%s\n",intToboolean(capabilities->can_pop_frame));
    fprintf(fh,"I:capabilities->can_redefine_classes=%s\n",intToboolean(capabilities->can_redefine_classes));
    fprintf(fh,"I:capabilities->can_signal_thread=%s\n",intToboolean(capabilities->can_signal_thread));
    fprintf(fh,"I:capabilities->can_get_source_file_name=%s\n",intToboolean(capabilities->can_get_source_file_name));
    fprintf(fh,"I:capabilities->can_get_line_numbers=%s\n",intToboolean(capabilities->can_get_line_numbers));
    fprintf(fh,"I:capabilities->can_get_source_debug_extension=%s\n",intToboolean(capabilities->can_get_source_debug_extension));
    fprintf(fh,"I:capabilities->can_access_local_variables=%s\n",intToboolean(capabilities->can_access_local_variables));
    fprintf(fh,"I:capabilities->can_maintain_original_method_order=%s\n",intToboolean(capabilities->can_maintain_original_method_order));
    fprintf(fh,"I:capabilities->can_generate_single_step_events=%s\n",intToboolean(capabilities->can_generate_single_step_events));
    fprintf(fh,"I:capabilities->can_generate_exception_events=%s\n",intToboolean(capabilities->can_generate_exception_events));
    fprintf(fh,"I:capabilities->can_generate_frame_pop_events=%s\n",intToboolean(capabilities->can_generate_frame_pop_events));
    fprintf(fh,"I:capabilities->can_generate_breakpoint_events=%s\n",intToboolean(capabilities->can_generate_breakpoint_events));
    fprintf(fh,"I:capabilities->can_suspend=%s\n",intToboolean(capabilities->can_suspend));
    fprintf(fh,"I:capabilities->can_redefine_any_class=%s\n",intToboolean(capabilities->can_redefine_any_class));
    fprintf(fh,"I:capabilities->can_get_current_thread_cpu_time=%s\n",intToboolean(capabilities->can_get_current_thread_cpu_time));
    fprintf(fh,"I:capabilities->can_get_thread_cpu_time=%s\n",intToboolean(capabilities->can_get_thread_cpu_time));
    fprintf(fh,"I:capabilities->can_generate_method_entry_events=%s\n",intToboolean(capabilities->can_generate_method_entry_events));
    fprintf(fh,"I:capabilities->can_generate_method_exit_events=%s\n",intToboolean(capabilities->can_generate_method_exit_events));
    fprintf(fh,"I:capabilities->can_generate_all_class_hook_events=%s\n",intToboolean(capabilities->can_generate_all_class_hook_events));
    fprintf(fh,"I:capabilities->can_generate_compiled_method_load_events=%s\n",intToboolean(capabilities->can_generate_compiled_method_load_events));
    fprintf(fh,"I:capabilities->can_generate_monitor_events=%s\n",intToboolean(capabilities->can_generate_monitor_events));
    fprintf(fh,"I:capabilities->can_generate_vm_object_alloc_events=%s\n",intToboolean(capabilities->can_generate_vm_object_alloc_events));
    fprintf(fh,"I:capabilities->can_generate_native_method_bind_events=%s\n",intToboolean(capabilities->can_generate_native_method_bind_events));
    fprintf(fh,"I:capabilities->can_generate_garbage_collection_events=%s\n",intToboolean(capabilities->can_generate_garbage_collection_events));
    fprintf(fh,"I:capabilities->can_generate_object_free_events=%s\n",intToboolean(capabilities->can_generate_object_free_events));
}

#define BUFFER_SIZE_INC 8192

static void readClassData(const char* infile, unsigned char** new_class_data, jint* new_class_data_len) {
    unsigned char* buffer = NULL;
    jint buffer_offset = 0;
    jint buffer_length = 0;

    FILE* fh = fopen(infile, "r");
    if (fh!=NULL) {
        size_t number_read;

        do {
            if (buffer_length == buffer_offset) {
                buffer_length = buffer_length + BUFFER_SIZE_INC;
                unsigned char* tmp = (unsigned char*)malloc(buffer_length);

                if (buffer != NULL) {
                    memcpy((void*)tmp, (void*)buffer, sizeof(unsigned char)*buffer_offset);
                    free(buffer);
                }

                buffer = tmp;
            }

            number_read = fread(buffer+buffer_offset, sizeof(unsigned char), buffer_length-buffer_offset, fh);

            if (ferror(fh)==0)
                buffer_offset += number_read;
            else {
                if (buffer!=NULL) free(buffer);
                fprintf(stderr,"error reading data from \"%s\"",infile);
                fclose(fh);
                exit(1);
            }
        } while (! feof(fh));

        fclose(fh);
    }

    *new_class_data     = 0;
    *new_class_data_len = 0;

    if (0<buffer_offset) {
        /* now that the class buffer has been created who owns it? */
        *new_class_data     = buffer;
        *new_class_data_len = buffer_offset;
    } else {
        /* free the buffer if it has been created but not used */
        if (buffer!=NULL) free(buffer);
    }
}

void writeClassData(const char* outfile, const unsigned char* class_data, jint class_data_len) {
    FILE* fh = fopen(outfile, "w");

    if(fh == NULL) {
        fprintf(stderr, "failed to open \"%s\" for output of class file contents",outfile);
        exit(1);
    }

    if ((size_t)class_data_len != fwrite((const void*)class_data, sizeof(unsigned char), class_data_len, fh)) {
        fprintf(stderr, "failed to write all %d bytes to \"%s\"",class_data_len,outfile);
        exit(1);
    }

    fclose(fh);

    return;
}

#define TMP_FILE_NAME "class_data"

static void generateFileName(char* file_name, int max_file_name_len) {
    strncpy(file_name,TMP_FILE_NAME,max_file_name_len-1);
    file_name[max_file_name_len-1]='\0';
}

static char* generateTmpClassFileName() {
	int   agentOptionsLength = strlen(agentOptions);
	int   slashLength = (agentOptionsLength<=0 || agentOptions[agentOptionsLength-1]=='/')?0:1;
	char* classFileName = (char*)malloc(sizeof(char)*(agentOptionsLength+slashLength+strlen(TMP_FILE_NAME)+1));

	if (0<agentOptionsLength) {
		strcpy(classFileName, agentOptions);
		if (slashLength!=0)
			classFileName[agentOptionsLength++] = '/';
	}
	strcpy(classFileName+agentOptionsLength,TMP_FILE_NAME);
	
	return classFileName;
}


/* packages to be excluded from transformation */
#define ASM_PACKAGE_NAME     "org/objectweb/asm/"
#define DACAPO_PACKAGE_NAME  "org/dacapo/instrument/"

#define EXCLUDE_CHECK "sun/reflect/"

static jboolean isNotExcluded(const char* name)
{
	struct exclude_list_s* chk;

	if (strncmp(DACAPO_PACKAGE_NAME,name,strlen(DACAPO_PACKAGE_NAME))==0) return FALSE;

	for(chk = exclude_package_list_head; chk!=NULL; chk = chk->next) {
		if (strncmp(name,chk->name,chk->length)==0) return FALSE;
	}

	for(chk = exclude_class_list_head; chk!=NULL; chk = chk->next) {
		if (strcmp(name,chk->name)==0) return FALSE;
	}

	return !FALSE;
}

/*
 */
#define JAVA_COMMAND "java"

static int
invokeProcessClassFile(const char* jarSet, const char* infile, const char* outfile, const char* name, const char* agentOptions) {
	if (name == NULL) name = "NULL";

	pid_t pid = fork();
	if (pid == 0) {
		char* jarSet_Tmp = (char*)malloc(sizeof(char)*(strlen(jarSet)+1));
		char* infile_Tmp = (char*)malloc(sizeof(char)*(strlen(infile)+1));
		char* outfile_Tmp = (char*)malloc(sizeof(char)*(strlen(outfile)+1));
		char* agentOptions_Tmp = (char*)malloc(sizeof(char)*(strlen(agentOptions)+1));
		char* name_Tmp = (char*)malloc(sizeof(char)*(strlen(name)+1));

		strcpy(jarSet_Tmp, jarSet);
		strcpy(infile_Tmp, infile);
		strcpy(outfile_Tmp, outfile);
		strcpy(agentOptions_Tmp, agentOptions);
		strcpy(name_Tmp, name);

		/* child execvp */
		char *const args[] = {
				JAVA_COMMAND,
				"-classpath",
				jarSet_Tmp,
				"org.dacapo.instrument.Instrument",
				infile_Tmp,
				outfile_Tmp,
				name_Tmp,
				agentOptions_Tmp,
				NULL
		};

		execvp(JAVA_COMMAND,args);
		exit(10);
	} else if (pid == -1) {
		/* failed to create child */
		return -10;
	} else {
		/* parent wait4 child */
		int status  = 0;
		int options = 0;
		pid_t cpid = waitpid(pid,&status,options);

		return (pid!=cpid)?-10:WEXITSTATUS(status);
	}
}

static void JNICALL
callbackClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv* env,
                jclass class_being_redefined, jobject loader,
                const char* name, jobject protection_domain,
                jint class_data_len, const unsigned char* class_data,
                jint* new_class_data_len,
                unsigned char** new_class_data)
{
    if (jvmRunning && !jvmStopped) {
        rawMonitorEnter(&lockClass);

        if (instrumentClasses && isNotExcluded(name)) { 
            /* strncmp(DACAPO_PACKAGE_NAME,name,strlen(DACAPO_PACKAGE_NAME))!=0) { */
            char command[1024];
            /* 
            char outfile[256];
            char infile [256];

            generateFileName(outfile, sizeof(outfile));
            generateFileName(infile, sizeof(infile));
            */
            char* outfile = generateTmpClassFileName();
            char* infile  = generateTmpClassFileName();

            writeClassData(outfile,class_data,class_data_len);

		    *new_class_data     = NULL;
		    *new_class_data_len = 0;

            /*
            sprintf(command, "java -classpath \"%s\" org.dacapo.instrument.Instrument \"%s\" \"%s\" \"%s\" \"%s\"",jarSet,infile,outfile,(name!=NULL?name:"NULL"),agentOptions);

            if (system(command) == 0) {
                readClassData(infile,new_class_data,new_class_data_len);
            }
            */

            if (invokeProcessClassFile(jarSet, infile, outfile, name, agentOptions) == 0) {
            	readClassData(infile,new_class_data,new_class_data_len);
            }
            
            free(infile);
            free(outfile);
        }

        rawMonitorExit(&lockClass);
    }
	
    if (storeClassFiles) {
	    char temp[10240];
        jboolean old = !storeClassFilesTXed || *new_class_data == NULL;
        makePath(name);
        sprintf(temp,"%s/%s.class",storeClassFileBase,name);
        // fprintf(stderr,"writing %s class files to store %s [%lx:%d,%lx:%d]\n",old?"old":"new",temp,(unsigned long)class_data,class_data_len,(unsigned long)(*new_class_data),*new_class_data_len);
        writeClassData(temp,(old?class_data:*new_class_data),(old?class_data_len:*new_class_data_len));
    }

	if (breakOnLoad) {
		if (strcmp(name,breakOnLoadClass)==0) exit(1);
	}
}

/* JVMTI_EVENT_VM_INIT */
static void JNICALL
callbackVMInit(jvmtiEnv *env, JNIEnv *jnienv, jthread thread)
{
    jvmRunning = !FALSE;
    
    allocation_live(env, jnienv);
    exception_live(env, jnienv);
    method_live(env, jnienv);
    monitor_live(env, jnienv);
    thread_live(env, jnienv);
    
    while (class_list_head!=NULL) {
	    struct class_list_s* next = class_list_head->next;
        jclass klass = class_list_head->klass;

		processClassPrepare(env, jnienv, thread, klass);

		(*jnienv)->DeleteGlobalRef(jnienv,klass);

	    free(class_list_head);
	    class_list_head = next;
	}  
}

/* JVMTI_EVENT_VM_DEATH */
static void JNICALL
callbackVMDeath(jvmtiEnv *jvmti, JNIEnv *env)
{
    logState   = FALSE;
    jvmStopped = !FALSE;
    
    rawMonitorEnter(&agentLock);
    rawMonitorNotify(&agentLock);
    rawMonitorExit(&agentLock);
}

static void reportMethod(char* class_name, jlong class_tag, jmethodID method) {
	if (! logFileOpen()) return;
	
	char* name_ptr = NULL;
	char* signature_ptr  = NULL;
	char* generic_ptr = NULL;

	jint res = JVMTI_FUNC_PTR(baseEnv,GetMethodName)(baseEnv,method,&name_ptr,&signature_ptr,&generic_ptr);

	if (res!=JNI_OK) return;

	void* buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_METHOD_PREPARE);
	log_field_current_time(buffer);
	log_field_string(buffer, class_name);
	log_field_jlong(buffer, (jlong)method);
	/* log_field_jlong(buffer, class_tag); */
	log_field_string(buffer, name_ptr);
	log_field_string(buffer, signature_ptr);
	log_eol(buffer);

	if (name_ptr!=NULL)      JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)name_ptr);
	if (signature_ptr!=NULL) JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)signature_ptr);
	if (generic_ptr!=NULL)   JVMTI_FUNC_PTR(baseEnv,Deallocate)(baseEnv,(unsigned char*)generic_ptr);
}


static void processClassPrepare(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jclass klass) {
	jlong thread_tag = 0;
	jlong class_tag = 0;
	
	// jclass GetObjectClass(JNIEnv *env, jobject obj);
	/*
	rawMonitorEnter(&lockTag);
	jboolean klass_new_tag  = getTag(klass,&class_tag);
	rawMonitorExit(&lockTag);
	*/
	
	jint       method_count = 0;
	jmethodID* methods = NULL;
	          
	jint res = JVMTI_FUNC_PTR(jvmti_env,GetClassMethods)(jvmti_env,klass,&method_count,&methods);

	if (res!=JNI_OK) return;

	char* signature = NULL;
	char* generic   = NULL;

	res = JVMTI_FUNC_PTR(jvmti_env,GetClassSignature)(jvmti_env, klass, &signature, &generic);
	
	if (res!=JNI_OK) {
		JVMTI_FUNC_PTR(jvmti_env,Deallocate)(jvmti_env,(unsigned char*)methods);
		fprintf(stderr,"processclassPrepare fail\n");
		exit(10);
	}	

	int i=0;	
	rawMonitorEnter(&lockLog);
	void* buffer = log_buffer_get();
	log_field_string(buffer, LOG_PREFIX_CLASS_PREPARE);
	log_field_current_time(buffer);
	// log_field_jlong(buffer, class_tag);
	log_field_string(buffer, signature);
	log_eol(buffer);
	rawMonitorExit(&lockLog);
	while(i<method_count) {
		rawMonitorEnter(&lockLog);
		reportMethod(signature,class_tag,methods[i++]);
		rawMonitorExit(&lockLog);
	}

	JVMTI_FUNC_PTR(jvmti_env,Deallocate)(jvmti_env,(unsigned char*)methods);
	JVMTI_FUNC_PTR(jvmti_env,Deallocate)(jvmti_env,(unsigned char*)signature);
	JVMTI_FUNC_PTR(jvmti_env,Deallocate)(jvmti_env,(unsigned char*)generic);
	
	allocation_class(jvmti_env, jni_env, thread, klass);
	exception_class(jvmti_env, jni_env, thread, klass);
	method_class(jvmti_env, jni_env, thread, klass);
	monitor_class(jvmti_env, jni_env, thread, klass);
	thread_class(jvmti_env, jni_env, thread, klass);
	
}

#define BIN_STR(x) ((x)?"true":"false")

/* JVMTI_EVENT_CLASS_PREPARE */
void JNICALL
callbackClassPrepare(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jclass klass) {
    if (jvmStopped || !logFileOpen()) return;
    
    if (jvmRunning)
		processClassPrepare(jvmti_env, jni_env, thread, klass);
	else {
		/* delay class processing */
		struct class_list_s* gklass = (struct class_list_s *)malloc(sizeof(struct class_list_s));
		
		gklass->klass = (*jni_env)->NewGlobalRef(jni_env,klass);
		gklass->next  = NULL;
		if (class_list_tail==NULL) {
			class_list_head = class_list_tail = gklass;
		} else {
			class_list_tail->next = gklass;
			class_list_tail = gklass;
		}
	}
}

/* */


static void makePath(const char* name) {
    char fullName[10240];
    int  lenP = strlen(storeClassFileBase);
    int  lenN = strlen(name);
    
    strcpy(fullName,storeClassFileBase);
    fullName[lenP] = '/';
    strcpy(fullName+lenP+1,name);
    fullName[lenP+lenN+1] = '\0';
    
    int pos = lenP;
    while(fullName[pos]=='/') {
        fullName[pos]='\0';
        mkdir(fullName,S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
        fullName[pos]='/';
        pos++;
        while(fullName[pos]!='\0'&&fullName[pos]!='/') pos++;
    }
}
