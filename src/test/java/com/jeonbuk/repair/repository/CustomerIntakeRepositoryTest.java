package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.support.DatabaseExtension;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DatabaseExtension.class)
class CustomerIntakeRepositoryTest {

    private final CustomerIntakeRepository repo = new CustomerIntakeRepository();

    @Test
    @DisplayName("save — 새 입고 저장 후 id 가 채워진다")
    void save_assigns_id() {
        CustomerIntake i = TestFixtures.intake();
        i.setIntakeNo("260424-01");
        CustomerIntake saved = repo.save(i);
        assertNotNull(saved.getId());
    }

    @Test
    @DisplayName("findByIntakeNo — 저장된 항목 조회")
    void find_by_intake_no() {
        CustomerIntake i = TestFixtures.intake();
        i.setIntakeNo("260424-01");
        repo.save(i);

        Optional<CustomerIntake> found = repo.findByIntakeNo("260424-01");
        assertTrue(found.isPresent());
        assertEquals("쏘나타", found.get().getVehicleName());
    }

    @Test
    @DisplayName("findAll — 입고일 내림차순 정렬")
    void find_all_orders_by_date_desc() {
        CustomerIntake older = TestFixtures.intake(LocalDate.of(2026, 4, 20), "11가1111");
        older.setIntakeNo("260420-01");
        CustomerIntake newer = TestFixtures.intake(LocalDate.of(2026, 4, 24), "22나2222");
        newer.setIntakeNo("260424-01");

        repo.save(older);
        repo.save(newer);

        List<CustomerIntake> list = repo.findAll();
        assertEquals(2, list.size());
        assertEquals("260424-01", list.get(0).getIntakeNo());
        assertEquals("260420-01", list.get(1).getIntakeNo());
    }

    @Test
    @DisplayName("search — 차량번호 부분 일치")
    void search_by_vehicle_number() {
        CustomerIntake a = TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456");
        a.setIntakeNo("260424-01");
        CustomerIntake b = TestFixtures.intake(LocalDate.of(2026, 4, 24), "99나9999");
        b.setIntakeNo("260424-02");
        repo.save(a);
        repo.save(b);

        List<CustomerIntake> hit = repo.search("12가");
        assertEquals(1, hit.size());
        assertEquals("260424-01", hit.get(0).getIntakeNo());
    }

    @Test
    @DisplayName("findLastIntakeNoOfDate — 같은 날짜 가장 최근 번호")
    void find_last_intake_no() {
        CustomerIntake a = TestFixtures.intake(LocalDate.of(2026, 4, 24), "11가1111");
        a.setIntakeNo("260424-01");
        CustomerIntake b = TestFixtures.intake(LocalDate.of(2026, 4, 24), "22나2222");
        b.setIntakeNo("260424-02");
        CustomerIntake otherDay = TestFixtures.intake(LocalDate.of(2026, 4, 25), "33다3333");
        otherDay.setIntakeNo("260425-01");
        repo.save(a);
        repo.save(b);
        repo.save(otherDay);

        Optional<String> last = repo.findLastIntakeNoOfDate(LocalDate.of(2026, 4, 24));
        assertTrue(last.isPresent());
        assertEquals("260424-02", last.get());
    }

    @Test
    @DisplayName("findLastIntakeNoOfDate — 해당 날짜에 없으면 empty")
    void find_last_intake_no_empty() {
        Optional<String> last = repo.findLastIntakeNoOfDate(LocalDate.of(2026, 4, 24));
        assertTrue(last.isEmpty());
    }

    @Test
    @DisplayName("delete — 삭제 후 조회 안 됨")
    void delete_removes_row() {
        CustomerIntake i = TestFixtures.intake();
        i.setIntakeNo("260424-01");
        CustomerIntake saved = repo.save(i);

        repo.delete(saved.getId());
        assertTrue(repo.findById(saved.getId()).isEmpty());
    }

    @Test
    @DisplayName("count — 저장 갯수")
    void count() {
        assertEquals(0, repo.count());

        CustomerIntake i = TestFixtures.intake();
        i.setIntakeNo("260424-01");
        repo.save(i);

        assertEquals(1, repo.count());
    }

    @Test
    @DisplayName("findDistinctVehicleNames — 중복 제거 + 오름차순 정렬")
    void find_distinct_vehicle_names() {
        CustomerIntake a = TestFixtures.intake(LocalDate.of(2026, 4, 24), "11가1111");
        a.setIntakeNo("260424-01");
        a.setVehicleName("쏘나타");
        CustomerIntake b = TestFixtures.intake(LocalDate.of(2026, 4, 25), "22나2222");
        b.setIntakeNo("260425-01");
        b.setVehicleName("아반떼");
        CustomerIntake c = TestFixtures.intake(LocalDate.of(2026, 4, 26), "33다3333");
        c.setIntakeNo("260426-01");
        c.setVehicleName("쏘나타");  // 중복
        repo.save(a);
        repo.save(b);
        repo.save(c);

        List<String> names = repo.findDistinctVehicleNames();
        assertEquals(List.of("쏘나타", "아반떼"), names);  // 한글 사전순: 쏘 < 아
    }
}
