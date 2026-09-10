@echo off
rem Build & run the JavaFX poker-deck app (uses local JDK + jars under lib).
setlocal
cd /d %~dp0

if not exist lib\javafx-controls-21.0.2-win.jar (
    echo [lib] prepare JavaFX runtime jars...
    if not exist lib mkdir lib
    if exist "%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar" (
        copy /y "%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar" lib\ >nul
        copy /y "%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar" lib\ >nul
        copy /y "%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar" lib\ >nul
    ) else (
        echo [lib] JavaFX jars not found. Resolve deps with IDEA/Maven or place them in lib.
        pause & exit /b 1
    )
)

if not exist out mkdir out
xcopy /y /s /i src\main\resources out >nul 2>&1

set SRC=
for /r src\main\java %%f in (*.java) do call set SRC=%%SRC%% "%%f"

echo [build] compiling...
javac --module-path lib --add-modules javafx.controls -encoding UTF-8 -parameters -cp "lib\jackson\*" -d out %SRC%
if errorlevel 1 pause & exit /b 1

echo [run] launching...
java --enable-native-access=javafx.graphics --module-path lib --add-modules javafx.controls -cp "out;lib\jackson\*" com.cards.DeckApp %*
endlocal
