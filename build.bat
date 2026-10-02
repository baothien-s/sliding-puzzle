@echo off
rem ---------------------------------------------------------------
rem  Rebuilds SlidingPuzzle.jar from the source in src\
rem  Needs a JDK (javac + jar), not just a JRE.
rem ---------------------------------------------------------------
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo javac was not found - a JDK is required to build.
    echo Install a JDK 11 or newer, e.g. from https://adoptium.net
    if "%1"=="" pause
    exit /b 1
)

if exist out rmdir /s /q out
mkdir out

rem Collect every .java file (quoted, so folders with spaces are fine)
set FILES=
for /r src %%f in (*.java) do call set FILES=%%FILES%% "%%f"

javac -d out %FILES%
if errorlevel 1 (
    echo.
    echo Build failed.
    if "%1"=="" pause
    exit /b 1
)

jar cfe SlidingPuzzle.jar game.Main -C out .
if errorlevel 1 (
    echo Could not create the jar.
    if "%1"=="" pause
    exit /b 1
)

echo Built SlidingPuzzle.jar
if "%1"=="" pause
