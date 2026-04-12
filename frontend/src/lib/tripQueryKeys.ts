export const normalizeTripId = (tripId: string | number) => String(tripId);

export const tripQueryKeys = {
  trip: (tripId: string | number) => ["trip", normalizeTripId(tripId)] as const,
  categories: (tripId: string | number) => ["categories", normalizeTripId(tripId)] as const,
  directions: (tripId: string | number) => ["directions", normalizeTripId(tripId)] as const,
  wishlist: (tripId: string | number, query = "") =>
    ["wishlist", normalizeTripId(tripId), query] as const,
  wishlistRoot: (tripId: string | number) => ["wishlist", normalizeTripId(tripId)] as const,
  expenses: (tripId: string | number) => ["expenses", normalizeTripId(tripId)] as const,
  expense: (tripId: string | number, expenseId: number | null | undefined) =>
    ["expense", normalizeTripId(tripId), expenseId ?? null] as const,
  dailySettlementRoot: (tripId: string | number) =>
    ["dailySettlement", normalizeTripId(tripId)] as const,
  dailySettlement: (tripId: string | number, date: string | null | undefined) =>
    ["dailySettlement", normalizeTripId(tripId), date ?? null] as const,
  totalSettlement: (tripId: string | number) => ["totalSettlement", normalizeTripId(tripId)] as const,
  chat: (tripId: string | number) => ["chat", normalizeTripId(tripId)] as const,
  reviews: (tripId: string | number) => ["reviews", normalizeTripId(tripId)] as const,
  review: (tripId: string | number, reviewId: number | null | undefined) =>
    ["review", normalizeTripId(tripId), reviewId ?? null] as const,
};
