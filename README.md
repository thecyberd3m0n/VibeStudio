# VibeStudio

**VibeStudio** is a native Android application designed to provide a lightweight, self-contained AI-driven development environment directly on mobile devices.

> **Note:** VibeStudio is currently a **Work in Progress (WIP)** and under active development.

---

## Features

- **Models Management**: Interface to configure, monitor, and switch between local or remote AI models.
- **MCP Integration**: Connect and manage Model Context Protocol (MCP) tool providers and services.
- **Built-in Terminal**: Direct terminal interface for executing shell commands and scripts.
- **Interactive AI Chat**: Chat experience connected with your active AI models and tools.
- **Permissions Management**: Granular control over system permissions and access scopes.
- **Native Stepper Onboarding**: Guided setup walkthrough for first-time app configuration.

---

## Architecture & Layout

VibeStudio now follows the standard Android Gradle Plugin (AGP) structure:

- `app/src/main/java/` - Java source files
- `app/src/main/res/` - UI layouts, strings, and resources
- `app/src/main/AndroidManifest.xml` - App configuration
- `build.gradle` / `app/build.gradle` - Build system configuration

## Getting Started

### 1. Clone the Repository
VibeStudio uses git submodules for its core terminal functionality. Clone recursively to ensure all components are downloaded:

```bash
git clone --recursive https://github.com/AeonCoreX-Lab/VibeStudio.git
```

If you have already cloned the repository without submodules, run:
```bash
git submodule update --init --recursive
```

---

## Building & Development

### 1. In Android Studio (Recommended)
1. Open Android Studio.
2. Select **Open** and navigate to the `VibeStudio` root directory.
3. Wait for the Gradle sync to finish.
4. Click **Run** or use the **Build** menu to generate the APK.

### 2. In Termux (CLI Build)
VibeStudio is designed to be buildable directly on Android using Termux.

#### A. Building with Gradle (Easiest)
Install the required packages and run the Gradle wrapper:
```bash
pkg install openjdk-17 gradle
./gradlew assembleDebug
```
The resulting APK will be located in `app/build/outputs/apk/debug/`.

#### B. Native Build (Legacy/Advanced)
For developers who prefer using native tools (`aapt`, `javac`, `kotlinc`, `dx`) without Gradle, custom scripts are provided:
- `compile_libtermux.sh`: Compiles the Kotlin core library.
- `build.sh`: Packages and signs the final application.

> [!WARNING]
> These scripts use hardcoded paths for `android.jar` and specific toolchains. You may need to edit them to match your local Termux environment.

---

## License

Work in Progress.
