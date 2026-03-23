#!/bin/bash
# Start Android emulator in headless mode for CI/testing
# Usage: ./scripts/start-emulator-headless.sh [AVD_NAME]

set -e

AVD_NAME="${1:-Medium_Phone_API_34}"
ANDROID_SDK="${ANDROID_SDK:-$HOME/Android/Sdk}"
EMULATOR="$ANDROID_SDK/emulator/emulator"
ADB="$ANDROID_SDK/platform-tools/adb"

echo "Starting emulator: $AVD_NAME (headless mode)"

# Kill any existing emulator
pkill -9 -f "qemu-system-x86_64.*$AVD_NAME" 2>/dev/null || true
sleep 2

# Start emulator in headless mode
$EMULATOR \
    -avd "$AVD_NAME" \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -accel on \
    -no-metrics \
    2>&1 &

EMU_PID=$!
echo "Emulator started with PID: $EMU_PID"

# Wait for emulator to boot
echo "Waiting for emulator to boot..."
$ADB wait-for-device

# Wait for boot complete
BOOT_COMPLETE=""
for i in {1..120}; do
    BOOT_COMPLETE=$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [ "$BOOT_COMPLETE" = "1" ]; then
        echo "Emulator boot completed after ${i} seconds"
        break
    fi
    sleep 1
done

if [ "$BOOT_COMPLETE" != "1" ]; then
    echo "WARNING: Boot completion not detected after 120 seconds"
fi

$ADB devices
echo "Emulator is ready!"
