import { authenticatedAxios, type ApiResponse } from "@/api/http";

export interface TripMemberDetails {
  memberId: number | null;
  nickname: string;
  avatar: string | null;
  role: string;
}

export interface WishlistItem {
  wishlistItemId: number;
  placeId: number;
  name: string;
  address: string | null;
  latitude: number;
  longitude: number;
  adder: TripMemberDetails;
}

export interface WishlistAddRequest {
  externalPlaceId: string;
  placeName: string;
  address: string;
  latitude: number;
  longitude: number;
}

export interface WishlistSearchResponse {
  content: WishlistItem[];
  pageNumber: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  isLast: boolean;
}

interface BackendAdder {
  tripMemberId: number;
  memberId: number | null;
  nickname: string;
}

interface BackendWishlistItem {
  wishlistItemId: number;
  placeId: number;
  name: string;
  address: string | null;
  latitude: number;
  longitude: number;
  adder: BackendAdder;
}

const toWishlistItem = (item: BackendWishlistItem): WishlistItem => ({
  wishlistItemId: item.wishlistItemId,
  placeId: item.placeId,
  name: item.name,
  address: item.address,
  latitude: Number(item.latitude),
  longitude: Number(item.longitude),
  adder: {
    memberId: item.adder.memberId,
    nickname: item.adder.nickname,
    avatar: null,
    role: "MEMBER",
  },
});

export const wishlistApi = {
  addWishlist: async (tripId: number, request: WishlistAddRequest): Promise<WishlistItem> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendWishlistItem>>(`/trips/${tripId}/wishlists`, request);
    return toWishlistItem(response.data.data as BackendWishlistItem);
  },

  getWishlist: async (tripId: number, query?: string, page = 0, size = 300): Promise<WishlistSearchResponse> => {
    const response = await authenticatedAxios.get<ApiResponse<WishlistSearchResponse>>(`/trips/${tripId}/wishlists`, {
      params: { query, page, size },
    });

    return {
      ...(response.data.data as WishlistSearchResponse),
      content: (response.data.data?.content ?? []).map((item) => toWishlistItem(item as unknown as BackendWishlistItem)),
    };
  },

  deleteWishlistItems: async (tripId: number, wishlistItemIds: number[]): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}/wishlists`, {
      data: { wishlistItemIds },
    });
  },
};
