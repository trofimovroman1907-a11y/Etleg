# Nexora homeserver deployment

This directory provisions a self-hosted Matrix Synapse homeserver, PostgreSQL and Caddy TLS reverse proxy.

## Requirements

- An always-on Linux host or VPS with Docker Compose v2.
- A domain you control. Point \`MATRIX_DOMAIN\` to the host's public IP.
- TCP ports 80 and 443 reachable from the internet.
- Persistent disk and tested off-host backups. A stopped laptop or a free sleeping container is not a reliable messenger server.

## First deployment

1. Copy \`.env.example\` to \`.env\` and set the real domain, email and a long random PostgreSQL password.
2. Generate Synapse configuration once:
   \`\`\`sh
   docker compose run --rm --entrypoint /start.py synapse generate
   \`\`\`
   Follow the official Synapse Docker instructions if the pinned image changes its configuration-generation command.
3. Edit the generated \`homeserver.yaml\` in the Synapse data volume. Configure PostgreSQL with the credentials from \`.env\`, use \`host: postgres\`, and keep public registration disabled.
4. Start services:
   \`\`\`sh
   docker compose up -d
   \`\`\`
5. Create the first server administrator locally:
   \`\`\`sh
   docker compose exec synapse register_new_matrix_user -c /data/homeserver.yaml http://localhost:8008
   \`\`\`
6. Create friend accounts locally with \`./admin.sh register <localpart>\`. Keep public registration disabled and do not publish admin credentials.
7. Verify HTTPS and \`https://MATRIX_DOMAIN/_matrix/client/versions\`.

## Privacy and security

- The current Nexora Android branch uses the official Matrix Rust SDK. Rooms created by the app are marked encrypted and messages are sent through the SDK timeline queue, which applies Matrix E2EE for encrypted rooms.
- Synapse stores encrypted room events as ciphertext. It does not receive plaintext message bodies for correctly encrypted room events.
- The homeserver still processes account IDs, room membership, event timing, IP addresses and other protocol metadata.
- Server operators can suspend accounts and administer room membership. Admin privileges must not include a custom backdoor to decrypt private rooms.
- Nexora initializes SDK cross-signing and key backups, but the app still needs a user-facing device verification and recovery flow, multi-device interoperability testing, encrypted media support and an independent security review before a production security claim.
- Existing unencrypted rooms do not become historically encrypted just because a client supports E2EE. Nexora blocks message sending in rooms whose current encryption state is not encrypted.
- Configure off-host encrypted backups, updates, firewall rules, monitoring and a documented restore test before inviting friends.
- Never commit \`.env\`, signing keys, database dumps, access tokens or generated Synapse secrets.

## Operational notes

This Compose file does not provide free hosting. Synapse needs persistent storage and an always-on host. Voice/video additionally requires a properly configured MatrixRTC stack and TURN service; these are not enabled by this deployment.
