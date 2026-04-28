# DB 스키마 변경 가이드

이 문서는 **컬럼 추가·테이블 변경·시드 보정** 같은 DB 스키마 작업을 할 때 **지인 PC 의 기존 데이터가 깨지지 않도록** 따라야 할 절차를 정리한다.

---

## 핵심 원칙 3가지

1. **기존 V*.sql 을 절대 수정하지 않는다.** 항상 새 V### 파일을 추가한다.
2. 모든 DDL 은 **멱등(idempotent)** 으로 작성한다. (`IF NOT EXISTS`, `INSERT OR IGNORE` 등)
3. 데이터 변환이 필요한 변경은 **빈 DB · 기존 데이터 양쪽으로 테스트** 한다.

이 세 가지만 지키면 지인 PC 의 데이터는 안전하다. 추가로 앱 시작 시 `BackupUtil` 이 일일 백업을 만들어두기 때문에 어제 상태로 복구할 안전망도 있다.

---

## 마이그레이션 시스템 동작 방식

`DatabaseInitializer` 가 앱 시작마다 `src/main/resources/db/migration/V*.sql` 를 **이름순으로** 모두 실행한다. 멱등성을 위해 다음을 자동 처리한다:

- `CREATE TABLE IF NOT EXISTS` · `CREATE INDEX IF NOT EXISTS` → SQLite 가 직접 처리
- `INSERT OR IGNORE` → 중복 시드 자동 무시
- `ALTER TABLE ADD COLUMN` → SQLite 는 `IF NOT EXISTS` 미지원이므로 `DatabaseInitializer.runScript()` 가 `"duplicate column name"` 예외를 swallow

새 마이그레이션은 `MIGRATIONS` 리스트에 추가:

```java
// DatabaseInitializer.java
private static final List<String> MIGRATIONS = List.of(
        "db/migration/V001__init.sql",
        "db/migration/V002__seed.sql",
        "db/migration/V003__add_tow_info.sql",
        "db/migration/V004__your_change.sql"     // ← 여기에 추가
);
```

---

## 시나리오별 작성 패턴

### A. 컬럼 추가

가장 흔하고 가장 안전한 변경. **NULL 허용** 으로 추가하면 기존 row 가 자동으로 NULL 로 채워진다.

```sql
-- V004__add_customer_email.sql
ALTER TABLE customer_intake ADD COLUMN email TEXT;
```

NOT NULL 로 추가해야 한다면 반드시 DEFAULT 값을 함께 지정:

```sql
ALTER TABLE customer_intake ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING';
```

같은 마이그레이션에서 Entity 도 갱신:

```java
// CustomerIntake.java
@Column(name = "email", length = 128)
private String email;
// + getter/setter
```

### B. 컬럼 이름 변경 / 타입 변경

SQLite 는 `ALTER TABLE RENAME COLUMN` 을 지원하지만 다른 RDBMS 로 옮길 가능성을 고려하면 보통 **신규 컬럼 추가 → 데이터 복사 → 구 컬럼은 nullable 로 방치** 가 가장 안전하다.

```sql
-- V005__migrate_phone_format.sql
ALTER TABLE customer_intake ADD COLUMN phone_normalized TEXT;
UPDATE customer_intake
   SET phone_normalized = REPLACE(phone, '-', '')
 WHERE phone IS NOT NULL AND phone_normalized IS NULL;
```

이후 코드에서 새 컬럼만 쓰도록 바꾸고, 다음 릴리스에서 구 컬럼을 정리한다 (단계적 마이그레이션).

### C. 새 테이블 추가

```sql
-- V006__add_audit_log.sql
CREATE TABLE IF NOT EXISTS audit_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    event       TEXT    NOT NULL,
    payload     TEXT,
    created_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at);
```

Entity 클래스 추가 + `hibernate.cfg.xml` 에 `<mapping class="..."/>` 추가.

### D. 시드 데이터 추가 / 보정

```sql
-- V007__add_new_insurance_company.sql
INSERT OR IGNORE INTO insurance_company (name) VALUES ('새보험'), ('또다른보험');
```

`INSERT OR IGNORE` 가 핵심 — 이미 같은 unique key 의 행이 있으면 그냥 무시한다.

기존 시드 값을 **수정** 해야 한다면 `UPDATE ... WHERE` 를 쓴다 (`INSERT OR REPLACE` 는 PK 가 바뀌어 외래키 깨짐):

```sql
UPDATE rental_vehicle SET fuel = '하이브리드' WHERE number = '31구0771';
```

### E. 인덱스 / 제약 추가

```sql
CREATE INDEX IF NOT EXISTS idx_intake_phone ON customer_intake(phone);
```

UNIQUE 제약을 사후에 추가하려면 데이터에 중복이 없는지 확인 후 새 인덱스로:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_intake_no ON customer_intake(intake_no);
```

### F. 컬럼 삭제 (가능하면 피한다)

SQLite 3.35+ 부터 `ALTER TABLE DROP COLUMN` 지원하지만, 안 쓰는 컬럼은 그냥 **두는 게** 안전하다. 정말 지워야 한다면 ScratchPad 패턴:

```sql
-- V0XX__drop_legacy_column.sql  -- 주의: 운영 DB 에서 충분히 검증 후
CREATE TABLE customer_intake__new (
    -- 새 스키마 (legacy_col 제외)
);
INSERT INTO customer_intake__new SELECT id, intake_no, ... FROM customer_intake;
DROP TABLE customer_intake;
ALTER TABLE customer_intake__new RENAME TO customer_intake;
-- 인덱스도 다시 만들어야 함
```

이 패턴은 트랜잭션 + FK 임시 비활성 + 백업 복구 절차까지 묶어야 안전하므로 **마지막 수단**. 보통 그냥 컬럼을 nullable 로 두고 코드에서 안 쓰는 게 낫다.

---

## 검증 절차 (PR 머지 전 필수)

마이그레이션 추가 시 다음 4가지를 모두 확인한다.

### 1. 빈 DB 에서 그린

```bash
rm "$env:APPDATA/전북공업사/repair.db"  # PowerShell
# 또는 환경변수로 임시 위치 사용
$env:JEONBUK_REPAIR_HOME = "$env:TEMP/migration-test"
./gradlew run
```

앱이 정상 기동하면 OK.

### 2. 기존 DB 에서도 그린 (가장 중요)

지인 PC 의 현재 상태를 모사. 백업 파일 (또는 로컬 운영 DB) 을 옆에 복사해두고:

```powershell
$env:JEONBUK_REPAIR_HOME = "C:\temp\migration-test"
mkdir $env:JEONBUK_REPAIR_HOME
cp "$env:APPDATA/전북공업사/repair.db" "$env:JEONBUK_REPAIR_HOME/"
./gradlew run
```

기존 데이터가 그대로 보이고 새 컬럼만 NULL 로 채워졌는지 UI 에서 확인.

### 3. 앱을 두 번 실행해도 그린 (멱등성)

마이그레이션 파일은 매 시작마다 재실행되므로 두 번째 실행도 깨지면 안 된다. 위 2번 절차 직후 앱을 한 번 더 띄워본다.

### 4. 자동 테스트 추가

`@ExtendWith(DatabaseExtension.class)` 통합 테스트로 신규 컬럼/테이블의 CRUD 한 가지를 검증한다:

```java
@Test
void new_email_column_persists() {
    CustomerIntake i = new CustomerIntake();
    // ... 필수 필드 셋업
    i.setEmail("test@example.com");
    Tx.run(s -> s.persist(i));

    CustomerIntake reloaded = ServiceRegistry.get().intakeRepo().findById(i.getId()).orElseThrow();
    assertEquals("test@example.com", reloaded.getEmail());
}
```

---

## 지인 PC 에서 마이그레이션이 깨졌을 때 (복구)

만약 신규 빌드를 깔자마자 데이터가 이상해 보인다면:

1. **앱 종료**
2. `%APPDATA%\전북공업사\backup\` 폴더 열기
3. 가장 최근의 정상 백업 (예: `repair-20260428.db`) 을 복사
4. 상위 폴더 `%APPDATA%\전북공업사\` 의 `repair.db` 를 삭제
5. 복사한 백업을 그 자리에 `repair.db` 로 이름 바꿔 붙여넣기
6. 앱 다시 실행

이 시점엔 잘못된 마이그레이션이 다시 적용되므로, 개발자 측에서 마이그레이션 SQL 을 수정한 새 빌드를 먼저 받아야 한다. 임시로 굴리려면 환경변수 `JEONBUK_REPAIR_HOME` 을 다른 폴더로 돌려 깨끗한 DB 로 작업.

---

## 체크리스트 (PR 템플릿용)

```
- [ ] 새 V###__*.sql 파일을 추가했고, 기존 V*.sql 은 손대지 않았다
- [ ] DDL 이 멱등이다 (IF NOT EXISTS / INSERT OR IGNORE / ALTER ADD COLUMN)
- [ ] 빈 DB 에서 ./gradlew run 통과
- [ ] 기존 데이터 DB 에서 ./gradlew run 통과 (UI 에서 데이터 확인)
- [ ] 두 번째 실행도 통과 (멱등성)
- [ ] DatabaseInitializer.MIGRATIONS 에 새 파일을 추가했다
- [ ] 신규 컬럼/테이블의 통합 테스트를 추가했다
- [ ] CHANGELOG.md 에 변경사항을 기록했다
```
