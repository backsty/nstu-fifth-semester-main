#!/bin/bash

# Принудительная установка UTF-8 для текущей сессии
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
export LESSCHARSET=utf-8

# Java настройки UTF-8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
export GRADLE_OPTS="-Dfile.encoding=UTF-8"

echo "=== Запуск с принудительной UTF-8 кодировкой ==="
echo "LANG: $LANG"
echo "LC_ALL: $LC_ALL"
echo "JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS"
echo ""

# Для Windows - используем cmd с chcp
if [[ "$OSTYPE" == "msys" ]]; then
    echo "Используем Windows cmd с UTF-8..."
    cmd.exe /c "chcp 65001 > nul && gradlew.bat run"
else
    # Для Linux/Mac
    ./gradlew run
fi

echo ""
echo "=== Выполнение завершено ==="
read -p "Нажмите Enter для продолжения..."