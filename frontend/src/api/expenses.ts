import { authenticatedAxios, type ApiResponse } from "@/api/http";

export interface ExpenseItem {
  itemName: string;
  price: number;
}

export interface CreateExpenseRequest {
  tripId: number;
  expenseTitle: string;
  totalAmount?: number;
  itineraryItemId: number;
  payerId: number;
  inputMethod: string;
  items: ExpenseItem[];
  currency?: string;
  date?: string;
}

export interface ItemAssignment {
  itemId: number;
  participantIds: number[];
}

export interface ParticipantAssignRequest {
  tripId: number;
  items: ItemAssignment[];
}

export interface ParticipantInfo {
  id: number;
  name: string;
  isGuest: boolean;
}

export interface ItemDetailResponse {
  itemId: number;
  itemName: string;
  price: number;
  participants: ParticipantInfo[];
}

export interface ItemSimpleResponse {
  itemId: number;
  itemName: string;
  price: number;
}

export interface ExpenseDetailResponse {
  expenseId: number;
  expenseTitle: string;
  totalAmount: number;
  paymentDate: string;
  payer: ParticipantInfo;
  items: ItemDetailResponse[];
  currency?: string;
  receiptImageUrl?: string;
}

export interface ExpenseCreateResponse {
  expenseId: number;
  expenseTitle: string;
  totalAmount: number;
  paymentDate: string;
  payer: ParticipantInfo;
  items: ItemSimpleResponse[];
  currency?: string;
}

export interface ExpenseSimpleResponse {
  expenseId: number;
  itineraryItemId: number;
  totalAmount: number;
  currency: string | null;
  payer: string;
  expenseTitle: string;
}

export interface ItemUpdate {
  itemId?: number | null;
  itemName: string;
  price: number;
}

export interface UpdateExpenseRequest {
  expenseTitle: string;
  totalAmount: number;
  payerId: number;
  currency: string;
  items: ItemUpdate[];
  date?: string;
}

interface BackendParticipantInfo {
  tripMemberId: number;
  memberId: number | null;
  nickname: string;
  isGuest: boolean;
}

interface BackendItemParticipant extends BackendParticipantInfo {
  amount?: number;
}

interface BackendExpenseItem {
  itemId: number;
  itemName: string;
  price: number;
  participants?: BackendItemParticipant[];
}

interface BackendExpenseDetail {
  expenseId: number;
  itineraryItemId: number | null;
  title: string;
  amount: number;
  currencyCode: string;
  spentAt: string;
  payerTripMemberId: number;
  payerName: string;
  receiptImageUrl: string | null;
  items: BackendExpenseItem[];
}

interface BackendExpenseSummary {
  expenseId: number;
  itineraryItemId: number | null;
  title: string;
  amount: number;
  currencyCode: string;
  payerName: string;
}

const FALLBACK_EXPENSE_CATEGORY = "OTHER";
const FALLBACK_SPLIT_TYPE = "CUSTOM";

const todayString = () => new Date().toISOString().slice(0, 10);

const toInputMethod = (inputMethod: string) => (inputMethod === "receipt" ? "SCAN" : "HANDWRITE");

const toParticipantInfo = (participant: BackendParticipantInfo): ParticipantInfo => ({
  id: participant.tripMemberId,
  name: participant.nickname,
  isGuest: participant.isGuest,
});

const toExpenseDetail = (expense: BackendExpenseDetail): ExpenseDetailResponse => ({
  expenseId: expense.expenseId,
  expenseTitle: expense.title,
  totalAmount: Number(expense.amount),
  paymentDate: expense.spentAt,
  payer: {
    id: expense.payerTripMemberId,
    name: expense.payerName,
    isGuest: false,
  },
  items: expense.items.map((item) => ({
    itemId: item.itemId,
    itemName: item.itemName,
    price: Number(item.price),
    participants: (item.participants ?? []).map(toParticipantInfo),
  })),
  currency: expense.currencyCode,
  receiptImageUrl: expense.receiptImageUrl || undefined,
});

const toCreatePayload = (request: CreateExpenseRequest) => ({
  title: request.expenseTitle,
  amount: request.totalAmount ?? request.items.reduce((sum, item) => sum + item.price, 0),
  currencyCode: request.currency || "KRW",
  spentAt: request.date || todayString(),
  category: FALLBACK_EXPENSE_CATEGORY,
  splitType: FALLBACK_SPLIT_TYPE,
  payerTripMemberId: request.payerId,
  itineraryItemId: request.itineraryItemId || null,
  inputMethod: toInputMethod(request.inputMethod),
  note: null,
  items: request.items.map((item) => ({
    itemName: item.itemName,
    price: item.price,
  })),
});

const toUpdatePayload = (request: UpdateExpenseRequest, itineraryItemId?: number | null) => ({
  title: request.expenseTitle,
  amount: request.totalAmount,
  currencyCode: request.currency || "KRW",
  spentAt: request.date || todayString(),
  category: FALLBACK_EXPENSE_CATEGORY,
  splitType: FALLBACK_SPLIT_TYPE,
  payerTripMemberId: request.payerId,
  itineraryItemId: itineraryItemId ?? null,
  inputMethod: "HANDWRITE",
  note: null,
  items: request.items.map((item) => ({
    itemId: item.itemId ?? null,
    itemName: item.itemName,
    price: item.price,
  })),
});

export const expensesApi = {
  createExpense: async (request: CreateExpenseRequest, imageFile?: File): Promise<ExpenseCreateResponse> => {
    const formData = new FormData();
    formData.append("request", new Blob([JSON.stringify(toCreatePayload(request))], { type: "application/json" }));

    if (imageFile) {
      formData.append("image", imageFile);
    }

    const response = await authenticatedAxios.post<ApiResponse<BackendExpenseDetail>>(
      `/trips/${request.tripId}/expenses`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      },
    );

    const detail = toExpenseDetail(response.data.data as BackendExpenseDetail);
    return {
      ...detail,
      items: detail.items.map((item) => ({
        itemId: item.itemId,
        itemName: item.itemName,
        price: item.price,
      })),
    };
  },

  getExpenseDetail: async (tripId: number, expenseId: number): Promise<ExpenseDetailResponse> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendExpenseDetail>>(`/trips/${tripId}/expenses/${expenseId}`);
    return toExpenseDetail(response.data.data as BackendExpenseDetail);
  },

  assignParticipants: async (expenseId: number, request: ParticipantAssignRequest): Promise<ExpenseDetailResponse> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendExpenseDetail>>(
      `/trips/${request.tripId}/expenses/${expenseId}/assignments`,
      { items: request.items },
    );
    return toExpenseDetail(response.data.data as BackendExpenseDetail);
  },

  updateExpense: async (expenseId: number, request: UpdateExpenseRequest & { tripId?: number; itineraryItemId?: number | null }): Promise<ExpenseDetailResponse> => {
    const tripId = request.tripId;
    if (!tripId) {
      throw new Error("tripId is required to update an expense.");
    }

    const response = await authenticatedAxios.patch<ApiResponse<BackendExpenseDetail>>(
      `/trips/${tripId}/expenses/${expenseId}`,
      toUpdatePayload(request, request.itineraryItemId),
      {
        headers: {
          "Content-Type": "application/json",
        },
      },
    );

    return toExpenseDetail(response.data.data as BackendExpenseDetail);
  },

  deleteExpense: async (tripId: number, expenseId: number): Promise<void> => {
    await authenticatedAxios.delete(`/trips/${tripId}/expenses/${expenseId}`);
  },

  getTripExpenses: async (tripId: number): Promise<ExpenseSimpleResponse[]> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendExpenseSummary[]>>(`/trips/${tripId}/expenses`);
    return (response.data.data ?? []).map((expense) => ({
      expenseId: expense.expenseId,
      itineraryItemId: expense.itineraryItemId ?? 0,
      totalAmount: Number(expense.amount),
      currency: expense.currencyCode,
      payer: expense.payerName,
      expenseTitle: expense.title,
    }));
  },
};
