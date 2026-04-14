import { useCallback, useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useQueryClient } from "@tanstack/react-query";
import {
  buildChatRoomTopic,
  buildTripTopic,
  type ChatRealtimeEvent,
  type TripRealtimeEvent,
} from "@/api/websocket";
import { getStoredAccessToken, reissueAccessToken, WEBSOCKET_ENDPOINT } from "@/api/http";
import { useToast } from "@/hooks/use-toast";
import { normalizeTripId, tripQueryKeys } from "@/lib/tripQueryKeys";

interface UseTripWebSocketOptions {
  tripId: number | string;
  currentUserId?: number;
  enabled?: boolean;
}

interface TripMemberRealtimePayload {
  action?: string;
  member?: {
    nickname?: string;
    role?: string;
  };
}

export const useTripWebSocket = ({
  tripId,
  currentUserId,
  enabled = true,
}: UseTripWebSocketOptions) => {
  const normalizedTripId = normalizeTripId(tripId);
  const clientRef = useRef<Client | null>(null);
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [isConnected, setIsConnected] = useState(false);

  const notifyTripMemberChange = useCallback((payload?: TripMemberRealtimePayload) => {
    const displayName = payload?.member?.nickname || "멤버";

    if (payload?.action === "ROLE_CHANGED") {
      const nextRole = payload.member?.role === "ADMIN" ? "관리자" : "멤버";
      toast({
        title: "역할 변경",
        description: `${displayName}님의 역할이 ${nextRole}(으)로 변경되었습니다.`,
      });
      return;
    }

    const messages: Record<string, string> = {
      GUEST_ADDED: `${displayName}님이 게스트로 추가되었습니다.`,
      GUEST_DELETED: `${displayName}님이 게스트 목록에서 제거되었습니다.`,
      MEMBER_JOINED: `${displayName}님이 여행에 참여했습니다.`,
      MEMBER_LEFT: `${displayName}님이 여행에서 나갔습니다.`,
      MEMBER_KICKED: `${displayName}님이 여행에서 제외되었습니다.`,
    };

    if (payload?.action) {
      toast({
        title: "멤버 변경",
        description: messages[payload.action] || `${displayName}님의 상태가 변경되었습니다.`,
      });
    }
  }, [toast]);

  const handleTripMessage = useCallback((message: IMessage) => {
    try {
      const data: TripRealtimeEvent = JSON.parse(message.body);

      if (currentUserId && data.actorId === currentUserId) {
        return;
      }

      switch (data.type) {
        case "ITINERARY":
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.itinerary(normalizedTripId) });
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.directions(normalizedTripId) });
          break;

        case "WISHLIST":
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.wishlistRoot(normalizedTripId) });
          break;

        case "TRIP_MEMBER":
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.trip(normalizedTripId) });
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.expenses(normalizedTripId) });
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.chat(normalizedTripId) });
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.dailySettlementRoot(normalizedTripId) });
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.totalSettlement(normalizedTripId) });
          notifyTripMemberChange(data.member as TripMemberRealtimePayload | undefined);
          break;

        case "TRIP_LIFECYCLE":
          queryClient.invalidateQueries({ queryKey: ["trips"] });
          queryClient.invalidateQueries({ queryKey: tripQueryKeys.trip(normalizedTripId) });
          break;
      }
    } catch (error) {
      console.error("[WebSocket] Parse error:", error);
    }
  }, [currentUserId, normalizedTripId, notifyTripMemberChange, queryClient]);

  const handleChatMessage = useCallback((message: IMessage) => {
    try {
      const data: ChatRealtimeEvent = JSON.parse(message.body);

      if (data.type === "MESSAGE" && currentUserId && data.message?.senderId === currentUserId) {
        return;
      }

      queryClient.invalidateQueries({ queryKey: tripQueryKeys.chat(normalizedTripId) });
    } catch (error) {
      console.error("[WebSocket] Chat parse error:", error);
    }
  }, [currentUserId, normalizedTripId, queryClient]);

  useEffect(() => {
    if (!enabled || !normalizedTripId) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(WEBSOCKET_ENDPOINT, null, {
        transports: ["websocket", "xhr-streaming", "xhr-polling"],
      }),
      connectHeaders: {},
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (value) => {
        console.log("[STOMP Debug]", value);
      },
      beforeConnect: async () => {
        const accessToken = getStoredAccessToken() || (await reissueAccessToken());
        if (!accessToken) {
          throw new Error("No access token available for websocket connection.");
        }

        client.connectHeaders = {
          Authorization: `Bearer ${accessToken}`,
        };
        console.log("[WebSocket] Connecting...");
      },
      onConnect: () => {
        client.subscribe(buildTripTopic(normalizedTripId), handleTripMessage);
        client.subscribe(buildChatRoomTopic(normalizedTripId), handleChatMessage);
        setIsConnected(true);
      },
      onWebSocketClose: () => {
        setIsConnected(false);
      },
      onDisconnect: () => {
        setIsConnected(false);
      },
      onStompError: (frame) => {
        console.error("[WebSocket] STOMP Error:", frame.headers.message);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
    };
  }, [enabled, handleChatMessage, handleTripMessage, normalizedTripId]);

  return {
    isConnected,
  };
};
