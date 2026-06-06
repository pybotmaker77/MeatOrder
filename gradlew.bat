@echo off
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set GRADLE_USER_HOME=%APP_HOME%\.gradle_home
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"

set DEFAULT_JVM_OPTS=-Xmx1024m -Xms256m
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

java -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo Java not found in PATH. Please ensure JDK 11 is set in AIDE.
goto fail

:execute
java %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain app:assembleDebug %*

:fail
exit /b %ERRORLEVEL%
