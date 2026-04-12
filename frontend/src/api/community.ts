import { authenticatedAxios } from "@/api/auth";
import type { ApiResponse } from "@/api/http";

export interface PostListDto {
  postId: number;
  title: string;
  authorId: number;
  authorNickname: string;
  country: string;
  representativeImageUrl: string | null;
  createdAt?: string;
  updatedAt?: string | null;
}

export interface SharedItineraryItem {
  itineraryId: number;
  name: string;
  time: string | null;
  order: number;
  memo: string | null;
  location: {
    lat: number;
    lng: number;
    address: string;
  } | null;
}

export interface SharedCategory {
  categoryId: number;
  name: string;
  day: number;
  itineraries: SharedItineraryItem[];
}

export interface SharedTripDetail {
  tripId: number;
  title: string;
  startDate: string;
  endDate: string;
  categories: SharedCategory[];
  members?: Array<{
    memberId: number;
    nickname: string;
    avatar: string | null;
  }>;
}

export interface PostDetailDto {
  postId: number;
  title: string;
  content: string;
  authorId: number;
  authorNickname: string;
  country: string;
  representativeImageUrl: string | null;
  createdAt?: string;
  updatedAt?: string | null;
  trip?: SharedTripDetail | null;
}

export interface PostPageResponse {
  content: PostListDto[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
}

export interface CreatePostRequest {
  tripId: number;
  title: string;
  content: string;
}

export interface UpdatePostRequest {
  title: string;
  content: string;
}

interface BackendPostSummary {
  id: number;
  title: string;
  authorId: number;
  authorNickname: string;
  country: string;
  representativeImageUrl: string | null;
  createdAt?: string;
  updatedAt?: string | null;
}

interface BackendPostDetail extends BackendPostSummary {
  content: string;
}

const toPostSummary = (post: BackendPostSummary): PostListDto => ({
  postId: post.id,
  title: post.title,
  authorId: post.authorId,
  authorNickname: post.authorNickname,
  country: post.country,
  representativeImageUrl: post.representativeImageUrl,
  createdAt: post.createdAt,
  updatedAt: post.updatedAt,
});

const toPostDetail = (post: BackendPostDetail): PostDetailDto => ({
  postId: post.id,
  title: post.title,
  content: post.content,
  authorId: post.authorId,
  authorNickname: post.authorNickname,
  country: post.country,
  representativeImageUrl: post.representativeImageUrl,
  createdAt: post.createdAt,
  updatedAt: post.updatedAt,
  trip: null,
});

const toPostPage = (posts: BackendPostSummary[], page: number, size: number): PostPageResponse => ({
  content: posts.map(toPostSummary),
  pageable: {
    pageNumber: page,
    pageSize: size,
  },
  totalPages: posts.length < size ? 1 : 2,
  totalElements: posts.length,
  last: posts.length < size,
  first: page === 0,
});

export const communityApi = {
  getPosts: async (country?: string, page = 0, size = 10, authorId?: number): Promise<PostPageResponse> => {
    const params: Record<string, number | string> = { page, size };
    if (country) {
      params.country = country;
    }
    if (authorId != null) {
      params.authorId = authorId;
    }

    const response = await authenticatedAxios.get<ApiResponse<{ posts: BackendPostSummary[] }>>("/community/posts", { params });
    return toPostPage(response.data.data?.posts ?? [], page, size);
  },

  getPostById: async (postId: number): Promise<PostDetailDto> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendPostDetail>>(`/community/posts/${postId}`);
    return toPostDetail(response.data.data as BackendPostDetail);
  },

  createPost: async (request: CreatePostRequest, imageFile?: File): Promise<PostDetailDto> => {
    const formData = new FormData();
    formData.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
    if (imageFile) {
      formData.append("image", imageFile);
    }

    const response = await authenticatedAxios.post<ApiResponse<BackendPostDetail>>("/community/posts", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return toPostDetail(response.data.data as BackendPostDetail);
  },

  updatePost: async (postId: number, request: UpdatePostRequest, imageFile?: File): Promise<PostDetailDto> => {
    const formData = new FormData();
    formData.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
    if (imageFile) {
      formData.append("image", imageFile);
    }

    const response = await authenticatedAxios.patch<ApiResponse<BackendPostDetail>>(`/community/posts/${postId}`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return toPostDetail(response.data.data as BackendPostDetail);
  },

  deletePost: async (postId: number): Promise<void> => {
    await authenticatedAxios.delete(`/community/posts/${postId}`);
  },

  getPostsByAuthorId: async (memberId: number, page = 0, size = 12): Promise<PostPageResponse> => {
    return communityApi.getPosts(undefined, page, size, memberId);
  },
};
