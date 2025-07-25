#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# @author date: 20/03/2021
# @author author: gildor
#
# This script is a copy of the gradlew script from a standard Android project.
# It is used to execute Gradle commands.
#

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to this script.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if ${cygwin} ; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$GRADLE_HOME" ] && GRADLE_HOME=`cygpath --unix "$GRADLE_HOME"`
fi

# Attempt to set APP_HOME
if [ -z "$APP_HOME" ] ; then
  # In Cygwin, the gradlew script is a symlink to the wrapper jar, which is in the same directory.
  if ${cygwin} && [ -L "$0" ] ; then
    APP_HOME=`dirname "$0"`
  else
    # a messy way to get the directory of the script
    APP_HOME=`dirname "$0"`
    if [ "$APP_HOME" = "." ] ; then
      APP_HOME=`pwd`
    fi
  fi
fi

# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
APP_HOME=`dirname "$PRG"`

# For Cygwin, switch paths to Windows format before running java
if ${cygwin} ; then
  APP_HOME=`cygpath --path --windows "$APP_HOME"`
  JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  GRADLE_HOME=`cygpath --path --windows "$GRADLE_HOME"`
fi

# Add a semi-colon to the beginning of the CLASSPATH if it's not empty
if [ -n "$CLASSPATH" ] ; then
  CLASSPATH=";$CLASSPATH"
fi

# Set the CLASSPATH
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar$CLASSPATH"

# Set the JVM options
if [ -z "$JAVA_OPTS" ] ; then
  JAVA_OPTS="$DEFAULT_JVM_OPTS"
fi

# Execute Gradle
exec "$JAVA_HOME/bin/java" $JAVA_OPTS -Dorg.gradle.appname=$APP_BASE_NAME -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
