# Build Matrix

`v0.2.0` 开始，Release 工作流覆盖以下目标。

## Desktop

| Platform | Architecture | Artifact |
| --- | --- | --- |
| Windows | x86_64 | `ArchIToken-VPN-Client-windows-x86_64.exe` |
| Windows | arm64 | `ArchIToken-VPN-Client-windows-arm64.exe` |
| Linux | x86_64 | `ArchIToken-VPN-Client-linux-x86_64` |
| Linux | arm64 | `ArchIToken-VPN-Client-linux-arm64` |
| macOS | x86_64 | `ArchIToken-VPN-Client-macos-x86_64` via `macos-15-intel` |
| macOS | arm64 | `ArchIToken-VPN-Client-macos-arm64` |

## Mobile

| Platform | Coverage | Artifact |
| --- | --- | --- |
| Android | universal debug APK | `ArchIToken-VPN-Client-android-universal-debug.apk` |
| iOS | unsigned simulator build | `ArchIToken-VPN-Client-ios-simulator.zip` |

## Signing Boundary

- Windows binaries are unsigned until a code-signing certificate is configured.
- macOS binaries are unsigned and not notarized until Apple Developer signing is configured.
- Android debug APK is for internal validation. Release APK/AAB needs signing keys.
- iOS true device install/TestFlight requires Apple Developer certificates and provisioning profiles.
