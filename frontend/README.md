# Tribe Frontend

Tribe 프런트엔드는 그룹 여행 계획, 일정 편집, 정산, 커뮤니티, 실시간 채팅 기능을 제공하는 SPA입니다.
Vite 기반 React 애플리케이션이며, 백엔드 API와 WebSocket 서버는 별도 origin으로 분리되어 동작합니다.

## 기술 스택

- React 18
- TypeScript
- Vite
- React Router
- TanStack Query
- Tailwind CSS
- shadcn/ui
- SockJS + STOMP
- Leaflet

## 디렉터리 구조

```text
frontend/
├── src/api          # REST / WebSocket 클라이언트
├── src/components   # 화면 단위 UI 컴포넌트
├── src/hooks        # 공통 훅, 실시간 동기화 훅
├── src/lib          # 도메인 유틸, query key, 지도/표현 계층
├── src/pages        # 라우트 페이지
├── public/          # 정적 리소스
└── vercel.json      # SPA rewrite 설정
```

## 주요 화면

| Route | 설명 |
| --- | --- |
| `/` | 대시보드, 여행 목록/진입점 |
| `/signup` | 회원가입 |
| `/oauth/callback` | OAuth 로그인 콜백 처리 |
| `/invite` | 초대 링크 참여 처리 |
| `/trip/:tripId` | 여행 일정 편집, 장소 검색, 채팅, 멤버 관리 |
| `/settlement/:tripId` | 여행 정산 화면 |
| `/community` | 커뮤니티 포스트 목록 |
| `/post/:postId` | 커뮤니티 포스트 상세 |
| `/profile/:memberId` | 사용자 프로필 |

## 개발 환경

### 요구 사항

- Node.js 20 계열 권장
- npm

### 실행

```bash
cd frontend
npm install
npm run dev
```

기본 개발 서버는 `http://localhost:8081`입니다.

### 주요 스크립트

```bash
npm run dev
npm run build
npm run build:dev
npm run lint
npm run typecheck
npm run preview
```

참고:

- 저장소에 `bun.lockb`가 있지만 실제 실행 스크립트와 Dockerfile은 `npm` 기준입니다.
- 프런트엔드 전용 CI 워크플로는 현재 저장소에 없습니다.

## 로컬 백엔드 연동

`vite.config.ts` 기준 개발 서버는 아래 경로를 백엔드 `http://localhost:8080`으로 프록시합니다.

- `/api`
- `/oauth2`
- `/login`
- `/ws`

즉, 프런트엔드만 따로 실행하더라도 백엔드가 `8080`에서 떠 있으면 로그인, REST API, WebSocket을 함께 테스트할 수 있습니다.

## 환경 변수

| 이름 | 기본값 | 설명 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api/v1` | REST API base URL |
| `VITE_BACKEND_ORIGIN` | `VITE_API_BASE_URL`에서 유도 또는 현재 origin | OAuth 로그인과 WebSocket 접속에 사용하는 origin |
| `VITE_WS_PATH` | `/ws` | WebSocket endpoint path |

운영 기본값 예시는 [frontend/.env.production.example](/Users/wingwogus/Projects/Tribe/frontend/.env.production.example)에 있습니다.

```bash
VITE_API_BASE_URL=https://api.tri-be.app/api/v1
VITE_BACKEND_ORIGIN=https://api.tri-be.app
```

## 인증 흐름

- OAuth 로그인은 백엔드의 `/oauth2/authorization/kakao`로 시작합니다.
- 액세스 토큰은 `localStorage`의 `accessToken` 키에 저장됩니다.
- API 요청은 Axios interceptor를 통해 `Authorization: Bearer ...` 헤더를 자동 부착합니다.
- 액세스 토큰이 만료되면 `/auth/reissue`를 호출해 재발급을 시도합니다.
- 세션 유지에는 쿠키와 액세스 토큰이 함께 사용됩니다.

운영용 카카오 OAuth 리다이렉트 URI:

```text
https://api.tri-be.app/login/oauth2/code/kakao
```

## 실시간 동기화

WebSocket 연결은 SockJS + STOMP 조합으로 구성되어 있습니다.

- 엔드포인트: `${VITE_BACKEND_ORIGIN}/ws`
- 여행 이벤트 토픽: `/sub/trips/{tripId}`
- 채팅 이벤트 토픽: `/sub/chat/rooms/{tripId}`

프런트엔드는 실시간 이벤트 수신 시 React Query 캐시를 invalidate하여 화면을 갱신합니다.

동기화되는 대표 데이터:

- 일정 아이템
- 이동 경로
- 위시리스트
- 여행 멤버 상태
- 채팅 이력
- 정산 데이터

## 데이터 계층

- `src/api/*`: 도메인별 REST API 래퍼
- `src/api/http.ts`: Axios 인스턴스, 토큰 재발급, OAuth/WebSocket URL 계산
- `src/lib/tripQueryKeys.ts`: React Query key 표준화
- `src/hooks/useTripWebSocket.ts`: 여행 단위 실시간 구독

`main.tsx`의 기본 QueryClient 설정:

- `staleTime`: 5분
- `gcTime`: 10분
- query retry: 1회
- `refetchOnWindowFocus`: 비활성화

## 배포

프런트엔드는 Vercel 배포를 기준으로 구성되어 있습니다.

- 운영 도메인: `https://tri-be.app`
- API 도메인: `https://api.tri-be.app`
- `vercel.json`은 모든 경로를 `/`로 rewrite 해서 SPA 라우팅을 보장합니다.

Vercel 배포 시 반드시 아래 환경 변수를 설정해야 합니다.

- `VITE_API_BASE_URL=https://api.tri-be.app/api/v1`
- `VITE_BACKEND_ORIGIN=https://api.tri-be.app`

## 검증 체크리스트

프런트엔드 변경 후 기본 확인 명령:

```bash
cd frontend
npm run lint
npm run typecheck
npm run build
```
