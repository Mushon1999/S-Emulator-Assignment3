@echo off
echo Starting S-Emulator Client...

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or later
    pause
    exit /b 1
)

REM Set JavaFX path (adjust as needed)
set JAVAFX_PATH=C:\path\to\javafx\lib

REM Check if JavaFX is available
if exist "%JAVAFX_PATH%\javafx.controls.jar" (
    echo Using JavaFX from: %JAVAFX_PATH%
    java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -cp "s-emulator-client.jar;lib\*" se.emulator.client.SEMulatorClientApp
) else (
    echo JavaFX not found at %JAVAFX_PATH%
    echo Trying to run without JavaFX module path...
    java -cp "s-emulator-client.jar;lib\*" se.emulator.client.SEMulatorClientApp
)

pause
