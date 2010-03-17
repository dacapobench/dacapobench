#ifndef DACAPO_TAG_H
#define DACAPO_TAG_H

#include "dacapo.h"

extern jrawMonitorID       lockTag;

_Bool dacapo_tag_init();
jboolean getTag(jobject object, jlong*  tag);
jlong    setTag(jobject object, jlong  size);

#endif
