# ParkCast for iPhone — iOS 27 prototype

A free experimental screen-sharing app using Apple's iOS 27 ScreenCaptureKit and system permission picker. It serves reduced-frame-rate JPEG images over a token-protected local Wi-Fi browser link.

## Intended CarPlay product (not yet built)

The intended experience is: open ParkCast on the iPhone, approve screen sharing, connect to CarPlay, **tap the ParkCast icon on the car display**, and view the selected iPhone screen inside the CarPlay app while the vehicle permits parked video. The current browser viewer is a technical prototype, not this CarPlay experience.

The exact technical and entitlement requirements are documented in [CARPLAY_REQUIREMENTS.md](CARPLAY_REQUIREMENTS.md). Apple’s video CarPlay app entitlement and a vehicle with video-in-car support are required before we can claim this works in a real car.

## Scope and limitations

- **iOS 27+ on a physical iPhone** required. Compilation on a simulator is not a real capture test.
- **Video only**; no audio, remote touch, or protected-video bypass.
- This is **not a CarPlay app** and won't enable YouTube inside standard CarPlay.
- The private viewer is plain HTTP, **not encrypted**. Use trusted home Wi-Fi only. Anyone on that network with the private URL can view your screen.
- The app declares Apple's `screen-capture` background mode, but actual background capture still requires device testing.
- No payments, tracking, cloud storage, or App Store distribution.

## Build for free on a Mac

Install Xcode 27 and XcodeGen (`brew install xcodegen`).
From this `ios` folder, run `xcodegen generate`, open `ParkCastIOS.xcodeproj`, choose a physical iPhone running iOS 27+, and enable automatic signing with your Apple developer team. Change the bundle identifier if necessary. Build and run.

Connect your phone and viewer to the same Wi-Fi. Choose your screen through Apple's picker, open the displayed URL on a browser, and stop sharing from ParkCast when done.

GitHub Actions compiles against the iOS 27 physical-device SDK with code signing disabled. This confirms source compilation, but **does not create an installable iPhone IPA** or prove screen sharing works on hardware.

## Structure

- `ParkCastIOSApp.swift`, `HomeView.swift`: SwiftUI app and interface.
- `ScreenSharingController.swift`: ScreenCaptureKit picker and image conversion.
- `LocalMJPEGServer.swift`: local HTTP stream and private token.
- `LocalAddress.swift`: Wi-Fi IPv4 detection.
- `project.yml`: XcodeGen project definition.

References: https://developer.apple.com/documentation/screencapturekit/capturing-screen-content-on-ios and https://developer.apple.com/carplay/

**Use only when stationary.** Do not operate car video content while driving.
