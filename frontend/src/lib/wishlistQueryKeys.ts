export const wishlistQueryKeys = {
  memberWishlist: (query = "", sort = "") => ["wishlist", "member", query, sort] as const,
  memberWishlistRoot: () => ["wishlist", "member"] as const,
};
