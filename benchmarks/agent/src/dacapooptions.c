/*
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <stdarg.h>
*/

#include "dacapooptions.h"

#ifndef FALSE
#define FALSE 0
#endif

/*
	public static final String CLASSES_INITIALIZATION = "clinit";
	public static final String METHOD_CALLS           = "method_calls";
	public static final String LOG_START              = "log_start";
	public static final String LOG_STOP               = "log_stop";
	public static final String RUNTIME                = "runtime";
*/

struct option_s {
    struct option_s*  next;
    const char*       option;
    int               length;
    const char*       argument;
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

_Bool isSelected(const char* option, char* argument) {
	struct option_s* opt = findOption(option);

	if (opt!=NULL && argument!=NULL) {
		if (opt->argument!=NULL)
			strncpy(argument,opt->argument,opt->argLength);
		argument[opt->argLength]='\0';
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

void makeOptionList(const char* options) {
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

