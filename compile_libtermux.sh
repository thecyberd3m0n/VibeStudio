#!/bin/sh
set -e

echo "=== Compiling LibTermux Kotlin sources ==="
mkdir -p libtermux_classes

kotlinc -d libtermux_classes \
  -classpath "/data/data/com.termux/files/home/android-build-tooling/buildAPKs/sources/applications/agehua/achartengine/agehua-achartengine-2a318ee/achartengine/lib/android.jar" \
  $(find /data/data/com.termux/files/home/src/libtermux-android/core/src/main/kotlin -name "*.kt") \
  $(find /data/data/com.termux/files/home/src/libtermux-android/terminal-view/src/main/kotlin -name "*.kt") \
  $(find /data/data/com.termux/files/home/src/libtermux-android/os/src/main/kotlin -name "*.kt")

jar cvf libtermux.jar -C libtermux_classes .
echo "=== LibTermux Compiled Successfully ==="
