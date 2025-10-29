#!/bin/bash

# Build script for S-Emulator Server
echo "Building S-Emulator Server..."

# Create build directory
mkdir -p build/classes
mkdir -p build/lib

# Copy JARs to lib directory
cp lib/gson-2.10.1.jar build/lib/
cp lib/servlet-api-4.0.1.jar build/lib/

# Compile server classes
echo "Compiling server classes..."
javac -cp "build/lib/*" -d build/classes \
    src/main/java/se/emulator/engine/*.java \
    src/main/java/se/emulator/server/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Create WAR file
echo "Creating WAR file..."
cd build
jar -cf ../s-emulator-server.war -C classes . -C ../src/main/webapp .

cd ..
echo "Server build complete! WAR file: s-emulator-server.war"
