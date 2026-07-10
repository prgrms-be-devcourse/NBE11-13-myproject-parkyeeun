# Repoary

Repoary는 GitHub 학습 저장소의 커밋과 변경 파일을 분석해 TIL 초안과 월별 README 행 생성을 돕는 서비스이다.

## 프로젝트 목표

GitHub 커밋과 변경 파일을 분석해서 TIL 초안과 월별 README 행을 생성하고, 사용자가 편집해 가져갈 수 있게 한다.

## MVP 기능

* GitHub OAuth 로그인
* GitHub 저장소 목록 조회 및 연결
* 저장소별 기본 규칙 생성
* 날짜별 커밋 및 변경 파일 분석
* TIL Markdown 초안 생성 및 편집
* 월별 README Summary 행 생성

## MVP 제외 범위

* GitHub 파일 직접 생성·수정
* 고급 분류 규칙 관리
* 고급 커밋 컨벤션 관리
* AI API 연동
* 로컬 미커밋 파일 분석

## 기술 스택

### Backend

* Java 17
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL
* Flyway

### Frontend

* React
* TypeScript
* Vite
* Tailwind CSS

### Infra

* Render
* Vercel
* Supabase PostgreSQL

## 프로젝트 구조

```text
repoary/
├── backend/
├── frontend/
├── README.md
└── .gitignore
```

## 개발 순서

1. `/api/health`와 기본 화면 구현
2. GitHub OAuth 로그인 구현
3. GitHub 저장소 목록 조회
4. 저장소 연결 및 기본 규칙 생성
5. 날짜별 커밋과 변경 파일 분석
6. 기본 점검 수행
7. TIL 초안 생성
8. README 행 생성

## 개발 원칙

* `.env`, DB 비밀번호, GitHub OAuth Secret, JWT Secret 등은 커밋하지 않는다.
* MVP 범위를 우선 완성하고 확장 기능은 이후 단계로 둔다.
