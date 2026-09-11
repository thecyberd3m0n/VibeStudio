#!/bin/sh
set -e

# Note for Termux Users:
# Since the migration to AndroidX, you must ensure your classpath includes
# the new AndroidX libraries. The old android-support-v4.jar is no longer sufficient
# for MainActivity.java (which now uses androidx.drawerlayout.widget.DrawerLayout).
#
# Recommended: Install gradle in Termux and use './gradlew assembleDebug' instead.

ANDROID_JAR="/data/data/com.termux/files/home/android-build-tooling/buildAPKs/sources/applications/agehua/achartengine/agehua-achartengine-2a318ee/achartengine/lib/android.jar"
SUPPORT_V4="/data/data/com.termux/files/home/android-build-tooling/buildAPKs/sources/applications/alexandraHospital/1024Histoires/alexandraHospital-1024Histoires-fa3682b/libs/android-support-v4.jar"

echo "=== Packaging resources with AAPT ==="
rm -rf bin obj
mkdir -p bin obj

aapt package -f -m \
  -J app/src/main/java \
  -M app/src/main/AndroidManifest.xml \
  -S app/src/main/res \
  -I "$ANDROID_JAR" \
  -F bin/app.unsigned.apk

echo "=== Compiling Java sources ==="
javac -source 1.8 -target 1.8 -d obj \
  -classpath "$ANDROID_JAR:$SUPPORT_V4" \
  app/src/main/java/com/vibestudio/app/*.java

echo "=== Converting bytecode to DEX ==="
dx --dex --output=bin/classes.dex obj/ "$SUPPORT_V4"

echo "=== Adding classes.dex to APK ==="
aapt add bin/app.unsigned.apk bin/classes.dex

echo "=== Zip-aligning APK ==="
zipalign -v -p 4 bin/app.unsigned.apk bin/app.aligned.apk > /dev/null

echo "=== Signing APK ==="
if [ ! -f debug.keystore ]; then
  keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi
apksigner sign --ks debug.keystore --ks-pass pass:android --out bin/VibeStudio.apk bin/app.aligned.apk

echo "=== Build Complete ==="
