#!/usr/bin/env bash
# 2단계 완료 기준 스모크 테스트: 배포된 ALB 주소에 대고 1단계 Happy Path를 그대로 재현한다.
# 사용법: ./infra/smoke.sh <base-url>
#   예:   ./infra/smoke.sh http://eventflow-dev-alb-1316319028.ap-northeast-2.elb.amazonaws.com
set -euo pipefail

BASE_URL="${1:?사용법: $0 <base-url> (예: http://<alb-dns>)}"
BASE_URL="${BASE_URL%/}"

command -v jq >/dev/null || { echo "jq가 필요합니다 (brew install jq)"; exit 1; }
command -v python3 >/dev/null || { echo "python3가 필요합니다"; exit 1; }

RUN_ID=$(date +%s)
ORGANIZER_EMAIL="smoke-organizer-${RUN_ID}@eventflow.test"
CUSTOMER_EMAIL="smoke-customer-${RUN_ID}@eventflow.test"
PASSWORD="password1234"

pass=0
fail=0

step() { echo "▶ $1" >&2; }
ok()   { echo "  ✅ $1" >&2; pass=$((pass + 1)); }
ng()   { echo "  ❌ $1" >&2; fail=$((fail + 1)); }

# now로부터 offset_days(+시간 hours) 뒤 ISO LocalDateTime 문자열 (mac/Linux 공통)
iso_offset() {
  local days="$1" hours="${2:-0}"
  python3 -c "
import datetime
print((datetime.datetime.now() + datetime.timedelta(days=${days}, hours=${hours})).strftime('%Y-%m-%dT%H:%M:%S'))
"
}

SALE_START=$(iso_offset -1)
EVENT_START=$(iso_offset 7)
EVENT_END=$(iso_offset 7 3)
SALE_END=$(iso_offset 6)   # 판매 종료는 공연 시작(EVENT_START) 이전이어야 함

# METHOD PATH TOKEN BODY_JSON -> stdout: 응답바디, 마지막 줄에 상태코드
req() {
  local method="$1" path="$2" token="$3" body="$4"
  local args=(-sS -X "$method" "${BASE_URL}${path}" -H "Content-Type: application/json")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer ${token}")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}" -w '\n%{http_code}'
}

# desc METHOD PATH TOKEN BODY_JSON EXPECT_STATUS -> stdout: 응답바디만 (성공/실패 로그는 stderr)
call() {
  local desc="$1" method="$2" path="$3" token="$4" body="$5" expect="$6"
  local resp status respbody
  resp=$(req "$method" "$path" "$token" "$body")
  status=$(echo "$resp" | tail -n1)
  respbody=$(echo "$resp" | sed '$d')
  if [ "$status" = "$expect" ]; then
    ok "$desc ($status)"
  else
    ng "$desc (expected $expect, got $status) — $respbody"
  fi
  echo "$respbody"
}

step "0. 헬스체크"
health=$(curl -sS "${BASE_URL}/actuator/health")
[ "$(echo "$health" | jq -r .status)" = "UP" ] && ok "actuator/health UP" || ng "actuator/health != UP ($health)"

step "1. 운영자 회원가입/로그인"
call "운영자 회원가입" POST /api/auth/signup "" \
  "$(jq -nc --arg e "$ORGANIZER_EMAIL" --arg p "$PASSWORD" '{email:$e,password:$p,name:"스모크 운영자"}')" \
  201 > /dev/null

organizer_login=$(call "운영자 로그인" POST /api/auth/login "" \
  "$(jq -nc --arg e "$ORGANIZER_EMAIL" --arg p "$PASSWORD" '{email:$e,password:$p}')" \
  200)
ORGANIZER_TOKEN=$(echo "$organizer_login" | jq -r .result.accessToken)

step "2. 관객 회원가입/로그인"
call "관객 회원가입" POST /api/auth/signup "" \
  "$(jq -nc --arg e "$CUSTOMER_EMAIL" --arg p "$PASSWORD" '{email:$e,password:$p,name:"스모크 관객"}')" \
  201 > /dev/null

customer_login=$(call "관객 로그인" POST /api/auth/login "" \
  "$(jq -nc --arg e "$CUSTOMER_EMAIL" --arg p "$PASSWORD" '{email:$e,password:$p}')" \
  200)
CUSTOMER_TOKEN=$(echo "$customer_login" | jq -r .result.accessToken)

step "3. 행사/회차/좌석 등록"
event=$(call "행사 생성" POST /api/events "$ORGANIZER_TOKEN" \
  "$(jq -nc --arg s "$EVENT_START" --arg e "$EVENT_END" \
    '{title:"스모크 테스트 공연",description:"CI 스모크",location:"스모크 공연장",startAt:$s,endAt:$e}')" \
  201)
EVENT_ID=$(echo "$event" | jq -r .result.id)

schedule=$(call "회차 생성" POST "/api/events/${EVENT_ID}/schedules" "$ORGANIZER_TOKEN" \
  "$(jq -nc --arg s "$EVENT_START" --arg e "$EVENT_END" --arg ss "$SALE_START" --arg se "$SALE_END" \
    '{name:"1회차",startAt:$s,endAt:$e,saleStartAt:$ss,saleEndAt:$se}')" \
  201)
SCHEDULE_ID=$(echo "$schedule" | jq -r .result.id)

call "좌석 등록" POST "/api/schedules/${SCHEDULE_ID}/seats" "$ORGANIZER_TOKEN" \
  '{"groups":[{"grade":"VIP","price":100000,"rowPrefix":"A","startNumber":1,"endNumber":3}]}' \
  201 > /dev/null

seats=$(call "좌석 조회" GET "/api/schedules/${SCHEDULE_ID}/seats" "" "" 200)
SEAT_ID=$(echo "$seats" | jq -r .result.seats[0].id)

step "4. 예매 → Mock 결제 → 티켓 발급"
reservation=$(call "예매" POST /api/reservations "$CUSTOMER_TOKEN" \
  "$(jq -nc --argjson sid "$SEAT_ID" '{seatId:$sid}')" \
  201)
RESERVATION_ID=$(echo "$reservation" | jq -r .result.reservationId)

call "결제" POST "/api/reservations/${RESERVATION_ID}/payment" "$CUSTOMER_TOKEN" "" 201 > /dev/null

tickets=$(call "티켓 조회" GET /api/tickets "$CUSTOMER_TOKEN" "" 200)
QR_TOKEN=$(echo "$tickets" | jq -r '.result[0].qrToken')

step "5. QR 입장 (성공 → 재입장 거부)"
entry1=$(call "첫 입장" POST /api/entries "$ORGANIZER_TOKEN" \
  "$(jq -nc --arg q "$QR_TOKEN" '{qrToken:$q}')" \
  200)
[ "$(echo "$entry1" | jq -r .result.result)" = "SUCCESS" ] && ok "첫 입장 SUCCESS" || ng "첫 입장 결과 이상: $entry1"

entry2=$(call "재입장" POST /api/entries "$ORGANIZER_TOKEN" \
  "$(jq -nc --arg q "$QR_TOKEN" '{qrToken:$q}')" \
  200)
[ "$(echo "$entry2" | jq -r .result.result)" = "ALREADY_USED" ] && ok "재입장 ALREADY_USED" || ng "재입장 결과 이상: $entry2"

echo >&2
echo "── 결과: ${pass}개 통과 / ${fail}개 실패 ──" >&2
[ "$fail" -eq 0 ]
