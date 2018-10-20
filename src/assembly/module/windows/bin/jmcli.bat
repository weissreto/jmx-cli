@ECHO OFF

SET PROGRAM_BIN_DIR=%~dp0
SET PROGRAM_HOME_DIR=%PROGRAM_BIN_DIR%..
SET MAIN_MODULE=ch.rweiss.jmx.client.cli/ch.rweiss.jmx.client.cli.JmxClientCli
SET MODULE_PATH=%PROGRAM_HOME_DIR%\jmods

IF NOT DEFINED JAVA_HOME (
  SET JAVA_EXECUTABLE=java.exe
) ELSE (
  SET JAVA_EXECUTABLE="%JAVA_HOME%\bin\java.exe"
)

%JAVA_EXECUTABLE% -p %MODULE_PATH% --add-modules java.sql -m %MAIN_MODULE% %*