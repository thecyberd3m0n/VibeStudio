# DEVELOPMENT_TERMUX.MD - VibeStudio Termux Development & Debugging Guide

This guide explains how to build, test, and debug VibeStudio APK installation on Android via Termux.

---

## 1. Quick Start Commands

- **Install dependencies (one-time setup)**:
  `./install_dependencies_termux.sh`
- **Build APK**:
  `./build.sh`
- **Open / Install APK**:
  `termux-open bin/VibeStudio.apk`

---

## 2. Debugging APK Installation Issues on Android

When Android Package Installer throws generic errors like *"Pakiet wygląda na nieprawidłowy"* (*Package appears to be invalid*), use the following steps to diagnose the exact system root cause:

### Step 1: Verify APK Structure & Signatures

1. **Verify Signatures (v1, v2, v3)**:
   `apksigner verify --verbose --min-sdk-version 24 bin/VibeStudio.apk`
   Ensure both **v1 (JAR signing)** and **v2/v3 (APK Signature Scheme)** are verified.

2. **Inspect APK Contents**:
   `unzip -l bin/VibeStudio.apk`
   CRITICAL: `classes.dex` MUST be located at the root level of the ZIP archive (`classes.dex`), NOT nested under a subdirectory like `bin/classes.dex`. Subdirectories cause PackageManager to throw `INSTALL_FAILED_INVALID_APK: Package code is missing`.

3. **Check Badging / Uses-SDK**:
   `aapt dump badging bin/VibeStudio.apk`
   Verify `sdkVersion` and `targetSdkVersion` match your device API level.

---

### Step 2: Common Installation Errors & Fixes

- **`INSTALL_FAILED_INVALID_APK: Package code is missing`**:
  `classes.dex` is missing or nested in a folder inside the APK.
- **`INSTALL_PARSE_FAILED_NO_CERTIFICATES`**:
  APK is missing v1/v2 signatures. Ensure `apksigner` is run on the aligned APK with `debug.keystore`.
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**:
  An app with the same package name (`com.vibestudio.app`) was installed using a different signing key. Uninstall the previous app first.
