#
# Copyright (c) 2009 The Australian National University.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License v2.0.
# You may obtain the license at
# 
#    http://www.opensource.org/licenses/apache2.0.php
#

# Source lists
LIBNAME=dacapoagent
SOURCES=dacapooptions.c dacapoagent.c dacapoexception.c dacapolog.c dacapotag.c dacapolock.c dacapomonitor.c dacapothread.c \
	dacapoallocation.c dacapomethod.c dacapocallchain.c 

# Linux GNU C Compiler
ifeq ($(OSNAME), linux)
    # GNU Compiler options needed to build it
    COMMON_FLAGS=-fno-strict-aliasing -fPIC -fno-omit-frame-pointer -D_LARGEFILE64_SOURCE=1
    # Options that help find errors
    COMMON_FLAGS+= -W -Wall  -Wno-unused -Wno-parentheses -pthread
    # ifeq ($(OPT), true)
        CFLAGS=-O2 $(COMMON_FLAGS) 
    # else
    #     CFLAGS=-g $(COMMON_FLAGS) 
    # endif
    # Object files needed to create library
    OBJECTS=$(SOURCES:%.c=%.o)
    # Library name and options needed to build it
    LIBRARY=lib$(LIBNAME).so
    LDFLAGS=-Wl,-soname=$(LIBRARY) -static-libgcc -mimpure-text
    # Libraries we are dependent on
    LIBRARIES=-L $(JAVA_HOME)/jre/lib/$(LIBARCH) -lc
    # LIBRARIES+= -L $(ZLIB_DIR) -l $(ZLIB_LIB)
    OBJECTS+= $(ZLIB_DIR)/lib$(ZLIB_LIB).a 
    # Building a shared library
    LINK_SHARED=$(LINK.c) -shared -o $@
endif

# Common -I options
CFLAGS += -I. -I$(ZLIB_DIR)
CFLAGS += -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(OSNAME)

# Default rule (build both native library and jar file)
all: $(LIBRARY)

# Build native library
$(LIBRARY): $(OBJECTS)
	$(LINK_SHARED) $(OBJECTS) $(LIBRARIES)

# Cleanup the built bits
clean:
	rm -f -r classes
	rm -f $(LIBRARY) $(OBJECTS)

# Simple tester
test: all
	LD_LIBRARY_PATH=. $(JAVA_HOME)/bin/java -agentlib:$(LIBNAME) -Xbootclasspath/a:./$(JARFILE) -version


