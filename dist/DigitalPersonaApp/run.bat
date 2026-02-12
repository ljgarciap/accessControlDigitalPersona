@echo off
echo --- MODO DEPURACION ---
cd /d "%~dp0"

set JAR_PATH=app\digitalpersona-desktop-1.0-SNAPSHOT-jar-with-dependencies.jar
set JAVA_EXE=runtime\bin\java.exe

if not exist %JAVA_EXE% (
    echo [ERROR] No se encuentra el entorno Java en: %JAVA_EXE%
    echo Ejecutando con Java del sistema...
    set JAVA_EXE=java
)

%JAVA_EXE% -Djava.library.path=libs -jar %JAR_PATH%

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] El programa falló con código %ERRORLEVEL%
)

pause
