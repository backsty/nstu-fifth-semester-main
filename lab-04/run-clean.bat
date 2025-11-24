@REM filepath: d:\nstu-fifth-semester-main\lab-04\run-clean.bat
@echo off
chcp 65001 > nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
set GRADLE_OPTS=-Dorg.gradle.console=plain
cls
echo ========================================
echo   Индикатор уровня - Лаб. работа №4
echo ========================================
echo.
call gradlew.bat --quiet --console=plain run
echo.
pause