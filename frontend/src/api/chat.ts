import { authenticatedAxios } from './auth';
import type { TripRole } from './trips';

export interface ChatSender {
  memberId: number | null;
  tripMemberId: number | null;
  nickname: string;
  avatar: string | null;
  role: TripRole;
}

export interface ChatMessage {
  messageId: number;
  sender: ChatSender;
  content: string;
  timestamp: string;
}

export interface ChatHistoryResponse {
  content: ChatMessage[];
  nextCursor: string | null;
  hasNext: boolean;
}

export const chatApi = {
  // 채팅 기록 조회 (커서 기반 무한 스크롤)
  getChatHistory: async (
    tripId: number,
    cursor?: string,
    pageSize: number = 20,
  ): Promise<ChatHistoryResponse> => {
    const res = await authenticatedAxios.get<{ data: ChatHistoryResponse }>(
      `/trips/${tripId}/chat`,
      { params: { cursor, pageSize } },
    );
    return res.data.data;
  },

  // 채팅 전송: 서버에서 ChatMessage(Response)를 반환
  sendChat: async (tripId: number, content: string): Promise<ChatMessage> => {
    const res = await authenticatedAxios.post<{ data: ChatMessage }>(
      `/trips/${tripId}/chat`,
      { content },
    );
    return res.data.data;
  },
};
