@echo off
REM Name des Servers und der Logdatei
set SERVER_MAIN_CLASS=main.Main
set LOG_FILE=server.log
set CRASH_LOG=crash.log

REM Projektverzeichnis als Arbeitsverzeichnis setzen (aktuelles Verzeichnis)
set PROJECT_DIR=%~dp0
set SRC_DIR=%PROJECT_DIR%\src
set BUILD_DIR=%PROJECT_DIR%\build

REM Stelle sicher, dass das Build-Verzeichnis existiert
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

REM Kompilieren des Projekts
echo [INFO] Kompiliere Projekt...
javac -d "%BUILD_DIR%" -cp ".;lib/*" -sourcepath "%SRC_DIR%" "%SRC_DIR%\main\Main*.java" 2> "%PROJECT_DIR%\compile_errors.log"
if %errorlevel% neq 0 (
    echo [FEHLER] Fehler beim Kompilieren. Details in compile_errors.log
    PAUSE
    exit /b 1
)

REM Kopiere die Konfigurationsdatei ins Build-Verzeichnis
echo [INFO] Kopiere Konfigurationsdateien...
if not exist "%PROJECT_DIR%\config.properties" (
    copy "%SRC_DIR%\config.properties" "%PROJECT_DIR%\config.properties" > nul
)
copy "%PROJECT_DIR%\config.properties" "%BUILD_DIR%\config.properties" > nul
if %errorlevel% neq 0 (
    echo [FEHLER] Konnte config.properties nicht kopieren.
    PAUSE
    exit /b 1
)

:START_SERVER
REM Aktuelles Datum und Uhrzeit für Logging (im Format: YYYY-MM-DD HH:MM:SS)
for /f "tokens=1-3 delims=/- " %%a in ("%date%") do (
    set "LOG_DATE=%%c-%%b-%%a"
)
for /f "tokens=1-2 delims=: " %%a in ("%time%") do (
    set "LOG_TIME=%%a:%%b"
)
set "TIMESTAMP=%LOG_DATE% %LOG_TIME%"

REM Starte den Server und leite Ausgaben in die Logdatei um
echo [INFO] [%TIMESTAMP%] Starte Server...
echo [INFO] [%TIMESTAMP%] Starte Server... >> "%PROJECT_DIR%\%LOG_FILE%"
powershell -Command "java -cp '%BUILD_DIR%;lib/*' %SERVER_MAIN_CLASS% 2>&1 | Tee-Object -FilePath '%PROJECT_DIR%\%LOG_FILE%'"

if %errorlevel% neq 0 (
    REM Terminal-Ausgabe
    echo [WARNUNG] [%TIMESTAMP%] Server wurde unerwartet beendet. Neustart in 5 Sekunden...

    REM In Logdatei schreiben
    echo [WARNUNG] [%TIMESTAMP%] Server wurde unerwartet beendet. Neustart in 5 Sekunden... >> "%PROJECT_DIR%\%LOG_FILE%"
    
    REM Crash-Log aktualisieren
    echo [%TIMESTAMP%] Server-Absturz erkannt. >> "%PROJECT_DIR%\%CRASH_LOG%"
    type "%PROJECT_DIR%\%LOG_FILE%" >> "%PROJECT_DIR%\%CRASH_LOG%"
    echo. >> "%PROJECT_DIR%\%CRASH_LOG%"

    timeout /t 5 > nul
    goto START_SERVER
) else (
    echo [INFO] [%TIMESTAMP%] Der Server wurde erfolgreich beendet.
    echo [INFO] [%TIMESTAMP%] Der Server wurde erfolgreich beendet. >> "%PROJECT_DIR%\%LOG_FILE%"
    PAUSE
)