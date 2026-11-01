# Nexora

Nexora is a private-community Android messenger built on Matrix.

## Android client

The Android app currently supports Matrix password login, registration requests, joined-room listing, private room creation, invites, text history, text sending and foreground sync.

## Homeserver

See [server/README.md](server/README.md) for the Synapse + PostgreSQL + Caddy deployment.

## Security status — read before use

The current Android client uses direct Matrix Client-Server API calls and **does not yet implement Matrix end-to-end encryption**. Messages sent by this build are ordinary Matrix message events. Do not use it for sensitive private conversations. E2EE requires integrating the official Matrix Rust SDK Android bindings and completing device verification, cross-signing, key backup/recovery and encrypted media handling.

## Not yet production complete

Push notifications, background sync service, encrypted media, voice/video calling, channel moderation UI, admin dashboard, abuse reporting, accessibility review, automated UI tests, release signing and production server deployment remain unfinished.

## Build

```sh
./gradlew --no-daemon -p Nexora :app:assembleDebug
```
