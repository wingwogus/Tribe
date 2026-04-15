export interface PlaceTypeSummary {
  primaryType: string | null;
  types: string[];
  displayPrimaryLabel: string | null;
}

export type NormalizedPlaceCategoryKey =
  | "KOREAN_FOOD"
  | "JAPANESE_FOOD"
  | "CHINESE_FOOD"
  | "RESTAURANT"
  | "CAFE"
  | "BAKERY"
  | "BAR"
  | "ATTRACTION"
  | "SHOPPING"
  | "STAY"
  | "PARK"
  | "MUSEUM"
  | "TRANSPORT"
  | "ETC";

export interface PlacePhotoHint {
  photoUri?: string | null;
  photoUrl?: string | null;
  imageUrl?: string | null;
  widthPx?: number | null;
  heightPx?: number | null;
  authorAttributions?: string[];
}

export interface PlaceDetailSummary {
  businessStatus?: string | null;
  rating?: number | null;
  userRatingCount?: number | null;
  editorialSummary?: string | null;
}
