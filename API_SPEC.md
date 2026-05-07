# API 명세서

Base URL: `http://localhost:8080`

---

## 공통

### 인증
JWT Bearer 토큰 방식 사용.
인증이 필요한 API는 모든 요청 헤더에 아래를 포함해야 한다.

```
Authorization: Bearer {accessToken}
```

### 공통 에러 응답

```json
{
  "code": "에러코드",
  "message": "에러 메시지",
  "errors": { "필드명": "상세 메시지" }  // validation 에러 시만 포함
}
```

| HTTP 상태 | code | 설명 |
|-----------|------|------|
| 400 | `BAD_REQUEST` | 요청 값 유효성 검사 실패 |
| 400 | `IMAGE_UPLOAD_FAILED` | 이미지 업로드 실패 (빈 파일, 크기 초과, 지원하지 않는 형식) |
| 401 | `UNAUTHORIZED` | 인증 실패 또는 토큰 만료 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 중복 리소스 (이메일 등) |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 오류 |

---

## Auth

### 회원가입
`POST /api/auth/signup`

인증 불필요

**Request Body**
```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "password": "password1234",
  "profileImageUrl": "/images/profile/abc123.jpg"
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| name | string | Y | 최대 20자 |
| email | string | Y | 이메일 형식 |
| password | string | Y | 8~64자 |
| profileImageUrl | string | N | 이미지 업로드 후 받은 URL |

**Response** `201`
```json
1
```
> 생성된 memberId 반환

---

### 로그인
`POST /api/auth/login`

인증 불필요

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password1234"
}
```

**Response** `200`
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci..."
}
```

---

### 토큰 재발급
`POST /api/auth/refresh`

인증 불필요

**Request Body**
```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response** `200`
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci..."
}
```

---

### 로그아웃
`POST /api/auth/logout`

인증 필요

**Request Header**
```
Authorization: Bearer {accessToken}
```

**Response** `200` (body 없음)

---

### 내 정보 조회
`GET /api/auth/me`

인증 필요

**Response** `200`
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동#A1B2",
  "role": "ROLE_USER",
  "profileImageUrl": "/images/profile/abc123.jpg"
}
```

> `name` 필드는 `{name}#{tag}` 형식의 표시 이름
> `profileImageUrl`은 미설정 시 `null`

---

## 이미지

### 이미지 업로드
`POST /api/images/upload`

인증 필요

**Request** `multipart/form-data`

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| file | file | Y | 업로드할 이미지 파일 |
| directory | string | N | 저장 경로 구분자 (기본값: `common`) |

`directory` 권장 값: `profile`, `diary`

제약 사항:
- 최대 파일 크기: **5MB**
- 허용 형식: `image/jpeg`, `image/png`, `image/webp`, `image/gif`

**Response** `200`
```json
{
  "url": "/images/diary/550e8400-e29b-41d4-a716-446655440000.jpg"
}
```

**이미지 접근**
업로드된 이미지는 아래 URL로 인증 없이 접근 가능하다.
```
GET /images/{directory}/{filename}
```

---

## 다이어리

### 다이어리 작성
`POST /api/diaries`

인증 필요

**Request Body**
```json
{
  "title": "오늘의 일기",
  "content": "오늘은 정말 좋은 하루였다.",
  "diaryDate": "2026-03-29",
  "weather": "SUNNY",
  "isSecret": false,
  "imageUrls": [
    "/images/diary/uuid1.jpg",
    "/images/diary/uuid2.jpg"
  ]
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| title | string | Y | 최대 100자 |
| content | string | Y | - |
| diaryDate | string | Y | `yyyy-MM-dd` 형식 |
| weather | string | N | `SUNNY` `CLOUDY` `RAINY` `SNOWY` |
| isSecret | boolean | Y | 공개 여부 (`false` = 공개) |
| imageUrls | array | N | 이미지 업로드 후 받은 URL 목록, 순서 유지 |

**Response** `201`
```json
1
```
> 생성된 diaryId 반환

---

### 다이어리 단건 조회
`GET /api/diaries/{diaryId}`

인증 필요 (본인 다이어리만 조회 가능)

**Response** `200`
```json
{
  "id": 1,
  "title": "오늘의 일기",
  "content": "오늘은 정말 좋은 하루였다.",
  "diaryDate": "2026-03-29",
  "weather": "SUNNY",
  "isSecret": false,
  "status": "PENDING",
  "createdAt": "2026-03-29T12:00:00",
  "emotions": [
    { "emotionName": "기쁨", "score": 0.85 }
  ],
  "keywords": ["좋은 하루", "산책"],
  "imageUrls": [
    "/images/diary/uuid1.jpg",
    "/images/diary/uuid2.jpg"
  ]
}
```

| 필드 | 설명 |
|------|------|
| status | `PENDING` (분석 대기) · `ANALYZING` (분석 중) · `COMPLETED` (분석 완료) |
| emotions | AI 감정 분석 결과, `status`가 `COMPLETED`일 때만 채워짐 |
| keywords | AI 키워드 분석 결과, `status`가 `COMPLETED`일 때만 채워짐 |
| imageUrls | 등록 순서대로 반환 |

---

### 다이어리 목록 조회
`GET /api/diaries?page=0&size=20`

인증 필요 (본인 다이어리만 조회)

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | integer | 0 | 페이지 번호 (0부터 시작) |
| size | integer | 20 | 페이지 크기 |

**Response** `200`
```json
{
  "content": [
    {
      "id": 1,
      "title": "오늘의 일기",
      "diaryDate": "2026-03-29",
      "weather": "SUNNY",
      "isSecret": false,
      "status": "PENDING",
      "createdAt": "2026-03-29T12:00:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

> 목록에는 본문·감정·키워드·이미지는 포함되지 않음. 단건 조회로 가져올 것.

---

### 다이어리 수정
`POST /api/diaries/{diaryId}/update`

인증 필요 (본인 다이어리만 수정 가능)

**Request Body**
```json
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "weather": "RAINY",
  "isSecret": true,
  "imageUrls": [
    "/images/diary/uuid3.jpg"
  ]
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| title | string | Y | 최대 100자 |
| content | string | Y | - |
| weather | string | N | `SUNNY` `CLOUDY` `RAINY` `SNOWY` |
| isSecret | boolean | Y | - |
| imageUrls | array | N | 기존 이미지를 이 목록으로 **전체 교체**, `null` 또는 빈 배열이면 이미지 전체 삭제 |

**Response** `200` (body 없음)

---

### 다이어리 삭제
`POST /api/diaries/{diaryId}/delete`

인증 필요 (본인 다이어리만 삭제 가능)

**Response** `200` (body 없음)

---

## OAuth2 소셜 로그인

소셜 로그인은 브라우저 리다이렉트 방식으로 동작하며 Postman으로 직접 테스트가 어렵다.

| 제공자 | 로그인 시작 URL |
|--------|----------------|
| Google | `GET /oauth2/authorization/google` |
| Naver | `GET /oauth2/authorization/naver` |
| Kakao | `GET /oauth2/authorization/kakao` |

로그인 성공 시 `accessToken`과 `refreshToken`을 쿼리 파라미터로 프론트엔드 리다이렉트 URL에 전달한다.
