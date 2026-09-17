# Plan: Moving LibTermux Installation to OnboardingActivity

## Context & Current State
- Currently, `OnboardingActivity` creates the LibTermux environment and bootstrap installation, overriding hardcoded paths and saving env configuration to SQLite database.
- `TerminalFragment` direct launches session with `LibTermux` without triggering installation.

## Implementation Steps

### Phase 1: Move LibTermux Installation to `OnboardingActivity`
- [x] **Imports & Setup**: Add LibTermux imports (`LibTermux`, `TermuxConfig`, `LogLevel`, `InstallState`, Kotlin Flow collectors) to `OnboardingActivity.java`.
- [x] **Execute Installation Flow**: Build `TermuxConfig`, initialize `LibTermux`, collect install flow events, and update status and logs.
- [x] **Environment Setup & Path Overriding**: Run APT environment setup (`setupAptEnvironment`) and override hardcoded termux paths (`overrideSTermuxPaths`).
- [x] **Database Readiness Flag**: Save path settings (`env_prefix`, `env_home`, `env_shell`) and mark environment ready using `mDbHelper.setEnvInitialized(true)`.

### Phase 2: Refactor `TerminalFragment` / `TerminalView`
- [x] **Remove Installation Logic from Fragment**: Remove bootstrap downloading, extraction, path overriding, and install flow collection from `TerminalFragment.java`.
- [x] **Direct Session Launch**: Quickly obtain/initialize `LibTermux` handle and create/attach `SessionHandle` to `mTerminalView`.

### Phase 3: DB & Startup Flow Verification
- [x] `OnboardingActivity` checks `mDbHelper.isEnvInitialized()`. If `true`, redirects directly to `MainActivity`.
- [x] `TerminalFragment` opens new terminal session on launch assuming tool & environment are ready.

### Phase 4: Build & Verification
- [x] Execute `./build.sh` to compile Kotlin/Java sources and package APK (`bin/VibeStudio.apk`).
