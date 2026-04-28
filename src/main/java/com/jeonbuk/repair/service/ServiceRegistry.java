package com.jeonbuk.repair.service;

import com.jeonbuk.repair.repository.*;

/**
 * 단일 사용자 데스크탑 앱 — DI 프레임워크 없이 간단한 서비스 로케이터.
 */
public final class ServiceRegistry {

    private static final ServiceRegistry INSTANCE = new ServiceRegistry();

    private final CustomerIntakeRepository    intakeRepo    = new CustomerIntakeRepository();
    private final InsuranceClaimRepository    claimRepo     = new InsuranceClaimRepository();
    private final RentalHistoryRepository     rentalRepo    = new RentalHistoryRepository();
    private final RentalVehicleRepository     vehicleRepo   = new RentalVehicleRepository();
    private final InsuranceCompanyRepository  companyRepo   = new InsuranceCompanyRepository();

    private final CustomerIntakeService   intakeService   = new CustomerIntakeService(intakeRepo);
    private final InsuranceClaimService   claimService    = new InsuranceClaimService(claimRepo);
    private final RentalService           rentalService   = new RentalService(rentalRepo, vehicleRepo);
    private final IntakeWorkflowService   workflowService = new IntakeWorkflowService(intakeRepo);

    private ServiceRegistry() {}

    public static ServiceRegistry get() { return INSTANCE; }

    public CustomerIntakeService   intakeService()    { return intakeService; }
    public InsuranceClaimService   claimService()     { return claimService; }
    public RentalService           rentalService()    { return rentalService; }
    public IntakeWorkflowService   workflowService()  { return workflowService; }
    public InsuranceCompanyRepository companyRepo()   { return companyRepo; }
    public RentalVehicleRepository    vehicleRepo()   { return vehicleRepo; }
    public CustomerIntakeRepository   intakeRepo()    { return intakeRepo; }
    public InsuranceClaimRepository   claimRepo()     { return claimRepo; }
    public RentalHistoryRepository    rentalRepo()    { return rentalRepo; }
}
