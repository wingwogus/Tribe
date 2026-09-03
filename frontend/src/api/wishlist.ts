import { authenticatedAxios, type ApiResponse } from "@/api/http";
import type {
  NormalizedPlaceCategoryKey,
  OpeningSummary,
  PlaceDetailSummary,
  PlacePhotoHint,
  PlaceTypeSummary,
} from "@/api/placeMetadata";

export type TripWishlistSort =
  | "rating_desc"
  | "review_count_desc"
  | "review_good_desc"
  | "like_count_desc"
  | "like_count_asc";

export type MemberWishlistSort = "rating_desc" | "review_count_desc" | "review_good_desc";

export interface TripMemberDetails {
  tripMemberId: number | null;
  memberId: number | null;
  nickname: string;
  avatar: string | null;
  role: string;
}

export interface WishlistItem {
  wishlistItemId: number;
  placeId: number;
  externalPlaceId: string;
  name: string;
  address: string | null;
  latitude: number;
  longitude: number;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  photoHint?: PlacePhotoHint | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingSummary?: OpeningSummary | null;
  adder: TripMemberDetails;
  likeCount: number;
  likedByMe: boolean;
}

export interface MemberWishlistItem {
  memberWishlistItemId: number;
  placeId: number;
  externalPlaceId: string;
  name: string;
  address: string | null;
  latitude: number;
  longitude: number;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  photoHint?: PlacePhotoHint | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingSummary?: OpeningSummary | null;
}

export interface WishlistAddRequest {
  externalPlaceId: string;
  placeName: string;
  address?: string | null;
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

export interface MemberWishlistSearchResponse {
  content: MemberWishlistItem[];
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
  avatar?: string | null;
}

interface BackendWishlistItem {
  wishlistItemId: number;
  placeId: number;
  externalPlaceId: string;
  name: string;
  address: string | null;
  latitude: number | string;
  longitude: number | string;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  photoHint?: PlacePhotoHint | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingSummary?: OpeningSummary | null;
  adder: BackendAdder;
  likeCount?: number | null;
  likedByMe?: boolean | null;
}

interface BackendMemberWishlistItem {
  memberWishlistItemId: number;
  placeId: number;
  externalPlaceId: string;
  name: string;
  address: string | null;
  latitude: number | string;
  longitude: number | string;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  photoHint?: PlacePhotoHint | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingSummary?: OpeningSummary | null;
}

const toWishlistItem = (item: BackendWishlistItem): WishlistItem => ({
  wishlistItemId: item.wishlistItemId,
  placeId: item.placeId,
  externalPlaceId: item.externalPlaceId,
  name: item.name,
  address: item.address,
  latitude: Number(item.latitude),
  longitude: Number(item.longitude),
  placeTypeSummary: item.placeTypeSummary ?? null,
  normalizedCategoryKey: item.normalizedCategoryKey ?? null,
  photoHint: item.photoHint ?? null,
  placeDetailSummary: item.placeDetailSummary ?? null,
  openingSummary: item.openingSummary ?? null,
  adder: {
    tripMemberId: item.adder.tripMemberId,
    memberId: item.adder.memberId,
    nickname: item.adder.nickname,
    avatar: item.adder.avatar ?? null,
    role: "MEMBER",
  },
  likeCount: item.likeCount ?? 0,
  likedByMe: item.likedByMe ?? false,
});

const toMemberWishlistItem = (item: BackendMemberWishlistItem): MemberWishlistItem => ({
  memberWishlistItemId: item.memberWishlistItemId,
  placeId: item.placeId,
  externalPlaceId: item.externalPlaceId,
  name: item.name,
  address: item.address,
  latitude: Number(item.latitude),
  longitude: Number(item.longitude),
  placeTypeSummary: item.placeTypeSummary ?? null,
  normalizedCategoryKey: item.normalizedCategoryKey ?? null,
  photoHint: item.photoHint ?? null,
  placeDetailSummary: item.placeDetailSummary ?? null,
  openingSummary: item.openingSummary ?? null,
});

export const wishlistApi = {
  addWishlist: async (tripId: number, request: WishlistAddRequest): Promise<WishlistItem> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendWishlistItem>>(
      `/trips/${tripId}/wishlists`,
      request,
    );
    return toWishlistItem(response.data.data as BackendWishlistItem);
  },

  addMemberWishlist: async (request: WishlistAddRequest): Promise<MemberWishlistItem> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendMemberWishlistItem>>(
      "/members/me/wishlists",
      request,
    );
    return toMemberWishlistItem(response.data.data as BackendMemberWishlistItem);
  },

  addTripWishlistFromMemberWishlist: async (
    tripId: number,
    memberWishlistItemId: number,
  ): Promise<WishlistItem> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendWishlistItem>>(
      `/trips/${tripId}/wishlists/from-member-wishlist`,
      { memberWishlistItemId },
    );
    return toWishlistItem(response.data.data as BackendWishlistItem);
  },

  addWishlistFromPlace: async (tripId: number, placeId: number): Promise<WishlistItem> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendWishlistItem>>(
      `/trips/${tripId}/wishlists/from-place`,
      { placeId },
    );
    return toWishlistItem(response.data.data as BackendWishlistItem);
  },

  getWishlist: async (
    tripId: number,
    query?: string,
    page = 0,
    size = 300,
    wishlistSort?: TripWishlistSort,
  ): Promise<WishlistSearchResponse> => {
    const response = await authenticatedAxios.get<ApiResponse<Omit<WishlistSearchResponse, "content"> & {
      content: BackendWishlistItem[];
    }>>(`/trips/${tripId}/wishlists`, {
      params: { query, page, size, wishlistSort },
    });
    const data = response.data.data;

    return {
      pageNumber: data?.pageNumber ?? page,
      pageSize: data?.pageSize ?? size,
      totalPages: data?.totalPages ?? 0,
      totalElements: data?.totalElements ?? 0,
      isLast: data?.isLast ?? true,
      content: (data?.content ?? []).map(toWishlistItem),
    };
  },

  getMemberWishlist: async (
    query?: string,
    page = 0,
    size = 300,
    wishlistSort?: MemberWishlistSort,
  ): Promise<MemberWishlistSearchResponse> => {
    const response = await authenticatedAxios.get<ApiResponse<Omit<MemberWishlistSearchResponse, "content"> & {
      content: BackendMemberWishlistItem[];
    }>>("/members/me/wishlists", {
      params: { query, page, size, wishlistSort },
    });
    const data = response.data.data;

    return {
      pageNumber: data?.pageNumber ?? page,
      pageSize: data?.pageSize ?? size,
      totalPages: data?.totalPages ?? 0,
      totalElements: data?.totalElements ?? 0,
      isLast: data?.isLast ?? true,
      content: (data?.content ?? []).map(toMemberWishlistItem),
    };
  },

  deleteWishlistItems: async (tripId: number, wishlistItemIds: number[]): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}/wishlists`, {
      data: { wishlistItemIds },
    });
  },

  deleteMemberWishlistItems: async (memberWishlistItemIds: number[]): Promise<void> => {
    await authenticatedAxios.delete("/members/me/wishlists", {
      data: { memberWishlistItemIds },
    });
  },

  likeWishlistItem: async (tripId: number, wishlistItemId: number) => {
    const response = await authenticatedAxios.post<ApiResponse<{ likeCount: number; likedByMe: boolean }>>(
      `/trips/${tripId}/wishlists/${wishlistItemId}/likes`,
    );
    return response.data.data;
  },

  unlikeWishlistItem: async (tripId: number, wishlistItemId: number) => {
    const response = await authenticatedAxios.delete<ApiResponse<{ likeCount: number; likedByMe: boolean }>>(
      `/trips/${tripId}/wishlists/${wishlistItemId}/likes`,
    );
    return response.data.data;
  },
};
