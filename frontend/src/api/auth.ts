import {
  authenticatedAxios,
  clearStoredSession,
  publicAxios,
  readApiErrorMessage,
  setStoredAccessToken,
  type ApiResponse,
} from "@/api/http";

export { authenticatedAxios } from "@/api/http";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  data?: {
    accessToken: string;
    isFirstLogin: boolean;
  };
  message?: string;
}

export interface ReissueResponse {
  success: boolean;
  data?: {
    accessToken: string;
    isFirstLogin: boolean;
  };
  message?: string;
}

export interface SignupRequest {
  email: string;
  nickname: string;
  password: string;
  avatar?: string;
}

export interface SignupResponse {
  success: boolean;
  message?: string;
}

export interface VerificationRequest {
  email: string;
}

export interface VerificationResponse {
  success: boolean;
  message: string;
}

export interface CodeVerificationRequest {
  email: string;
  code: string;
}

export interface CodeVerificationResponse {
  success: boolean;
  message: string;
}

export interface NicknameCheckRequest {
  nickname: string;
}

export interface NicknameCheckResponse {
  success: boolean;
  available: boolean;
  message: string;
}

export interface UpdateNicknameRequest {
  nickname: string;
}

export interface MemberResponse {
  memberId: number;
  nickname: string;
  email: string;
  avatar: string | null;
  isNewUser: boolean;
}

export const reissue = async (): Promise<ReissueResponse> => {
  try {
    const response = await publicAxios.post<ApiResponse<{ accessToken: string; isFirstLogin: boolean }>>("/auth/reissue");
    const payload = response.data;
    setStoredAccessToken(payload.data?.accessToken ?? null);

    return {
      success: payload.success,
      data: payload.data ?? undefined,
      message: payload.error?.message,
    };
  } catch (error) {
    return {
      success: false,
      message: readApiErrorMessage(error, "토큰 재발급에 실패했습니다."),
    };
  }
};

export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  try {
    const response = await publicAxios.post<ApiResponse<{ accessToken: string; isFirstLogin: boolean }>>("/auth/login", data);
    const payload = response.data;
    setStoredAccessToken(payload.data?.accessToken ?? null);

    return {
      success: payload.success,
      data: payload.data ?? undefined,
      message: payload.error?.message,
    };
  } catch (error) {
    return {
      success: false,
      message: readApiErrorMessage(error, "로그인에 실패했습니다."),
    };
  }
};

export const sendVerificationEmail = async (data: VerificationRequest): Promise<VerificationResponse> => {
  try {
    const response = await publicAxios.post<ApiResponse<null>>("/auth/email/send-code", data);
    return {
      success: response.data.success,
      message: response.data.error?.message || "인증 코드가 이메일로 발송되었습니다.",
    };
  } catch (error) {
    return {
      success: false,
      message: readApiErrorMessage(error, "이메일 전송에 실패했습니다."),
    };
  }
};

export const verifyEmailCode = async (data: CodeVerificationRequest): Promise<CodeVerificationResponse> => {
  try {
    const response = await publicAxios.post<ApiResponse<null>>("/auth/email/verify-code", data);
    return {
      success: response.data.success,
      message: response.data.error?.message || "이메일 인증이 완료되었습니다.",
    };
  } catch (error) {
    return {
      success: false,
      message: readApiErrorMessage(error, "인증에 실패했습니다."),
    };
  }
};

export const checkNickname = async (data: NicknameCheckRequest): Promise<NicknameCheckResponse> => {
  try {
    const response = await publicAxios.post<ApiResponse<null>>("/auth/nickname/check", data);
    return {
      success: response.data.success,
      available: response.data.success,
      message: response.data.error?.message || "사용 가능한 닉네임입니다.",
    };
  } catch (error) {
    return {
      success: false,
      available: false,
      message: readApiErrorMessage(error, "닉네임 확인에 실패했습니다."),
    };
  }
};

export const signup = async (data: SignupRequest): Promise<SignupResponse> => {
  try {
    const response = await publicAxios.post<ApiResponse<null>>("/auth/signup", data);
    return {
      success: response.data.success,
      message: response.data.error?.message || "회원가입이 완료되었습니다.",
    };
  } catch (error) {
    return {
      success: false,
      message: readApiErrorMessage(error, "회원가입에 실패했습니다."),
    };
  }
};

export const getMemberInfo = async (): Promise<MemberResponse | null> => {
  try {
    const response = await authenticatedAxios.get<ApiResponse<MemberResponse>>("/members/me");
    return response.data.data;
  } catch {
    return null;
  }
};

export const updateNickname = async (data: UpdateNicknameRequest): Promise<{ success: boolean; message: string }> => {
  try {
    const response = await authenticatedAxios.patch<ApiResponse<MemberResponse>>("/members/me", data);
    return {
      success: response.data.success,
      message: response.data.error?.message || "닉네임이 변경되었습니다.",
    };
  } catch (error) {
    return {
      success: false,
      message: readApiErrorMessage(error, "닉네임 변경에 실패했습니다."),
    };
  }
};

export const logout = async (): Promise<{ success: boolean }> => {
  try {
    await authenticatedAxios.post<ApiResponse<null>>("/auth/logout");
  } catch {
    // Logout is best-effort once local session is cleared.
  } finally {
    clearStoredSession();
  }

  return { success: true };
};
