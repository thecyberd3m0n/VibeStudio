# TerminalView Styling, Alignment & Input Fix Plan

## 1. Context & Diagnosis
Our `TerminalView` currently exhibits broken styling, cursor mispositioning, and fails to accept user input from soft keyboards.

When compared against Termux (`~/src/termux-app`), three key issues were identified:
1. **Cursor Mispositioning & Character Alignment:**
   - Text rendering calculates baseline y using `row * charHeight * lineSpacing` without taking font metrics into account (`ascent` / `descent`).
   - Cursor bounding rectangle is offset from character cells because height and baseline calculations mismatch.
2. **Input Interception / Failure to Receive Input:**
   - Soft keyboard input creates a `BaseInputConnection` with `inputType = TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_NO_SUGGESTIONS`, causing keyboards like Gboard, Samsung, or Hacker's Keyboard to suppress raw key events or commit text improperly.
   - `commitText` and key dispatching are not properly binding back to the terminal session.
3. **Layout & View Attributes:**
   - Missing focus and scrollbar XML attributes (`defaultFocusHighlightEnabled="false"`, `focusableInTouchMode="true"`).
   - Missing horizontal margins to prevent edge character truncation.

---

## 2. Planned Changes

> **Constraint Note:** Keeping current background color (`#1E1E2E`).

### A. TerminalView Rendering & Alignment Fixes
File: `libtermux-android/terminal-view/src/main/kotlin/com/libtermux/view/TerminalView.kt`

1. **Preserve Color Scheme:**
   - Retain background color: `terminalBackgroundColor = Color.parseColor("#1E1E2E")`.
2. **Correct Text & Cursor Baseline Calculations:**
   - Calculate precise font metrics using `fontSpacing` and `ascent`.
   - Adjust `onDraw()` text baseline Y coordinate so characters sit properly on grid rows.
   - Compute cursor top/bottom bounds using font ascent and line height so cursor block aligns exactly on the active input character cell.
3. **Fix Soft Keyboard Input Handling:**
   - Update `onCreateInputConnection`:
     - Use `InputType.TYPE_NULL` (or `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`) matching Termux standard to prevent keyboards from swallowing key events.
     - Set `outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN`.
   - Update `BaseInputConnection`:
     - Correctly process `commitText()`, `deleteSurroundingText()`, and key events, piping text directly into the session executor.

---

### B. Application XML Layout Fixes (`VibeStudio`)
File: `app/src/main/res/layout/fragment_terminal.xml`

1. **Add View Attributes & Horizontal Padding/Margin:**
   - Add `android:focusable="true"`, `android:focusableInTouchMode="true"`, `android:defaultFocusHighlightEnabled="false"`.
   - Add horizontal margin (`3dp`) inside terminal container matching Termux root relative layout.

---

### C. Application Fragment Integration (`VibeStudio`)
File: `app/src/main/java/com/vibestudio/app/fragments/TerminalFragment.java`

1. Request view focus on touch event to ensure the soft keyboard pops up.
2. Bind input handling methods on session attach.

---

## 3. Verification & Testing

1. Run `./build.sh` to compile updated `libtermux-android` and `VibeStudio.apk`.
2. Install via `termux-open bin/VibeStudio.apk`.
3. Verify terminal screen rendering:
   - Confirm cursor aligns accurately over character positions.
   - Confirm soft keyboard input works cleanly.
