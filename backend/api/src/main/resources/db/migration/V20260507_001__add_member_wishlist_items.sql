CREATE TABLE IF NOT EXISTS member_wishlist_items (
    member_wishlist_item_id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    CONSTRAINT fk_member_wishlist_items_member
        FOREIGN KEY (member_id) REFERENCES "member"(member_id),
    CONSTRAINT fk_member_wishlist_items_place
        FOREIGN KEY (place_id) REFERENCES place(place_id),
    CONSTRAINT uk_member_wishlist_items_member_place
        UNIQUE (member_id, place_id)
);

CREATE INDEX IF NOT EXISTS idx_member_wishlist_items_member_id
    ON member_wishlist_items(member_id);

CREATE INDEX IF NOT EXISTS idx_member_wishlist_items_place_id
    ON member_wishlist_items(place_id);

COMMENT ON TABLE member_wishlist_items IS 'Member-owned wishlist places independent of a trip';
COMMENT ON COLUMN member_wishlist_items.member_wishlist_item_id IS 'Member wishlist item primary key';
COMMENT ON COLUMN member_wishlist_items.member_id IS 'Wishlist owner member id';
COMMENT ON COLUMN member_wishlist_items.place_id IS 'Canonical place id saved by the member';
