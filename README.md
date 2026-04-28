# 전북공업사 자동차 정비업체 통합 관리 시스템

자동차 정비업체의 차량 입출고·보험청구·대차·미수금 업무를 통합 관리하는 데스크탑 앱.

---

## ✅ Phase 1 (MVP) 완료 항목

- [x] 프로젝트 셋업 (Gradle 8.x, JavaFX 21, SQLite, Hibernate 6.4)
- [x] DB 스키마 + Hibernate Entity + 시드 데이터 (대차차량 19대 / 보험사 12개)
- [x] 입고관리 CRUD 화면 (검색·정렬·진행상태 자동 산출)
- [x] 대차관리 CRUD 화면 (대차중 차량 충돌 검증, 일수 자동 계산)
- [x] 보험청구 CRUD 화면 (자차/상대 구분, 30일 경과 미수령 빨간색 표시)

---

## 🛠 빌드 환경 준비

### 1단계 — Gradle 설치 (최초 1회)

이 PC 에는 Gradle 이 설치돼 있지 않습니다. 둘 중 하나를 선택하세요.

**(권장) IntelliJ IDEA Community 로 열기** — Gradle Wrapper 자동 생성

1. IntelliJ IDEA Community 다운로드 → 설치
2. `Open` → `C:\Users\HongSeongMin\Documents\car` 폴더 선택
3. Gradle 인덱싱 자동 실행 → 의존성 다운로드 자동 완료
4. 우측 Gradle 도구창 → `Tasks > application > run` 더블클릭

**(대안) Gradle CLI 설치**

```powershell
# winget 사용
winget install Gradle.Gradle

# 또는 Chocolatey
choco install gradle
```

설치 후 프로젝트 루트에서:

```bash
gradle wrapper --gradle-version 8.7   # gradlew / gradlew.bat 생성
./gradlew run                         # 실행
```

### 2단계 — 실행

```bash
./gradlew run
```

최초 실행 시 사용자 영역 (`%APPDATA%\전북공업사\repair.db`, 비-Windows 는 `~/.jeonbuk-repair/repair.db`) 에 DB 가 자동 생성되고 마이그레이션 SQL 이 실행됩니다. 다른 위치를 쓰려면 환경변수 `JEONBUK_REPAIR_HOME` 으로 오버라이드.

릴리스 빌드(.exe)는 [`BUILD.md`](BUILD.md), DB 스키마 변경은 [`MIGRATIONS.md`](MIGRATIONS.md) 참고.

---

## 📁 프로젝트 구조

```
auto-repair-manager/
├── build.gradle                       Gradle 빌드 정의
├── settings.gradle
├── gradle.properties
├── src/main/java/com/jeonbuk/repair/
│   ├── Main.java                      애플리케이션 진입점
│   ├── controller/                    JavaFX 컨트롤러 (입고/대차/청구/메인)
│   ├── model/                         Hibernate Entity + Enum
│   ├── repository/                    DAO
│   ├── service/                       비즈니스 로직 + ServiceRegistry
│   ├── util/                          HibernateUtil, Tx, Formatters, Validators, Dialogs, DatabaseInitializer
│   └── view/                          ViewLoader (FXML 로딩 헬퍼)
├── src/main/resources/
│   ├── fxml/                          화면 정의 (main / customer_intake / rental_history / insurance_claim)
│   ├── css/app.css                    스타일
│   ├── db/migration/                  V001__init.sql, V002__seed.sql, V003__add_tow_info.sql
│   ├── hibernate.cfg.xml              SessionFactory 설정
│   └── logback.xml                    로깅
└── %APPDATA%\전북공업사\
    ├── repair.db                      SQLite 단일 파일 DB (사용자 영역 — 재설치해도 보존)
    └── backup\repair-YYYYMMDD.db      앱 시작 시 일일 백업 (최근 10개 유지)
```

---

## 🗂 데이터 모델

| 테이블 | 설명 |
|---|---|
| `customer_intake`    | 입고관리. 입고번호 자동 생성 (`YYMMDD-NN`) |
| `insurance_claim`    | 보험청구 (자차 OWN / 상대 OPPONENT) |
| `rental_history`     | 대차이력 (`rental_end_date IS NULL` = 대차중) |
| `rental_vehicle`     | 회사 보유 대차차량 19대 (시드) |
| `insurance_company`  | 보험사 12개 (시드) |

---

## 🔜 Phase 2 ~ 4 (미구현)

- Phase 2 — 자동화 (영문 자동 대문자 변환, 전화번호 입력 즉시 포매팅, 사고유형 다중선택 ↔ 보험청구 자동 활성화)
- Phase 3 — 대시보드, 통계 화면, Excel import/export, DB 백업
- Phase 4 — UI 다듬기, jpackage `.exe` 빌드, 사용 매뉴얼

자세한 도메인·요구사항은 `CLAUDE.md` 참고.
