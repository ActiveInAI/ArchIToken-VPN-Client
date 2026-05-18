#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$ROOT/build/ios-signed"
KEYCHAIN="$BUILD_DIR/architoken-signing.keychain-db"
PROFILE_DIR="$HOME/Library/MobileDevice/Provisioning Profiles"

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "Missing required environment variable: $name" >&2
    exit 1
  fi
}

require_env APPLE_TEAM_ID
require_env APPLE_CERTIFICATE_BASE64
require_env APPLE_CERTIFICATE_PASSWORD
require_env APPLE_PROVISIONING_PROFILE_BASE64
require_env APPLE_TUNNEL_PROVISIONING_PROFILE_BASE64

mkdir -p "$BUILD_DIR" "$PROFILE_DIR"

echo "$APPLE_CERTIFICATE_BASE64" | base64 --decode > "$BUILD_DIR/signing.p12"
echo "$APPLE_PROVISIONING_PROFILE_BASE64" | base64 --decode > "$PROFILE_DIR/architoken-app.mobileprovision"
echo "$APPLE_TUNNEL_PROVISIONING_PROFILE_BASE64" | base64 --decode > "$PROFILE_DIR/architoken-tunnel.mobileprovision"

security create-keychain -p "" "$KEYCHAIN"
security set-keychain-settings -lut 21600 "$KEYCHAIN"
security unlock-keychain -p "" "$KEYCHAIN"
security import "$BUILD_DIR/signing.p12" -k "$KEYCHAIN" -P "$APPLE_CERTIFICATE_PASSWORD" -T /usr/bin/codesign -T /usr/bin/security
security list-keychains -d user -s "$KEYCHAIN" $(security list-keychains -d user | tr -d '"')
security set-key-partition-list -S apple-tool:,apple: -s -k "" "$KEYCHAIN"

if [ -n "${IOS_EXPORT_OPTIONS_PLIST_BASE64:-}" ]; then
  echo "$IOS_EXPORT_OPTIONS_PLIST_BASE64" | base64 --decode > "$BUILD_DIR/exportOptions.plist"
else
  cat > "$BUILD_DIR/exportOptions.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key>
  <string>development</string>
  <key>teamID</key>
  <string>$APPLE_TEAM_ID</string>
  <key>signingStyle</key>
  <string>manual</string>
</dict>
</plist>
EOF
fi

xcodebuild \
  -project "$ROOT/ios/ArchITokenVPNClient.xcodeproj" \
  -scheme ArchITokenVPNClient \
  -configuration Release \
  -sdk iphoneos \
  -archivePath "$BUILD_DIR/ArchITokenVPNClient.xcarchive" \
  DEVELOPMENT_TEAM="$APPLE_TEAM_ID" \
  CODE_SIGN_STYLE=Manual \
  CODE_SIGNING_ALLOWED=YES \
  clean archive

xcodebuild \
  -exportArchive \
  -archivePath "$BUILD_DIR/ArchITokenVPNClient.xcarchive" \
  -exportPath "$BUILD_DIR/export" \
  -exportOptionsPlist "$BUILD_DIR/exportOptions.plist"

IPA="$(find "$BUILD_DIR/export" -name '*.ipa' -print -quit)"
if [ -n "$IPA" ]; then
  cp "$IPA" "$BUILD_DIR/ArchIToken-VPN-Client.ipa"
fi
