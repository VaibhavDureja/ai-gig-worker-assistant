# Validation and release checklist

## Verified from source

- Android project contains its Gradle settings, manifest, native Kotlin source, and README
- `MediaProjection` is requested through Android's user-facing capture consent prompt
- Foreground service declares `mediaProjection` and includes a notification Stop action
- The local HTTP viewer uses a random session token and limits concurrent streams
- No code path uploads screen images to an external service

## Not verified yet

- Android Studio/Gradle compilation (the build environment used for this repository did not contain Android SDK 35)
- Successful media capture on physical Android 10 through 16 devices
- Performance, battery usage, thermal behaviour, low-latency video smoothness
- Car-browser compatibility, OEM features, CarPlay, Android Auto or head-unit support
- YouTube protected playback, audio, remote interaction or payment handling

## First real-device tests

1. Build the project in Android Studio; record any compiler errors or SDK-version issues.
2. Install on a phone. Approve capture consent; confirm the foreground notification appears.
3. Open the displayed URL on a laptop on the same Wi-Fi.
4. Confirm a stream appears and stops when Stop is pressed on the phone.
5. Test wrong/missing session tokens: server must respond `403` and never serve frames.
6. Test screen rotation, lock/unlock, capture permission revocation and backgrounding.
7. Test Wi-Fi hotspot and a second trusted device.
8. Confirm that protected content is never deliberately bypassed.
9. Test that the viewer cannot control the phone.
10. Test a car **only while parked** and only if its system already provides an independent browser. Do not assume standard CarPlay supports this.

## Known engineering concerns

- The demo uses MJPEG over unencrypted local HTTP. A hostile LAN participant could potentially sniff viewer traffic; the random token mitigates guessing but not passive interception. Use only a trusted private local network.
- It shows the phone screen to anyone holding the session URL. Stop sharing before opening sensitive apps.
- Device and app manufacturers may black out protected content.
- Some phones aggressively restrict background services or block hotspot client communication.
- Production should move to a secure lower-latency transport with explicit pairing and robust permission/session lifecycle management.
