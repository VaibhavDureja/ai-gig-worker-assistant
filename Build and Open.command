#!/bin/bash
set -euo pipefail
cd -- "$(dirname -- "$0")"

if [[ "$(uname -s)" != Darwin ]]; then
    echo 'LidLoop requires macOS 13 or later.'
    exit 1
fi

if ! xcode-select -p >/dev/null 2>&1; then
    echo 'Install the Apple Command Line Tools, then run this script again.'
    xcode-select --install
    exit 1
fi

if ! sdk="$(xcrun --sdk macosx --show-sdk-path)" ||
   ! sdk_version="$(xcrun --sdk macosx --show-sdk-version)" ||
   ! xcrun --sdk macosx --find swiftc >/dev/null; then
    echo 'Update Apple Command Line Tools or Xcode in Software Update, then run this script again.'
    exit 1
fi

architecture="$(uname -m)"
if [[ "$(sysctl -n hw.optional.arm64 2>/dev/null || true)" == 1 ]]; then
    architecture=arm64
fi
echo "Building LidLoop for $architecture with the macOS $sdk_version SDK."

staging="$(mktemp -d "$PWD/.lidloop-build.XXXXXX")"
trap 'rm -rf -- "$staging"' EXIT
app="$staging/LidLoop.app"
mkdir -p "$app/Contents/MacOS"
cp Info.plist "$app/Contents/Info.plist"
xcrun --sdk macosx swiftc -parse-as-library -swift-version 5 -O -sdk "$sdk" \
    -target "${architecture}-apple-macosx13.0" \
    LidLoop.swift -o "$app/Contents/MacOS/LidLoop"
codesign --force --sign - "$app"
codesign --verify --strict "$app"
ditto "$app" "$PWD/LidLoop.app"
open "$PWD/LidLoop.app"
