# Signing

## Android

Android supports test builds with debug signing, but production release builds should use a project-owned private keystore stored outside the repository.

Acceptable routes:

- GitHub Actions secrets with a private keystore.
- Google Play App Signing after uploading a signed AAB.
- A reproducible open-source distribution channel that builds from source and signs with its own distribution key.

Do not use a shared public keystore for production. Anyone with the same key can ship updates under the same application identity, which breaks update trust.

Supported GitHub Actions secrets:

- `ANDROID_RUNTIME_ZIP_BASE64`: optional zip containing `runner` or `xray` + `tun2socks` assets.
- `ANDROID_KEYSTORE_BASE64`: base64-encoded private keystore.
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## iOS

iOS does not have a safe general-purpose public/community signing model for VPN apps.

Acceptable routes:

- Apple Developer Program signing for TestFlight and App Store.
- Apple Business Manager / enterprise distribution where legally and contractually appropriate.
- Personal Apple ID signing only for developer-side local testing, not team distribution.

VPN functionality requires Network Extension entitlements. Those entitlements are tied to Apple-managed signing, provisioning profiles, and bundle identifiers.

Supported GitHub Actions secrets:

- `ARCHITOKEN_PACKET_CORE_ZIP_BASE64`: optional private PacketTunnel runner framework/source payload.
- `APPLE_TEAM_ID`
- `IOS_APP_BUNDLE_ID`
- `IOS_TUNNEL_BUNDLE_ID`
- `APPLE_CERTIFICATE_BASE64`
- `APPLE_CERTIFICATE_PASSWORD`
- `APPLE_PROVISIONING_PROFILE_BASE64`
- `APPLE_TUNNEL_PROVISIONING_PROFILE_BASE64`
- `IOS_EXPORT_OPTIONS_PLIST_BASE64`
