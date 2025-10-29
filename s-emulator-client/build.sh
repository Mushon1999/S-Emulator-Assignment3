#!/bin/bash

# Build script for S-Emulator Client
echo "Building S-Emulator Client..."

# Create build directories
mkdir -p build/classes
mkdir -p build/lib

# Copy JAR files to lib directory
cp lib/gson-2.10.1.jar build/lib/

# Add JavaFX to classpath
export JAVAFX_PATH=/home/user/project3/java-install/javafx-sdk-17.0.2/lib

# Compile client classes
echo "Compiling client classes..."
javac --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.fxml -cp "build/lib/*" -d build/classes \
    src/main/java/se/emulator/server/User.java \
    src/main/java/se/emulator/server/ProgramInfo.java \
    src/main/java/se/emulator/server/FunctionInfo.java \
    src/main/java/se/emulator/server/ExecutionHistory.java \
    src/main/java/se/emulator/server/ChatMessage.java \
    src/main/java/se/emulator/server/ExecutionResult.java \
    src/main/java/se/emulator/console/*.java \
    src/main/java/se/emulator/client/*.java

# Check for compilation errors
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# ✅ Copy resources (CSS files) into build/classes
echo "Copying CSS resources..."
mkdir -p build/classes/se/emulator/console
cp src/main/resources/se/emulator/console/*.css build/classes/se/emulator/console/ 2>/dev/null || echo "No CSS files found."

# Create JAR file
echo "Creating JAR file..."
cd build
jar -cfm ../s-emulator-client.jar ../MANIFEST.MF -C classes .

cd ..
echo "Client build complete! JAR file: s-emulator-client.jar"

