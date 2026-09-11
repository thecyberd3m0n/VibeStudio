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

---

## Building & Development

### 1. In Android Studio (Recommended)
Simply open the root directory in Android Studio and click **Run**. Gradle will automatically download all dependencies.

### 2. In Termux (Manual Shell Build)
If you prefer building manually on-device using `build.sh`, you will need the following libraries in your classpath:

- **Android SDK (`android.jar`)**: targetSdkVersion 34.
- **AndroidX Libraries**: Since the project was migrated to AndroidX, `javac` now requires:
    - `androidx.appcompat:appcompat`
    - `androidx.drawerlayout:drawerlayout`
    - `androidx.core:core`
    - `com.google.android.material:material`

> [!TIP]
> The easiest way to build in Termux is to install `gradle` (`pkg install gradle`) and run:
> ```bash
> ./gradlew assembleDebug
> ```
> This will manage all AndroidX dependencies automatically.

---

## License

Work in Progress.
