#!/bin/bash
set -e

echo "=== VibeStudio Termux Standalone Build ==="

# 1. Ensure android.jar (API 30+) exists in libs/
mkdir -p libs/deps bin obj libtermux_classes compiled_res

if [ ! -f "libs/android.jar" ]; then
    echo "=== Downloading Android API 30 platform jar ==="
    curl -sL "https://dl.google.com/android/repository/platform-30_r03.zip" -o libs/platform-30.zip
    unzip -q -j libs/platform-30.zip "android-11/android.jar" -d libs/
    rm -f libs/platform-30.zip
fi

# 2. Download dependencies via Maven
echo "=== Resolving dependencies via Maven ==="
mvn dependency:copy-dependencies -DoutputDirectory=libs/deps -q

# 3. Extract classes.jar from all AAR files in libs/deps
echo "=== Extracting classes.jar from AAR dependencies ==="
for aar in libs/deps/*.aar; do
    if [ -f "$aar" ]; then
        name=$(basename "$aar" .aar)
        unzip -q -o "$aar" classes.jar -d libs/deps/
        mv libs/deps/classes.jar "libs/deps/${name}.jar"
    fi
done

# Deduplicate conflicting annotation jars
rm -f libs/deps/annotation-1.1.0.jar libs/deps/annotation-1.0.0.jar libs/deps/annotation-1.2.0.jar libs/deps/annotation-1.6.0.jar

# Build Classpath for Kotlin / Java
CLASSPATH="libs/android.jar"
for j in libs/deps/*.jar; do
    CLASSPATH="$CLASSPATH:$j"
done

# 4. Compile LibTermux Kotlin sources
echo "=== Compiling LibTermux Kotlin sources ==="
rm -rf libtermux_classes
mkdir -p libtermux_classes

kotlinc -d libtermux_classes   -classpath "$CLASSPATH"   $(find libtermux-android/core/src/main/kotlin -name "*.kt")   $(find libtermux-android/terminal-view/src/main/kotlin -name "*.kt")

jar cvf libs/libtermux.jar -C libtermux_classes . > /dev/null
echo "=== LibTermux Compiled Successfully ==="

# 5. Packaging resources with AAPT2
echo "=== Packaging resources with AAPT2 ==="
rm -rf bin obj compiled_res
mkdir -p bin obj compiled_res

aapt2 compile --dir app/src/main/res -o compiled_res/
aapt2 link -o bin/app.unsigned.apk   -I libs/android.jar   --manifest app/src/main/AndroidManifest.xml   --java app/src/main/java   compiled_res/*.flat   --auto-add-overlay

# 6. Compiling VibeStudio Java sources
APP_CLASSPATH="$CLASSPATH:libs/libtermux.jar"

echo "=== Compiling VibeStudio Java sources ==="
javac -source 1.8 -target 1.8 -d obj   -classpath "$APP_CLASSPATH"   app/src/main/java/com/vibestudio/app/*.java

# 7. Converting bytecode to DEX (d8)
echo "=== Converting bytecode to DEX (d8) ==="
jar cvf bin/app_classes.jar -C obj . > /dev/null

DEX_LIBS="libs/libtermux.jar"
for j in libs/deps/*.jar; do
    DEX_LIBS="$DEX_LIBS $j"
done

d8 --min-api 24 --lib libs/android.jar --output bin/ bin/app_classes.jar $DEX_LIBS

echo "=== Adding classes.dex to APK ==="
cd bin && aapt add app.unsigned.apk classes.dex > /dev/null && cd ..

echo "=== Signing & Aligning APK ==="
jarsigner -keystore debug.keystore -storepass android -keypass android bin/app.unsigned.apk androiddebugkey > /dev/null

rm -f bin/app.aligned.apk
zipalign -v -p 4 bin/app.unsigned.apk bin/app.aligned.apk > /dev/null

if [ ! -f debug.keystore ]; then
  keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

apksigner sign --ks debug.keystore --ks-pass pass:android --min-sdk-version 1 --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out bin/VibeStudio.apk bin/app.aligned.apk

echo "=== VibeStudio Build Complete! APK generated at bin/VibeStudio.apk ==="
