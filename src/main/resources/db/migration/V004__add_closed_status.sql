-- 입고 종결 상태: closed_at 이 NULL 이면 진행 중, 값이 있으면 종결.
-- 종결은 사용자가 수동으로 처리/취소하는 플래그 (computeStatus 자동 산출보다 우선).
ALTER TABLE customer_intake ADD COLUMN closed_at TEXT;
