#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

#include "dacapooptions.h"

#ifndef FALSE
#define FALSE 0
#endif

struct option_s {
    struct option_s*  next;
    char*             option;
    int               length;
    char*             argument;
    int               argLength;
};

struct option_s*    optionList = NULL;
struct option_s*    optionTail = NULL;

static struct option_s* findOption(const char* option) {
	struct option_s* check = optionList;

	while (check!=NULL) {
	    if (strncmp(check->option,option,check->length)==0 &&
	        (int)strlen(option)==check->length) {
    		return check;
	    }
	    check = check->next;
	}
	return NULL;
}

_Bool isSelected(const char* option, char** argument) {
	struct option_s* opt = findOption(option);

	if (opt != NULL && argument != NULL) {
		if (*argument==NULL)
			*argument = (char*)malloc(opt->argLength+1);
		if (0 < opt->argLength) strncpy(*argument,opt->argument,opt->argLength);
		(*argument)[opt->argLength]='\0';
	}

	return opt!=NULL;
}

_Bool hasArgument(const char* option) {
	struct option_s* opt = findOption(option);

	return opt!=NULL && opt->argument!=NULL && opt->argLength>0;
}

void reportOptionsList() {
	char tmp[10240];

	struct option_s* opt = optionList;

    fprintf(stderr,"reportOptionsList\n");
	while (opt!=NULL) {
	    strncpy(tmp,opt->option,opt->length);
	    tmp[opt->length] = '\0';
		if (opt->argument!=NULL) {
		    tmp[opt->length]=':';
			strncpy(tmp+opt->length+1,opt->argument,opt->argLength);
			tmp[opt->length+1+opt->argLength]='\0';
		}
		fprintf(stderr,"  %s\n",tmp);
		opt = opt->next;
	}
}

void makeOptionList(char* options) {
	if (options != NULL) {
        int start = 0;
		while (options[start]!='\0') {
		    int end = start+(start==0?0:1);

		    while (options[end]!=','&&options[end]!='\0'&&options[end]!='=') end++;

		    int arg = end;

		    if (options[arg]=='=')
		    	while(options[arg]!=','&&options[arg]!='\0') arg++;

		    if (0<(end-start)) {
		        struct option_s *tmp = (struct option_s*)malloc(sizeof(struct option_s));
		        tmp->next = NULL;
		        tmp->option = options+start;
		        tmp->length = end-start;

		        if (arg==end) {
		        	tmp->argument  = NULL;
		        	tmp->argLength = 0;
		        } else {
		        	tmp->argument  = options+end+1;
		        	tmp->argLength = arg-end-1;
		        }

		        if (optionList==NULL) {
		        	optionList = optionTail = tmp;
		        } else {
		            optionTail->next = tmp;
		            optionTail = tmp;
		        }
		    }

		    start = arg + (options[arg]=='\0'?0:1);
		}
	}

	if (isSelected(OPT_REPORT_OPTIONS, NULL))
		reportOptionsList();
}

#define CONFIG_FILE_NAME "config.txt"
#define DEFAULT_DIR "."
#define FILE_SEP_CHAR '/'

#define FILE_TYPE int

#define FILE_IS_CLOSED -1
#define FILE_FLAGS      (O_RDONLY)
#define FILE_MODE       (S_IRUSR)

/*
close(logFile);
logFile = open(f,FILE_FLAGS,FILE_MODE);
logFileSequenceLength += write(logFile,b,(e)*(n));
ssize_t read(int fd, void *buf, size_t count);

*/

enum State {
	UNKNOWN,
	COMMENT,
	KEY,
	KEY_ESCAPE,
	VALUE,
	VALUE_ESCAPE
};

void append(char** buf, int* length, int* maxLength, char c)
{
	if (*length == *maxLength) {
		*maxLength = 2 * *maxLength;
		char* tmp = (char*)malloc(sizeof(char) * *maxLength);

		int i;		
		for(i = 0; i < *length; i++) tmp[i] = (*buf)[i];
		
		free(*buf);
		*buf = tmp;
	}
	
	(*buf)[*length] = c;
	*length = *length + 1;
}

static void addOption(char* key, int keyLength, char* value, int valueLength) {
	struct option_s* opt = (struct option_s*)malloc(sizeof(struct option_s));
	
	opt->next = NULL;
	
	opt->option = (char*)malloc(sizeof(char)*(keyLength+1));
	opt->length = keyLength;
	strncpy(opt->option,key,sizeof(char)*keyLength);
	opt->option[keyLength] = '\0';
				
	opt->argument = (char*)malloc(sizeof(char)*(valueLength+1));
	opt->argLength = valueLength;
	strncpy(opt->argument,value,sizeof(char)*valueLength);
	opt->argument[valueLength] = '\0';
				
	if (optionList == NULL)
		optionList = optionTail = opt;
	else {
		optionTail->next = opt;
		optionTail = opt;
	}
}
	
void makeOptionListFromFile(char* agentDir) 
{
	char* fileName = CONFIG_FILE_NAME;
	char* fullFileName = NULL;
	
	if (agentDir == NULL) agentDir = DEFAULT_DIR; 
	
	int   length = strlen(agentDir);
	
	if (agentDir[length] != FILE_SEP_CHAR) {
		fullFileName = (char*)malloc(sizeof(char)*(length + 1 + strlen(fileName)));
		strcpy(fullFileName, agentDir);
		fullFileName[length] = FILE_SEP_CHAR;
		strcpy(fullFileName + length + 1, fileName);
	} else {
		strcpy(fullFileName, agentDir);
		strcpy(fullFileName + length, fileName);
	}

    FILE_TYPE fh = open(fullFileName,FILE_FLAGS,FILE_MODE);
	
	if (fh == FILE_IS_CLOSED) {
		fprintf(stderr, "unable to read config file %s\n", fullFileName);
		exit(10);
	}

	char c = '\0';
	size_t bytes = read(fh, &c, sizeof(char));
	
	enum State state = UNKNOWN;
	
	int keyLength      = 0;
	int keyMaxLength   = 128;
	char* key          = (char*)malloc(keyMaxLength);

	int valueLength    = 0;
	int valueMaxLength = 1024;
	char* value        = (char*)malloc(valueMaxLength);
		
	while (bytes != 0) {
		if (state == UNKNOWN) {
			switch (c) {
			case '-': case '#': 
				state = COMMENT;
				break;
			case '\n': case '\r':
				break;
			case '\\':
				state = KEY_ESCAPE;
				break;
			default:
				state = KEY;
				append(&key, &keyLength, &keyMaxLength, c);
				break;
			}
		} else if (state == KEY) {
			switch (c) {
			case '\\':
				state = KEY_ESCAPE;
				break;
			case '=':
				state = VALUE;
				break;
			case '\r': case '\n':
				state = UNKNOWN;

				addOption(key,keyLength,value,valueLength);

				keyLength   = 0;
				valueLength = 0;
				break;
			default:
				append(&key, &keyLength, &keyMaxLength, c);
				break;
			}
		} else if (state == KEY_ESCAPE) {
			switch (c) {
			case 'n':
				append(&key, &keyLength, &keyMaxLength, '\n');
				break;
			case 'r':
				append(&key, &keyLength, &keyMaxLength, '\r');
				break;
			case 't':
				append(&key, &keyLength, &keyMaxLength, '\t');
				break;
			default:
				append(&key, &keyLength, &keyMaxLength, c);
				break;
			}
			state = KEY;
		} else if (state == VALUE) {
			switch (c) {
			case '\\':
				state = VALUE_ESCAPE;
				break;
			case '\r': case '\n':
				state = UNKNOWN;

				addOption(key,keyLength,value,valueLength);

				keyLength   = 0;
				valueLength = 0;
				break;
			default:
				append(&value, &valueLength, &valueMaxLength, c);
				break;
			}
		} else if (state == VALUE_ESCAPE) {
			switch (c) {
			case 'n':
				append(&value, &valueLength, &valueMaxLength, '\n');
				break;
			case 'r':
				append(&value, &valueLength, &valueMaxLength, '\r');
				break;
			case 't':
				append(&value, &valueLength, &valueMaxLength, '\t');
				break;
			default:
				append(&value, &valueLength, &valueMaxLength, c);
				break;
			}
			state = VALUE;
		} else if (state == COMMENT) {
			switch (c) {
			case '\r': case '\n':
				state = UNKNOWN;
				break;
			default:
				break;
			}
		} else {
			fprintf(stderr, "we should never reach this state\n");
			exit(10);
		}
	
		bytes = read(fh, &c, sizeof(char));
	}	
	
	close(fh);
	free(fullFileName);
}
