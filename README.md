# API-SUKIMA

> 스키마바이트(すきまバイト)를 모티브로 한 단기 알바 매칭 플랫폼 백엔드 API

<br>

## 📌 프로젝트 개요

일본의 단기 알바 매칭 서비스 **스키마바이트**를 모티브로 한 포트폴리오 프로젝트입니다.  
학습한 기술들을 실제 서비스 흐름에 적용하고, 실무에서 마주치는 동시성·실시간 처리·배치 등의 문제를 직접 다루는 것을 목표로 개발했습니다.

<br>

## 🏗️ 아키텍처

### 멀티모듈 구조

```
API-SUKIMA/
├── sukima-domain/          # 도메인 타입, 예외 정의 (순수 Java)
├── sukima-application/     # UseCase(Port/in), Service
├── sukima-infrastructure/  # JPA Entity, Repository
├── sukima-web/             # Controller, Security, JWT, SSE
└── sukima-batch/           # Spring Batch (만료 공고, 패널티 만료 알림)
```

### 아키텍처 선택 배경

헥사고날 아키텍처를 참고하되, 1인 개발 효율을 고려해 실용적인 구조를 채택했습니다.

| 구분 | 헥사고날 퓨리스트 | 본 프로젝트 |
|------|---------------|------------|
| Port/in | UseCase 인터페이스 정의 | ✅ 유지 |
| Port/out | application에 인터페이스 정의 | 일부만 사용 (NoShow, QR, Lock, Notification) |
| 의존성 방향 | application → port/out ← infrastructure | application → infrastructure 직접 참조 |

<br>

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.1, Spring Batch |
| ORM | Spring Data JPA, Hibernate Spatial |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| Cache | Redis 7.2 |
| Auth | JWT (jjwt 0.12.5), Spring Security |
| 동시성 | Redisson 3.27.2 (분산락) |
| 비동기 | ThreadPoolTaskExecutor, CompletableFuture |
| API 문서 | Swagger (springdoc-openapi 2.5.0) |
| Infra | Docker, Docker Compose, Nginx |
| CI/CD | GitHub Actions |
| Build | Gradle (멀티모듈) |
| Test | JUnit5, Mockito, BDDMockito |

<br>

## 🔑 핵심 기능 및 기술 포인트

### 1. 위치 기반 공고 조회 / 알림 (PostGIS)
- `GEOMETRY(POINT, 4326)` 타입으로 공고 위치 저장
- `ST_DWithin` + `GIST 인덱스`로 반경 N미터 이내 공고 조회
- 공고 등록 시 Worker의 알림 위치 기준 반경 내 대상자 **단 1번 쿼리**로 추출 → 즉시 알림

### 2. 지원 수락 동시성 제어 (Redisson 분산락)
- 같은 공고에 대한 수락 요청을 `lock:accept:job:{jobPostingId}` 키로 직렬화
- 다른 공고 요청은 서로 영향 없이 동시 처리 가능

### 3. QR 체크인/아웃
- 인증 JWT와 별개의 시크릿으로 QR 전용 JWT 발급
- 재발급 시 `qrVersion` 증가 → 이전 토큰 자동 무효화
- Haversine 공식으로 근무지 반경 **100m 이내** 검증
- 체크아웃 시 `시급 × 실근무시간(분) / 60` 자동 정산 (PENDING 상태로 생성)

### 4. 노쇼 감지 (Redis Keyspace Notification)
- 매칭 확정 시 `noshow:{matchId}` TTL 키 등록 (근무시작 + 15분)
- TTL 만료 → `__keyevent@0__:expired` 이벤트 → 체크인 없으면 노쇼 처리
- **DB 백업**: 서버 재시작 시 `@PostConstruct`로 미처리 스케줄 복구
- 패널티: 3회 → 7일, 5회 → 30일 자동 부여

### 5. SSE 실시간 알림 + 유실 방지
- `ConcurrentHashMap`으로 userId별 SseEmitter 관리 (단일 기기)
- 알림 발생 시 **DB에 먼저 저장** → SSE 전송 (유실 방지, 다중 서버 대응)
- SSE 미연결 시에도 DB에 저장 → 재연결 후 미수신 알림 조회 가능
- 이벤트 종류: `MATCH_CONFIRMED` / `NO_SHOW` / `NEW_JOB_POSTING` / `PENALTY_EXPIRED`

### 6. JWT 단일 기기 로그인
- AccessToken(15분) + RefreshToken(7일) RTR 방식
- **AccessToken도 Redis에 저장** → 새 기기 로그인 시 기존 기기 즉시 무효화
- 매 요청마다 Redis의 AccessToken과 일치 여부 검증

### 7. 병렬 알림 발송 (ThreadPoolTaskExecutor + CompletableFuture)
- 공고 등록 시 반경 내 Worker들에게 알림을 **병렬로 동시 발송**
- `ThreadPoolTaskExecutor` (core 5, max 10, queue 200)
- 일부 실패해도 나머지 Worker 알림은 정상 발송

### 8. Spring Batch (별도 서버)
- **만료 공고 자동 CLOSED**: 매 시간, JpaPagingItemReader 100건 청크 처리
- **패널티 만료 알림**: 매 시간 30분, Worker/Employer Step 분리

<br>

## 📦 모듈 의존성

```
sukima-domain
    ↑
sukima-infrastructure (domain 참조)
    ↑
sukima-application (domain + infrastructure 참조)
    ↑
sukima-web (전체 참조 + Security, JWT, SSE)
sukima-batch (전체 참조 + Spring Batch)
```

## 🗄️ DDL

📄 [노션에서 보기](https://button-pixie-53d.notion.site/29c9adcc9f4d80f9a203ebcac2df5bbb)

<br>

## 📡 API 명세

Swagger UI를 통해 확인할 수 있습니다.

```
https://sukima-api.duckdns.org/swagger-ui/index.html
```
