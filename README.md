# Tribe

![Tribe Main](https://github.com/user-attachments/assets/793d1dc9-d765-445b-817a-006a9bcf40d3)

> 함께 여행을 계획하고, 실시간으로 소통하고, 비용을 정산하고, 여행 경험까지 공유하는 그룹 여행 협업 서비스

Tribe는 여행의 전 과정을 하나의 흐름으로 연결합니다.
여행을 만들고, 멤버를 초대하고, 장소와 동선을 조정하고, 채팅으로 의사결정을 맞추고, 지출을 정리하고, 여행 후에는 리뷰와 커뮤니티 포스트로 경험을 확장할 수 있습니다.

## What Tribe Solves

- 여러 명이 동시에 여행 일정을 수정해도 흐름이 끊기지 않는 공동 편집 경험
- 이동 거리와 소요 시간을 반영한 현실적인 일정 구성
- 다국적 통화와 복잡한 분담 관계를 고려한 스마트 정산
- 여행 중 대화와 여행 후 기록이 분리되지 않는 연속적인 사용자 경험
- AI 기반 여행 리뷰 생성과 장소 피드백을 통한 여행 품질 향상

## Main Features

### 1. Real-time Collaborative Planning

- 여러 사용자가 같은 여행 일정에 동시에 접근해 장소를 추가하고 순서를 조정할 수 있습니다.
- 이벤트 기반 처리와 WebSocket/STOMP 전송으로 변경 사항을 빠르게 공유합니다.
- Google Maps 기반 이동 거리와 시간을 반영해 일정 간 동선을 계산합니다.

### 2. Smart Expense Settlement

- 여행 멤버 간 지출과 분담을 집계해 최소 송금 횟수 기준의 정산 결과를 계산합니다.
- 외화 지출은 가장 가까운 시점의 환율을 찾아 KRW 기준으로 정리합니다.
- 일별 정산과 전체 여행 정산을 모두 제공합니다.

### 3. Travel Chat That Stays Fast

- 여행 단위 그룹 채팅을 제공하며, 실시간 메시지 전달과 이력 조회를 함께 지원합니다.
- 커서 기반 페이징을 적용해 채팅 데이터가 많아져도 안정적으로 이전 메시지를 불러올 수 있습니다.

### 4. AI-powered Travel Review

- Gemini 기반 AI가 여행 일정과 컨셉을 바탕으로 리뷰 초안을 생성합니다.
- 장소 맥락과 추천 포인트를 반영해 여행 후 콘텐츠 작성을 돕습니다.

### 5. Community-driven Reuse

- 여행 후기를 커뮤니티 포스트로 공유할 수 있습니다.
- 커뮤니티에 공개된 여행 흐름을 내 여행으로 가져와 재사용할 수 있습니다.

## 🛠 Tech Stack

| Category | Stack |
| :--- | :--- |
| **Frontend** | ![React](https://img.shields.io/badge/react-20232A.svg?style=for-the-badge&logo=react&logoColor=61DAFB) ![TypeScript](https://img.shields.io/badge/typescript-3178C6.svg?style=for-the-badge&logo=typescript&logoColor=white) ![Vite](https://img.shields.io/badge/vite-646CFF.svg?style=for-the-badge&logo=vite&logoColor=white) ![TailwindCSS](https://img.shields.io/badge/tailwindcss-38B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white) |
| **Backend** | ![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white) ![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=Spring%20Security&logoColor=white) |
| **Database & Cache** | ![PostgreSQL](https://img.shields.io/badge/postgresql-4169E1.svg?style=for-the-badge&logo=postgresql&logoColor=white) ![H2](https://img.shields.io/badge/h2-09476B.svg?style=for-the-badge&logoColor=white) ![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white) |
| **Infra & Realtime** | ![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white) ![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white) ![STOMP](https://img.shields.io/badge/STOMP-101010?style=for-the-badge&logoColor=white) |
| **DevOps** | ![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white) ![Vercel](https://img.shields.io/badge/vercel-000000.svg?style=for-the-badge&logo=vercel&logoColor=white) ![Argo CD](https://img.shields.io/badge/argo%20cd-EF7B4D.svg?style=for-the-badge&logo=argo&logoColor=white) ![Kubernetes](https://img.shields.io/badge/kubernetes-326CE5.svg?style=for-the-badge&logo=kubernetes&logoColor=white) |
| **AI & External API** | ![Google Gemini](https://img.shields.io/badge/google%20gemini-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white) ![Google Maps](https://img.shields.io/badge/google%20maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white) ![Kakao](https://img.shields.io/badge/kakao-FFCD00.svg?style=for-the-badge&logo=kakaotalk&logoColor=000000) |

## Service Architecture
![Tribe Architecture](https://github.com/user-attachments/assets/bd37b422-e0ec-4dbd-b387-2fd1f6b5af1f)

Tribe는 여행의 각 단계를 별도 도메인으로 나누고, 각 도메인이 분명한 책임을 가지도록 설계되어 있습니다.
계획, 소통, 정산, 회고를 한 시스템 안에서 이어가되, 내부 구현은 서비스 역할 기준으로 분리되어 있습니다.

| Service | 설명 |
| --- | --- |
| **Trip Service** | 여행 생성, 멤버 관리, 권한(OWNER/ADMIN/MEMBER), Redis 기반 초대 흐름 담당 |
| **Itinerary Service** | 장소 추가, 일차별 일정 구성, 순서 변경, 이동 경로/시간 계산 담당 |
| **Expense Service** | 지출 등록, 분담 관리, 환율 반영, 최소 송금 정산 계산 담당 |
| **Chat Service** | 실시간 그룹 채팅, 메시지 저장, 커서 기반 이력 조회 담당 |
| **Community Service** | 여행 후기 공유, 커뮤니티 포스트 조회, 여행 일정 재사용 담당 |
| **AI Review Service** | Gemini 기반 여행 리뷰 생성과 피드백 조합 담당 |

## Core Logic

### 1. Smart Settlement Strategy

정산은 각 멤버의 순 잔액을 계산한 뒤, 가장 많이 줄 사람과 가장 많이 받을 사람을 차례대로 매칭하는 방식으로 최소 송금 경로를 계산합니다.

```kotlin
val transferOriginal = debtorBalance.abs().min(creditorBalance)
val transferKrw = transferOriginal.multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP)
```

- 다자간 채무 관계를 단순한 송금 관계로 압축
- 외화 정산 시 환율을 함께 반영해 KRW 기준 결과 제공
- 일별/전체 기준 모두 동일한 계산 전략 유지

### 2. Redis-based Invitation Flow

초대 링크는 Redis에 토큰을 저장하는 방식으로 관리됩니다.

1. `SecureRandom`으로 토큰 생성
2. Redis에 초대 토큰과 여행 식별자 저장
3. 참여 요청 시 토큰 유효성 검사
4. 신규 가입 또는 기존 탈퇴 멤버 복구 처리

이 구조 덕분에 만료 관리와 재참여 처리를 비교적 단순하게 유지할 수 있습니다.

### 3. Cursor-based Chat History

채팅 이력은 `cursor(createdAt, id)` 기반으로 페이지를 탐색합니다.

- 대용량 채팅에서도 오프셋 기반 조회보다 안정적
- 실시간 메시지 수신과 과거 이력 탐색을 분리해 성능 유지
- 무한 스크롤 UX에 적합한 응답 구조 제공

### 4. Route-aware Planning

장소 검색과 상세 조회는 Google Places 계열 API를 활용하고, 일정 간 경로 계산은 이동 수단별 거리와 시간을 기준으로 제공합니다.

- 장소 검색
- 장소 상세/사진 조회
- 인접 일정 간 이동 시간 계산
- 전체 여행 동선 확인

## API Preview

### Trip

| URI | Method | 설명 |
| --- | --- | --- |
| `/api/v1/trips` | `POST` | 새로운 여행 생성 |
| `/api/v1/trips/{tripId}/invite` | `POST` | Redis 기반 초대 링크 생성 |
| `/api/v1/trips/join` | `POST` | 초대 토큰으로 여행 참여 |
| `/api/v1/trips/import` | `POST` | 커뮤니티 기반 여행 가져오기 |

### Itinerary

| URI | Method | 설명 |
| --- | --- | --- |
| `/api/v1/trips/{tripId}/items` | `POST` | 여행 일정 아이템 추가 |
| `/api/v1/trips/{tripId}/items/order` | `PATCH` | 일정 순서 일괄 변경 |
| `/api/v1/trips/{tripId}/items/directions` | `GET` | 전체 이동 경로 및 소요 시간 조회 |
| `/api/v1/places/search` | `GET` | 장소 검색 |

### Expense & Settlement

| URI | Method | 설명 |
| --- | --- | --- |
| `/api/v1/trips/{tripId}/expenses` | `POST` | 지출 등록 |
| `/api/v1/trips/{tripId}/settlements/daily` | `GET` | 특정 날짜 기준 정산 현황 조회 |
| `/api/v1/trips/{tripId}/settlements/total` | `GET` | 전체 여행 기준 최종 정산 조회 |

### Chat & Review

| URI | Method | 설명 |
| --- | --- | --- |
| `/api/v1/trips/{tripId}/chat` | `POST` | 채팅 메시지 전송 |
| `/api/v1/trips/{tripId}/chat` | `GET` | 커서 기반 채팅 이력 조회 |
| `/api/v1/trips/{tripId}/reviews` | `POST` | AI 여행 리뷰 생성 |
| `/api/v1/community/posts` | `POST` | 커뮤니티 포스트 작성 |

## Repository Layout

```text
.
├── frontend/   # 사용자 웹 애플리케이션
├── backend/    # Spring Boot Kotlin 멀티모듈 API 서버
└── ops/        # Vercel / Kubernetes / Argo CD 운영 구성
```

더 자세한 개발 정보는 아래 문서를 참고하면 됩니다.

- `frontend/README.md`
- `backend/README.md`
- `ops/README.md`
