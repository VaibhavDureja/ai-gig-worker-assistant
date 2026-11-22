# ParkCast

**Free, open-source Android screen-casting prototype. Working title.**

ParkCast captures an Android phone's screen (after the user accepts Android's built-in screen-sharing prompt) and serves a **view-only** stream over the local Wi-Fi network to any compatible web browser. This is the first technical building block towards a parked-car entertainment-screen product.

> **What it is not:** ParkCast does **not** integrate with Apple CarPlay or Android Auto, put YouTube in their interfaces, override OEM lockouts, stream audio, or control the phone remotely. It is **not** yet a commercial or car-certified app. A car needs an independently usable browser and network access for this proof of concept. For most standard CarPlay systems, this prototype will **not** display on the car screen.

## Features in v0.1

- Native Android app written in Kotlin (no third-party runtime dependencies)
- Android `MediaProjection` consent for screen capture
- Foreground service with clear, persistent notification and a **Stop sharing** action
- Low-bandwidth (up to 960px wide, ~8fps) JPEG stream over local HTTP
- One-time random private link per session (24 random bytes, URL-safe)
- A responsive browser viewer with **parked-only** safety notice
- No analytics, accounts, outside servers or subscription
- Up to two concurrent LAN viewers
- `MIT` source license

## Requirements

- Android phone running **Android 10 / API 29 or later**
- A second device **with an actual web browser** on the same reachable local network (laptop, tablet, or certain aftermarket Android head units)
- Android Studio with Android SDK 35, AGP 8.7.3, Java 17 and Kotlin 2.0.21
- The phone and viewer must be able to reach each other over local IPv4 TCP port **8765**; client isolation, cellular networks or some hotspot modes may block traffic

## Run it for free

1. Install Android Studio.
2. Open the **ParkCast** folder as an Android Studio project.
3. Let Gradle download the Android build tools, SDK and Kotlin plugin (first build needs internet).
4. Connect an Android phone with developer options and USB debugging enabled, or use a supported emulator.
5. Press **Run** to install ParkCast.
6. On the phone, connect to Wi-Fi or turn on a hotspot to which your second device can connect.
7. Open ParkCast; tap **Start screen sharing** and choose **Entire screen** if the Android capture dialogue offers that option.
8. On the receiving device, visit the private `http://LOCAL-IP:8765/?token=...` link shown inside the ParkCast app.
9. You should see frames from your phone on the other device. Tap **Stop sharing** to end the session.

The viewer URL contains a temporary token. Do not share it beyond trusted devices on your local network. The app does not request or transmit your browser history, contacts, location or accounts. **Everything visible on your screen during an active cast may be visible to anyone with the link.**

## Privacy and limitations

- **Use only while safely parked**. Do not operate or watch the screen while driving. This is not a driving interface and cannot independently verify the car is parked.
- **No audio** in v0.1. YouTube video may appear on a compatible receiving browser, but sound is not captured.
- DRM/protected video, secure app windows, some streaming services and passwords may be blacked out by Android. ParkCast does not bypass these protections.
- Frame quality and FPS are deliberately modest and depend on the phone, Wi-Fi and viewer. This is not guaranteed smooth video playback.
- New Android versions may prompt for single-app or entire-screen sharing; restart the session after rotation if the stream becomes mis-scaled.
- No remote touch input, CarPlay entitlement, Android Auto support, DRM circumvention, billing or background upload.
- Local **HTTP** is not TLS-encrypted. Only use on a trusted Wi-Fi/hotspot; do not expose TCP 8765 to the internet.
- It **successfully built in GitHub Actions** (Android unit tests and debug APK creation both passed): [verified CI run](https://github.com/VaibhavDureja/ai-gig-worker-assistant/actions/runs/45061179832). It has **not** yet been installed or tested on a real Android phone or car display. Review the [validation notes](docs/VALIDATION.md).

## Technical architecture

```
Android phone (MediaProjection consent)
   └── Foreground CastService
          ├── VirtualDisplay + ImageReader (RGBA)
          ├── JPEG encoder (max ~8fps, <=960px)
          └── HttpCastServer (port 8765, private session token)
                 └── Browser on same LAN (multipart MJPEG <img>)
```

The socket server accepts `GET /?token=...` for the landing page and `GET /stream?token=...` for the MJPEG stream. Every request requires the session token. There is a limit of two streaming viewers. No frames are recorded to storage or uploaded to a cloud service.

## Project roadmap

- [x] Android screen capture and local browser-viewer source implementation
- [x] Session authentication and stop-sharing notification
- [x] Documentation and safety notices
- [ ] Compile and run on physical Android devices, including Android 14/15/16
- [ ] Test rotation, background behaviour and hotspot networks
- [ ] Add audio with explicit user consent (if feasible)
- [ ] Replace MJPEG with a faster WebRTC or H.264 transport
- [ ] Investigate **official** parked-video APIs, compatible vehicle displays, and app entitlements
- [ ] Accessibility, performance profiling, automated UI tests and reliable release builds
- [ ] Consider a trial/lifetime purchase **only after** device compatibility is verified

## Why it starts on Android

Android provides a public, user-consented `MediaProjection` API for screen capture. That is not the same as permission to present arbitrary video inside Android Auto or Apple CarPlay. This repository intentionally avoids undocumented CarPlay/Android Auto bypasses and unsupported marketing claims.

References: [Android Media Projection](https://developer.android.com/media/grow/media-projection) · [Android Foreground Service Types](https://developer.android.com/develop/background-work/services/fgs/service-types) · [Apple CarPlay](https://developer.apple.com/carplay/) · [Android Auto parked apps](https://developer.android.com/training/cars/parked/auto)

## License

MIT — see [LICENSE](LICENSE).

## Automated build (GitHub Actions)

A GitHub Actions workflow is included at `.github/workflows/android-build.yml`. When uploaded to a GitHub repository's `main` branch (or the temporary `parkcast-prototype` branch), it attempts to compile an unsigned-for-distribution but **debug-signed** test APK using GitHub's free hosted CI. If the build succeeds, download the `ParkCast-debug-apk` artifact from the completed Actions run. This is a development build, not a Play Store release. Only install APKs from code and build runs you trust.

**Verified:** The Android app compiled successfully, its automated token tests passed, and GitHub uploaded a debug APK in [this successful build](https://github.com/VaibhavDureja/ai-gig-worker-assistant/actions/runs/45061179832). Physical-device playback remains untested.
