package com.jeonbuk.repair.service;

import com.jeonbuk.repair.repository.CustomerIntakeRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 입고번호 생성 — YYMMDD-NN.
 * 같은 날짜 내에서 연번이 1부터 증가, 0-padding 2자리.
 */
public class IntakeNumberGenerator {

    private final CustomerIntakeRepository repo;

    public IntakeNumberGenerator(CustomerIntakeRepository repo) {
        this.repo = repo;
    }

    public String next(LocalDate date) {
        String prefix = CustomerIntakeRepository.formatDatePrefix(date);
        Optional<String> last = repo.findLastIntakeNoOfDate(date);
        int next = last.map(no -> {
            int dash = no.indexOf('-');
            if (dash < 0) return 0;
            try {
                return Integer.parseInt(no.substring(dash + 1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }).orElse(0) + 1;
        return prefix + "-" + String.format("%02d", next);
    }
}
