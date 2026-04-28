# 테스트 가이드

이 문서는 **새 기능을 추가할 때 어디에 어떤 테스트를 작성할지** 결정하는 기준과 컨벤션을 정리합니다. 새 기능은 반드시 테스트와 함께 PR 됩니다.

---

## TL;DR

| 새로 만든 / 바꾼 것 | 테스트 위치 | 형식 |
|---|---|---|
| `util/` 의 포매터·검증기 | `src/test/java/com/jeonbuk/repair/util/` | 순수 단위 테스트 (DB 없음) |
| `model/` 의 enum·도메인 메서드 | `src/test/java/com/jeonbuk/repair/model/` | 순수 단위 테스트 |
| `service/` 의 검증·계산 로직 (DB 안 탐) | `src/test/java/com/jeonbuk/repair/service/` | 순수 단위 테스트, repo는 `null` 주입 가능 |
| `service/` 의 트랜잭션 흐름 (create/update) | `src/test/java/com/jeonbuk/repair/service/*IntegrationTest.java` | `@ExtendWith(DatabaseExtension.class)` |
| `repository/` 의 쿼리 / 매핑 | `src/test/java/com/jeonbuk/repair/repository/` | `@ExtendWith(DatabaseExtension.class)` |
| `controller/` 의 UI 동작 | (미구현) Phase 4 에서 TestFX 도입 검토 | — |

```bash
./gradlew test                  # 전체 테스트
./gradlew test --tests "*Service*"   # 패턴 매칭
./gradlew check                 # test + 정적 검사 (현재는 test와 동일)
```

리포트:
- HTML: `build/reports/tests/test/index.html`
- 커버리지: `build/reports/jacoco/test/html/index.html`

---

## 분류 가이드 — "어느 계층에 테스트?"

### 1) 순수 단위 테스트 (no DB)

대상은 **외부 상태를 안 만지는 함수**.

- 입력 → 출력만 보면 검증 끝
- 실행 100ms 미만, 신뢰도 100%

예: `Formatters.formatPhone`, `Validators.isValidVehicleNumber`, `AccidentType.parse/join`, `InsuranceClaimService.isOverdue`, `CustomerIntakeService.computeStatus`

작성 패턴:

```java
class FooTest {
    @Test
    void something() {
        assertEquals(expected, Foo.bar(input));
    }
}
```

여러 입력 케이스가 있으면 `@ParameterizedTest`.

### 2) 통합 테스트 (with SQLite)

대상은 **DB 를 거쳐야 의미가 있는 로직**.

- Repository 의 쿼리·정렬·필터
- Service 의 `create`/`update`/`delete` 흐름 (자동 입고번호 생성, 캐스케이드 등)
- 다중 행이 얽힌 검증 (예: 대차 차량 충돌)

작성 패턴:

```java
@ExtendWith(DatabaseExtension.class)
class FooRepositoryTest {

    private final FooRepository repo = new FooRepository();

    @Test
    void scenario() {
        // given
        repo.save(...);
        // when
        var result = repo.find(...);
        // then
        assertEquals(..., result);
    }
}
```

`DatabaseExtension` 동작:
- `@BeforeAll` — 임시 SQLite 파일 생성, 마이그레이션·시드 적용, 전역 `SessionFactory` 교체
- `@AfterEach` — `customer_intake`, `insurance_claim`, `rental_history` 데이터만 비움 (시드는 유지)
- `@AfterAll` — `SessionFactory` 종료, 임시 파일 삭제

격리 보장: 각 테스트 클래스마다 신규 DB. 테스트 메서드 간엔 시드만 공유.

### 3) UI / 컨트롤러 테스트 (현재 미구현)

JavaFX 컨트롤러는 Phase 4 에서 [TestFX](https://github.com/TestFX/TestFX) 도입을 검토. 그때까지는 컨트롤러 메서드를 얇게 유지하고 핵심 로직을 service/util 에 두어 단위 테스트로 커버.

---

## 새 기능 → 테스트 체크리스트

기능 PR 머지 전에 다음을 확인:

- [ ] 새 public 메서드마다 **happy path** 1개 + **edge case** 1~2개
- [ ] 입력 검증을 추가했다면 → 실패 케이스 (`assertThrows`)
- [ ] 새 SQL 쿼리를 작성했다면 → 통합 테스트
- [ ] 버그 수정이라면 → **그 버그를 재현하는 테스트** 먼저 작성하고 fix
- [ ] 모든 테스트 그린: `./gradlew test`
- [ ] 커버리지 리포트 확인: `build/reports/jacoco/test/html/index.html`

---

## 컨벤션

### 명명
- 클래스: `XxxTest` (단위) / `XxxIntegrationTest` (DB 통합 — 명시)
- 메서드: `이름_상황_기대` 또는 `한국어_DisplayName` 둘 다 OK. 가독성 우선.
- `@DisplayName("…")` 으로 한국어 시나리오 설명 권장.

### 픽스처
- 도메인 객체는 `TestFixtures` 헬퍼 사용 (`TestFixtures.intake()`, `TestFixtures.claim(...)`).
- 같은 픽스처가 다섯 번 이상 반복되면 헬퍼에 메서드 추가.

### 어설션
- JUnit 5 의 `org.junit.jupiter.api.Assertions.*` 사용 (필요 시 AssertJ 추가 검토).
- 메시지 인자는 *왜 실패했는지*만 적기 — `assertEquals(a, b, "...")` 의 세번째 인자.

### Mocking
- 현재 Mockito 미도입. Service 검증 테스트는 `new XxxService(null)` 로 충분.
- 의존성을 mock 해야 더 깨끗해지는 케이스가 생기면 그때 도입.

---

## CI

GitHub Actions 워크플로우 `.github/workflows/ci.yml`:
- `push` / `pull_request` 시 자동 실행
- Ubuntu + Temurin JDK 17 + Gradle 캐시
- `xvfb` 위에서 `./gradlew check`
- 테스트 리포트와 커버리지 HTML 을 아티팩트로 업로드

PR 이 빨간색이면 머지 금지. (브랜치 보호 규칙은 GitHub 측 설정)

---

## 로컬 빠른 사이클

```bash
# 변경한 파일만 영향받는 테스트만 (Gradle 캐시 활용)
./gradlew test

# 특정 클래스
./gradlew test --tests com.jeonbuk.repair.service.CustomerIntakeServiceTest

# 특정 메서드
./gradlew test --tests "com.jeonbuk.repair.service.CustomerIntakeServiceTest.computeStatus_settled"

# 실패한 것만 재실행
./gradlew test --rerun-tasks
```

IntelliJ: 클래스 좌측 ▶ 아이콘 클릭으로 단일 실행. JUnit 5 자동 인식.
