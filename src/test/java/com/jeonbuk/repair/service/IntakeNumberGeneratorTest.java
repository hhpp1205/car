package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.repository.CustomerIntakeRepository;
import com.jeonbuk.repair.support.DatabaseExtension;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DatabaseExtension.class)
class IntakeNumberGeneratorTest {

    private final CustomerIntakeRepository repo = new CustomerIntakeRepository();
    private final IntakeNumberGenerator gen = new IntakeNumberGenerator(repo);

    @Test
    @DisplayName("해당 날짜에 없으면 -01 부여")
    void first_of_day() {
        assertEquals("260424-01", gen.next(LocalDate.of(2026, 4, 24)));
    }

    @Test
    @DisplayName("같은 날짜 두 번째 입고는 -02")
    void second_of_day() {
        save("260424-01", LocalDate.of(2026, 4, 24));
        assertEquals("260424-02", gen.next(LocalDate.of(2026, 4, 24)));
    }

    @Test
    @DisplayName("9건 → 10건은 두 자리 패딩 그대로")
    void zero_padding_2_digits() {
        for (int i = 1; i <= 9; i++) {
            save(String.format("260424-%02d", i), LocalDate.of(2026, 4, 24));
        }
        assertEquals("260424-10", gen.next(LocalDate.of(2026, 4, 24)));
    }

    @Test
    @DisplayName("다른 날짜는 다시 -01 부터")
    void different_day_resets() {
        save("260424-01", LocalDate.of(2026, 4, 24));
        save("260424-02", LocalDate.of(2026, 4, 24));
        assertEquals("260425-01", gen.next(LocalDate.of(2026, 4, 25)));
    }

    @Test
    @DisplayName("월/일 한자리수도 0-padding")
    void single_digit_month_day_padded() {
        assertEquals("260101-01", gen.next(LocalDate.of(2026, 1, 1)));
    }

    private void save(String intakeNo, LocalDate date) {
        CustomerIntake i = TestFixtures.intake(date, "12가" + (1000 + Integer.parseInt(intakeNo.substring(7))));
        i.setIntakeNo(intakeNo);
        repo.save(i);
    }
}
