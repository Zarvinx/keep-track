#!/bin/bash
# Development watch script for Keep Track app
# Watches for changes and auto-rebuilds/reinstalls

printf "🔍 Starting development watch mode...\n"
printf "📱 Make sure your device is connected (adb devices)\n"
printf "💡 Press Ctrl+C to stop\n"
printf "\n"

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    printf "❌ No Android device connected!\n"
    printf "Connect a device or start an emulator first.\n"
    exit 1
fi

# Initial build and install
printf "🔨 Initial build...\n"
./gradlew installStandardDebug

if [ $? -eq 0 ]; then
    printf "✅ Initial install complete!\n"
    printf "\n"
    printf "👀 Watching for changes in app/src/...\n"
    printf "\n"
else
    printf "❌ Initial build failed\n"
    exit 1
fi

# Watch for changes (requires inotify-tools: sudo apt install inotify-tools)
if command -v inotifywait &> /dev/null; then
    while true; do
        inotifywait -r -e modify,create,delete \
            --exclude '(\.gradle|build|\.git)' \
            app/src/
        
        printf "\n"
        printf "🔄 Change detected! Rebuilding...\n"
        
        if ./gradlew installStandardDebug; then
            printf "✅ App updated on device!\n"
            printf "🔔 You may need to restart the app to see changes\n"
            printf "\n"
        else
            printf "❌ Build failed - fix errors and save again\n"
            printf "\n"
        fi
    done
else
    printf "⚠️  inotifywait not found. Install it for auto-watch:\n"
    printf "   sudo apt install inotify-tools\n"
    printf "\n"
    printf "For now, manually run after each change:\n"
    printf "   ./gradlew installStandardDebug\n"
fi
