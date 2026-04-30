package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.repository.CustomerIntakeRepository;
import com.jeonbuk.repair.support.DatabaseExtension;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DatabaseExtension.class)
class CustomerIntakeServiceIntegrationTest {

    private final CustomerIntakeRepository repo = new CustomerIntakeRepository();
    private final CustomerIntakeService service = new CustomerIntakeService(repo);

    @Test
    @DisplayName("create — 입고번호 미지정 시 자동 부여")
    void create_assigns_intake_no() {
        CustomerIntake i = TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456");
        CustomerIntake saved = service.create(i);

        assertNotNull(saved.getId());
        assertEquals("260424-01", saved.getIntakeNo());
    }

    @Test
    @DisplayName("create — 같은 날짜 연속 생성은 -01, -02")
    void create_increments_intake_no() {
        CustomerIntake a = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "11가1111"));
        CustomerIntake b = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "22나2222"));

        assertEquals("260424-01", a.getIntakeNo());
        assertEquals("260424-02", b.getIntakeNo());
    }

    @Test
    @DisplayName("update — id 가 있으면 같은 입고번호 유지하며 갱신")
    void update_keeps_intake_no() {
        CustomerIntake saved = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456"));
        saved.setVehicleName("그랜저");
        CustomerIntake updated = service.update(saved);

        assertEquals(saved.getId(), updated.getId());
        assertEquals("260424-01", updated.getIntakeNo());
        assertEquals("그랜저", updated.getVehicleName());
    }

    @Test
    @DisplayName("delete — cascade 로 보험청구도 함께 삭제")
    void delete_cascades() {
        CustomerIntake saved = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456"));
        Long id = saved.getId();
        service.delete(id);

        assertTrue(service.findById(id).isEmpty());
    }

    @Test
    @DisplayName("close — closedAt 시각이 채워지고 isClosed true")
    void close_sets_timestamp() {
        CustomerIntake saved = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456"));
        assertFalse(saved.isClosed());

        CustomerIntake closed = service.close(saved.getId());
        assertNotNull(closed.getClosedAt());
        assertTrue(closed.isClosed());
    }

    @Test
    @DisplayName("close — 이미 종결된 건은 no-op (closedAt 유지)")
    void close_idempotent() {
        CustomerIntake saved = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456"));
        CustomerIntake first = service.close(saved.getId());
        var firstAt = first.getClosedAt();

        CustomerIntake again = service.close(saved.getId());
        assertEquals(firstAt, again.getClosedAt());
    }

    @Test
    @DisplayName("reopen — closedAt 이 NULL 로 돌아간다")
    void reopen_clears_state() {
        CustomerIntake saved = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456"));
        service.close(saved.getId());

        CustomerIntake reopened = service.reopen(saved.getId());
        assertNull(reopened.getClosedAt());
        assertFalse(reopened.isClosed());
    }

    @Test
    @DisplayName("findAll(IntakeFilter) — ACTIVE/CLOSED/ALL 필터")
    void find_all_with_filter() {
        CustomerIntake a = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "11가1111"));
        CustomerIntake b = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 25), "22나2222"));
        service.close(b.getId());

        var active = service.findAll(IntakeFilter.ACTIVE);
        assertEquals(1, active.size());
        assertEquals(a.getId(), active.get(0).getId());

        var closed = service.findAll(IntakeFilter.CLOSED);
        assertEquals(1, closed.size());
        assertEquals(b.getId(), closed.get(0).getId());

        var all = service.findAll(IntakeFilter.ALL);
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("closeAll — 진행/종결 혼합 + 없는 id 처리: processed/skipped/failures 분류")
    void close_all_mixed() {
        CustomerIntake a = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "11가1111"));
        CustomerIntake b = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 25), "22나2222"));
        CustomerIntake c = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 26), "33다3333"));
        service.close(c.getId());  // c 는 이미 종결

        var result = service.closeAll(List.of(a.getId(), b.getId(), c.getId(), 999_999L));

        assertEquals(2, result.processed());     // a, b 새로 종결
        assertEquals(1, result.skipped());       // c 이미 종결
        assertEquals(1, result.failures().size()); // 999999 없음
        assertTrue(service.findById(a.getId()).orElseThrow().isClosed());
        assertTrue(service.findById(b.getId()).orElseThrow().isClosed());
    }

    @Test
    @DisplayName("reopenAll — 종결 건들을 일괄 취소")
    void reopen_all() {
        CustomerIntake a = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "11가1111"));
        CustomerIntake b = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 25), "22나2222"));
        service.close(a.getId());
        service.close(b.getId());

        var result = service.reopenAll(List.of(a.getId(), b.getId()));
        assertEquals(2, result.processed());
        assertEquals(0, result.skipped());
        assertTrue(result.failures().isEmpty());
        assertFalse(service.findById(a.getId()).orElseThrow().isClosed());
        assertFalse(service.findById(b.getId()).orElseThrow().isClosed());
    }

    @Test
    @DisplayName("closeAll — null 컬렉션은 빈 결과 반환")
    void close_all_null_safe() {
        var result = service.closeAll(null);
        assertEquals(0, result.total());
    }

    @Test
    @DisplayName("search(keyword, IntakeFilter) — 키워드 + 필터 함께 적용")
    void search_with_filter() {
        CustomerIntake a = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "11가1111"));
        CustomerIntake b = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 25), "11가2222"));
        service.close(b.getId());

        // "11가" 가 두 건 모두에 매치되지만 ACTIVE 필터는 a 만
        var hits = service.search("11가", IntakeFilter.ACTIVE);
        assertEquals(1, hits.size());
        assertEquals(a.getId(), hits.get(0).getId());

        var closedHits = service.search("11가", IntakeFilter.CLOSED);
        assertEquals(1, closedHits.size());
        assertEquals(b.getId(), closedHits.get(0).getId());
    }
}
