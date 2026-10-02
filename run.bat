@echo off
rem ---------------------------------------------------------------
rem  Sliding Puzzle launcher - double-click to play.
rem  Needs Java 11+ installed (a JRE is enough). No IDE required.
rem ---------------------------------------------------------------
cd /d "%~dp0"

where javaw >nul 2>nul
if errorlevel 1 (
    echo Java was not found on this computer.
    echo Install Java 11 or newer, e.g. from https://adoptium.net
    echo then double-click run.bat again.
    echo.
    pause
    exit /b 1
)

rem First run from source only: build the jar if it is missing.
if not exist SlidingPuzzle.jar (
    call build.bat nopause
    if errorlevel 1 exit /b 1
)

start "" javaw -jar SlidingPuzzle.jar
