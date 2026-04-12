import {authenticatedAxios} from './auth';

// Backend response types matching PlaceDto
export interface PlaceSearchResult {
  placeId?: number;
  externalPlaceId: string;
  placeName: string;
  address: string;
  latitude: number;
  longitude: number;
}

// API functions
export const placesApi = {
  // Search places using Google Maps API
  searchPlaces: async (
    query?: string,
    region?: string,
    language: string = 'ko'
  ): Promise<PlaceSearchResult[]> => {
    const response = await authenticatedAxios.get<{ data: PlaceSearchResult[] }>(
      '/places/search',
      { 
        params: { query, region, language } 
      }
    );
    return response.data.data;
  }
};
