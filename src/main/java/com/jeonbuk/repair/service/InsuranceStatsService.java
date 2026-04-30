package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.repository.InsuranceClaimRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 보험사별 청구·수령·미수금 집계.
 * 기간 필터는 청구일(claimDate) 기준.
 */
public class InsuranceStatsService {

    /** 보험사 이름이 비어있는 청구를 묶는 라벨. */
    public static final String UNNAMED_COMPANY = "(미지정)";

    private final InsuranceClaimRepository claimRepo;
    private final InsuranceClaimService claimService;

    public InsuranceStatsService(InsuranceClaimRepository claimRepo, InsuranceClaimService claimService) {
        this.claimRepo = claimRepo;
        this.claimService = claimService;
    }

    /**
     * @param from        청구일 시작 (포함). null 이면 하한 없음.
     * @param to          청구일 종료 (포함). null 이면 상한 없음.
     * @param sideFilter  자차/상대 필터. null 이면 전체.
     */
    public List<CompanyStat> aggregateByCompany(LocalDate from, LocalDate to, ClaimSide sideFilter) {
        // 기간/측면 필터는 DB 에서 적용. 이후 보험사별 group-by 와 평균/overdue 계산은 메모리에서.
        return aggregate(claimRepo.findFiltered(from, to, sideFilter), from, to, sideFilter);
    }

    /**
     * 보험사별 행을 더블클릭했을 때 보여줄 청구 상세 목록.
     * {@link #aggregateByCompany}와 같은 필터(기간·측면·companyKey)를 그대로 사용한다 — 따라서
     * 반환 리스트의 길이는 해당 회사의 {@code count} 와, 청구액 합계는 {@code claimSum} 과 정확히 일치.
     *
     * @param company  {@link CompanyStat#company()} 와 동일한 표기. {@link #UNNAMED_COMPANY} 도 가능.
     */
    public List<InsuranceClaim> findClaimsForCompany(String company,
                                                     LocalDate from, LocalDate to,
                                                     ClaimSide sideFilter) {
        return filterClaimsForCompany(claimRepo.findFiltered(from, to, sideFilter),
                company, from, to, sideFilter);
    }

    /** 단위 테스트용 — fake claim list 직접 주입. */
    List<InsuranceClaim> filterClaimsForCompany(List<InsuranceClaim> claims,
                                                String company,
                                                LocalDate from, LocalDate to,
                                                ClaimSide sideFilter) {
        return claims.stream()
                .filter(c -> matchesSide(c, sideFilter))
                .filter(c -> withinPeriod(c.getClaimDate(), from, to))
                .filter(c -> companyKey(c).equals(company))
                .sorted(Comparator
                        .comparing(InsuranceClaim::getClaimDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(InsuranceClaim::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** 단위 테스트가 fake claim list 를 직접 넘기기 위한 패키지-프라이빗 진입점. */
    List<CompanyStat> aggregate(List<InsuranceClaim> claims,
                                LocalDate from, LocalDate to, ClaimSide sideFilter) {
        Map<String, List<InsuranceClaim>> byCompany = claims.stream()
                .filter(c -> matchesSide(c, sideFilter))
                .filter(c -> withinPeriod(c.getClaimDate(), from, to))
                .collect(Collectors.groupingBy(InsuranceStatsService::companyKey));

        List<CompanyStat> result = new ArrayList<>(byCompany.size());
        for (Map.Entry<String, List<InsuranceClaim>> e : byCompany.entrySet()) {
            result.add(toStat(e.getKey(), e.getValue()));
        }
        // 청구액 큰 순 (사용자가 한눈에 비중을 파악하기 좋도록)
        result.sort(Comparator.comparingLong(CompanyStat::claimSum).reversed());
        return result;
    }

    private CompanyStat toStat(String company, List<InsuranceClaim> list) {
        int count = list.size();
        long claimSum    = list.stream().mapToLong(c -> nz(c.getClaimAmount())).sum();
        long receivedSum = list.stream().mapToLong(c -> nz(c.getReceivedAmount())).sum();
        long outstanding = Math.max(0L, claimSum - receivedSum);

        // 청구일·입금일이 모두 있는 건만 평균에 포함.
        double avgDays = list.stream()
                .filter(c -> c.getClaimDate() != null && c.getReceivedDate() != null)
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getClaimDate(), c.getReceivedDate()))
                .average()
                .orElse(Double.NaN);

        int overdue = (int) list.stream().filter(claimService::isOverdue).count();

        return new CompanyStat(company, count, claimSum, receivedSum, outstanding, avgDays, overdue);
    }

    private static boolean matchesSide(InsuranceClaim c, ClaimSide filter) {
        return filter == null || c.getClaimSide() == filter;
    }

    private static boolean withinPeriod(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return false;
        if (from != null && date.isBefore(from)) return false;
        if (to != null   && date.isAfter(to))    return false;
        return true;
    }

    private static String companyKey(InsuranceClaim c) {
        String name = c.getInsuranceCompany();
        return (name == null || name.isBlank()) ? UNNAMED_COMPANY : name.trim();
    }

    private static long nz(Integer v) {
        return v == null ? 0L : v;
    }

    /**
     * 보험사 1개 행의 집계 결과.
     *
     * @param avgDaysToReceive 청구→입금 평균일수. 입금된 건이 0건이면 {@code Double.NaN}.
     * @param outstandingSum   청구액 - 수령액. 음수는 0 으로 클램프.
     */
    public record CompanyStat(
            String company,
            int    count,
            long   claimSum,
            long   receivedSum,
            long   outstandingSum,
            double avgDaysToReceive,
            int    overdueCount
    ) {
        /** 0~1 사이 비율. claimSum 이 0 이면 0 반환. */
        public double receiveRate() {
            if (claimSum <= 0) return 0.0;
            return Math.min(1.0, (double) receivedSum / (double) claimSum);
        }
    }
}
