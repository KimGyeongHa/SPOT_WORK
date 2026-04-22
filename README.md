# API-SUKIMA

> 스키마바이트(すきまバイト) 서비스를 모티브로 한 단기 알바 매칭 플랫폼 API

<br>

## 📌 프로젝트 개요

본 프로젝트는 일본의 단기 알바 매칭 서비스 **스키마바이트**를 모티브입니다.
학습했던 기술들을 사용하여, 새로운 아키텍처 패턴을 직접 적용해보기 위한 **학습 목적의 포트폴리오 프로젝트**입니다.

<br>

## 🏗️ 아키텍처

### 멀티모듈 구조

```
API-SUKIMA/
├── sukima-domain/          # 도메인 엔티티, 타입 정의 (순수 Java)
├── sukima-application/     # 유스케이스(Port/in), 서비스 구현체
├── sukima-infrastructure/  # JPA Entity, Repository, DB 연동
└── sukima-web/             # Controller, DTO, Security, 진입점
```

### 아키텍처 선택 배경

본 프로젝트는 **헥사고날 아키텍처(Ports & Adapters)** 를 참고하되,  
1인 개발 환경과 학습 효율을 고려하여 **실무에서 많이 사용하는 방식으로 타협한 구조**를 채택했습니다.

| 구분 | 헥사고날 퓨리스트 | 본 프로젝트 |
|------|---------------|------------|
| Port/out | application 레이어에 인터페이스 정의 | 생략 |
| 의존성 방향 | application → port/out ← infrastructure | application → infrastructure 직접 참조 |
| 장점 | 완전한 의존성 역전 | 구조 단순화, 개발 속도 향상 |

> **학습 포인트**: 헥사고날 아키텍처의 핵심인 Port/in(UseCase 인터페이스)은 유지하여  
> 레이어 간 역할 분리와 인터페이스 기반 설계는 유지합니다.

<br>

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.1 |
| ORM | Spring Data JPA, Hibernate Spatial |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| Cache | Redis 7.2 |
| Auth | JWT (jjwt 0.12.5), Spring Security |
| Infra | Docker, Docker Compose, Nginx |
| Build | Gradle (멀티모듈) |

<br>

## 🔑 핵심 기능 및 기술 포인트

### 1. 위치 기반 공고 조회 (PostGIS)
- `GEOMETRY(POINT, 4326)` 타입으로 공고 위치 저장
- `ST_DWithin`으로 현재 위치 기준 반경 N미터 이내 공고 조회
- `GIST 인덱스`로 공간 검색 성능 최적화

### 2. 동시성 제어 (Redis)
- 공고 지원 시 정원 초과 방지를 Redis로 atomic하게 처리
- 분산 환경에서도 안전한 동시성 보장

### 3. QR 체크인/아웃 + 위치 검증
- 근무 시작/종료 시 QR 스캔
- Haversine 공식으로 현재 위치가 근무지 반경 100m 이내인지 검증
- 체크아웃 시 근무 시간 기반 자동 정산

### 4. SSE 실시간 알림
- 매칭 확정 시 Worker에게 Server-Sent Events로 실시간 알림

### 5. JWT 인증 (Access + Refresh Token)
- AccessToken (15분) + RefreshToken (7일) 이중 토큰 구조
- RefreshToken은 Redis에 저장, RTR(Refresh Token Rotation) 방식 적용
- 재발급 시 기존 RefreshToken 폐기 → 탈취 방어

<br>

## 📦 모듈별 의존성

```
sukima-domain
    ↑
sukima-infrastructure (domain 참조)
    ↑
sukima-application (domain + infrastructure 참조)
    ↑
sukima-web (전체 참조 + Security, JWT)
```

<br>

## 🗄️ DDL

📄 [노션에서 보기](https://button-pixie-53d.notion.site/29c9adcc9f4d80f9a203ebcac2df5bbb?source=copy_link)

<br>

## 📡 API 명세

Swagger UI를 통해 확인할 수 있습니다.
```
http://localhost:8080/swagger-ui/index.html
```

<br>