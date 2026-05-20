ALTER TABLE place_detail_snapshot
    ADD COLUMN IF NOT EXISTS opening_hours_synced_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS current_opening_hours_synced_at TIMESTAMP;

COMMENT ON COLUMN place_detail_snapshot.opening_hours_synced_at IS 'Last time regular opening hours and parsed regular periods were refreshed from Google Places';
COMMENT ON COLUMN place_detail_snapshot.current_opening_hours_synced_at IS 'Last time current opening hours were refreshed from Google Places';
