# Mobile VPN Tunnel

## Android

The Android app now contains the platform VPN control surface:

- `ArchITokenVpnService` is declared with `android.permission.BIND_VPN_SERVICE`.
- The UI requests Android VPN permission through `VpnService.prepare`.
- The selected VLESS Reality node is saved as Xray outbound JSON.
- The app exposes start/stop controls.
- The service checks for a private mobile runtime before creating a TUN interface.

Expected private runtime locations inside the app sandbox:

```text
files/architoken-mobile/runner
```

or:

```text
files/architoken-mobile/xray
files/architoken-mobile/tun2socks
```

The public repository does not bundle those binaries because they are platform-specific and may carry upstream licensing, export, signing, and store-review obligations. If no runtime is installed, Android reports the missing runtime and does not establish the VPN interface.

Production forwarding options:

- Package an ArchIToken-owned mobile runner that combines Xray outbound config with a tun2socks bridge.
- Fork v2rayNG under GPL-3.0-or-later and rebrand it as the Android client.
- Keep this app as the configuration shell and hand off the generated subscription/link to v2rayNG.

## iOS

The iOS app now contains the NetworkExtension control surface:

- `TunnelManager` installs a `NETunnelProviderManager` profile.
- The selected VLESS Reality node is saved into the provider configuration.
- The UI exposes install/start/stop controls.
- `ios/PacketTunnel` contains a PacketTunnelProvider template.

True iPhone/iPad VPN operation requires:

- Apple Developer Program membership.
- Bundle IDs for the app and packet tunnel extension.
- Network Extension entitlement with `packet-tunnel-provider`.
- Signed provisioning profiles for both targets.
- A packet runner inside the PacketTunnel extension that can forward packets through Xray-compatible outbound logic.

The public CI builds the simulator app and packages the Xcode source. It does not create a signed device IPA because Apple signing material must remain private.

## Safety Boundary

Do not enable a system VPN interface until a real packet runner is present. A TUN interface without packet forwarding will capture traffic and break device networking.
