# ParkCast — iPhone-to-CarPlay product specification

**Status: planned CarPlay integration, NOT implemented.**  
**Platform target:** iOS 27+ (with an eligible CarPlay video app entitlement and a vehicle supporting video-in-car).  
**Name:** ParkCast is the project's working name; "Carcast" is used as an example of the desired user experience.

## The user's exact intended flow

1. Install the ParkCast iPhone app.
2. Open ParkCast on the **iPhone** and tap **Start screen mirroring**.
3. Approve Apple's screen-sharing picker.
4. Connect the iPhone to the vehicle's Apple CarPlay system.
5. On the CarPlay home screen, tap the **ParkCast app icon**, like opening Spotify.
6. Inside ParkCast on CarPlay, display the active iPhone screen capture.
7. Navigate or change content on the **iPhone** (e.g. YouTube or Safari); the vehicle's ParkCast view follows the screen when permitted.
8. Stop screen sharing on the iPhone. Hide video or switch to audio-only whenever the vehicle indicates playback is unavailable (including while driving).

This is **not** automatic projection to the CarPlay home screen and is **not** merely a browser page on a second device. Controls on the vehicle's display are not intended to remotely operate other iPhone apps.

## Requirements before a real-car CarPlay icon can appear

- Obtain Apple's **CarPlay video entitlement** (`com.apple.developer.carplay-video`) and the corresponding signed provisioning profile.
- Use a **CarPlay scene** and Apple-provided **CarPlay framework templates** for the car-facing launch/browsing UI.
- Target a car that supports Apple's **video-in-car** capability. The icon for a video-only CarPlay app will not appear in vehicles without that capability.
- Confirm playback availability using `CPSessionConfiguration.supportsVideoPlayback`; respect transitions to audio-only or hidden video when the car does not permit video.
- Implement compliant **AirPlay video delivery** for the supported video-capable CarPlay display.
- Capture selected iPhone screen content with the system-approved **ScreenCaptureKit** picker. Protected/DRM content may be unavailable and must not be bypassed.
- Test video, audio synchronization (if later supported), app-switching, disconnect/reconnect, privacy, foreground/background states, and support for the targeted vehicle.

**Important:** The existing ParkCast iOS prototype is a **screen-to-browser MJPEG streamer**, which does not satisfy this specification. It has NO CarPlay scene, CarPlay entitlement, or AirPlay-to-CarPlay playback implementation. A successful iOS compilation is not proof of CarPlay support.

## Proposed architecture (subject to Apple's entitlement and review)

- iPhone-side SwiftUI onboarding, capture consent, source selection, and Stop Sharing.
- ScreenCaptureKit captures user-selected content.
- Convert captured frames into a video playback stream using Apple media frameworks; validate an AirPlay-compatible delivery path.
- Dedicated CarPlay scene with a **ParkCast** icon and supported templates: "Start on iPhone" or "Play mirrored screen" based on current capture state.
- Only present video where CarPlay reports allowed parked playback; enforce automatic interruption.
- Local first and privacy-preserving. No remote cloud relay, advertising, or payment gate in v0.1.

## Acceptance criteria

- [ ] iOS project compiles on current Xcode and has a working iPhone screen-sharing flow
- [ ] Apple video CarPlay entitlement approved and provisioned
- [ ] ParkCast icon appears in CarPlay **on an eligible video-in-car vehicle**
- [ ] Tapping ParkCast opens a CarPlay-native app screen
- [ ] Starting capture on the iPhone renders actual permitted content in ParkCast on the car screen
- [ ] Switching from one iPhone app to another updates the mirrored screen
- [ ] Video stops when the vehicle does not permit video playback or the user stops sharing
- [ ] Test on at least one real eligible iPhone/car combination
- [ ] Document compatibility and limitations before public distribution

## References

- Apple WWDC26, *Rev up your CarPlay app*: https://developer.apple.com/videos/play/wwdc2026/212/
- Apple's CarPlay guidance: https://developer.apple.com/carplay/
- Apple's video entitlement: https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.carplay-video
- ScreenCaptureKit for iPhone: https://developer.apple.com/documentation/screencapturekit/capturing-screen-content-on-ios
- Apple iPhone User Guide, video with CarPlay: https://support.apple.com/en-gb/guide/iphone/xykemxkw6he5/27/ios/27
- Example of a commercial product that advertises this experience (claims are not independent verification): https://cartv.app/

## Not included

Bypassing vehicle safety lockouts, DRM circumvention, pretending all ordinary CarPlay systems support video, or describing the unfinished prototype as fully working in a vehicle.
