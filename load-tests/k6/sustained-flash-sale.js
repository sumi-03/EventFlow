// flash-sale-with-queue.js는 20초 안팎의 순간부하라 오토스케일링(CPU target tracking)이
// 반응할 시간이 없었다. 대기열의 permit 발급 속도(기본 admit-batch-size=50 / admit-interval-millis=2000ms
// = 초당 25명)는 고정이므로, 대기 인원(VUS)을 크게 늘리면 그 속도로 예매 API에 트래픽이 몇 분간
// 꾸준히 흘러들어가는 지속 부하를 별도 executor 없이 만들 수 있다.
//
// 좌석은 VUS만큼 만들지 않고 고정 풀(SEATS)만 만든다 - 매진 이후에도 대기열 통과/예매 시도 트래픽
// 자체는 계속돼야 지속 부하가 유지되고, 실제 인기 행사 오픈 상황과도 더 비슷하다.
//
// 사용법:
//   로컬 확인 (짧게): k6 run -e BASE_URL=http://localhost:8080 -e TARGET_MINUTES=2 sustained-flash-sale.js
//   AWS 실행:         k6 run -e BASE_URL=http://<alb-dns> -e TARGET_MINUTES=10 sustained-flash-sale.js
//
// VUS는 ADMIT_RATE_PER_SEC * TARGET_MINUTES * 60 으로 자동 계산된다 (직접 -e VUS=... 로 덮어쓰기도 가능).
// TARGET_MINUTES=10이면 VUS=15000 정도로 커지니, 로컬 장비 부하가 크면 TARGET_MINUTES를 낮추거나
// 클라우드 VM에서 실행할 것.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
const SEATS = Number(__ENV.SEATS || 2000); // 실제 판매 좌석 풀 (VUS보다 훨씬 작아도 됨)
const USERS = Number(__ENV.USERS || 300); // 재사용할 관객 계정 풀
const ADMIT_RATE_PER_SEC = Number(__ENV.ADMIT_RATE_PER_SEC || 25); // 큐 기본 설정(50/2000ms) 기준
const TARGET_MINUTES = Number(__ENV.TARGET_MINUTES || 10); // 지속 부하를 유지하고 싶은 시간
const VUS = Number(__ENV.VUS || Math.ceil(ADMIT_RATE_PER_SEC * TARGET_MINUTES * 60));
const PASSWORD = 'password1234';
const POLL_INTERVAL_SECONDS = 1;
// 대기열 뒤쪽 순번은 전체 유입이 끝날 때까지 기다려야 하므로, 최소 목표 시간보다 여유있게 잡는다
const ADMISSION_TIMEOUT_SECONDS = Number(__ENV.ADMISSION_TIMEOUT_SECONDS || TARGET_MINUTES * 60 + 120);

if (!BASE_URL) {
  throw new Error('BASE_URL 환경변수가 필요합니다. 예: k6 run -e BASE_URL=http://<alb-dns> sustained-flash-sale.js');
}

const reservationDuration = new Trend('reservation_duration');
const admissionWaitDuration = new Trend('admission_wait_duration');
const reserveSuccess = new Counter('reserve_success');
const reserveFailed = new Counter('reserve_failed');

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  setupTimeout: '10m',
  scenarios: {
    sustained_flash_sale: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: `${TARGET_MINUTES + 10}m`,
    },
  },
};

function post(path, body, token, tags) {
  const headers = Object.assign({}, JSON_HEADERS);
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return http.post(`${BASE_URL}${path}`, body ? JSON.stringify(body) : null, { headers, tags });
}

function get(path, token, tags) {
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return http.get(`${BASE_URL}${path}`, { headers, tags });
}

function isoOffset(days, hours = 0) {
  return new Date(Date.now() + days * 86400000 + hours * 3600000).toISOString().slice(0, 19);
}

function signupAndLogin(email, name) {
  const signup = post('/api/auth/signup', { email, password: PASSWORD, name });
  check(signup, { '회원가입 201': (r) => r.status === 201 });
  const login = post('/api/auth/login', { email, password: PASSWORD });
  check(login, { '로그인 200': (r) => r.status === 200 });
  return login.json('result.accessToken');
}

export function setup() {
  const runId = Date.now();
  const organizerToken = signupAndLogin(`k6-organizer-${runId}@eventflow.test`, 'k6 운영자');

  const eventStart = isoOffset(7);
  const eventEnd = isoOffset(7, 3);

  const event = post('/api/events', {
    title: 'k6 지속 부하(오토스케일링) 실험',
    description: '대기열 통과 후에도 지속되는 유입으로 오토스케일링 확인',
    location: 'k6 공연장',
    startAt: eventStart,
    endAt: eventEnd,
  }, organizerToken);
  check(event, { '행사 생성 201': (r) => r.status === 201 });
  const eventId = event.json('result.id');

  const schedule = post(`/api/events/${eventId}/schedules`, {
    name: '1회차',
    startAt: eventStart,
    endAt: eventEnd,
    saleStartAt: isoOffset(-1),
    saleEndAt: isoOffset(6),
  }, organizerToken);
  check(schedule, { '회차 생성 201': (r) => r.status === 201 });
  const scheduleId = schedule.json('result.id');

  const seatsRes = post(`/api/schedules/${scheduleId}/seats`, {
    groups: [{ grade: 'VIP', price: 100000, rowPrefix: 'A', startNumber: 1, endNumber: SEATS }],
  }, organizerToken);
  check(seatsRes, { '좌석 일괄 등록 201': (r) => r.status === 201 });

  const seats = http.get(`${BASE_URL}/api/schedules/${scheduleId}/seats`).json('result.seats');
  const seatIds = seats.map((s) => s.id);

  const tokens = [];
  for (let i = 0; i < USERS; i++) {
    tokens.push(signupAndLogin(`k6-customer-${runId}-${i}@eventflow.test`, `k6 관객 ${i}`));
  }

  return { scheduleId, seatIds, tokens };
}

function joinQueueAndWait(scheduleId, token) {
  const joinRes = post(`/api/schedules/${scheduleId}/queue`, null, token, { name: 'queue-join' });
  if (joinRes.status !== 201) {
    return null;
  }
  let result = joinRes.json('result');
  const start = Date.now();

  while (!result.admitted) {
    if ((Date.now() - start) / 1000 > ADMISSION_TIMEOUT_SECONDS) {
      return null;
    }
    sleep(POLL_INTERVAL_SECONDS);
    const statusRes = get(`/api/schedules/${scheduleId}/queue/${result.queueToken}`, token, { name: 'queue-status' });
    if (statusRes.status !== 200) {
      return null;
    }
    result = statusRes.json('result');
  }

  admissionWaitDuration.add(Date.now() - start);
  return result.queueToken;
}

export default function (data) {
  // 풀이 VUS보다 작으므로 여러 VU가 같은 토큰/좌석을 나눠 쓴다 - 좌석 경합으로 인한 실패도
  // 지속 부하의 일부로 취급한다 (매진 후 트래픽이 끊기지 않는 게 목적)
  const tokenIdx = (__VU - 1) % data.tokens.length;
  const seatIdx = (__VU - 1) % data.seatIds.length;
  const token = data.tokens[tokenIdx];
  const seatId = data.seatIds[seatIdx];

  const queueToken = joinQueueAndWait(data.scheduleId, token);
  if (!queueToken) {
    reserveFailed.add(1);
    return;
  }

  const res = post('/api/reservations', { seatId, queueToken }, token, { name: 'reserve' });
  reservationDuration.add(res.timings.duration);

  if (res.status === 201) {
    reserveSuccess.add(1);
  } else {
    reserveFailed.add(1);
  }
  check(res, { '예매 응답 수신됨': (r) => r.status !== 0 });
}

export function handleSummary(data) {
  const count = (name) => (data.metrics[name] ? data.metrics[name].values.count : 0);
  const p = (name, pct) => (data.metrics[name] ? data.metrics[name].values[pct] : undefined);

  const success = count('reserve_success');
  const failed = count('reserve_failed');
  const p95 = p('reservation_duration', 'p(95)');
  const p99 = p('reservation_duration', 'p(99)');
  const admissionP95 = p('admission_wait_duration', 'p(95)');
  const bar = '='.repeat(52);

  const out = [
    bar,
    ` 대상                    : ${BASE_URL}`,
    ` 목표 지속 시간           : ${TARGET_MINUTES}분 (admit ${ADMIT_RATE_PER_SEC}/s 기준)`,
    ` 총 참가자(VUS)           : ${VUS}`,
    ` 좌석 풀                 : ${SEATS}`,
    ` 예매 성공(201)           : ${success}`,
    ` 예매 실패(매진/타임아웃 등) : ${failed}`,
    ` 예매 API p95            : ${p95 ? p95.toFixed(0) + 'ms' : 'N/A'}`,
    ` 예매 API p99            : ${p99 ? p99.toFixed(0) + 'ms' : 'N/A'}`,
    ` 대기열 통과까지 p95       : ${admissionP95 ? (admissionP95 / 1000).toFixed(1) + 's' : 'N/A'} (참고용)`,
    bar,
    '',
  ].join('\n');

  return { stdout: out + '\n' };
}
