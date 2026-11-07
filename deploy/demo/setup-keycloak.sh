#!/usr/bin/env bash
# Доводит realm eidos до состояния, пригодного для публичного стенда:
#
#   1. прописывает публичные адреса консолей в redirectUris и webOrigins —
#      в realm-eidos.json записан только localhost, и вход извне без этого
#      завершится ошибкой "Invalid parameter: redirect_uri";
#   2. задаёт пароль пользователя консолей из DEMO_CONSOLE_PASSWORD — пароль
#      из realm-eidos.json лежит в открытом репозитории и на публичном стенде
#      непригоден.
#
# Запускать из корня репозитория после подъёма стенда. Идемпотентно.
set -euo pipefail

cd "$(dirname "$0")/../.."
COMPOSE="docker compose -f docker-compose.demo.yml"

[ -f .env ] || { echo "Нет .env рядом с docker-compose.demo.yml" >&2; exit 1; }

# .env читается разбором, а не source: в формате Compose значение может
# содержать пробелы без кавычек (CONSENT_EXPIRY_CRON=0 5 0 * * *), и шелл
# принял бы его за команду. Окружающие кавычки снимаем — так же делает Compose.
read_env() {
    local value
    value=$(grep -m1 -E "^[[:space:]]*$1=" .env | cut -d= -f2- || true)
    value=${value#\"}; value=${value%\"}
    value=${value#\'}; value=${value%\'}
    printf '%s' "$value"
}

DEMO_ADMIN_HOST=$(read_env DEMO_ADMIN_HOST)
DEMO_CDP_HOST=$(read_env DEMO_CDP_HOST)
KEYCLOAK_ADMIN_USER=$(read_env KEYCLOAK_ADMIN_USER)
KEYCLOAK_ADMIN_PASSWORD=$(read_env KEYCLOAK_ADMIN_PASSWORD)
DEMO_CONSOLE_USER=$(read_env DEMO_CONSOLE_USER)
DEMO_CONSOLE_PASSWORD=$(read_env DEMO_CONSOLE_PASSWORD)

for var in DEMO_ADMIN_HOST DEMO_CDP_HOST KEYCLOAK_ADMIN_USER KEYCLOAK_ADMIN_PASSWORD DEMO_CONSOLE_USER DEMO_CONSOLE_PASSWORD; do
    [ -n "${!var:-}" ] || { echo "Не задано в .env: $var" >&2; exit 1; }
done

echo "Ожидаю Keycloak…"
until $COMPOSE exec -T keycloak bash -c 'exec 3<>/dev/tcp/localhost/8180' 2>/dev/null; do sleep 3; done

$COMPOSE exec -T \
    -e KC_USER="$KEYCLOAK_ADMIN_USER" \
    -e KC_PASS="$KEYCLOAK_ADMIN_PASSWORD" \
    -e ADMIN_HOST="$DEMO_ADMIN_HOST" \
    -e CDP_HOST="$DEMO_CDP_HOST" \
    -e CONSOLE_USER="$DEMO_CONSOLE_USER" \
    -e CONSOLE_PASS="$DEMO_CONSOLE_PASSWORD" \
    keycloak bash -s <<'INNER'
set -euo pipefail
KCADM=/opt/keycloak/bin/kcadm.sh

$KCADM config credentials --server http://localhost:8180 \
    --realm master --user "$KC_USER" --password "$KC_PASS" >/dev/null

CID=$($KCADM get clients -r eidos -q clientId=eidos-console \
        --fields id --format csv --noquotes | head -1)
[ -n "$CID" ] || { echo "Клиент eidos-console не найден в realm eidos" >&2; exit 1; }

# Локальные адреса оставляем: они нужны для разработки и публичному стенду
# не мешают — Keycloak сверяет редирект со списком целиком.
$KCADM update "clients/$CID" -r eidos \
    -s "redirectUris=[\"https://$ADMIN_HOST/*\",\"https://$CDP_HOST/*\",\"http://localhost:8088/*\",\"http://localhost:8089/*\",\"http://localhost:5173/*\",\"http://localhost:5174/*\"]" \
    -s "webOrigins=[\"https://$ADMIN_HOST\",\"https://$CDP_HOST\"]"
echo "redirectUris и webOrigins обновлены: https://$ADMIN_HOST, https://$CDP_HOST"

UID_=$($KCADM get users -r eidos -q username="$CONSOLE_USER" --exact true \
        --fields id --format csv --noquotes | head -1)
[ -n "$UID_" ] || { echo "Пользователь $CONSOLE_USER не найден в realm eidos" >&2; exit 1; }

$KCADM set-password -r eidos --userid "$UID_" --new-password "$CONSOLE_PASS"
echo "Пароль пользователя $CONSOLE_USER заменён"
INNER

echo "Готово. Realm настроен под публичные имена."
