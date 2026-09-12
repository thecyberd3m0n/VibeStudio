# DEVELOPMENT_TERMUX.MD - VibeStudio Termux Development & Debugging Guide

This guide explains how to build, test, and debug VibeStudio APK installation on Android via Termux.

---

## 1. Quick Start Commands

- **Install dependencies (one-time setup)**:
  ```bash
  ./install_dependencies_termux.sh
  ```
- **Build APK**:
  ```bash
  ./build.sh
  ```
- **Open / Install APK**:
  ```bash
  termux-open bin/VibeStudio.apk
  ```

---

## 2. Debugging APK Installation Issues on Android

When Android Package Installer throws generic errors like *"Pakiet wygląda na nieprawidłowy"* (*Package appears to be invalid*), use the following steps to diagnose the exact system root cause:

### Step 1: Verify APK Signature & Structure

1. **Verify Signatures (v1, v2, v3)**:
   ```bash
   apksigner verify --verbose --min-sdk-version 24 bin/VibeStudio.apk
   ```
   Ensure both **v1 (JAR signing)** and **v2/v3 (APK Signature Scheme)** are verified.

2. **Inspect APK Contents**:
   ```bash
   unzip -l bin/VibeStudio.apk
   ```
   CRITICAL: `classes.dex` MUST be located at the root level of the ZIP archive (`classes.dex`), NOT nested under a subdirectory like `bin/classes.dex`. Subdirectories cause PackageManager to throw `INSTALL_FAILED_INVALID_APK: Package code is missing`.

3. **Check Badging / Uses-SDK**:
   ```bash
   aapt dump badging bin/VibeStudio.apk
   ```
   Verify `sdkVersion` and `targetSdkVersion` match your device API level.

---

### Step 2: Inspect Android System Logcat for PackageManager Errors

Run logcat via `shizuku` or `logcat` to view real-time system package installation failures:

```bash
shizuku exec "logcat -d" | grep -i "PackageManager|PackageInstaller" | tail -n 40
```

**Common Errors & Fixes**:
- **`INSTALL_FAILED_INVALID_APK: Package code is missing`**:
  `classes.dex` is missing or nested in a folder inside the APK.
- **`INSTALL_PARSE_FAILED_NO_CERTIFICATES`**:
  APK is missing v1/v2 signatures. Ensure `apksigner` is run on the aligned APK with `debug.keystore`.
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**:
  An app with the same package name (`com.vibestudio.app`) was installed using a different signing key. Uninstall the previous app first.

---

## 3. Shizuku Terminal Commands for Direct Installation

If you have Shizuku active on your phone, you can install the APK directly from CLI:

```bash
shizuku exec "pm install -r /data/data/com.termux/files/home/src/VibeStudio/bin/VibeStudio.apk"
```
