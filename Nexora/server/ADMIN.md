# Nexora server administration

Use a trusted administrator account on the homeserver. Admin access tokens are secrets; never paste them into chat, commit them, or store them in the repository.

Set environment variables in your local shell:

```sh
export MATRIX_BASE_URL=https://matrix.example.org
export SYNAPSE_ADMIN_TOKEN='YOUR_ADMIN_ACCESS_TOKEN'
```

Then run:

```sh
chmod +x admin.sh
./admin.sh users
./admin.sh room-members '!roomid:example.org'
./admin.sh deactivate '@former-member:example.org'
```

Deactivation asks for confirmation and does not erase account data by default. Account deactivation revokes that user's ability to continue using the account; it does not grant the administrator access to E2EE room keys or decrypt prior messages. Room membership and room power levels should be managed through Matrix room administration by an authorized room moderator.

Keep this script on the server administrator's machine. Do not publish administrator tokens.
