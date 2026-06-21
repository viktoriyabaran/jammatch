@echo off
setlocal
cd /d "%~dp0"

if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"
)

call mvn -pl server -am install -DskipTests -q
if %errorlevel% neq 0 exit /b %errorlevel%

call mvn -pl server exec:java -Dexec.mainClass=server.net.GameServer
