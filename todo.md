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
- [x] Processed `SYMLINKS.txt` and core symlinks (`sh`, `rm`, `tar`, `diff`, `dpkg-deb`, `start-stop-daemon`, and busybox applets) using POSIX `android.system.Os.symlink` and `android.system.Os.remove` in `OnboardingActivity.java` without modifying the `libtermux-android` submodule.
- [x] Verified full build with `./build.sh` generating `bin/VibeStudio.apk` without compilation errors.
- [x] Preserved required terminal background color (`#1E1E2E`) and kept `libtermux-android` submodule completely untouched.
