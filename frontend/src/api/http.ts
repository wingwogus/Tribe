import axios, { type AxiosError, type AxiosRequestConfig } from "axios";

export interface ApiError {
  code: string;
  message: string;
  detail?: unknown;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error?: ApiError | null;
}

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1";
const FALLBACK_ORIGIN = typeof window === "undefined" ? "http://localhost:8081" : window.location.origin;
export const BACKEND_ORIGIN = (import.meta.env.VITE_BACKEND_ORIGIN ||
  (API_BASE_URL.startsWith("http") ? new URL(API_BASE_URL).origin : FALLBACK_ORIGIN)).replace(/\/$/, "");
export const OAUTH_LOGIN_URL = `${BACKEND_ORIGIN}/oauth2/authorization/kakao`;
export const WEBSOCKET_ENDPOINT = `${BACKEND_ORIGIN}${(import.meta.env.VITE_WS_PATH || "/ws").startsWith("/") ? import.meta.env.VITE_WS_PATH || "/ws" : `/${import.meta.env.VITE_WS_PATH}`}`;

const ACCESS_TOKEN_KEY = "accessToken";

export const getStoredAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY);

export const setStoredAccessToken = (accessToken: string | null | undefined) => {
  if (accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    return;
  }

  localStorage.removeItem(ACCESS_TOKEN_KEY);
};

export const clearStoredSession = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
};

const createAxiosInstance = () =>
  axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
  });

export const publicAxios = createAxiosInstance();
export const authenticatedAxios = createAxiosInstance();

let refreshPromise: Promise<string | null> | null = null;

authenticatedAxios.interceptors.request.use((config) => {
  const accessToken = getStoredAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

const refreshAccessToken = async () => {
  if (!refreshPromise) {
    refreshPromise = publicAxios
      .post<ApiResponse<{ accessToken: string }>>("/auth/reissue")
      .then((response) => {
        const nextAccessToken = response.data.data?.accessToken ?? null;
        setStoredAccessToken(nextAccessToken);
        return nextAccessToken;
      })
      .catch(() => {
        clearStoredSession();
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
};

export const reissueAccessToken = async () => refreshAccessToken();

authenticatedAxios.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (AxiosRequestConfig & { _retry?: boolean }) | undefined;

    if (!originalRequest || originalRequest._retry || error.response?.status !== 401) {
      throw error;
    }

    if (originalRequest.url?.includes("/auth/reissue")) {
      clearStoredSession();
      throw error;
    }

    originalRequest._retry = true;

    const nextAccessToken = await refreshAccessToken();
    if (!nextAccessToken) {
      throw error;
    }

    originalRequest.headers = originalRequest.headers ?? {};
    (originalRequest.headers as Record<string, string>).Authorization = `Bearer ${nextAccessToken}`;
    return authenticatedAxios(originalRequest);
  },
);

export const readApiErrorMessage = (error: unknown, fallback = "요청 처리 중 오류가 발생했습니다.") => {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    return error.response?.data?.error?.message || fallback;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
};
