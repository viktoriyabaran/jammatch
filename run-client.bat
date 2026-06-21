@echo off
setlocal
cd /d "%~dp0"

if not "%~1"=="" set "JAMMATCH_CLIENT_TOKEN=%~1"

call mvn -pl common -am install -DskipTests -q
if %errorlevel% neq 0 exit /b %errorlevel%

call mvn -pl client org.openjfx:javafx-maven-plugin:0.0.8:run
