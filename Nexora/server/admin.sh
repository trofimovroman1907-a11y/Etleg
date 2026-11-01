#!/usr/bin/env sh
set -eu

: "${MATRIX_BASE_URL:?Set MATRIX_BASE_URL, e.g. https://matrix.example.org}"
: "${SYNAPSE_ADMIN_TOKEN:?Set SYNAPSE_ADMIN_TOKEN to an administrator access token}"

api="${MATRIX_BASE_URL%/}/_synapse/admin/v2"
cmd="${1:-help}"

case "$cmd" in
  users)
    curl --fail --silent --show-error \
      -H "Authorization: Bearer $SYNAPSE_ADMIN_TOKEN" \
      "$api/users?limit=${2:-100}"
    ;;
  deactivate)
    user_id="${2:?Usage: admin.sh deactivate @user:example.org}"
    printf 'Deactivate %s? [y/N] ' "$user_id"
    read answer
    [ "$answer" = "y" ] || [ "$answer" = "Y" ] || exit 0
    curl --fail --silent --show-error -X POST \
      -H "Authorization: Bearer $SYNAPSE_ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"erase":false}' \
      "${MATRIX_BASE_URL%/}/_synapse/admin/v1/deactivate/$user_id"
    ;;
  room-members
    room_id="${2:?Usage: admin.sh room-members !roomid:example.org}"
    curl --fail --silent --show-error \
      -H "Authorization: Bearer $SYNAPSE_ADMIN_TOKEN" \
      "${MATRIX_BASE_URL%/}/_synapse/admin/v1/rooms/$room_id/members"
    ;;
  help|*)
    echo "Commands:"
    echo "  users [limit]                 List registered accounts"
    echo "  deactivate @user:domain       Deactivate an account (asks confirmation)"
    echo "  room-members !room:domain     Inspect room membership"
    ;;
esac
