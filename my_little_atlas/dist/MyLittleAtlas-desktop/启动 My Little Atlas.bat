@echo off
setlocal
if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java.exe" -jar "%~dp0MyLittleAtlas.jar"
) else (
  java -jar "%~dp0MyLittleAtlas.jar"
)
