package com.jeonbuk.repair.util;

import com.jeonbuk.repair.model.AccidentType;
import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.model.RentalVehicle;
import com.jeonbuk.repair.model.RepairType;
import com.jeonbuk.repair.service.IntakeWorkflowService;
import com.jeonbuk.repair.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 100건의 테스트 데이터를 IntakeWorkflowService 경유로 적재.
 *
 * 실행: {@code ./gradlew seedTestData}  또는 IDE 에서 main() 직접 실행.
 * 멱등성 없음 — 매 실행마다 100건이 추가된다. 데이터 초기화가 필요하면
 * {@link AppPaths#dbFile()} (기본 {@code %APPDATA%\전북공업사\repair.db}) 를 삭제하고 앱을 재기동.
 * 별도 위치를 쓰려면 환경변수 {@code JEONBUK_REPAIR_HOME} 으로 오버라이드.
 */
public final class TestDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TestDataSeeder.class);

    private static final int COUNT = 100;
    /** 시드 분포의 기준일. 결과 재현성 위해 고정. */
    private static final LocalDate ANCHOR = LocalDate.of(2026, 4, 28);
    private static final int SPREAD_DAYS = 150;  // ANCHOR 기준 과거 150일 내 분포

    private static final List<String> KOREAN_VEHICLE_NAMES = List.of(
            "쏘나타", "아반떼", "그랜저", "투싼", "싼타페", "팰리세이드", "코나", "베뉴",
            "캐스퍼", "스타리아", "포터", "포터2", "아이오닉5", "아이오닉6",
            "K3", "K5", "K8", "K9", "모닝", "레이", "쏘렌토", "스포티지", "셀토스",
            "카니발", "EV6", "EV9", "니로", "G70", "G80", "G90", "GV70", "GV80",
            "SM6", "QM6", "XM3", "토레스", "렉스턴스포츠", "티볼리",
            "스파크", "트랙스", "말리부", "트레일블레이저"
    );
    private static final List<String> FOREIGN_VEHICLE_NAMES = List.of(
            "BMW 3시리즈", "BMW 5시리즈", "BMW X3", "BMW X5",
            "벤츠 C클래스", "벤츠 E클래스", "벤츠 GLC",
            "아우디 A6", "아우디 Q5",
            "테슬라 모델3", "테슬라 모델Y",
            "렉서스 ES", "토요타 캠리",
            "볼보 XC60", "미니 쿠퍼"
    );
    private static final List<String> INSURANCE_COMPANIES = List.of(
            "삼성화재", "현대해상", "DB손해보험", "KB손해보험",
            "메리츠화재", "한화손해보험", "롯데손해보험", "MG손해보험",
            "흥국화재", "AXA손해보험", "하나손해보험", "캐롯손해보험"
    );
    private static final List<String> TOW_DRIVERS = List.of(
            "김기사", "이기사", "박기사", "최기사", "정기사", "강기사", "조기사", "윤기사"
    );
    /** 한국 차량번호 한글 분류문자. */
    private static final char[] PLATE_HANGUL = {
            '가', '나', '다', '라', '마', '거', '너', '더', '러', '머',
            '버', '서', '어', '저', '고', '노', '도', '로', '모', '보',
            '소', '오', '조', '구', '누', '두', '루', '무', '부', '수',
            '우', '주'
    };

    private TestDataSeeder() {}

    public static void main(String[] args) {
        log.info("테스트 데이터 시드 시작 — {}건 적재 예정", COUNT);
        try {
            HibernateUtil.getSessionFactory();  // DB 마이그레이션·시드 적용
            run();
            log.info("시드 완료. 입고 총 {}건", ServiceRegistry.get().intakeRepo().count());
        } finally {
            HibernateUtil.shutdown();
        }
    }

    static int run() {
        IntakeWorkflowService workflow = ServiceRegistry.get().workflowService();
        List<RentalVehicle> rentalVehicles = ServiceRegistry.get().rentalService().listVehicles();

        // 같은 시드면 동일한 데이터 — 재현 가능한 테스트 환경.
        Random rnd = new Random(20260428L);
        int created = 0;

        for (int i = 0; i < COUNT; i++) {
            IntakeWorkflowService.Form form = buildRandomForm(rnd, rentalVehicles);
            try {
                workflow.save(form);
                created++;
            } catch (RuntimeException e) {
                log.warn("시드 #{} 저장 실패 — {}", i, e.getMessage());
            }
        }
        return created;
    }

    private static IntakeWorkflowService.Form buildRandomForm(Random rnd, List<RentalVehicle> vehicles) {
        IntakeWorkflowService.Form form = new IntakeWorkflowService.Form();

        // ----- 입고 -----
        CustomerIntake intake = new CustomerIntake();
        LocalDate intakeDate = ANCHOR.minusDays(rnd.nextInt(SPREAD_DAYS));
        intake.setIntakeDate(intakeDate);

        boolean foreign = rnd.nextInt(100) < 25;
        intake.setVehicleName(pick(rnd, foreign ? FOREIGN_VEHICLE_NAMES : KOREAN_VEHICLE_NAMES));
        intake.setVehicleNumber(randomPlate(rnd));
        intake.setPhone(randomPhone(rnd));

        boolean insuranceRepair = rnd.nextInt(100) < 60;
        intake.setRepairType(insuranceRepair ? RepairType.INSURANCE : RepairType.GENERAL);

        Set<AccidentType> accidents = EnumSet.noneOf(AccidentType.class);
        if (insuranceRepair) {
            int kind = rnd.nextInt(100);
            if (kind < 35)      accidents.add(AccidentType.SELF);
            else if (kind < 70) accidents.add(AccidentType.OPPONENT);
            else if (kind < 90) { accidents.add(AccidentType.SELF); accidents.add(AccidentType.OPPONENT); }
            else                accidents.add(AccidentType.FAULT);
        } else {
            accidents.add(AccidentType.GENERAL);
        }
        intake.setAccidentTypes(accidents);

        // 진행상태 분포 — 4단계로 골고루
        int stage = rnd.nextInt(4);  // 0: 수리중, 1: 출고됨, 2: 청구완료, 3: 수령완료
        if (stage >= 1) {
            intake.setReleaseDate(intakeDate.plusDays(3 + rnd.nextInt(15)));
            if (insuranceRepair && accidents.contains(AccidentType.SELF)) {
                int self = (rnd.nextInt(15) + 5) * 10000;  // 50,000 ~ 200,000
                intake.setSelfPayAmount(self);
                if (stage >= 1 && rnd.nextInt(100) < 70) {
                    intake.setSelfPayDate(intake.getReleaseDate().plusDays(rnd.nextInt(7)));
                }
            }
        }

        // 견인 — 30% 확률
        if (rnd.nextInt(100) < 30) {
            intake.setTowDriver(pick(rnd, TOW_DRIVERS));
            intake.setTowAmount((rnd.nextInt(15) + 5) * 10000);  // 50K~200K
        }

        intake.setMemo(pickMemo(rnd, intake));
        form.intake = intake;

        // ----- 대차 (~30%) -----
        if (!vehicles.isEmpty() && rnd.nextInt(100) < 30) {
            RentalHistory rental = new RentalHistory();
            rental.setRentalVehicle(vehicles.get(rnd.nextInt(vehicles.size())));
            rental.setRentalStartDate(intakeDate);
            // 출고가 있는 케이스의 70%는 종료일 설정, 나머진 대차중
            if (intake.getReleaseDate() != null && rnd.nextInt(100) < 70) {
                rental.setRentalEndDate(intake.getReleaseDate());
            }
            form.rental = rental;
        }

        // ----- 보험청구 -----
        if (insuranceRepair) {
            if (accidents.contains(AccidentType.SELF)) {
                form.ownClaim = randomClaim(rnd, intakeDate, stage);
            }
            if (accidents.contains(AccidentType.OPPONENT)) {
                form.opponentClaim = randomClaim(rnd, intakeDate, stage);
            }
        }

        return form;
    }

    private static InsuranceClaim randomClaim(Random rnd, LocalDate intakeDate, int stage) {
        InsuranceClaim c = new InsuranceClaim();
        c.setInsuranceCompany(pick(rnd, INSURANCE_COMPANIES));

        if (stage >= 2) {
            // 청구 완료 — 청구일·청구액 입력
            c.setClaimDate(intakeDate.plusDays(5 + rnd.nextInt(20)));
            int claim = (rnd.nextInt(48) + 2) * 100000;  // 20만 ~ 500만
            c.setClaimAmount(claim);
            if (stage >= 3) {
                // 수령 완료 — 입금일·실수령액
                int recv = claim - (rnd.nextInt(10) * 10000);  // 약간의 자기부담/공제
                if (recv < 0) recv = claim;
                c.setReceivedAmount(recv);
                c.setReceivedDate(c.getClaimDate().plusDays(15 + rnd.nextInt(40)));
            }
        }
        return c;
    }

    private static String randomPlate(Random rnd) {
        int prefix = 10 + rnd.nextInt(290);          // 10~299
        char hangul = PLATE_HANGUL[rnd.nextInt(PLATE_HANGUL.length)];
        int suffix = 1000 + rnd.nextInt(9000);       // 1000~9999
        return prefix + String.valueOf(hangul) + suffix;
    }

    private static String randomPhone(Random rnd) {
        int mid  = 1000 + rnd.nextInt(9000);
        int last = 1000 + rnd.nextInt(9000);
        return "010-" + mid + "-" + last;
    }

    private static String pickMemo(Random rnd, CustomerIntake i) {
        if (rnd.nextInt(100) < 60) return null;
        String[] templates = {
                "전면 범퍼 교체 및 도색",
                "후미등 교체",
                "엔진 오일 누유 점검",
                "사이드미러 파손",
                "광택·세차 추가",
                "에어컨 가스 충전",
                "타이어 4본 교체",
                "브레이크 패드 교체"
        };
        return templates[rnd.nextInt(templates.length)];
    }

    private static <T> T pick(Random rnd, List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }
}
