// flash-sale-rush.js(대기열 없음)와 똑같은 상황(좌석 수만큼 VU가 동시에 몰림)에서,
// 이번엔 대기열(번호표 -> 순번 확인 -> 통과되면 예매)을 거치게 한다.
// before(flash-sale-rush.js) 대비 예매 API 자체의 p95/p99가 얼마나 좋아지는지 비교하는 게 목적.
//
// 사용법:
//   로컬 확인:  k6 run -e BASE_URL=http://localhost:8080 -e VUS=50  flash-sale-with-queue.js
//   AWS 실행:   k6 run -e BASE_URL=http://<alb-dns>       -e VUS=500 flash-sale-with-queue.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
const VUS = Number(__ENV.VUS || 500); // 좌석 수 = 대기열에 몰리는 관객 수
const PASSWORD = 'password1234';
const POLL_INTERVAL_SECONDS = 1;
const ADMISSION_TIMEOUT_SECONDS = 90; // 이 시간 안에 admitted 안 되면 실패 처리

if (!BASE_URL) {
  throw new Error('BASE_URL 환경변수가 필요합니다. 예: k6 run -e BASE_URL=http://<alb-dns> flash-sale-with-queue.js');
}

// duplicate-seat.js/flash-sale-rush.js와 같은 이름으로 맞춰서 before/after 비교가 쉽게
const reservationDuration = new Trend('reservation_duration'); // 예매 API 자체의 응답시간만
const admissionWaitDuration = new Trend('admission_wait_duration'); // 참고용: 대기열 통과까지 걸린 시간
const reserveSuccess = new Counter('reserve_success');
const reserveFailed = new Counter('reserve_failed');

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  setupTimeout: '10m',
  scenarios: {
    flash_sale_with_queue: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '5m',
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
    title: 'k6 오픈런(대기열) 실험',
    description: '대기열 적용 후 응답시간 비교',
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
    groups: [{ grade: 'VIP', price: 100000, rowPrefix: 'A', startNumber: 1, endNumber: VUS }],
  }, organizerToken);
  check(seatsRes, { '좌석 일괄 등록 201': (r) => r.status === 201 });

  const seats = http.get(`${BASE_URL}/api/schedules/${scheduleId}/seats`).json('result.seats');
  const seatIds = seats.map((s) => s.id);

  const tokens = [];
  for (let i = 0; i < VUS; i++) {
    tokens.push(signupAndLogin(`k6-customer-${runId}-${i}@eventflow.test`, `k6 관객 ${i}`));
  }

  return { scheduleId, seatIds, tokens };
}

// 대기열 진입 -> admitted 될 때까지 순번 확인 반복 -> queueToken 반환
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
  const idx = (__VU - 1) % data.tokens.length;
  const token = data.tokens[idx];
  const seatId = data.seatIds[idx];

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
    ` 대상                 : ${BASE_URL}`,
    ` 좌석 수(=VU 수)        : ${VUS}`,
    ` 예매 성공(201)        : ${success}`,
    ` 예매 실패(그 외)       : ${failed}`,
    ` 예매 API p95          : ${p95 ? p95.toFixed(0) + 'ms' : 'N/A'}`,
    ` 예매 API p99          : ${p99 ? p99.toFixed(0) + 'ms' : 'N/A'}`,
    ` 대기열 통과까지 p95    : ${admissionP95 ? (admissionP95 / 1000).toFixed(1) + 's' : 'N/A'} (참고용)`,
    bar,
    '',
  ].join('\n');

  return { stdout: out + '\n' };
}
