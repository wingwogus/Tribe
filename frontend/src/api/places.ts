import {authenticatedAxios} from './auth';
import type { NormalizedPlaceCategoryKey, PlaceDetailSummary, PlacePhotoHint, PlaceTypeSummary } from "@/api/placeMetadata";

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
  formattedPhoneNumber?: string | null;
  internationalPhoneNumber?: string | null;
  websiteUri?: string | null;
  googleMapsUri?: string | null;
  regularOpeningHoursJson?: string | null;
  currentOpeningHoursJson?: string | null;
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
    const response = await authenticatedAxios.get<{ data: PlaceSearchResult[] }>(
      '/places/search',
      { 
        params: { query, region, latitude, longitude, radiusMeters, regionContextKey, language } 
      }
    );
    return response.data.data;
  },

  getPlaceDetail: async (placeId: number, language: string = "ko"): Promise<PlaceDetailResponse> => {
    const response = await authenticatedAxios.get<{ data: PlaceDetailResponse }>(
      `/places/${placeId}`,
      {
        params: { language },
      },
    );
    return response.data.data;
  },
};
