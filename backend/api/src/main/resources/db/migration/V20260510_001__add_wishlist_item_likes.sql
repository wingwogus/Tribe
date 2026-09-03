CREATE TABLE IF NOT EXISTS wishlist_item_likes (
    wishlist_item_like_id BIGSERIAL PRIMARY KEY,
    wishlist_item_id BIGINT NOT NULL,
    trip_member_id BIGINT NOT NULL,
    CONSTRAINT fk_wishlist_item_likes_wishlist_item
        FOREIGN KEY (wishlist_item_id) REFERENCES wishlist_item(wishlist_item_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_item_likes_trip_member
        FOREIGN KEY (trip_member_id) REFERENCES trip_member(trip_member_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_wishlist_item_likes_item_member
        UNIQUE (wishlist_item_id, trip_member_id)
);

CREATE INDEX IF NOT EXISTS idx_wishlist_item_likes_wishlist_item_id
    ON wishlist_item_likes(wishlist_item_id);

CREATE INDEX IF NOT EXISTS idx_wishlist_item_likes_trip_member_id
    ON wishlist_item_likes(trip_member_id);

COMMENT ON TABLE wishlist_item_likes IS 'Likes by trip members on trip wishlist items';
COMMENT ON COLUMN wishlist_item_likes.wishlist_item_like_id IS 'Wishlist item like primary key';
COMMENT ON COLUMN wishlist_item_likes.wishlist_item_id IS 'Trip wishlist item id';
COMMENT ON COLUMN wishlist_item_likes.trip_member_id IS 'Trip member id that liked the wishlist item';
