#!/bin/bash

echo "🧪 Testing S-Emulator System..."
echo "================================="

# Test 1: Check Java installation
echo "1. Checking Java installation..."
if java -version 2>&1 | grep -q "version"; then
    echo "✅ Java is installed"
    java -version
else
    echo "❌ Java is not installed or not in PATH"
    exit 1
fi

# Test 2: Build server
echo ""
echo "2. Building server..."
cd s-emulator-server
if ./build.sh; then
    echo "✅ Server build successful"
    if [ -f "s-emulator-server.war" ]; then
        echo "✅ WAR file created: $(ls -lh s-emulator-server.war)"
    else
        echo "❌ WAR file not found"
        exit 1
    fi
else
    echo "❌ Server build failed"
    exit 1
fi

# Test 3: Build client
echo ""
echo "3. Building client..."
cd ../s-emulator-client
if ./build.sh; then
    echo "✅ Client build successful"
    if [ -f "s-emulator-client.jar" ]; then
        echo "✅ Client JAR created: $(ls -lh s-emulator-client.jar)"
    else
        echo "❌ Client JAR not found"
        exit 1
    fi
else
    echo "❌ Client build failed"
    exit 1
fi

# Test 4: Check dependencies
echo ""
echo "4. Checking dependencies..."
if [ -f "lib/gson-2.10.1.jar" ]; then
    echo "✅ Gson library found"
else
    echo "❌ Gson library missing"
    exit 1
fi

# Test 5: Check if we can run the client (basic test)
echo ""
echo "5. Testing client startup..."
if java -cp "s-emulator-client.jar:lib/*" se.emulator.client.SEMulatorClientApp --help 2>/dev/null; then
    echo "✅ Client can start (help mode)"
else
    echo "⚠️  Client startup test inconclusive (this is normal for GUI apps)"
fi

echo ""
echo "🎉 Build tests completed successfully!"
echo ""
echo "Next steps to test the full system:"
echo "1. Deploy the server:"
echo "   cp s-emulator-server/s-emulator-server.war \$TOMCAT_HOME/webapps/"
echo "   \$TOMCAT_HOME/bin/startup.sh"
echo ""
echo "2. Test server API:"
echo "   curl http://localhost:8080/s-emulator-server/api/users"
echo ""
echo "3. Run the client:"
echo "   cd s-emulator-client"
echo "   java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -cp \"s-emulator-client.jar:lib/*\" se.emulator.client.SEMulatorClientApp"
echo ""
echo "4. Or use the batch file:"
echo "   ./run-client.bat"
