#!/bin/sh
APP_HOME=`pwd`
GRADLE_USER_HOME="$APP_HOME/.gradle_home"
export GRADLE_USER_HOME
mkdir -p "$GRADLE_USER_HOME"

DEFAULT_JVM_OPTS="-Xmx1024m -Xms256m"
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec java $DEFAULT_JVM_OPTS -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
