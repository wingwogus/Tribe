export type TripRealtimeEventType =
  | "TRIP_LIFECYCLE"
  | "TRIP_MEMBER"
  | "ITINERARY"
  | "WISHLIST";

export interface TripRealtimeEvent<T = unknown> {
  type: TripRealtimeEventType;
  tripId: number;
  actorId: number;
  lifecycle?: T;
  member?: T;
  itinerary?: T;
  wishlist?: T;
}

export type ChatRealtimeEventType = "MESSAGE" | "READ" | "UNREAD_COUNT";

export interface ChatRealtimeMessagePayload {
  id: number;
  roomId: number;
  senderId: number;
  content: string;
  createdAt: string | null;
}

export interface ChatRealtimeReadPayload {
  readerId: number;
  lastReadMessageId: number;
}

export interface ChatRealtimeUnreadPayload {
  memberId: number;
  unreadCount: number;
}

export interface ChatRealtimeEvent {
  type: ChatRealtimeEventType;
  roomId: number;
  message?: ChatRealtimeMessagePayload | null;
  read?: ChatRealtimeReadPayload | null;
  unread?: ChatRealtimeUnreadPayload | null;
}

export const buildTripTopic = (tripId: string | number) => `/sub/trips/${tripId}`;

export const buildChatRoomTopic = (tripId: string | number) => `/sub/chat/rooms/${tripId}`;
