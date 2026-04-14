ALTER TABLE trip
    ADD COLUMN region_code VARCHAR(64);

COMMENT ON COLUMN trip.region_code IS 'Travel region catalog code such as KR_JEJU or JP_TOKYO';
