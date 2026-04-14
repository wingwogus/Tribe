import { authenticatedAxios, type ApiResponse } from "@/api/http";

export enum Country {
  SOUTH_KOREA = "SOUTH_KOREA",
  JAPAN = "JAPAN",
  CHINA = "CHINA",
  THAILAND = "THAILAND",
  VIETNAM = "VIETNAM",
  PHILIPPINES = "PHILIPPINES",
  SINGAPORE = "SINGAPORE",
  MALAYSIA = "MALAYSIA",
  INDONESIA = "INDONESIA",
  INDIA = "INDIA",
  UAE = "UAE",
  TURKEY = "TURKEY",
  EGYPT = "EGYPT",
  ITALY = "ITALY",
  FRANCE = "FRANCE",
  SPAIN = "SPAIN",
  UK = "UK",
  GERMANY = "GERMANY",
  SWITZERLAND = "SWITZERLAND",
  NETHERLANDS = "NETHERLANDS",
  GREECE = "GREECE",
  USA = "USA",
  CANADA = "CANADA",
  AUSTRALIA = "AUSTRALIA",
  NEW_ZEALAND = "NEW_ZEALAND",
  BRAZIL = "BRAZIL",
  ARGENTINA = "ARGENTINA",
  MEXICO = "MEXICO",
  SOUTH_AFRICA = "SOUTH_AFRICA",
  MOROCCO = "MOROCCO",
}

export type TripRole = "OWNER" | "ADMIN" | "MEMBER" | "GUEST" | "EXITED" | "KICKED";

export interface MemberInfo {
  memberId: number | null;
  tripMemberId: number | null;
  nickname: string;
  avatar: string | null;
  role: TripRole;
}

export interface TripDetail {
  tripId: number;
  title: string;
  startDate: string;
  endDate: string;
  country: string;
  regionCode: string | null;
  members: MemberInfo[];
}

export interface SimpleTrip {
  tripId: number;
  title: string;
  startDate: string;
  endDate: string;
  country: string;
  regionCode: string | null;
  memberCount: number;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

export interface CreateTripRequest {
  title: string;
  startDate: string;
  endDate: string;
  country: Country;
  regionCode?: string | null;
}

export interface UpdateTripRequest {
  title: string;
  startDate: string;
  endDate: string;
  country: Country;
  regionCode?: string | null;
}

export interface InvitationResponse {
  inviteLink: string;
}

export interface AddGuestRequest {
  name: string;
}

export interface AssignRoleRequest {
  requestRole: "ADMIN" | "MEMBER";
}

export interface GuestMember {
  id: number;
  name: string;
  isGuest: boolean;
}

export interface ImportTripRequest {
  postId: number;
  title: string;
  startDate: string;
  endDate: string;
}

interface BackendTripMember {
  tripMemberId: number;
  memberId: number | null;
  nickname: string;
  role: TripRole;
}

interface BackendSimpleTrip {
  tripId: number;
  title: string;
  startDate: string;
  endDate: string;
  country: string;
  regionCode: string | null;
  memberCount: number;
}

interface BackendTripDetail extends BackendSimpleTrip {
  members: BackendTripMember[];
}

const toMemberInfo = (member: BackendTripMember): MemberInfo => ({
  memberId: member.memberId,
  tripMemberId: member.tripMemberId,
  nickname: member.nickname,
  avatar: null,
  role: member.role,
});

const toTripDetail = (trip: BackendTripDetail): TripDetail => ({
  tripId: trip.tripId,
  title: trip.title,
  startDate: trip.startDate,
  endDate: trip.endDate,
  country: trip.country,
  regionCode: trip.regionCode ?? null,
  members: trip.members.map(toMemberInfo),
});

const toSimpleTrip = (trip: BackendSimpleTrip): SimpleTrip => ({
  tripId: trip.tripId,
  title: trip.title,
  startDate: trip.startDate,
  endDate: trip.endDate,
  country: trip.country,
  regionCode: trip.regionCode ?? null,
  memberCount: trip.memberCount,
});

const toPageResponse = <T>(content: T[], page: number, size: number): PageResponse<T> => ({
  content,
  pageable: {
    pageNumber: page,
    pageSize: size,
    sort: {
      empty: true,
      sorted: false,
      unsorted: true,
    },
    offset: page * size,
    paged: true,
    unpaged: false,
  },
  totalPages: content.length < size ? page + 1 : page + 2,
  totalElements: page * size + content.length,
  last: content.length < size,
  size,
  number: page,
  sort: {
    empty: true,
    sorted: false,
    unsorted: true,
  },
  numberOfElements: content.length,
  first: page === 0,
  empty: content.length === 0,
});

const findGuestMember = (trip: TripDetail, nickname: string): GuestMember => {
  const guest = [...trip.members]
    .reverse()
    .find((member) => member.role === "GUEST" && member.nickname === nickname);

  if (!guest?.tripMemberId) {
    throw new Error("게스트 정보를 찾을 수 없습니다.");
  }

  return {
    id: guest.tripMemberId,
    name: guest.nickname,
    isGuest: true,
  };
};

export const tripApi = {
  getTrips: async (page = 0, size = 10): Promise<PageResponse<SimpleTrip>> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendSimpleTrip[]>>("/trips", {
      params: { page, size },
    });

    return toPageResponse((response.data.data ?? []).map(toSimpleTrip), page, size);
  },

  getTripById: async (tripId: number): Promise<TripDetail> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendTripDetail>>(`/trips/${tripId}`);
    return toTripDetail(response.data.data as BackendTripDetail);
  },

  createTrip: async (tripData: CreateTripRequest): Promise<TripDetail> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendTripDetail>>("/trips", tripData);
    return toTripDetail(response.data.data as BackendTripDetail);
  },

  updateTrip: async (tripId: number, updates: UpdateTripRequest): Promise<TripDetail> => {
    const response = await authenticatedAxios.patch<ApiResponse<BackendTripDetail>>(`/trips/${tripId}`, updates);
    return toTripDetail(response.data.data as BackendTripDetail);
  },

  deleteTrip: async (tripId: number): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}`);
  },

  generateInvite: async (tripId: number): Promise<InvitationResponse> => {
    const response = await authenticatedAxios.post<ApiResponse<InvitationResponse>>(`/trips/${tripId}/invite`);
    return response.data.data as InvitationResponse;
  },

  joinTrip: async (token: string): Promise<TripDetail> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendTripDetail>>("/trips/join", { token });
    return toTripDetail(response.data.data as BackendTripDetail);
  },

  addGuest: async (tripId: number, request: AddGuestRequest): Promise<GuestMember> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendTripDetail>>(`/trips/${tripId}/guests`, {
      nickname: request.name,
    });

    return findGuestMember(toTripDetail(response.data.data as BackendTripDetail), request.name);
  },

  deleteGuest: async (tripId: number, guestId: number): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}/guests/${guestId}`);
  },

  assignRole: async (tripId: number, tripMemberId: number, request: AssignRoleRequest): Promise<GuestMember> => {
    const response = await authenticatedAxios.patch<ApiResponse<BackendTripDetail>>(
      `/trips/${tripId}/members/${tripMemberId}/role`,
      { role: request.requestRole },
    );

    const nextTrip = toTripDetail(response.data.data as BackendTripDetail);
    const targetMember = nextTrip.members.find((member) => member.tripMemberId === tripMemberId);
    if (!targetMember?.tripMemberId) {
      throw new Error("변경된 멤버 정보를 찾을 수 없습니다.");
    }

    return {
      id: targetMember.tripMemberId,
      name: targetMember.nickname,
      isGuest: targetMember.role === "GUEST",
    };
  },

  kickMember: async (tripId: number, tripMemberId: number): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}/members/${tripMemberId}`);
  },

  leaveTrip: async (tripId: number): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}/members/me`);
  },

  importTrip: async (request: ImportTripRequest): Promise<TripDetail> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendTripDetail>>("/trips/import", request);
    return toTripDetail(response.data.data as BackendTripDetail);
  },
};
