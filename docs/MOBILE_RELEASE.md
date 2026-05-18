# Mobile Release

## Android

The public CI now builds three Android artifacts:

- `ArchIToken-VPN-Client-android-universal-debug.apk`
- `ArchIToken-VPN-Client-android-universal-release-unsigned.apk`
- `ArchIToken-VPN-Client-android-universal-release-unsigned.aab`

The release APK/AAB is intentionally unsigned in the public workflow. To publish it to Google Play or internal enterprise distribution, sign it with a private keystore outside the repository.

Required private values:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Do not commit keystores, passwords, production subscriptions, production VLESS links, UUIDs, or Reality keys.

## iOS

The public CI now builds:

- `ArchIToken-VPN-Client-ios-simulator.zip`
- `ArchIToken-VPN-Client-ios-source.zip`

The simulator app is useful for UI and parsing validation. iPhone/iPad install, TestFlight, and App Store delivery require Apple Developer signing material.

Required private values:

- `APPLE_TEAM_ID`
- `APPLE_CERTIFICATE_BASE64`
- `APPLE_CERTIFICATE_PASSWORD`
- `APPLE_PROVISIONING_PROFILE_BASE64`
- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_BASE64`

Those values should live only in GitHub Actions secrets or a private signing system.

## Current Mobile Feature Set

- Parse `vless://` Reality links.
- Decode base64/plain subscription payloads and import the first VLESS node.
- Fetch subscription URLs from the mobile UI.
- Paste from clipboard.
- Copy parsed output.
- Generate Xray outbound JSON for the imported node.
- Android: request system VPN permission, persist the selected Xray outbound, and expose start/stop controls through `VpnService`.
- Android: inject private runtime assets, generate a full Xray config, and launch `xray` + `tun2socks` or a single `runner`.
- iOS: install/start/stop a `NETunnelProviderManager` profile and ship an embedded PacketTunnel target with a private PacketCore runner hook.

The app now has the system VPN control layer. Production traffic forwarding still requires a bundled or privately supplied mobile packet runner:

- Android: provide `ANDROID_RUNTIME_ZIP_BASE64` and signing secrets for a private signed build.
- iOS: provide Apple signing secrets and `ARCHITOKEN_PACKET_CORE_ZIP_BASE64` for a signed PacketTunnel build.

The Android service refuses to take over traffic when those runtime files are missing, so a test install will not accidentally blackhole the device network.
