# Mobile VPN Tunnel

## Android

The Android app now contains the platform VPN control surface and runtime launcher:

- `ArchITokenVpnService` is declared with `android.permission.BIND_VPN_SERVICE`.
- The UI requests Android VPN permission through `VpnService.prepare`.
- The selected VLESS Reality node is saved as Xray outbound JSON.
- The app exposes start/stop controls.
- Private CI can inject an Android runtime zip into APK assets.
- The app copies the matching ABI runtime into the private app sandbox.
- The service generates a complete Xray config with a local SOCKS inbound.
- The service starts either `runner` or `xray` + `tun2socks`.
- The service excludes the app UID from the VPN route to avoid proxy feedback loops.

Expected private runtime locations inside the app sandbox:

```text
files/architoken-mobile/runner
```

or:

```text
files/architoken-mobile/xray
files/architoken-mobile/tun2socks
files/architoken-mobile/tun2socks.args
```

Private APK assets can also be injected with this layout:

```text
android/src/main/assets/architoken-mobile/arm64-v8a/xray
android/src/main/assets/architoken-mobile/arm64-v8a/tun2socks
android/src/main/assets/architoken-mobile/arm64-v8a/tun2socks.args
android/src/main/assets/architoken-mobile/x86_64/xray
android/src/main/assets/architoken-mobile/x86_64/tun2socks
```

`tun2socks.args` is optional. It supports placeholders:

```text
{tun_fd}
{xray_config}
{socks}
```

Default tun2socks command arguments:

```text
-device fd://{tun_fd} -proxy socks5://127.0.0.1:10808
```

The public repository does not bundle those binaries because they are platform-specific and may carry upstream licensing, export, signing, and store-review obligations. If no runtime is installed, Android reports the missing runtime and does not establish the VPN interface.

Production forwarding options:

- Package an ArchIToken-owned mobile runner that combines Xray outbound config with a tun2socks bridge.
- Fork v2rayNG under GPL-3.0-or-later and rebrand it as the Android client.
- Keep this app as the configuration shell and hand off the generated subscription/link to v2rayNG.

## iOS

The iOS app now contains the NetworkExtension control surface and PacketTunnel target:

- `TunnelManager` installs a `NETunnelProviderManager` profile.
- The selected VLESS Reality node is saved into the provider configuration.
- The UI exposes install/start/stop controls.
- `ios/PacketTunnel` contains a PacketTunnelProvider target.
- `PacketRuntime` is the private runner hook. Production builds should link `ArchITokenPacketCore.xcframework` or replace that hook with the chosen packet runner.
- `PacketRunner.swift` is a public placeholder. Private CI can overwrite `ios/PacketTunnel/PacketRunner.swift` from `ARCHITOKEN_PACKET_CORE_ZIP_BASE64` with a real `ArchITokenPacketCoreRunner` implementation.

True iPhone/iPad VPN operation requires:

- Apple Developer Program membership.
- Bundle IDs for the app and packet tunnel extension.
- Network Extension entitlement with `packet-tunnel-provider`.
- Signed provisioning profiles for both targets.
- A packet runner inside the PacketTunnel extension that can forward packets through Xray-compatible outbound logic.

The public CI builds the simulator app and packages the Xcode source. When Apple signing secrets and a private PacketCore zip are supplied, CI can also build a signed IPA.

## Safety Boundary

Do not enable a system VPN interface until a real packet runner is present. A TUN interface without packet forwarding will capture traffic and break device networking.
