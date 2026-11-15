# EventFlow

> 인기 공연 티켓 오픈처럼 **트래픽이 한순간에 몰리는 예매 상황**에서 발생하는 문제를  
> AWS 환경에서 직접 재현하고 측정 가능한 수치로 해결한 티켓 예매 백엔드

- **수행 기간:**  2026.08 ~ 2026.09
- **역할:** 백엔드 + 클라우드 (개인 프로젝트)

<br>

## 핵심 성과

| 문제 | 해결 | 결과 |
|---|---|---|
| 동일 좌석 동시 예매 시 중복 예약 | 비관적 락 + DB 부분 유니크 인덱스 | 동시 100건 중 예약 **20건 → 1건** (AWS) |
| 대량 예매 트래픽에서 응답 지연 | Redis 대기열 입장 제어 | 예매 API p95 **2,150ms → 295ms** (약 86% 개선) |
| 지속 부하 시 고정 용량 한계 | ECS Auto Scaling (CPU 50% target tracking) | 태스크 **2 → 4 → 2개** 자동 확장·축소 |

<br>

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Data JPA, Spring Security + JWT |
| Database | PostgreSQL 16 (Flyway), Redis 7 |
| Cloud | AWS — ECS Fargate + Auto Scaling, RDS Multi-AZ, ElastiCache, ALB, ECR, SSM Parameter Store, CloudWatch |
| IaC · CI/CD | Terraform, GitHub Actions (OIDC) |
| Test | JUnit 5, Testcontainers, MockMvc, k6 |

<br>

## 아키텍처
<img width="1000" alt="구성도 drawio" src="https://github.com/user-attachments/assets/541404bf-3873-4376-bbcf-5f20384620c1" />

- **IaC**: Terraform 모듈화 (`network` · `alb` · `ecs` · `ecr` · `database` · `elasticache` · `cicd`), S3 원격 state
- **CI/CD**: GitHub Actions → OIDC 인증 → 테스트 → ECR 푸시 → ECS 롤링 배포 → 스모크 테스트
- **보안**: ECS 태스크 private 서브넷 배치, RDS 저장 암호화

<br>

## 주요 기능

```
회원가입·로그인 → 주최자가 행사/회차/좌석 등록 → 관객이 좌석 지정 예매 → 결제 → 티켓(QR) 발급 → 입장 시 QR 1회용 검증
```

- **도메인 검증** — 행사 상태, 판매 기간, 회차 시작 여부, 타인 예약 결제 차단
- **QR 1회용 입장** — 티켓 상태별 처리 (`ISSUED` 입장 / `USED` 재입장 거부 / `CANCELLED` 무효), 입장 로그 기록
- **통합테스트** — Testcontainers(PostgreSQL) 기반으로 회원가입부터 재입장 거부까지 전체 흐름 검증
- **API 문서** — Swagger UI (`/swagger-ui/index.html`)

<br>

## 문제 해결

### 1. 동일 좌석 중복 예매

**문제**
좌석이 `AVAILABLE`인지 확인만 하고 락이나 제약이 없어, 동시에 들어온 요청들이 서로의 변경을 보지 못하고 모두 예약에 성공

**재현**
동일 좌석에 100명이 동시에 요청할 때 실제로 중복 예약이 몇 건 생기는지 통합테스트(JVM 스레드)와 k6(HTTP)로 확인

**해결**
- `PESSIMISTIC_WRITE`(`SELECT ... FOR UPDATE`)로 좌석 행을 잠가 동시 요청 직렬화
- 최후 방어선으로 `reservations(seat_id)` **부분 유니크 인덱스** 추가 — 취소된 예약은 제외해 재판매 허용
- 인덱스 위반은 `409 SEAT_ALREADY_RESERVED`로 변환

**결과**
같은 테스트를 다시 실행해 **1건 성공 / 나머지 실패(409)** 로 바뀐 것을 확인

| 환경 (동시 100건) | Before | After |
|---|---|---|
| 로컬 통합테스트 (`ReservationConcurrencyTest`) | 예약 10건 | **예약 1건** |
| AWS k6 (ECS 2태스크) | 예약 20건 | **예약 1건** / 실패 99건 |

<details>
<summary>📸 Before — 중복 예매 재현</summary>

![중복 예매 재현](https://github.com/user-attachments/assets/e2c87629-3d49-49c2-81e4-1b0ad20e3bdb)
</details>

<details>
<summary>📸 After — 락 적용 후 예약 1건만 생성</summary>

![락 적용 후](https://github.com/user-attachments/assets/ca5b40f9-f89c-4d48-ae69-7ef845fa0503)
</details>

---

### 2. 대량 예매 트래픽에서 응답 지연

**문제**
서로 다른 좌석 500개에 500명이 동시에 예매해 락 경합이 없는데도, 고정 2태스크가 순간 트래픽을 받아내지 못해 예매 API p95가 2,150ms까지 악화
→ 정합성과는 별개인 **용량** 문제

**해결 ① Redis 대기열**
- ZSET에 도착 순서를 등록하고, 순번이 입장 커서 이내면 입장 허가(permit) 발급 → 예매 API는 permit 필수
- 커서를 스케줄러가 아닌 **경과 시간 기반 계산**으로 구해, ECS 태스크가 여러 개여도 중복 증가 없이 같은 값 보장

| 동시 500건 | 예매 성공 | 실패 | 예매 API p95 |
|---|---|---|---|
| Before | 500 | 0 | 2,150ms |
| After (대기열) | 500 | 0 | **295ms** (약 86% 개선) |

**해결 ② ECS Auto Scaling**
- CPU 평균 50% target tracking, 태스크 2~6개
- 20초 내외의 순간 부하에서는 지표 수집 · 반응 주기(분 단위) 때문에 발동하지 않음
  → 지속 부하 시나리오를 따로 만들어, CloudWatch 지표로 태스크가 **2개 → 4개 → 2개**로 늘었다 줄어드는 것을 검증

> 순간 폭주는 대기열이, 지속 부하는 Auto Scaling이 담당

<details>
<summary>📸 대기열 Before/After · CloudWatch Auto Scaling 지표</summary>

![대기열 및 오토스케일링](https://github.com/user-attachments/assets/7576cb66-b10b-4fd0-a8b5-6168b37dc0a2)
</details>

<br>

## 프로젝트 구조

```
EventFlow
├── src/main/java/com/example/eventflow
│   ├── domain                    # auth, user, event, schedule, seat, reservation, payment, ticket, entry, queue
│   └── global                    # 설정, JWT, 공통 응답, 전역 예외
├── src/main/resources/db/migration   # Flyway 마이그레이션
├── infra/terraform               # bootstrap, envs/dev, modules
├── load-tests/k6                 # 부하 테스트 스크립트
└── .github/workflows             # 배포 파이프라인
```

<br>

## 실행 방법

```bash
# 로컬 인프라 (PostgreSQL, Redis)
docker compose up -d

# 애플리케이션
./gradlew bootRun

# 테스트 (Docker 필요 — Testcontainers)
./gradlew test

# 부하 테스트
k6 run -e BASE_URL=http://localhost:8080 -e VUS=100 load-tests/k6/duplicate-seat.js
k6 run -e BASE_URL=http://localhost:8080 -e VUS=500 load-tests/k6/flash-sale-rush.js
k6 run -e BASE_URL=http://localhost:8080 -e VUS=500 load-tests/k6/flash-sale-with-queue.js
```
