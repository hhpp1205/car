-- 견인 정보 컬럼 추가 (기존 DB 업그레이드용)
ALTER TABLE customer_intake ADD COLUMN tow_driver TEXT;
ALTER TABLE customer_intake ADD COLUMN tow_amount INTEGER;
