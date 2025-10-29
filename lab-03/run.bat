@REM filepath: d:\nstu-fifth-semester-main\lab-03\run.bat
@echo off
chcp 65001 > nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dconsole.encoding=UTF-8
echo === Запуск индикатора уровня ===
call gradlew.bat run
pause