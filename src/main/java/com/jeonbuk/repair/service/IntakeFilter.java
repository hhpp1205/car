package com.jeonbuk.repair.service;

/**
 * 입고 목록 조회 필터 — 종결 여부 기준.
 */
public enum IntakeFilter {
    /** closedAt IS NULL — 진행 중 (기본). */
    ACTIVE,
    /** closedAt IS NOT NULL — 종결됨. */
    CLOSED,
    /** 종결 여부 무관 — 전체. */
    ALL
}
