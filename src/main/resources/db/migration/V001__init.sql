-- 자동차 정비 통합 관리 시스템 초기 스키마

-- 입고관리
CREATE TABLE IF NOT EXISTS customer_intake (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    intake_no       TEXT    NOT NULL UNIQUE,
    intake_date     TEXT    NOT NULL,
    vehicle_name    TEXT    NOT NULL,
    vehicle_number  TEXT    NOT NULL,
    phone           TEXT,
    repair_type     TEXT    NOT NULL,
    accident_type   TEXT,
    release_date    TEXT,
    self_pay_amount INTEGER,
    self_pay_date   TEXT,
    tow_driver      TEXT,
    tow_amount      INTEGER,
    memo            TEXT,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_intake_date    ON customer_intake(intake_date);
CREATE INDEX IF NOT EXISTS idx_intake_vehicle ON customer_intake(vehicle_number);

-- 보험사 마스터
CREATE TABLE IF NOT EXISTS insurance_company (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL UNIQUE
);

-- 보험청구
CREATE TABLE IF NOT EXISTS insurance_claim (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    intake_id          INTEGER NOT NULL,
    claim_side         TEXT    NOT NULL,            -- OWN / OPPONENT
    insurance_company  TEXT,
    claim_date         TEXT,
    claim_amount       INTEGER,
    received_amount    INTEGER,
    received_date      TEXT,
    memo               TEXT,
    created_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime')),
    updated_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime')),
    FOREIGN KEY (intake_id) REFERENCES customer_intake(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_claim_intake ON insurance_claim(intake_id);

-- 대차차량 마스터
CREATE TABLE IF NOT EXISTS rental_vehicle (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT    NOT NULL,
    number    TEXT    NOT NULL UNIQUE,
    fuel      TEXT,
    insurance TEXT,
    age_limit TEXT,
    owner     TEXT,
    active    INTEGER NOT NULL DEFAULT 1
);

-- 대차이력
CREATE TABLE IF NOT EXISTS rental_history (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    intake_id          INTEGER NOT NULL,
    rental_vehicle_id  INTEGER NOT NULL,
    rental_start_date  TEXT    NOT NULL,
    rental_end_date    TEXT,
    memo               TEXT,
    created_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime')),
    updated_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime')),
    FOREIGN KEY (intake_id)         REFERENCES customer_intake(id) ON DELETE CASCADE,
    FOREIGN KEY (rental_vehicle_id) REFERENCES rental_vehicle(id)
);

CREATE INDEX IF NOT EXISTS idx_rental_intake  ON rental_history(intake_id);
CREATE INDEX IF NOT EXISTS idx_rental_vehicle ON rental_history(rental_vehicle_id);
CREATE INDEX IF NOT EXISTS idx_rental_active  ON rental_history(rental_end_date);
