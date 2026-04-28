# 자동차 정비업체 통합 관리 시스템

## 📌 프로젝트 개요

전북공업사(자동차 정비업체)의 차량 입출고·보험청구·대차·미수금 업무를 통합 관리하는 **데스크탑 애플리케이션**.

기존에 Excel(.xlsx)로 운영하던 시스템을 Java 기반 데스크탑 앱으로 마이그레이션.

### 사용 환경
- **사용자**: 1명 (사무 직원)
- **사용 PC**: 단일 PC (로컬 전용)
- **인터넷**: 불필요 (오프라인 동작)
- **유지보수**: 사내 개발자 직접 유지

---

## 🛠 기술 스택

```
Java 17 (LTS, OpenJDK)
├── JavaFX 21       ← 데스크탑 UI
├── SQLite + JDBC   ← 로컬 DB (단일 파일)
├── Hibernate ORM   ← DB 매핑
├── Apache POI      ← Excel import/export
└── Gradle          ← 빌드 도구
```

**배포**: jpackage로 .exe 생성 (JDK 포함, 별도 설치 불필요)
**IDE**: IntelliJ IDEA Community (무료)

---

## 🏢 도메인 (업무 프로세스)

### 1. 차량 입고
- 입고일, 차량명, 차량번호, 전화번호, 수리구분(일반수리/보험수리), 사고유형 기록
- 입고번호 자동 생성: `YYMMDD-NN` (예: `260424-01`)

### 2. 대차 관리 (선택)
- 회사 보유 차량 19대 중 1대를 무료로 대여
- 대차 1건 = 1행으로 기록 (차종 변경 시 행 추가)
- 입고번호로 입출고와 연결

### 3. 차량 출고
- 출고일, 자기부담금(자차보험 시), 자부담수납일

### 4. 보험 청구
- 자차 보험: 보험사·청구일·청구액·실수령액·입금일
- 상대방 보험: 보험사·청구일·청구액·실수령액·입금일

### 5. 자동 집계
- 진행상태(수리중/출고완료/청구완료/수령완료)
- 미수금 = 청구액 - 실수령액
- 보험사별·월별 통계
- 대차 현황·미수금 현황

---

## 📊 도메인 데이터

### 보유 대차 차량 19대

```
SM3 2호       / 34버7483      / 휘발유  / 삼성  / 만24세 / 전북공업사
L43 2호       / 57구8647      / LPG     / 삼성  / 만26세 / 전북공업사
리베로        / 전북98사3693   / 경유    / DB    / 만26세 / 전북공업사
SM3 3호       / 377어6916     / 휘발유  / DB    / 만24세 / 전북공업사
포터2더블     / 80거0997      / 경유    / DB    / 만26세 / 전북공업사
QM3           / 02다7842      / 경유    / 삼성  / 만26세 / 전북공업사
니로          / 31구0771      / 무연전기 / 현대 / 28세   / 류명선
봉고3         / 86우2845      / 경유    / 현대  / 만24세 / 류명선
SM3 1호       / 41두8892      / 휘발유  / 삼성  / 만26세 / 전북공업사
티볼리        / 141버3059     / 경유    / 삼성  / 만26세 / 전북공업사
코나          / 22나3641      / 전기    / KB    / 임직원한정 35 / 전북공업사
뉴SM5         / 161조8450     / 휘발유  / 현대  / 만26세 / 전북공업사
아이오닉5     / 39주1360      / 전기    / KB    / 임직원한정 35 / 전북공업사
투싼          / 58저1364      / 경유    / 흥국  / 26세   / 전북공업사
쏘렌토        / 141조2749     / 경유    / 현대  / 만26세 / 전북공업사
투싼ix        / 51어6810      / 경유    / 현대  / 만26세 / 전북공업사
그랜져TG      / 49루3727      / LPG     / 현대  / 만26세 / 전북공업사
아반떼        / 151조8434     / LPG전기 / 현대  / 만24세 / 전북공업사
모닝          / 64두9795      / 휘발유  / 하나  / 만24세 / 전북공업사
```

### 보험사 12개

삼성화재, 현대해상, DB손해보험, KB손해보험, 메리츠화재, 한화손해보험, 롯데손해보험, MG손해보험, 흥국화재, AXA손해보험, 하나손해보험, 캐롯손해보험

---

## 🗂 DB 스키마 (초안)

### customer_intake (입고관리)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | INTEGER PK | |
| intake_no | TEXT UNIQUE | 자동생성 (260424-01) |
| intake_date | DATE | |
| vehicle_name | TEXT | 차량명 (영문은 대문자) |
| vehicle_number | TEXT | 차량번호 (한글 포함 강제) |
| phone | TEXT | 010-0000-0000 형식 |
| repair_type | TEXT | 일반수리 / 보험수리 |
| accident_type | TEXT | 자차 / 대물 / 일반수리 / 과실 (다중 선택) |
| release_date | DATE | 출고일 |
| self_pay_amount | INTEGER | 자기부담금 |
| self_pay_date | DATE | 자부담수납일 |
| created_at, updated_at | TIMESTAMP | |

### insurance_claim (보험청구)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | INTEGER PK | |
| intake_id | INTEGER FK | |
| claim_side | TEXT | OWN(자차) / OPPONENT(상대) |
| insurance_company | TEXT | |
| claim_date | DATE | |
| claim_amount | INTEGER | |
| received_amount | INTEGER | |
| received_date | DATE | 입금일 |

### rental_history (대차이력)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | INTEGER PK | |
| intake_id | INTEGER FK | |
| rental_vehicle_id | INTEGER FK | |
| rental_start_date | DATE | |
| rental_end_date | DATE | NULL이면 대차중 |
| memo | TEXT | |

### rental_vehicle (대차차량)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | INTEGER PK | |
| name | TEXT | 차종 |
| number | TEXT | 차량번호 |
| fuel | TEXT | |
| insurance | TEXT | |
| age_limit | TEXT | |
| owner | TEXT | |

### insurance_company (보험사)
| 컬럼 | 타입 |
|---|---|
| id | INTEGER PK |
| name | TEXT UNIQUE |

---

## 🖥 화면 구성 (초안)

1. **메인 대시보드** — 진행중 건수, 미수금 총액, 대차중 차량 수
2. **입고관리** — 차량 입고 등록·조회·수정
3. **대차관리** — 대차 이력 관리, 현재 대차중 차량
4. **보험청구** — 청구·수령 관리, 30일 경과 미수령 알림
5. **미수금현황** — 자동 집계
6. **통계** — 보험사별·월별 통계
7. **설정** — 대차차량·보험사 마스터 관리

---

## ✅ 기능 요구사항

### 자동 처리
- [ ] 입고번호 자동 생성 (날짜+순번)
- [ ] 차량명 영문 입력 시 자동 대문자 변환 (TextField focusLost 이벤트)
- [ ] 차량번호 한글 포함 검증 (정규식 `.*[가-힣]+.*`)
- [ ] 전화번호 자동 포매팅 (11자리 숫자 → 010-0000-0000)
- [ ] 사고유형 다중 선택 (CheckBox 4개: 자차/대물/일반수리/과실)
- [ ] 사고유형 선택 시 보험청구 입력칸 자동 활성화
- [ ] 대차중 셀 클릭 시 빌려준 차종 팝업 (Tooltip 또는 Dialog)
- [ ] 자차/일반수리 시 자기부담금 칸 색상 변경
- [ ] 진행상태 자동 산출
- [ ] 미수금 자동 계산
- [ ] 청구 후 30일 경과 미수령 시 빨간 표시

### 데이터 관리
- [ ] 기존 Excel(.xlsx) import 기능 (Apache POI)
- [ ] Excel export 기능 (백업·보고용)
- [ ] DB 자동 백업 (일일 단위)
- [ ] 검색·필터·정렬

### 통계
- [ ] 보험사별 청구·수령·미수금 집계
- [ ] 월별 입출고·청구·수령 집계

---

## ⚠️ 기존 Excel에서 막혔던 부분 (앱에서는 해결)

| Excel 한계 | 앱에서 해결 방법 |
|---|---|
| 금액 입력 시 자동으로 오늘 날짜 못 채움 (TODAY() 순환참조 불안정) | TextField listener에서 LocalDate.now() 자동 입력 |
| 영문 자동 대문자 불가 | TextFormatter로 입력 즉시 toUpperCase() |
| 셀 클릭 이벤트 없음 | TableView의 onMouseClicked 이벤트 |
| 다중 선택 드롭다운 불가 | CheckBox 그룹 또는 CheckComboBox |
| 시트별 데이터 정합성 깨지기 쉬움 | DB의 FK 제약 + 트랜잭션 |
| 동시 수정 시 파일 충돌 | DB 단일 진실 공급원 |

---

## 📁 프로젝트 구조 (제안)

```
auto-repair-manager/
├── build.gradle
├── src/main/java/com/jeonbuk/repair/
│   ├── Main.java
│   ├── controller/      ← JavaFX 컨트롤러
│   ├── model/           ← Entity (Hibernate)
│   ├── repository/      ← DAO
│   ├── service/         ← 비즈니스 로직
│   ├── util/            ← 포매터·검증
│   └── view/            ← FXML 로딩
├── src/main/resources/
│   ├── fxml/            ← 화면 정의
│   ├── css/             ← 스타일
│   └── db/migration/    ← DB 마이그레이션 스크립트
└── data/
    └── repair.db        ← SQLite 파일
```

---

## 🎯 개발 우선순위

### Phase 1 (MVP)
1. 프로젝트 셋업 (Gradle, JavaFX, SQLite, Hibernate)
2. DB 스키마 + Entity
3. 입고관리 CRUD 화면
4. 대차관리 CRUD 화면
5. 보험청구 CRUD 화면

### Phase 2 (자동화)
1. 입력 자동 처리 (대문자/한글검증/전화번호 포매팅)
2. 진행상태·미수금 자동 산출
3. 30일 경과 알림

### Phase 3 (편의 기능)
1. 메인 대시보드
2. 통계 화면
3. Excel import/export
4. DB 백업

### Phase 4 (마무리)
1. UI 다듬기 (CSS)
2. jpackage로 .exe 빌드
3. 사용 매뉴얼

---

## ✅ 테스트 정책 (필수)

**새 기능을 추가하거나 기존 로직을 수정할 때마다 같은 PR에 테스트 코드를 함께 작성한다.** 자세한 가이드는 `TESTING.md`.

핵심 룰:
1. `service/`, `util/`, `model/` 의 새 public 메서드 → 단위 테스트 (DB 없음)
2. `repository/` 의 새 쿼리 또는 service 의 트랜잭션 흐름 → `@ExtendWith(DatabaseExtension.class)` 통합 테스트
3. 버그 수정은 **재현 테스트 먼저**, 그 다음 fix
4. 머지 전 `./gradlew check` 그린 확인. CI(`.github/workflows/ci.yml`)도 그린.
5. UI/컨트롤러 테스트는 Phase 4 에서 TestFX 도입까지 보류 — 그 사이엔 컨트롤러를 얇게 유지하고 핵심 로직을 service/util 로 빼서 단위 테스트로 커버.

빠른 명령:
```bash
./gradlew test                        # 전체
./gradlew test --tests "*Service*"    # 패턴
./gradlew check                       # test + 정적 검사
```

리포트: `build/reports/tests/test/index.html` · `build/reports/jacoco/test/html/index.html`

---

## 📚 참고

기존 Excel 시스템은 9개 시트 (사용법, 입출고관리대장, 대차이력, 대차차량목록, 대차현황, 미수금현황, 보험사별집계, 월별집계, 설정) 로 구성. 앱 화면 설계 시 이 구조 참고.

---

작업 시작 전 다음 사항 확인 요망:
- Java 17 설치 여부
- IntelliJ IDEA 또는 VS Code + Java extension 설치
- 기존 Excel 파일 (`자동차정비_입출고관리.xlsx`) 위치 (import 시 필요)