// 동일 좌석 동시 예매 시 중복 예약이 발생하는지 실제 HTTP 부하로 재현한다.
//
// 사용법:
//   로컬 확인:  k6 run -e BASE_URL=http://localhost:8080 -e VUS=10 duplicate-seat.js
//   AWS 실행:   k6 run -e BASE_URL=http://<alb-dns>       -e VUS=100 duplicate-seat.js
//
// 통합테스트(ReservationConcurrencyTest)는 한 JVM 안 스레드로 재현하고,
// 이 스크립트는 배포된 서버에 실제 HTTP 요청을 동시에 던져 같은 결론을 확인한다.

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
const VUS = Number(__ENV.VUS || 100); // 같은 좌석 하나를 노리는 가상 사용자 수
const PASSWORD = 'password1234';

if (!BASE_URL) {
  throw new Error('BASE_URL 환경변수가 필요합니다. 예: k6 run -e BASE_URL=http://<alb-dns> duplicate-seat.js');
}

// "좌석 1개인데 예매가 몇 건이나 성공했는가"를 집계한다
const reserveSuccess = new Counter('reserve_success');   // 201 → 예매 성공
const reserveRejected = new Counter('reserve_rejected'); // 그 외 → 예매 실패(동시성 방어가 있다면 대부분 여기여야 정상)

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  setupTimeout: '3m', // setup에서 관객 토큰 VU 수만큼 발급 → 원격 서버면 시간이 걸린다
  scenarios: {
    // VU를 한꺼번에 띄우고 각자 딱 1번 예매를 시도 → 최대한 동시에 같은 좌석으로 몰린다
    same_seat_rush: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '2m',
    },
  },
};

function post(path, body, token) {
  const headers = Object.assign({}, JSON_HEADERS);
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return http.post(`${BASE_URL}${path}`, body ? JSON.stringify(body) : null, { headers });
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

// setup: 좌석 1개짜리 행사를 만들고, 예매에 쓸 관객 토큰을 VU 수만큼 미리 발급해 둔다
// (예매 순간에 회원가입/로그인 지연이 끼어들지 않도록 미리 준비)
export function setup() {
  const runId = Date.now();
  const organizerToken = signupAndLogin(`k6-organizer-${runId}@eventflow.test`, 'k6 운영자');

  const eventStart = isoOffset(7);
  const eventEnd = isoOffset(7, 3);

  const event = post('/api/events', {
    title: 'k6 동시성 실험',
    description: '동일 좌석 중복 예매 재현',
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

  // 좌석은 딱 1개(A1)만 등록한다
  const seats = post(`/api/schedules/${scheduleId}/seats`, {
    groups: [{ grade: 'VIP', price: 100000, rowPrefix: 'A', startNumber: 1, endNumber: 1 }],
  }, organizerToken);
  check(seats, { '좌석 등록 201': (r) => r.status === 201 });

  const seatId = http.get(`${BASE_URL}/api/schedules/${scheduleId}/seats`).json('result.seats.0.id');

  const tokens = [];
  for (let i = 0; i < VUS; i++) {
    tokens.push(signupAndLogin(`k6-customer-${runId}-${i}@eventflow.test`, `k6 관객 ${i}`));
  }

  return { seatId, tokens };
}

// 본 시나리오: VU마다 자기 토큰으로 같은 좌석을 1번 예매 시도
export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const res = post('/api/reservations', { seatId: data.seatId }, token);

  if (res.status === 201) {
    reserveSuccess.add(1);
  } else {
    reserveRejected.add(1);
  }
  check(res, { '예매 응답 수신됨': (r) => r.status !== 0 });
}

// 요약: 좌석 1개에 예매가 몇 건 성공했는지
export function handleSummary(data) {
  const count = (name) => (data.metrics[name] ? data.metrics[name].values.count : 0);
  const success = count('reserve_success');
  const rejected = count('reserve_rejected');
  const bar = '='.repeat(52);

  const out = [
    bar,
    ` 대상               : ${BASE_URL}`,
    ` 좌석 수             : 1`,
    ` 동시 예매 시도(VU)  : ${VUS}`,
    ` 예매 성공(201)      : ${success}`,
    ` 예매 실패(그 외)     : ${rejected}`,
    bar,
    success >= 2
      ? ' 결과: 좌석 1개에 예매 2건 이상 성공 → 중복 예매 재현됨'
      : ' 결과: 예매 성공 1건 → 이 실행에서는 재현 안 됨 (VUS를 올리거나 재실행)',
    '',
  ].join('\n');

  return { stdout: out + '\n' };
}
