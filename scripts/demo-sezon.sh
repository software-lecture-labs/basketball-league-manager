#!/usr/bin/env bash
# Çalışan uygulamaya demo sezonu ekler: sezon, sezonun takımları ve fikstür.
# Veri REST API üzerinden girilir; migration'lara demo verisi eklenmez.
# Tekrar çalıştırmak güvenlidir: var olan sezon kullanılır, üretilmiş fikstür yeniden üretilmez.
#
# Kullanım:  scripts/demo-sezon.sh [takım-sayısı]      (varsayılan 6, 2-20 arası)
# Ayarlar:   API_URL, SEASON_NAME, SEASON_START ortam değişkenleriyle değiştirilebilir.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080/basketball-league/api}"
SEASON_NAME="${SEASON_NAME:-2026-27 Demo Sezonu}"
SEASON_START="${SEASON_START:-2026-10-01}"
TEAM_COUNT="${1:-6}"

fail() {
    echo "Hata: $*" >&2
    exit 1
}

command -v curl >/dev/null || fail "curl bulunamadı."
command -v jq >/dev/null || fail "jq bulunamadı (macOS 15 ve sonrasında yüklü gelir; değilse: brew install jq)."
if ! [[ "$TEAM_COUNT" =~ ^[0-9]+$ ]] || ((TEAM_COUNT < 2 || TEAM_COUNT > 20)); then
    fail "Takım sayısı 2 ile 20 arasında olmalı: $TEAM_COUNT"
fi

RESPONSE_FILE=$(mktemp)
trap 'rm -f "$RESPONSE_FILE"' EXIT

# İsteği gönderir; durum kodunu STATUS'a, gövdeyi RESPONSE'a yazar.
request() {
    local method=$1 path=$2 body=${3:-}
    local args=(-s -o "$RESPONSE_FILE" -w '%{http_code}' -X "$method" -H 'Accept: application/json')
    if [[ -n "$body" ]]; then
        args+=(-H 'Content-Type: application/json' --data "$body")
    fi
    STATUS=$(curl "${args[@]}" "$API_URL$path") || STATUS=000
    RESPONSE=$(cat "$RESPONSE_FILE")
}

# Problem Details yanıtındaki hata kodu (ör. SEASON_NAME_TAKEN).
error_code() {
    jq -r '.code // empty' <<<"$RESPONSE" 2>/dev/null || true
}

request GET /ping
[[ "$STATUS" == 200 ]] || fail "API'ye ulaşılamadı ($API_URL/ping → $STATUS). Uygulama Payara'da çalışıyor mu?"

# 1. Sezon
request POST /seasons "$(jq -n --arg name "$SEASON_NAME" --arg start "$SEASON_START" \
    '{name: $name, startDate: $start}')"
if [[ "$STATUS" == 201 ]]; then
    SEASON_ID=$(jq -r '.id' <<<"$RESPONSE")
    echo "Sezon oluşturuldu: $SEASON_NAME (id $SEASON_ID)"
elif [[ "$STATUS" == 409 && "$(error_code)" == SEASON_NAME_TAKEN ]]; then
    request GET "/seasons?size=100"
    SEASON_ID=$(jq -r --arg name "$SEASON_NAME" '.items[] | select(.name == $name) | .id' <<<"$RESPONSE")
    [[ -n "$SEASON_ID" ]] || fail "\"$SEASON_NAME\" adlı sezon var ama listede bulunamadı."
    echo "Sezon zaten var, kullanılıyor: $SEASON_NAME (id $SEASON_ID)"
else
    fail "Sezon oluşturulamadı ($STATUS): $RESPONSE"
fi

# 2. Takımlar: kimlikleri varsaymak yerine ada göre ilk N takım alınır
request GET "/teams?size=$TEAM_COUNT"
TEAM_IDS=$(jq -r '.items[].id' <<<"$RESPONSE")
FOUND=$(wc -w <<<"$TEAM_IDS" | tr -d ' ')
((FOUND >= TEAM_COUNT)) || fail "$TEAM_COUNT takım istendi, veritabanında $FOUND takım var. flyway:migrate çalıştı mı?"
for team_id in $TEAM_IDS; do
    request POST "/seasons/$SEASON_ID/teams" "{\"teamId\": $team_id}"
    case "$STATUS:$(error_code)" in
        201:*) echo "  + takım $(jq -r '.name' <<<"$RESPONSE")" ;;
        409:TEAM_ALREADY_IN_SEASON) ;;
        409:SEASON_ALREADY_STARTED) echo "  Sezon başlamış; takım listesi değiştirilmedi."; break ;;
        *) fail "Takım $team_id eklenemedi ($STATUS): $RESPONSE" ;;
    esac
done

# 3. Fikstür
request POST "/seasons/$SEASON_ID/fixtures"
case "$STATUS:$(error_code)" in
    201:*) echo "Fikstür üretildi." ;;
    409:SEASON_ALREADY_STARTED) echo "Fikstür zaten üretilmiş." ;;
    *) fail "Fikstür üretilemedi ($STATUS): $RESPONSE" ;;
esac

request GET "/seasons/$SEASON_ID/fixtures"
ROUNDS=$(jq '.rounds | length' <<<"$RESPONSE")
GAMES=$(jq '[.rounds[].games | length] | add // 0' <<<"$RESPONSE")
echo "Hazır: $ROUNDS tur, $GAMES maç."
echo "Fikstür sayfası: ${API_URL%/api}/fixtures.html"
