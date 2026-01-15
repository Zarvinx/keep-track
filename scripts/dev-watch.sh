#!/bin/bash
# Development watch script for Episodes app
# Watches for changes and auto-rebuilds/reinstalls

echo "🔍 Starting development watch mode..."
echo "📱 Make sure your device is connected (adb devices)"
echo "💡 Press Ctrl+C to stop"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected!"
    echo "Connect a device or start an emulator first."
    exit 1
fi

# Initial build and install
echo "🔨 Initial build..."
./gradlew installStandardDebug

if [ $? -eq 0 ]; then
    echo "✅ Initial install complete!"
    echo ""
    echo "👀 Watching for changes in app/src/..."
    echo ""
else
    echo "❌ Initial build failed"
    exit 1
fi

# Watch for changes (requires inotify-tools: sudo apt install inotify-tools)
if command -v inotifywait &> /dev/null; then
    while true; do
        inotifywait -r -e modify,create,delete \
            --exclude '(\.gradle|build|\.git)' \
            app/src/
        
        echo ""
        echo "🔄 Change detected! Rebuilding..."
        
        if ./gradlew installStandardDebug; then
            echo "✅ App updated on device!"
            echo "🔔 You may need to restart the app to see changes"
            echo ""
        else
            echo "❌ Build failed - fix errors and save again"
            echo ""
        fi
    done
else
    echo "⚠️  inotifywait not found. Install it for auto-watch:"
    echo "   sudo apt install inotify-tools"
    echo ""
    echo "For now, manually run after each change:"
    echo "   ./gradlew installStandardDebug"
fi
