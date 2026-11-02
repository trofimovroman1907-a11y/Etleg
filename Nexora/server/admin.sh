#!/usr/bin/env sh
set -e

cmd=help
if [ "$#" -ge 1 ]; then cmd=$1; fi

# Admin endpoints are intentionally not exposed by the public Caddy proxy.
# Execute them from inside the Synapse container instead.
admin_api() {
  ADMIN_PATH=$1
  ADMIN_METHOD=$2
  ADMIN_BODY=$3
  docker compose exec -T \
    -e SYNAPSE_ADMIN_TOKEN="$SYNAPSE_ADMIN_TOKEN" \
    -e ADMIN_PATH="$ADMIN_PATH" \
    -e ADMIN_METHOD="$ADMIN_METHOD" \
    -e ADMIN_BODY="$ADMIN_BODY" \
    synapse python -c 'import os,urllib.request; path=os.environ["ADMIN_PATH"]; method=os.environ["ADMIN_METHOD"]; body=os.environ.get("ADMIN_BODY",""); data=body.encode() if body else None; req=urllib.request.Request("http://127.0.0.1:8008"+path,data=data,method=method,headers={"Authorization":"Bearer "+os.environ["SYNAPSE_ADMIN_TOKEN"],"Content-Type":"application/json"}); print(urllib.request.urlopen(req,timeout=20).read().decode())'
}

case "$cmd" in
  register)
    username=""
    if [ "$#" -ge 2 ]; then username=$2; fi
    if [ -z "$username" ]; then
      echo "Usage: admin.sh register <localpart>" >&2
      exit 2
    fi
    docker compose exec synapse register_new_matrix_user \
      -c /data/homeserver.yaml http://localhost:8008 \
      --user "$username" --no-admin
    ;;

  create-invite)
    if [ -z "$SYNAPSE_ADMIN_TOKEN" ]; then
      echo "Set SYNAPSE_ADMIN_TOKEN to an administrator access token." >&2
      exit 2
    fi
    uses=1
    if [ "$#" -ge 2 ]; then uses=$2; fi
    case "$uses" in
      ''|*[!0-9]*)
        echo "Uses must be a non-negative integer." >&2
        exit 2
        ;;
    esac
    admin_api "/_synapse/admin/v1/registration_tokens/new" POST "{\"uses_allowed\":$uses,\"length\":14}"
    ;;

  users)
    if [ -z "$SYNAPSE_ADMIN_TOKEN" ]; then
      echo "Set SYNAPSE_ADMIN_TOKEN to an administrator access token." >&2
      exit 2
    fi
    limit=100
    if [ "$#" -ge 2 ]; then limit=$2; fi
    admin_api "/_synapse/admin/v2/users?limit=$limit" GET ""
    ;;

  deactivate)
    if [ -z "$SYNAPSE_ADMIN_TOKEN" ]; then
      echo "Set SYNAPSE_ADMIN_TOKEN to an administrator access token." >&2
      exit 2
    fi
    user_id=""
    if [ "$#" -ge 2 ]; then user_id=$2; fi
    if [ -z "$user_id" ]; then
      echo "Usage: admin.sh deactivate @user:example.org" >&2
      exit 2
    fi
    printf 'Deactivate %s? [y/N] ' "$user_id"
    read -r answer
    [ "$answer" = "y" ] || [ "$answer" = "Y" ] || exit 0
    admin_api "/_synapse/admin/v1/deactivate/$user_id" POST '{"erase":false}'
    ;;

  room-members)
    if [ -z "$SYNAPSE_ADMIN_TOKEN" ]; then
      echo "Set SYNAPSE_ADMIN_TOKEN to an administrator access token." >&2
      exit 2
    fi
    room_id=""
    if [ "$#" -ge 2 ]; then room_id=$2; fi
    if [ -z "$room_id" ]; then
      echo "Usage: admin.sh room-members !roomid:example.org" >&2
      exit 2
    fi
    admin_api "/_synapse/admin/v1/rooms/$room_id/members" GET ""
    ;;

  help|*)
    echo "Commands:"
    echo "  register <localpart>          Create a non-admin account locally"
    echo "  create-invite [uses]           Create a registration token (default: one use)"
    echo "  users [limit]                  List registered accounts"
    echo "  deactivate @user:domain        Deactivate an account (asks confirmation)"
    echo "  room-members !room:domain      Inspect room membership"
    ;;
esac
