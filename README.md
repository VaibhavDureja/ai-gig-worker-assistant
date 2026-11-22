# LidLoop — Live Wallpaper for macOS

**LidLoop 1.1** is a small, native Swift menu-bar app for playing a local video as an animated desktop wallpaper on macOS. This repository contains the source recovered from the original LidLoop installer archive.

## Features

- Uses Apple's AppKit and AVFoundation; no third-party dependencies
- Plays a local MP4 or MOV behind desktop icons
- Loops playback across connected displays
- Menu-bar controls for choosing, pausing, resuming and removing a wallpaper
- Remembers the selected video and paused state
- Pauses during sleep/display sleep and attempts to restore playback after wake

**Audio is muted in this version.** The source explicitly sets `player.isMuted = true`. Sound support is not implemented.

## Build and run on a Mac

**Requirements:** macOS 13 or later, plus Xcode or Apple Command Line Tools with `swiftc`. Mac compatibility and sleep/wake behaviour have not been independently tested for every macOS release.

1. Download this branch and extract it.
2. Keep `LidLoop.swift`, `Info.plist`, and `Build and Open.command` in the same folder.
3. Open Terminal and run:

   ```bash
   cd /path/to/LidLoop_GitHub_Source
   bash "Build and Open.command"
   ```

4. The build script compiles and ad-hoc signs `LidLoop.app` locally, then opens it.
5. Click the **play-rectangle icon** in the macOS menu bar, select **Choose Video…**, and choose a local MP4/MOV video that QuickTime can open.

You can move the generated app to Applications and optionally enable it in macOS Login Items. Keep the selected video at its original location.

## Technical notes

- Targets macOS 13.0+ using the macOS SDK installed on your Mac.
- The output is **ad-hoc signed**, not Apple Developer ID signed or notarised.
- Video selection is stored as a local bookmark in `UserDefaults`, not sent to a server.
- This repository does not include any copyrighted video assets.
- Playback uses an `AVQueuePlayer` and `AVPlayerLooper` for each display.
- The script is not designed to run on Linux or Windows.

## Testing status

The original source archive was decoded and its contents inspected. The build script passes Bash syntax checking and the Info.plist parses as a property list. Actual macOS compilation, desktop playback, and lid-sleep/wake recovery must be tested on a Mac.

## Version history

- **1.1:** Recovered native Swift implementation, multi-display looping, sleep/wake handling, and menu-bar controls.

## Project structure

- `LidLoop.swift` — the application source
- `Info.plist` — macOS app metadata
- `Build and Open.command` — local build/sign/launch script
- `.gitignore` — excludes generated application and temporary files
