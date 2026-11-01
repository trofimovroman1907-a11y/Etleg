# Nexora Android client

Nexora is an Android Matrix client. The app now uses the Matrix Client-Server API for login, room listing, room creation/invites, message history and sending text messages.

## Build

From this directory:

```bash
./gradlew --no-daemon :app:assembleDebug
```

## Connect

On the login screen enter a Matrix homeserver URL (HTTPS), username and password. For a private Nexora deployment, use the homeserver URL configured by the server operator.

## Current implementation status

- Real Matrix password login and logout.
- Matrix account registration request (the homeserver may require registration tokens or disable public registration).
- Joined-room list and room name lookup.
- Create private rooms and invite a Matrix user ID.
- Load the latest 50 text messages and send text messages.
- Long-poll sync while the app is open.

## Security and limitations

This build currently sends ordinary Matrix `m.room.message` events. End-to-end encryption is **not yet implemented in this Android client**. Do not use it for sensitive conversations and do not describe this build as end-to-end encrypted. The Matrix Rust SDK / Kotlin bindings must be integrated before encrypted rooms, device verification, key backup and encrypted media can be enabled safely.

This repository does not contain a deployed homeserver. A real Nexora service needs an always-on host, a domain with HTTPS, Synapse, PostgreSQL, backups, SMTP configuration and an operator-managed invitation policy. The app does not include server passwords or API secrets.
