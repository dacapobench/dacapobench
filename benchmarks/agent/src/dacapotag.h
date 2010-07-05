#ifndef DACAPO_TAG_H
#define DACAPO_TAG_H

#include "dacapo.h"

#include "dacapolock.h"

extern MonitorLockType       lockTag;

#define getTag(obj,tag)  GET_TAG(__FILE__, __LINE__, obj, tag)
#define setTag(obj,tag)  SET_TAG(__FILE__, __LINE__, obj, tag)

_Bool dacapo_tag_init();
jboolean GET_TAG(char* file, int line, jobject object, jlong*  tag);
jlong    SET_TAG(char* file, int line, jobject object, jlong  size);

#endif
