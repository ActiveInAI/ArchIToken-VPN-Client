# Build Matrix

`v0.4.0` 开始，Release 工作流覆盖以下目标。

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
| Android | debug APK | `ArchIToken-VPN-Client-android-universal-debug.apk` |
| Android | unsigned release APK | `ArchIToken-VPN-Client-android-universal-release-unsigned.apk` |
| Android | unsigned release AAB | `ArchIToken-VPN-Client-android-universal-release-unsigned.aab` |
| iOS | unsigned simulator build | `ArchIToken-VPN-Client-ios-simulator.zip` |
| iOS | Xcode source package | `ArchIToken-VPN-Client-ios-source.zip` |

## Mobile VPN Controls

| Platform | Coverage |
| --- | --- |
| Android | `VpnService` declaration, VPN permission flow, start/stop service entry points, Xray outbound persistence, runtime guard before traffic takeover |
| iOS | `NETunnelProviderManager` profile install/start/stop flow, Xray outbound persistence, PacketTunnelProvider source template |

## Signing Boundary

- Windows binaries are unsigned until a code-signing certificate is configured.
- macOS binaries are unsigned and not notarized until Apple Developer signing is configured.
- Android debug APK is for internal validation. Release APK/AAB is generated unsigned until a project-owned private keystore is configured.
- iOS simulator artifacts are installable only on simulators. True device install/TestFlight requires Apple Developer certificates, provisioning profiles, and Network Extension entitlements.
