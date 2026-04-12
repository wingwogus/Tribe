import { authenticatedAxios, type ApiResponse } from "@/api/http";
import { fetchAllItinerariesForTrip, type ItineraryResponse } from "@/api/itinerary";

export interface CategoryResponse {
  categoryId: number;
  name: string;
  day: number;
  order: number;
  tripId: number;
  itineraryItems: ItineraryResponse[];
  memo: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryRequest {
  name: string;
  day: number;
  order: number;
}

export interface UpdateCategoryRequest {
  name?: string;
  day?: number;
  order?: number;
  memo?: string;
}

export interface OrderUpdateRequest {
  items: OrderCategory[];
}

export interface OrderCategory {
  categoryId: number;
  order: number;
}

type BackendCategory = Omit<CategoryResponse, "itineraryItems">;

const composeCategories = async (tripId: number, rawCategories: BackendCategory[]) => {
  const items = await fetchAllItinerariesForTrip(tripId);

  return rawCategories.map((category) => ({
    ...category,
    itineraryItems: items.filter((item) => item.categoryId === category.categoryId),
  }));
};

export const categoryApi = {
  createCategory: async (tripId: number, request: CreateCategoryRequest): Promise<CategoryResponse> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendCategory>>(`/trips/${tripId}/categories`, request);
    const [category] = await composeCategories(tripId, [response.data.data as BackendCategory]);
    return category;
  },

  getCategories: async (tripId: number, day?: number): Promise<CategoryResponse[]> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendCategory[]>>(`/trips/${tripId}/categories`, {
      params: { day },
    });

    return composeCategories(tripId, response.data.data ?? []);
  },

  getCategory: async (tripId: number, categoryId: number): Promise<CategoryResponse> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendCategory>>(`/trips/${tripId}/categories/${categoryId}`);
    const [category] = await composeCategories(tripId, [response.data.data as BackendCategory]);
    return category;
  },

  updateCategory: async (tripId: number, categoryId: number, request: UpdateCategoryRequest): Promise<CategoryResponse> => {
    const response = await authenticatedAxios.patch<ApiResponse<BackendCategory>>(`/trips/${tripId}/categories/${categoryId}`, request);
    const [category] = await composeCategories(tripId, [response.data.data as BackendCategory]);
    return category;
  },

  deleteCategory: async (tripId: number, categoryId: number): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}/categories/${categoryId}`);
  },

  updateCategoryOrder: async (tripId: number, request: OrderUpdateRequest): Promise<void> => {
    await authenticatedAxios.patch(`/trips/${tripId}/categories/order`, request);
  },
};
