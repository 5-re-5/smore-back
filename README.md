# smore-back

`s﻿more-back`은 학습 및 스터디 활동을 관리하는 백엔드 서비스입니다. 이 서비스는 스터디룸, 참여자, 집중도 기록, 포인트, 출석 등의 기능을 다룹니다.

---

##  기능 목록
- **스터디룸 관리**  
  - 스터디룸 생성, 조회, 수정, 삭제
  - 참여자 초대 및 관리
- **참여자(유저) 관리**  
  - 회원가입 및 로그인  
  - 프로필 관리
- **집중도(포커스) 기록 기능**  
  - 사용자의 집중 시간 기록 및 조회  
  - 일/주/월별 집중도 통계 제공
- **포인트 시스템**  
  - 학습 활동에 따른 포인트 적립 및 차감 기능  
  - 활동 보상 및 리워드 시스템 연동 가능
- **출석 관리 기능**  
  - 스터디룸별 출석 기록 기능 (예: 출석 체크, 지각 등)  
  - 출석 현황 통계 및 리포트 기능 제공
---
## 📦 Dependencies

| 항목 | 버전/설명 |
|------|-----------|
| **Java** | 17 이상 |
| **Spring Boot** | 3.x (Gradle 기반) |
| **Spring Web** | REST API 개발 |
| **Spring Security** | 인증/인가 처리 |
| **Spring Data JPA** | ORM, 데이터베이스 액세스 |
| **OAuth2 Client** | 소셜 로그인(Kakao 등) |
| **MySQL Driver** | MySQL 연결용 JDBC 드라이버 |
| **Lombok** | 보일러플레이트 코드 감소 |
| **Validation (Jakarta Validation)** | 요청 파라미터 유효성 검사 |
| **Jackson** | JSON 직렬화/역직렬화 |
| **Springdoc OpenAPI** | API 문서화(Swagger UI) |
| **JUnit 5** | 단위/통합 테스트 |
| **Mockito** | 테스트용 Mock 객체 생성 |
| **Redis Client (spring-data-redis)** | 캐싱, 토큰 저장 |
| **Dockerfile** | 컨테이너 빌드/배포 지원 |

---

##  Getting Started

### 1. Clone the repository  
```bash
git clone https://github.com/5-re-5/smore-back.git
cd smore-back
```
### 2. Install dependencies
```bash
npm install
# 또는
yarn install
```
### 3. Environment setup
프로젝트 루트에 .env 파일 생성 후 다음 항목 설정:

```env
DATABASE_URL=postgresql://username:password@localhost:5432/smoredb
JWT_SECRET=your_jwt_secret_key
PORT=3000
```

### 4. Run database migrations (ORM에 따라 명령 다름)
- Prisma:

```bash
npx prisma migrate dev --name init
```
- TypeORM:

```bash
npx typeorm migration:run
```

### 5. Start the server
```bash
npm run dev
# 또는
yarn dev
```

http://localhost:8081 에서 서버가 실행됩니다.

### 6. API 테스트
Postman, Insomnia 또는 Swagger 등을 이용해 엔드포인트 테스트를 진행할 수 있습니다.

## 📌 API Endpoints

- **POST** `/api/auth/signup` — 회원가입  
- **POST** `/api/auth/login` — 로그인 (JWT 발급)  
- **GET** `/api/users/me` — 사용자 정보 조회 (인증 필요)  
- **GET** `/api/rooms` — 스터디룸 목록 조회  
- **POST** `/api/rooms` — 스터디룸 생성  
- **GET** `/api/rooms/:roomId` — 특정 스터디룸 정보 조회  
- **PUT** `/api/rooms/:roomId` — 스터디룸 정보 수정  
- **DELETE** `/api/rooms/:roomId` — 스터디룸 삭제  
- **POST** `/api/rooms/:roomId/join` — 스터디룸 참가 요청  
- **GET** `/api/rooms/:roomId/participants` — 참여자 목록 조회  
- **POST** `/api/focus-records` — 집중도 기록 생성  
- **GET** `/api/focus-records?userId=&roomId=&start=&end=` — 집중도 기록 조회 및 필터링  
- **GET** `/api/user/:userId/points` — 사용자 포인트 조회  
- **POST** `/api/user/:userId/points` — 포인트 조정 (적립/차감)  
- **POST** `/api/attendance` — 출석 체크  
- **GET** `/api/attendance?roomId=&date=` — 출석 현황 조회  

---

## 📂 Folder Structure

```plaintext
└── smore-back/
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── org/
    │   │   │       └── oreo/
    │   │   │           └── smore/
    │   │   │               ├── domain/           # 도메인별 패키지 (auth, user, studyroom, focusrecord 등)
    │   │   │               │   ├── auth/         # JWT, OAuth2, Security 설정
    │   │   │               │   ├── user/         # 사용자 엔티티, 서비스, 컨트롤러
    │   │   │               │   ├── studyroom/    # 스터디룸 관련 로직
    │   │   │               │   ├── focusrecord/  # 집중도 기록
    │   │   │               │   └── common/       # 공통 유틸, 예외 처리
    │   │   │               └── global/           # 전역 설정, 시큐리티, 환경설정
    │   │   └── resources/
    │   │       ├── application.properties        # 기본 설정
    │   └── test/
    │       └── java/
    │           └── org/
    │               └── oreo/
    │                   └── smore/                # 테스트 코드
    ├── build.gradle
    ├── settings.gradle
    ├── Dockerfile
    ├── README.md
    └── .env
```

