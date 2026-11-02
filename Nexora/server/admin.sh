#!/usr/bin/env sh
set -eu

cmd="\${1:-help}"

case "$cmd" in
  register)
    username="\${2:?Usage: admin.sh register <localpart>}"
    # Run locally through Docker. The password is requested interactively and
    # is not placed in shell history or process arguments.
    docker compose exec synapse register_new_matrix_user \
      -c /data/homeserver.yaml http://localhost:8008 \
      --user "$username" --no-admin
    ;;

  users)
    : "\${MATRIX_BASE_URL:?Set MATRIX_BASE_URL, e.g. https://matrix.example.org}"
    : "\${SYNAPSE_ADMIN_TOKEN:?Set SYNAPSE_ADMIN_TOKEN to an administrator access token}"
    curl --fail --silent --show-error \
      -H "Authorization: Bearer $SYNAPSE_ADMIN_TOKEN" \
      "\${MATRIX_BASE_URL%/}/_synapse/admin/v2/users?limit=\${2:-100}"
    ;;

  deactivate)
    : "\${MATRIX_BASE_URL:?Set MATRIX_BASE_URL, e.g. https://matrix.example.org}"
    : "\${SYNAPSE_ADMIN_TOKEN:?Set SYNAPSE_ADMIN_TOKEN to an administrator access token}"
    user_id="\${2:?Usage: admin.sh deactivate @user:example.org}"
    printf 'Deactivate %s? [y/N] ' "$user_id"
    read -r answer
    [ "$answer" = "y" ] || [ "$answer" = "Y" ] || exit 0
    curl --fail --silent --show-error -X POST \
      -H "Authorization: Bearer $SYNAPSE_ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"erase":false}' \
      "\${MATRIX_BASE_URL%/}/_synapse/admin/v1/deactivate/$user_id"
    ;;

  room-members)
    : "\${MATRIX_BASE_URL:?Set MATRIX_BASE_URL, e.g. https://matrix.example.org}"
    : "\${SYNAPSE_ADMIN_TOKEN:?Set SYNAPSE_ADMIN_TOKEN to an administrator access token}"
    room_id="\${2:?Usage: admin.sh room-members !roomid:example.org}"
    curl --fail --silent --show-error \
      -H "Authorization: Bearer $SYNAPSE_ADMIN_TOKEN" \
      "\${MATRIX_BASE_URL%/}/_synapse/admin/v1/rooms/$room_id/members"
    ;;

  help|*)
    echo "Commands:"
    echo "  register <localpart>           Create a non-admin account locally"
    echo "  users [limit]                  List registered accounts"
    echo "  deactivate @user:domain        Deactivate an account (asks confirmation)"
    echo "  room-members !room:domain      Inspect room membership"
    ;;
esac
