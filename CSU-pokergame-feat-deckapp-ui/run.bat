@echo off
rem Build & run the JavaFX poker-deck app (uses local JDK + jars under lib).
rem NOTE: keep this file ASCII-only; Chinese comments break under the GBK console.
setlocal enabledelayedexpansion
cd /d %~dp0

if not exist lib\javafx-controls-21.0.2-win.jar (
    echo [lib] prepare JavaFX runtime jars...
    if not exist lib mkdir lib
    if exist "%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar" (
        copy /y "%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar" lib\ >nul
        copy /y "%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar" lib\ >nul
        copy /y "%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar" lib\ >nul
        copy /y "%USERPROFILE%\.m2\repository\org\openjfx\javafx-media\21.0.2\javafx-media-21.0.2-win.jar" lib\ >nul
    ) else (
        echo [lib] JavaFX jars not found. Resolve deps with IDEA/Maven or place them in lib.
        pause & exit /b 1
    )
)

if not exist out mkdir out
xcopy /y /s /i src\main\resources out >nul 2>&1

rem Too many source files to pass on one command line (cmd has an 8191-char
rem limit), so collect them into a javac argfile. javac treats backslash as an
rem escape character inside argfiles, therefore paths use forward slashes.
set SRCLIST=%TEMP%\poker_src_%RANDOM%%RANDOM%.txt
if exist "%SRCLIST%" del /f /q "%SRCLIST%" >nul 2>&1
for /r src\main\java %%f in (*.java) do (
    set "P=%%f"
    echo "!P:\=/!">>"%SRCLIST%"
)

echo [build] compiling...
javac --module-path lib --add-modules javafx.controls,javafx.media -encoding UTF-8 -parameters -cp "lib\jackson\*" -d out @"%SRCLIST%"
set BUILD_ERR=%errorlevel%
del /f /q "%SRCLIST%" >nul 2>&1
if not "%BUILD_ERR%"=="0" (
    echo [build] compile failed, exit code %BUILD_ERR%
    pause
    exit /b 1
)

echo [run] launching...
java --enable-native-access=javafx.graphics --module-path lib --add-modules javafx.controls,javafx.media -cp "out;lib\jackson\*" com.cards.DeckApp %*
endlocal
