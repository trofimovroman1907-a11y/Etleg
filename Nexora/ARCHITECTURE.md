# Nexora architecture decision

Status: selected foundation; deployment is not yet configured.

## Decision

Build Nexora as a branded Android client on the Matrix protocol, using a self-hosted Matrix Synapse homeserver and PostgreSQL. Use the official Matrix Rust SDK Android bindings for client sync, room timelines, device management, and end-to-end encryption.

This avoids writing a custom messaging protocol or cryptography. Matrix's Rust SDK is used by Element X on Android and provides E2EE support.

## Components

- Android client: Nexora UI, Matrix Rust SDK Android bindings, local encrypted SDK store.
- Homeserver: Matrix Synapse.
- Database: PostgreSQL.
- Realtime delivery: Matrix client sync / sliding sync where supported by the chosen SDK release.
- Media: Matrix media repository; encrypted-room attachments are encrypted on the client before upload.
- Email verification: Synapse email verification with a separately configured SMTP provider.
- Voice/video: MatrixRTC / Element Call stack, including LiveKit and a TURN service. This is a later phase.
- Operations: Docker Compose, HTTPS reverse proxy, backups, monitoring, and pinned image versions.

## Privacy and administration

- Private conversations and groups must be encrypted end-to-end by the client.
- The homeserver stores ciphertext for encrypted rooms; it must not receive plaintext message bodies or attachment contents.
- Device keys and recovery material remain under user control. Device verification and key backup UX are required before calling the client production-ready.
- Disable public federation and open registration for the initial friends-only deployment; invite users deliberately.
- Admins can suspend accounts, revoke access, and moderate public channels. Admin privileges must not include a backdoor to decrypt private rooms.
- Public channels may be readable by guests, but guests must not be able to send messages, upload files, or join private rooms.

## Cost and availability reality

The software components are open source and can run without a per-message SaaS quota. This does not make hosting free: an always-on server, electricity, internet connectivity, storage, backups, and possibly a domain are still required. Free cloud instances may sleep, expire, or change their terms, so they are not treated as reliable permanent hosting.

No production deployment should be claimed until a persistent host, domain/HTTPS, SMTP delivery, backup target, and recovery procedure have been verified.

## Delivery phases

1. Buildable Android shell and Matrix SDK integration.
2. Private accounts, email verification, device sessions, encrypted one-to-one rooms.
3. Encrypted groups, read-only public channels, invitations, moderation.
4. Encrypted media, voice notes, file transfers.
5. Voice/video calling and TURN infrastructure.
6. Security review, load tests, backup/restore drills, and release signing.

## Explicit non-goals

- No custom cryptographic protocol.
- No server-side message scanning or plaintext admin access.
- No promise of unlimited physical capacity or zero operational cost.
- No release-ready security claims before independent review and testing.
