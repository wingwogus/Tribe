import { authenticatedAxios, type ApiResponse } from "@/api/http";
import type {
  NormalizedPlaceCategoryKey,
  OpeningSummary,
  PlaceDetailSummary,
  PlacePhotoHint,
  PlaceTypeSummary,
} from "@/api/placeMetadata";

// Backend response types matching PlaceDto
export interface PlaceSearchResult {
  placeId?: number;
  externalPlaceId: string;
  placeName: string;
  address: string;
  latitude: number;
  longitude: number;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  photoHint?: PlacePhotoHint | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingSummary?: OpeningSummary | null;
}

export interface ResolvedPlaceResult extends PlaceSearchResult {
  placeId: number;
}

export interface PlaceDetailResponse {
  placeId: number;
  externalPlaceId: string;
  placeName: string;
  address: string | null;
  latitude: number;
  longitude: number;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  photoHint?: PlacePhotoHint | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingSummary?: OpeningSummary | null;
  formattedPhoneNumber?: string | null;
  internationalPhoneNumber?: string | null;
  websiteUri?: string | null;
  googleMapsUri?: string | null;
  priceLevel?: number | null;
  regularOpeningHoursJson?: string | null;
  currentOpeningHoursJson?: string | null;
}

export type NearbyPlaceCategory =
  | "RESTAURANT"
  | "CAFE"
  | "BAKERY"
  | "BAR"
  | "ATTRACTION"
  | "SHOPPING"
  | "PARK"
  | "MUSEUM"
  | "STAY";

export interface NearbyPlaceSearchRequest {
  latitude: number;
  longitude: number;
  radiusMeters: number;
  maxResultCount: number;
  category: NearbyPlaceCategory;
  language?: string;
  region?: string;
}

// API functions
export const placesApi = {
  // Search places using Google Maps API
  searchPlaces: async (
    query?: string,
    region?: string,
    latitude?: number,
    longitude?: number,
    radiusMeters?: number,
    regionContextKey?: string,
    language: string = 'ko'
  ): Promise<PlaceSearchResult[]> => {
    const response = await authenticatedAxios.get<ApiResponse<PlaceSearchResult[]>>(
      '/places/search',
      { 
        params: { query, region, latitude, longitude, radiusMeters, regionContextKey, language } 
      }
    );
    return response.data.data ?? [];
  },

  searchNearby: async (request: NearbyPlaceSearchRequest): Promise<PlaceSearchResult[]> => {
    const response = await authenticatedAxios.post<{ data: PlaceSearchResult[] }>(
      '/places/nearby',
      request,
    );
    return response.data.data;
  },

  resolveExternalPlace: async (
    externalPlaceId: string,
    language: string = "ko",
  ): Promise<ResolvedPlaceResult> => {
    const response = await authenticatedAxios.post<{ data: ResolvedPlaceResult }>(
      '/places/resolve',
      { externalPlaceId, language },
    );
    return response.data.data;
  },

  getPlaceDetail: async (placeId: number, language: string = "ko"): Promise<PlaceDetailResponse> => {
    const response = await authenticatedAxios.get<ApiResponse<PlaceDetailResponse>>(
      `/places/${placeId}`,
      {
        params: { language },
      },
    );
    return response.data.data as PlaceDetailResponse;
  },
};
