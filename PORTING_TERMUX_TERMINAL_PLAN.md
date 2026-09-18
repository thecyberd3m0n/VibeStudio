# Plan: Port Official Termux Terminal Engine into VibeStudio

## Overview
Replace the simplified custom text view with the official `termux-app` terminal emulator and rendering engine (`terminal-emulator` & `terminal-view` classes from `~/src/termux-app`).

---

## 1. Source File Migration
Copy the real Termux emulator, view, and text selection packages directly into `app/src/main/java/`:

* **`com.termux.terminal.*`** (from `termux-app/terminal-emulator`):
  - `TerminalSession`, `TerminalEmulator`, `TerminalBuffer`, `TerminalRow`, `TextStyle`, `TerminalColors`, `TerminalColorScheme`, `WcWidth`, `ByteQueue`, `KeyHandler`, `JNI`.
* **`com.termux.view.*`** (from `termux-app/terminal-view`):
  - `TerminalView`, `TerminalRenderer`, `GestureAndScaleRecognizer`, `CursorController`, `TextSelectionCursorController`, `TextSelectionHandleView`.

---

## 2. Native PTY JNI Layer Integration
* Utilize Termux's C/C++ PTY engine (`termux.c` / `JNI.java`) compiled into `libtermux.so` or bridged via `libtermux-android`'s `pty_helper.cpp`.
* Wire `TerminalSession` to spawn shells through true native PTY allocation (`openpty` / `createSubprocess`).

---

## 3. Font & Visual Styling Alignment
* Extract and bundle Termux's official default monospace font (or JetBrains Mono/DejaVu Sans Mono) into `app/src/main/assets/fonts/`.
* Configure `TerminalRenderer` to calculate exact integer font baselines and cell width/height metrics via `Paint.getFontMetricsInt()`.
* Configure the default color palette matching Termux (background `#1E1E2E`, cursor block, ANSI 16-color palette).

---

## 4. UI & Input Connection Wiring
* Update `fragment_terminal.xml` to use `com.termux.view.TerminalView`.
* Wire `TerminalFragment` to create a `TerminalSession` with default shell parameters and attach it to `TerminalView`.
* Retain workspace constraints: keep `libtermux-android` submodule unmodified, placing all ported code in `app/src/main/java/com/termux/`.
