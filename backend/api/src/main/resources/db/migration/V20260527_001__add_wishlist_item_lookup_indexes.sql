CREATE INDEX IF NOT EXISTS idx_wishlist_item_trip_id
    ON wishlist_item(trip_id);

CREATE INDEX IF NOT EXISTS idx_wishlist_item_place_id
    ON wishlist_item(place_id);

CREATE INDEX IF NOT EXISTS idx_wishlist_item_adder_id
    ON wishlist_item(adder_id);
