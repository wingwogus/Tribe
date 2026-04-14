import { authenticatedAxios, type ApiResponse } from "@/api/http";

export interface CreateItineraryRequest {
  visitDay?: number | null;
  placeId?: number | null;
  title?: string | null;
  time?: string | null;
  memo?: string | null;
}

export interface UpdateItineraryRequest {
  visitDay?: number | null;
  time?: string;
  memo?: string;
}

export interface OrderUpdateRequest {
  items: OrderItem[];
}

export interface OrderItem {
  itemId: number;
  visitDay: number;
  itemOrder: number;
}

export type TravelMode = "WALKING" | "DRIVING" | "TRANSIT";

export interface PlaceSimple {
  placeId: number | null;
  externalPlaceId: string;
  placeName: string;
  address: string;
  latitude: number;
  longitude: number;
}

export interface TransitDetails {
  lineName: string;
  vehicleType: string;
  vehicleIconUrl: string | null;
  numStops: number;
  departureStop: string;
  arrivalStop: string;
}

export interface RouteStep {
  travelMode: string;
  instructions: string;
  duration: string;
  distance: string;
  transitDetails: TransitDetails | null;
}

export interface RouteDetails {
  travelMode: string;
  originPlace: PlaceSimple;
  destinationPlace: PlaceSimple;
  totalDuration: string;
  totalDistance: string;
  steps: RouteStep[];
}

export interface LocationInfo {
  lat: number;
  lng: number;
  address: string | null;
}

export interface PlaceTypeSummary {
  primaryType: string | null;
  types: string[];
  localizedPrimaryLabel: string | null;
}

export interface ItineraryResponse {
  itineraryId: number;
  visitDay: number;
  itemOrder: number;
  placeId?: number | null;
  externalPlaceId?: string | null;
  name: string;
  time: string | null;
  order: number;
  memo: string | null;
  location: LocationInfo | null;
  placeTypeSummary?: PlaceTypeSummary | null;
  openingStatusWarning?: string | null;
}

interface BackendItinerary {
  itemId: number;
  visitDay?: number | null;
  day?: number | null;
  itemOrder?: number | null;
  placeId: number | null;
  externalPlaceId?: string | null;
  name: string;
  title: string | null;
  time: string | null;
  order: number;
  memo: string | null;
  location: LocationInfo | null;
  placeTypeSummary?: PlaceTypeSummary | null;
  openingStatusWarning?: string | null;
}

const toItinerary = (item: BackendItinerary): ItineraryResponse => ({
  itineraryId: item.itemId,
  visitDay: Number(item.visitDay ?? item.day ?? 1),
  itemOrder: Number(item.itemOrder ?? item.order ?? 0),
  placeId: item.placeId,
  externalPlaceId: item.externalPlaceId ?? null,
  name: item.title || item.name,
  time: item.time,
  order: Number(item.itemOrder ?? item.order ?? 0),
  memo: item.memo,
  location: item.location,
  placeTypeSummary: item.placeTypeSummary ?? null,
  openingStatusWarning: item.openingStatusWarning ?? null,
});

export const fetchAllItinerariesForTrip = async (tripId: number): Promise<ItineraryResponse[]> => {
  const response = await authenticatedAxios.get<ApiResponse<BackendItinerary[]>>(`/trips/${tripId}/items`);
  return (response.data.data ?? []).map(toItinerary);
};

export const itineraryApi = {
  createItinerary: async (
    tripId: number,
    visitDay: number,
    request: CreateItineraryRequest,
  ): Promise<ItineraryResponse> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendItinerary>>(`/trips/${tripId}/items`, {
      visitDay: request.visitDay ?? visitDay,
      placeId: request.placeId ?? null,
      title: request.title ?? null,
      time: request.time ?? null,
      memo: request.memo ?? null,
    });
    return toItinerary(response.data.data as BackendItinerary);
  },

  getItinerariesByDay: async (tripId: number, visitDay: number): Promise<ItineraryResponse[]> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendItinerary[]>>(`/trips/${tripId}/items`, {
      params: { visitDay },
    });
    return (response.data.data ?? []).map(toItinerary);
  },

  updateItinerary: async (
    tripId: number,
    visitDay: number,
    itemId: number,
    request: UpdateItineraryRequest,
  ): Promise<ItineraryResponse> => {
    const response = await authenticatedAxios.patch<ApiResponse<BackendItinerary>>(`/trips/${tripId}/items/${itemId}`, {
      visitDay: request.visitDay ?? visitDay,
      time: request.time ?? null,
      memo: request.memo ?? null,
    });
    return toItinerary(response.data.data as BackendItinerary);
  },

  deleteItinerary: async (tripId: number, _visitDay: number, itemId: number): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}/items/${itemId}`);
  },

  updateItineraryOrder: async (tripId: number, request: OrderUpdateRequest): Promise<ItineraryResponse[]> => {
    const response = await authenticatedAxios.patch<ApiResponse<BackendItinerary[]>>(`/trips/${tripId}/items/order`, request);
    return (response.data.data ?? []).map(toItinerary);
  },

  getAllDirections: async (tripId: number, mode: TravelMode): Promise<RouteDetails[]> => {
    const response = await authenticatedAxios.get<ApiResponse<RouteDetails[]>>(`/trips/${tripId}/items/directions`, {
      params: { mode },
    });
    return response.data.data ?? [];
  },
};
