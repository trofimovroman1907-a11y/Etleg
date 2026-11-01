# Nexora homeserver deployment

This directory provisions a self-hosted Matrix Synapse homeserver, PostgreSQL and Caddy TLS reverse proxy.

## Requirements

- An always-on Linux host or VPS with Docker Compose v2.
- A domain you control. Point `MATRIX_DOMAIN` to the host's public IP.
- TCP ports 80 and 443 reachable from the internet.
- Persistent disk and tested off-host backups. A stopped laptop or a free sleeping container is not a reliable messenger server.

## First deployment

1. Copy `.env.example` to `.env` and set the real domain, email and a long random PostgreSQL password.
2. Generate Synapse configuration once:
   ```sh
   docker compose run --rm --entrypoint /start.py synapse generate
   ```
   If the image entrypoint or generated path changes in a future Synapse release, follow the official Synapse Docker image instructions before proceeding.
3. Edit the generated `homeserver.yaml` in the `synapse_data` volume and configure PostgreSQL using the Compose service:
   ```yaml
   database:
     name: psycopg2
     args:
       user: synapse
       password: YOUR_POSTGRES_PASSWORD
       database: synapse
       host: postgres
       cp_min: 5
       cp_max: 10
   enable_registration: false
   report_stats: false
   ```
4. Start services with `docker compose up -d`.
5. Create the first administrator locally:
   ```sh
   docker compose exec synapse register_new_matrix_user -c /data/homeserver.yaml http://localhost:8008
   ```
6. Keep public registration disabled. Create accounts for friends through the administrator workflow; do not publish the server's admin credentials.
7. Verify HTTPS and `https://MATRIX_DOMAIN/_matrix/client/versions`.

## Privacy and security

- PostgreSQL and Synapse are not end-to-end encryption. E2EE is provided by compatible clients, with encryption keys held on user devices.
- The current Nexora Android client does **not** yet integrate the Matrix Rust SDK encryption stack. Do not use this build for sensitive private conversations and do not enable encrypted-room claims until the client SDK, device verification, cross-signing and recovery are implemented and tested.
- Server operators can administer accounts and room membership. They must not be given a custom decryption backdoor.
- Configure off-host encrypted backups, updates, firewall rules, monitoring and a documented restore test before inviting friends.
- Never commit `.env`, signing keys, database dumps, access tokens or generated Synapse secrets.

## Operational notes

This Compose file intentionally does not promise free hosting. Synapse needs persistent storage and an always-on host. Voice/video additionally requires a properly configured MatrixRTC stack and TURN service; it is not enabled by this deployment.
