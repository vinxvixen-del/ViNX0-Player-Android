#!/usr/bin/env sh

# Resolving links and paths
DIRNAME=`dirname "$0"`
[ -z "$DIRNAME" ] && DIRNAME="."
APP_BASE_NAME=`basename "$0"`
APP_HOME=`cd "$DIRNAME" && pwd`

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD="java"
    fi
else
    JAVACMD="java"
fi

DEFAULT_JVM_OPTS="-Xmx512m -Xms128m"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
