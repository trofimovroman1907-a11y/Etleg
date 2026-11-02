# Nexora

Nexora is a private-community Android messenger built on the Matrix protocol.

## Android client

The Android client uses the official Matrix Rust SDK Android bindings for session management, synchronization, room timelines and end-to-end encryption.

Implemented in the current branch:
- Matrix password login and account-registration request.
- Secure SDK session storage protected by Android Keystore AES-GCM.
- SDK-managed foreground synchronization and joined-room listing.
- Private room creation with Matrix E2EE enabled at creation time.
- Optional invitation of another Matrix user when creating a room.
- Timeline updates from the Matrix SDK; encrypted events are decrypted on-device by the SDK.
- Message sending through the SDK timeline send queue.
- Sending is blocked when the room's latest encryption state is not E2EE.

The Android UI does not send messages through the plaintext Matrix REST event endpoint and does not fetch message history through the plaintext \`/messages\` endpoint.

## Homeserver

See [server/README.md](server/README.md) for the Synapse + PostgreSQL + Caddy deployment.

## Security notes

- New rooms created by Nexora are configured as encrypted Matrix rooms (Olm/Megolm).
- The homeserver still sees Matrix metadata such as account IDs, room membership, event timing, IP addresses and traffic patterns.
- An encrypted room does not make every other room on an account encrypted. Nexora refuses to open/send in a room unless the SDK reports it as encrypted.
- Device verification, cross-signing and key backup are initialized through the SDK, but Nexora still needs a dedicated device-verification and recovery interface, multi-device tests, encrypted-media flows and an independent security review before production use.
- Do not treat a debug APK as a security-audited release.

## Server cost

Synapse, PostgreSQL and Caddy are open-source, but a reliable public messenger still needs an always-on host, storage, backups, internet connectivity and a domain/HTTPS setup. No paid hosting is provisioned by this repository.

## Build

\`\`\`sh
./gradlew --no-daemon -p Nexora :app:assembleDebug
\`\`\`
