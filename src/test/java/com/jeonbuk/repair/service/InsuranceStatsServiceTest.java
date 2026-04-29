package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DB 없이 fake claim list 로 집계 로직을 검증한다.
 * 실제 DB 쿼리·트랜잭션은 InsuranceClaimRepositoryTest 가 담당.
 */
class InsuranceStatsServiceTest {

    private final InsuranceClaimService claimService = new InsuranceClaimService(null);
    private final InsuranceStatsService stats = new InsuranceStatsService(null, claimService);

    @Test
    @DisplayName("보험사별로 group by — 같은 회사 청구를 합산한다")
    void groups_by_company() {
        InsuranceClaim a1 = claim("삼성화재", LocalDate.of(2026, 3, 1), 1_000_000, 800_000, null, ClaimSide.OWN);
        InsuranceClaim a2 = claim("삼성화재", LocalDate.of(2026, 3, 5),   500_000, 500_000, null, ClaimSide.OWN);
        InsuranceClaim b1 = claim("현대해상", LocalDate.of(2026, 3, 2),   300_000,       0, null, ClaimSide.OPPONENT);

        List<InsuranceStatsService.CompanyStat> result =
                stats.aggregate(List.of(a1, a2, b1), null, null, null);

        Map<String, InsuranceStatsService.CompanyStat> byCompany = byCompany(result);
        assertEquals(2, byCompany.get("삼성화재").count());
        assertEquals(1_500_000L, byCompany.get("삼성화재").claimSum());
        assertEquals(1_300_000L, byCompany.get("삼성화재").receivedSum());
        assertEquals(  200_000L, byCompany.get("삼성화재").outstandingSum());
        assertEquals(1, byCompany.get("현대해상").count());
        assertEquals(  300_000L, byCompany.get("현대해상").outstandingSum());
    }

    @Test
    @DisplayName("기간 필터는 청구일 기준 — from 이전·to 이후 건은 제외")
    void filters_by_claim_date() {
        InsuranceClaim before = claim("삼성화재", LocalDate.of(2026, 1, 31),  100_000, 0, null, ClaimSide.OWN);
        InsuranceClaim within = claim("삼성화재", LocalDate.of(2026, 2, 15),  200_000, 0, null, ClaimSide.OWN);
        InsuranceClaim atTo   = claim("삼성화재", LocalDate.of(2026, 3, 31),  400_000, 0, null, ClaimSide.OWN);
        InsuranceClaim after  = claim("삼성화재", LocalDate.of(2026, 4, 1),   800_000, 0, null, ClaimSide.OWN);

        List<InsuranceStatsService.CompanyStat> result = stats.aggregate(
                List.of(before, within, atTo, after),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 31),
                null);

        assertEquals(1, result.size());
        InsuranceStatsService.CompanyStat r = result.get(0);
        assertEquals(2, r.count(), "from 포함, to 포함이어야 한다");
        assertEquals(600_000L, r.claimSum());
    }

    @Test
    @DisplayName("청구일이 null 인 청구는 기간 필터에 의해 항상 제외된다")
    void null_claim_date_is_excluded_when_period_set() {
        InsuranceClaim noDate = claim("삼성화재", null, 100_000, 0, null, ClaimSide.OWN);

        List<InsuranceStatsService.CompanyStat> result = stats.aggregate(
                List.of(noDate),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("청구 구분 필터 — OWN 만 또는 OPPONENT 만")
    void filters_by_side() {
        InsuranceClaim own  = claim("삼성화재", LocalDate.of(2026, 3, 1), 100_000, 0, null, ClaimSide.OWN);
        InsuranceClaim opp  = claim("삼성화재", LocalDate.of(2026, 3, 1), 200_000, 0, null, ClaimSide.OPPONENT);

        List<InsuranceStatsService.CompanyStat> ownOnly =
                stats.aggregate(List.of(own, opp), null, null, ClaimSide.OWN);
        assertEquals(1, ownOnly.get(0).count());
        assertEquals(100_000L, ownOnly.get(0).claimSum());

        List<InsuranceStatsService.CompanyStat> oppOnly =
                stats.aggregate(List.of(own, opp), null, null, ClaimSide.OPPONENT);
        assertEquals(1, oppOnly.get(0).count());
        assertEquals(200_000L, oppOnly.get(0).claimSum());

        List<InsuranceStatsService.CompanyStat> all =
                stats.aggregate(List.of(own, opp), null, null, null);
        assertEquals(2, all.get(0).count());
    }

    @Test
    @DisplayName("보험사 이름이 null 또는 빈 문자열이면 (미지정) 으로 묶인다")
    void null_or_blank_company_grouped_as_unnamed() {
        InsuranceClaim a = claim(null,  LocalDate.of(2026, 3, 1), 100_000, 0, null, ClaimSide.OWN);
        InsuranceClaim b = claim("",    LocalDate.of(2026, 3, 2), 200_000, 0, null, ClaimSide.OWN);
        InsuranceClaim c = claim("   ", LocalDate.of(2026, 3, 3), 300_000, 0, null, ClaimSide.OWN);
        InsuranceClaim d = claim("삼성화재", LocalDate.of(2026, 3, 4), 999_000, 0, null, ClaimSide.OWN);

        List<InsuranceStatsService.CompanyStat> result =
                stats.aggregate(List.of(a, b, c, d), null, null, null);

        Map<String, InsuranceStatsService.CompanyStat> byCompany = byCompany(result);
        assertTrue(byCompany.containsKey(InsuranceStatsService.UNNAMED_COMPANY));
        assertEquals(3, byCompany.get(InsuranceStatsService.UNNAMED_COMPANY).count());
        assertEquals(600_000L, byCompany.get(InsuranceStatsService.UNNAMED_COMPANY).claimSum());
        assertEquals(1, byCompany.get("삼성화재").count());
    }

    @Test
    @DisplayName("미수금은 음수가 되지 않는다 — receivedSum 이 claimSum 보다 커도 0")
    void outstanding_clamped_to_zero() {
        InsuranceClaim c = claim("삼성화재", LocalDate.of(2026, 3, 1), 100_000, 200_000, null, ClaimSide.OWN);
        List<InsuranceStatsService.CompanyStat> result = stats.aggregate(List.of(c), null, null, null);
        assertEquals(0L, result.get(0).outstandingSum());
    }

    @Test
    @DisplayName("평균 수령일수 — 청구일·입금일 모두 있는 건만 평균에 포함")
    void avg_days_only_when_both_dates_present() {
        InsuranceClaim received10 = claim("삼성화재",
                LocalDate.of(2026, 3, 1), 100_000, 100_000, LocalDate.of(2026, 3, 11),
                ClaimSide.OWN);
        InsuranceClaim received20 = claim("삼성화재",
                LocalDate.of(2026, 3, 1), 100_000, 100_000, LocalDate.of(2026, 3, 21),
                ClaimSide.OWN);
        InsuranceClaim notReceived = claim("삼성화재",
                LocalDate.of(2026, 3, 1), 100_000, 0, null,
                ClaimSide.OWN);

        List<InsuranceStatsService.CompanyStat> result =
                stats.aggregate(List.of(received10, received20, notReceived), null, null, null);
        assertEquals(15.0, result.get(0).avgDaysToReceive(), 0.0001,
                "수령된 2건의 평균(10, 20) = 15. 미수령 건은 평균에서 제외");
    }

    @Test
    @DisplayName("평균 수령일수 — 수령된 건이 없으면 NaN")
    void avg_days_nan_when_none_received() {
        InsuranceClaim c = claim("삼성화재", LocalDate.of(2026, 3, 1), 100_000, 0, null, ClaimSide.OWN);
        List<InsuranceStatsService.CompanyStat> result = stats.aggregate(List.of(c), null, null, null);
        assertTrue(Double.isNaN(result.get(0).avgDaysToReceive()));
    }

    @Test
    @DisplayName("30일 경과 미수령 카운트 — InsuranceClaimService.isOverdue 정의에 위임")
    void overdue_count_uses_claim_service() {
        // 청구일 31일 전 + 미수령 → overdue
        InsuranceClaim overdue = claim("삼성화재",
                LocalDate.now().minusDays(31), 100_000, 0, null, ClaimSide.OWN);
        // 청구일 15일 전 + 미수령 → not overdue
        InsuranceClaim fresh = claim("삼성화재",
                LocalDate.now().minusDays(15), 100_000, 0, null, ClaimSide.OWN);
        // 청구일 60일 전 + 입금됨 → not overdue
        InsuranceClaim paid = claim("삼성화재",
                LocalDate.now().minusDays(60), 100_000, 100_000, LocalDate.now().minusDays(1), ClaimSide.OWN);

        List<InsuranceStatsService.CompanyStat> result =
                stats.aggregate(List.of(overdue, fresh, paid), null, null, null);
        assertEquals(1, result.get(0).overdueCount());
    }

    @Test
    @DisplayName("결과는 청구액 내림차순으로 정렬된다")
    void sorted_by_claim_sum_desc() {
        InsuranceClaim small = claim("A보험", LocalDate.of(2026, 3, 1), 100_000, 0, null, ClaimSide.OWN);
        InsuranceClaim big   = claim("B보험", LocalDate.of(2026, 3, 1), 900_000, 0, null, ClaimSide.OWN);
        InsuranceClaim mid   = claim("C보험", LocalDate.of(2026, 3, 1), 500_000, 0, null, ClaimSide.OWN);

        List<InsuranceStatsService.CompanyStat> result =
                stats.aggregate(List.of(small, big, mid), null, null, null);
        assertEquals(List.of("B보험", "C보험", "A보험"),
                result.stream().map(InsuranceStatsService.CompanyStat::company).toList());
    }

    @Test
    @DisplayName("findClaimsForCompany — 같은 회사·기간·측면 필터를 적용해 정확한 청구만 반환")
    void find_claims_for_company_basic() {
        InsuranceClaim a1 = claim("삼성화재", LocalDate.of(2026, 3, 1), 100_000, 0, null, ClaimSide.OWN);
        InsuranceClaim a2 = claim("삼성화재", LocalDate.of(2026, 3, 5), 200_000, 0, null, ClaimSide.OWN);
        InsuranceClaim b1 = claim("현대해상", LocalDate.of(2026, 3, 2), 300_000, 0, null, ClaimSide.OWN);

        List<InsuranceClaim> result = stats.filterClaimsForCompany(
                List.of(a1, a2, b1), "삼성화재", null, null, null);
        assertEquals(2, result.size());
        assertTrue(result.contains(a1));
        assertTrue(result.contains(a2));
        assertFalse(result.contains(b1));
    }

    @Test
    @DisplayName("findClaimsForCompany — aggregate 결과의 count·claimSum 과 정확히 일치한다 (일관성 보장)")
    void find_claims_consistency_with_aggregate() {
        InsuranceClaim a1 = claim("삼성화재", LocalDate.of(2026, 3, 1), 100_000, 50_000, null, ClaimSide.OWN);
        InsuranceClaim a2 = claim("삼성화재", LocalDate.of(2026, 3, 5), 200_000, 0,      null, ClaimSide.OPPONENT);
        InsuranceClaim a3 = claim("삼성화재", LocalDate.of(2026, 4, 1), 700_000, 0,      null, ClaimSide.OWN);
        // 다른 회사 노이즈
        InsuranceClaim b1 = claim("현대해상", LocalDate.of(2026, 3, 2), 999_000, 0,      null, ClaimSide.OWN);

        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to   = LocalDate.of(2026, 3, 31);

        List<InsuranceStatsService.CompanyStat> agg = stats.aggregate(List.of(a1, a2, a3, b1), from, to, ClaimSide.OWN);
        InsuranceStatsService.CompanyStat samsung = agg.stream()
                .filter(s -> s.company().equals("삼성화재")).findFirst().orElseThrow();

        List<InsuranceClaim> details = stats.filterClaimsForCompany(
                List.of(a1, a2, a3, b1), "삼성화재", from, to, ClaimSide.OWN);
        assertEquals(samsung.count(), details.size(),
                "aggregate 의 count 와 findClaimsForCompany 의 size 가 같아야 한다");
        long detailClaimSum = details.stream()
                .mapToLong(c -> c.getClaimAmount() == null ? 0L : c.getClaimAmount()).sum();
        assertEquals(samsung.claimSum(), detailClaimSum,
                "aggregate 의 claimSum 과 detail 의 청구액 합이 같아야 한다");
    }

    @Test
    @DisplayName("findClaimsForCompany — (미지정) 으로 호출 시 null/blank 보험사 청구만 반환")
    void find_claims_for_unnamed() {
        InsuranceClaim a = claim(null,        LocalDate.of(2026, 3, 1), 100_000, 0, null, ClaimSide.OWN);
        InsuranceClaim b = claim("",          LocalDate.of(2026, 3, 2), 200_000, 0, null, ClaimSide.OWN);
        InsuranceClaim c = claim("   ",       LocalDate.of(2026, 3, 3), 300_000, 0, null, ClaimSide.OWN);
        InsuranceClaim d = claim("삼성화재", LocalDate.of(2026, 3, 4), 999_000, 0, null, ClaimSide.OWN);

        List<InsuranceClaim> result = stats.filterClaimsForCompany(
                List.of(a, b, c, d),
                InsuranceStatsService.UNNAMED_COMPANY,
                null, null, null);
        assertEquals(3, result.size());
        assertFalse(result.contains(d));
    }

    @Test
    @DisplayName("findClaimsForCompany — 청구일 내림차순으로 정렬")
    void find_claims_sorted_desc_by_claim_date() {
        InsuranceClaim oldest = claim("삼성화재", LocalDate.of(2026, 1, 5), 100_000, 0, null, ClaimSide.OWN);
        InsuranceClaim middle = claim("삼성화재", LocalDate.of(2026, 2, 5), 200_000, 0, null, ClaimSide.OWN);
        InsuranceClaim newest = claim("삼성화재", LocalDate.of(2026, 3, 5), 300_000, 0, null, ClaimSide.OWN);

        List<InsuranceClaim> result = stats.filterClaimsForCompany(
                List.of(oldest, newest, middle), "삼성화재", null, null, null);
        assertEquals(List.of(newest, middle, oldest), result);
    }

    @Test
    @DisplayName("receiveRate — claimSum=0 이면 0, 정상 케이스는 받은 비율")
    void receive_rate() {
        InsuranceStatsService.CompanyStat zero =
                new InsuranceStatsService.CompanyStat("X", 1, 0, 0, 0, Double.NaN, 0);
        assertEquals(0.0, zero.receiveRate());

        InsuranceStatsService.CompanyStat partial =
                new InsuranceStatsService.CompanyStat("X", 1, 1000, 750, 250, 0, 0);
        assertEquals(0.75, partial.receiveRate(), 0.0001);

        InsuranceStatsService.CompanyStat over =
                new InsuranceStatsService.CompanyStat("X", 1, 1000, 1500, 0, 0, 0);
        assertEquals(1.0, over.receiveRate(), "receivedSum > claimSum 이어도 1.0 으로 클램프");
    }

    // ─── helpers ────────────────────────────────────────────────────

    private static InsuranceClaim claim(String company, LocalDate claimDate,
                                        Integer claimAmount, Integer receivedAmount,
                                        LocalDate receivedDate, ClaimSide side) {
        InsuranceClaim c = TestFixtures.claim(TestFixtures.intake(), side, claimAmount, receivedAmount);
        c.setInsuranceCompany(company);
        c.setClaimDate(claimDate);
        c.setReceivedDate(receivedDate);
        return c;
    }

    private static Map<String, InsuranceStatsService.CompanyStat> byCompany(
            List<InsuranceStatsService.CompanyStat> list) {
        return list.stream().collect(java.util.stream.Collectors.toMap(
                InsuranceStatsService.CompanyStat::company, s -> s));
    }
}
