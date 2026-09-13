// 인기 행사 오픈 순간, 서로 다른 좌석에 대량 동시 예매 요청이 몰릴 때
// (좌석 경합이 아니라 순수 트래픽 양만으로) 서버가 느려지거나 에러를 내는지 재현한다.
//
// duplicate-seat.js는 좌석 1개를 두고 경합(정합성)을 본다면,
// 이 스크립트는 좌석을 VU 수만큼 준비해서 각자 다른 좌석을 예매하게 한다 — 좌석 경합은 없음.
// 그래도 느려지거나 에러가 난다면, 그건 ECS 고정 태스크 수(용량) 문제라는 뜻이다.
//
// 사용법:
//   로컬 확인:  k6 run -e BASE_URL=http://localhost:8080 -e VUS=50  flash-sale-rush.js
//   AWS 실행:   k6 run -e BASE_URL=http://<alb-dns>       -e VUS=500 flash-sale-rush.js

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
const VUS = Number(__ENV.VUS || 500); // 동시에 각자 다른 좌석을 예매할 가상 관객 수 = 준비할 좌석 수
const PASSWORD = 'password1234';

if (!BASE_URL) {
  throw new Error('BASE_URL 환경변수가 필요합니다. 예: k6 run -e BASE_URL=http://<alb-dns> flash-sale-rush.js');
}

// 예매 API 응답시간만 따로 집계 (p95/p99를 보려고 setup의 회원가입/로그인과 분리)
const reservationDuration = new Trend('reservation_duration');
const reserveSuccess = new Counter('reserve_success');   // 201 → 예매 성공
const reserveFailed = new Counter('reserve_failed');     // 그 외 → 타임아웃/5xx 등 (좌석이 서로 다르므로 409는 나오면 안 됨)

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  setupTimeout: '10m', // 관객 토큰을 VU 수만큼 발급 → 수가 많으면 오래 걸린다
  scenarios: {
    // VU를 한꺼번에 띄우고 각자 딱 1번, 자기 좌석으로 예매 시도 → 최대한 동시에 터진다
    flash_sale: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '3m',
    },
  },
};

function post(path, body, token, tags) {
  const headers = Object.assign({}, JSON_HEADERS);
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return http.post(`${BASE_URL}${path}`, body ? JSON.stringify(body) : null, { headers, tags });
}

// now 기준 offset 뒤의 LocalDateTime 문자열(yyyy-MM-ddTHH:mm:ss)
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

// setup: 좌석을 VUS만큼 등록하고, 관객 토큰도 VUS만큼 미리 발급해 둔다
export function setup() {
  const runId = Date.now();
  const organizerToken = signupAndLogin(`k6-organizer-${runId}@eventflow.test`, 'k6 운영자');

  const eventStart = isoOffset(7);
  const eventEnd = isoOffset(7, 3);

  const event = post('/api/events', {
    title: 'k6 오픈런 실험',
    description: '좌석 대량 동시 예매로 용량 문제 재현',
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
    saleStartAt: isoOffset(-1), // 판매는 이미 시작
    saleEndAt: isoOffset(6),    // 공연 시작 전에 판매 종료
  }, organizerToken);
  check(schedule, { '회차 생성 201': (r) => r.status === 201 });
  const scheduleId = schedule.json('result.id');

  // 좌석을 VUS개만큼 한 구역으로 일괄 등록 (A1~A{VUS})
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

  return { seatIds, tokens };
}

// 본 시나리오: VU마다 자기 토큰으로, 자기 몫의(서로 다른) 좌석을 1번 예매 시도
export default function (data) {
  const idx = (__VU - 1) % data.tokens.length;
  const token = data.tokens[idx];
  const seatId = data.seatIds[idx];

  const res = post('/api/reservations', { seatId }, token, { name: 'reserve' });
  reservationDuration.add(res.timings.duration);

  if (res.status === 201) {
    reserveSuccess.add(1);
  } else {
    reserveFailed.add(1);
  }
  check(res, { '예매 응답 수신됨': (r) => r.status !== 0 });
}

// 요약: 좌석 경합 없이도 몰리는 트래픽 양 자체가 응답시간/에러에 영향을 주는지
export function handleSummary(data) {
  const count = (name) => (data.metrics[name] ? data.metrics[name].values.count : 0);
  const p = (name, pct) => (data.metrics[name] ? data.metrics[name].values[pct] : undefined);

  const success = count('reserve_success');
  const failed = count('reserve_failed');
  const p95 = p('reservation_duration', 'p(95)');
  const p99 = p('reservation_duration', 'p(99)');
  const bar = '='.repeat(52);

  const out = [
    bar,
    ` 대상                 : ${BASE_URL}`,
    ` 좌석 수(=VU 수)        : ${VUS}`,
    ` 예매 성공(201)        : ${success}`,
    ` 예매 실패(그 외)       : ${failed}`,
    ` 예매 API p95          : ${p95 ? p95.toFixed(0) + 'ms' : 'N/A'}`,
    ` 예매 API p99          : ${p99 ? p99.toFixed(0) + 'ms' : 'N/A'}`,
    bar,
    '',
  ].join('\n');

  return { stdout: out + '\n' };
}
