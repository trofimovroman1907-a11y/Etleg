# Nexora

Nexora is a private-community Android messenger built on the Matrix protocol.

## Android client

The Android client uses the official Matrix Rust SDK for session management, synchronization, room timelines and end-to-end encryption.

Implemented in the current branch:
- Redesigned dark-first interface with a persistent light/dark theme switch.
- Branded in-app status messages instead of Android system Toast popups.
- Fixed homeserver address in the client; there is no editable server URL field.
- Matrix password login and invitation-token account registration.
- Secure SDK session storage protected by Android Keystore AES-GCM.
- SDK-managed foreground synchronization and joined-room listing.
- Private room creation with Matrix E2EE enabled at creation time.
- Timeline updates from the Matrix SDK; encrypted events are decrypted on-device by the SDK.
- Message sending through the SDK timeline send queue.
- Sending is blocked when the room's latest encryption state is not E2EE.

The Android UI does not send messages through the plaintext Matrix REST event endpoint and does not fetch message history through the plaintext message-history endpoint.

## Registration

The Synapse template keeps registration closed unless the server administrator creates invitation tokens. See server/README.md and run the create-invite command from the server directory. The app supports Matrix's registration-token UI-authentication stage.

The homeserver URL is currently fixed to matrix.org in MainActivity.java. Replace that constant with the deployed Nexora homeserver before distributing the private-server build.

## Security notes

- New rooms created by Nexora are configured as encrypted Matrix rooms using Olm/Megolm.
- The homeserver still sees Matrix metadata such as account IDs, room membership, event timing, IP addresses and traffic patterns.
- An encrypted room does not make every other room on an account encrypted. Nexora refuses to open or send in a room unless the SDK reports it as encrypted.
- Device verification, cross-signing and key backup are initialized through the SDK, but the app still needs a dedicated device-verification and recovery interface, multi-device tests, encrypted-media flows and an independent security review before production use.
- Do not treat a debug APK as a security-audited release.

## Server cost

Synapse, PostgreSQL and Caddy are open-source, but a reliable public messenger still needs an always-on host, storage, backups, internet connectivity and a domain/HTTPS setup. No paid hosting is provisioned by this repository.

## Build

    ./gradlew --no-daemon -p Nexora :app:assembleDebug
