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
SOURCES=src/dacapoagent.c

# Linux GNU C Compiler
ifeq ($(OSNAME), linux)
    # GNU Compiler options needed to build it
    COMMON_FLAGS=-fno-strict-aliasing -fPIC -fno-omit-frame-pointer
    # Options that help find errors
    COMMON_FLAGS+= -W -Wall  -Wno-unused -Wno-parentheses
    ifeq ($(OPT), true)
        CFLAGS=-O2 $(COMMON_FLAGS) 
    else
        CFLAGS=-g $(COMMON_FLAGS) 
    endif
    # Object files needed to create library
    OBJECTS=$(SOURCES:%.c=%.o)
    # Library name and options needed to build it
    LIBRARY=lib/lib$(LIBNAME).so
    LDFLAGS=-Wl,-soname=$(LIBRARY) -static-libgcc -mimpure-text
    # Libraries we are dependent on
    LIBRARIES=-L $(JAVA_HOME)/jre/lib/$(LIBARCH) -lc
    # Building a shared library
    LINK_SHARED=$(LINK.c) -shared -o $@
endif

# Common -I options
CFLAGS += -I.
CFLAGS += -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(OSNAME)

# Default rule (build both native library and jar file)
all: $(LIBRARY)
	echo $(OSNAME)

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


