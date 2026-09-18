# Port official Termux Terminal Engine into VibeStudio

## Completed
- [x] Extracted terminal emulator source code (`com.termux.terminal`, `com.termux.view`, and text selection classes) into `app/src/main/java/com/termux/`.
- [x] Compiled JNI PTY helper (`termux.c`) into native library `app/src/main/jniLibs/arm64-v8a/libtermux.so`.
- [x] Added required drawable resources (`text_select_handle_left_material.xml`, `text_select_handle_right_material.xml`) and string resources.
- [x] Updated `fragment_terminal.xml` layout to use `com.termux.view.TerminalView`.
- [x] Updated `TerminalFragment.java` to initialize `TerminalSession` with native PTY support and implement `TerminalSessionClient` and `TerminalViewClient`.
- [x] Implemented system clipboard integration in `TerminalFragment.java` (`onCopyTextToClipboard` and `onPasteTextFromClipboard`) so copy/paste works between the terminal view and Android system clipboard.
- [x] Added `fixPermissionsRecursively` helper in both `OnboardingActivity.java` and `TerminalFragment.java` to recursively enforce standard directory (`0755`) and file (`0644`/`0755`) permissions across the LibTermux prefix hierarchy (`/data/user/0/com.vibestudio.app/files/libtermux/usr`), fixing permission denied errors when reading `/etc/profile` during onboarding and terminal launch.
- [x] Added comprehensive internal logging in `TerminalFragment.java` (`TerminalSessionClient` and `TerminalViewClient` methods bound to `LogViewerService`) for enhanced debugging.
- [x] Fixed runtime `RuntimeException` by passing `Looper.getMainLooper()` to `MainThreadHandler` in `TerminalSession.java`.
- [x] Fixed `NullPointerException` in `TerminalView.updateSize()` by initializing `mRenderer` with default font settings inside `TerminalView` constructor.
- [x] Fixed `UnsatisfiedLinkError: libtermux.so not found` by updating `build.sh` to package `lib/arm64-v8a/libtermux.so` into the APK.
- [x] Verified full build with `./build.sh` generating `bin/VibeStudio.apk` without compilation errors.
- [x] Preserved required terminal background color (`#1E1E2E`) and left `libtermux-android` submodule unchanged.

## Fixing Plan: Resolve Onboarding Bootstrap & dpkg Missing Symlinks Failure (Without Submodule Modifications)

### Problem Analysis
Official Termux (`TermuxInstaller.java`) handles bootstrap symlink setup by reading `SYMLINKS.txt` (`oldPath←newPath`) and calling `android.system.Os.symlink(oldPath, newPath)` directly via JNI POSIX bindings.
When `libtermux-android` attempts to run `Runtime.exec("ln -sf ...")` during bootstrap extraction, `ln` is unavailable in Android's non-root process environment, resulting in missing symlinks (`sh`, `rm`, `tar`, `diff`, `dpkg-deb`, `start-stop-daemon`). As a result, `dpkg` fails during onboarding script execution.

### Action Plan
1. **Revert Submodule**:
   - Keep `libtermux-android` submodule pristine and unmodified.

2. **Implement POSIX Symlink & Permissions Fix in VibeStudio (`OnboardingActivity.java`)**:
   - In `OnboardingActivity.java`, after `libTermux.install()` finishes, parse `SYMLINKS.txt` if present or verify/recreate all missing core symlinks (`sh` -> `dash`/`bash`, `dpkg-deb` -> `dpkg`, `start-stop-daemon` -> `dpkg`, `rm`/`tar`/`diff`/applets -> `busybox`) using `android.system.Os.symlink` and `android.system.Os.remove`.
   - Perform string replacement on hardcoded Termux paths (`/data/data/com.termux` -> `/data/data/com.absent` or sandboxed `$PREFIX`) in binary/ELF files.

3. **Configure Environment in `vibestudio-bootstrap.sh` & ProcessBuilder**:
   - Export `PATH="$PREFIX/bin:$PREFIX/bin/applets:/system/bin:/system/xbin:$PATH"` in `vibestudio-bootstrap.sh` and Java `ProcessBuilder`.
   - Export `DPKG_ADMINDIR="$PREFIX/var/lib/dpkg"` and `TERMUX_PKG_NO_MIRROR_SELECT="true"`.
   - Ensure pre-creation of `$PREFIX/var/lib/dpkg` state directories and `apt.conf`.

4. **Verification**:
   - Compile using `./build.sh` and verify APK generation.
