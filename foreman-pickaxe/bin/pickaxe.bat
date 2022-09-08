@echo off
setlocal enabledelayedexpansion
setlocal

rem # Set pickaxe home directory
set PICKAXE_HOME=%~dp0..
set "PICKAXE_HOST=%COMPUTERNAME%"

rem # Set java - use bundled first
if "%PROCESSOR_ARCHITECTURE%" == "AMD64" (
    if exist "%PICKAXE_HOME%\bin\support\win64\jre1.8.0_202" (
	    set JAVA="%PICKAXE_HOME%\bin\support\win64\jre1.8.0_202\bin\java.exe"
	    goto found_java
    )
) else (
    if exist "%PICKAXE_HOME%\bin\support\win32\jre1.8.0_202" (
	    set JAVA="%PICKAXE_HOME%\bin\support\win32\jre1.8.0_202\bin\java.exe"
	    goto found_java
    )
)

if defined JAVA_HOME (
    set JAVA="%JAVA_HOME%\bin\java.exe"
) else (
    for %%I in (java.exe) do set JAVA="%%~$PATH:I"
)

if exist %JAVA% goto found_java

echo Failed to find java - set JAVA_HOME or add java to the PATH 1>&2
timeout /t 10
exit /b 1

:found_java
rem # Set JVM options
set JVM_OPTS_FILE="%PICKAXE_HOME%\conf\jvm.options"
for /F "usebackq delims=" %%a in (`findstr /b \- %JVM_OPTS_FILE%`) do set options=!options! %%a
set "JVM_OPTS=!options! %JVM_OPTS%"

rem # Set JVM parameters
set JVM_PARAMS=-Dlog4j.configurationFile="%PICKAXE_HOME%\etc\log4j2.xml"
set JVM_PARAMS=%JVM_PARAMS% -DLOG_LOCATION="%PICKAXE_HOME%\logs"
set JVM_PARAMS=%JVM_PARAMS% -Dio.netty.tryReflectionSetAccessible=false

rem # Set command line arguments
set JVM_COMMAND_LINE=-c "%PICKAXE_HOME%\conf\pickaxe.yml"

echo Note: to run in the background, install and start as a service
echo;
echo To do that:
echo - run 'service-start.bat' to install and auto-start
echo - run 'service-stop.bat' to stop
echo;
echo Starting pickaxe...(leave this window open)
%JAVA% %JVM_OPTS% %JVM_PARAMS% -cp "%PICKAXE_HOME%\lib\*" mn.foreman.pickaxe.Main %JVM_COMMAND_LINE%
goto end

:end
endlocal