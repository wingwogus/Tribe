import { authenticatedAxios, type ApiResponse } from "@/api/http";

export interface DailyExpenseSummary {
  expenseId: number;
  title: string;
  payerName: string;
  totalAmount: number;
  originalAmount?: number;
  currencyCode?: string;
}

export interface MemberDailySummary {
  memberId: number;
  memberName: string;
  paidAmount: number;
  assignedAmount: number;
}

export interface DailySettlementResponse {
  date: string;
  dailyTotalAmount: number;
  expenses: DailyExpenseSummary[];
  memberSummaries: MemberDailySummary[];
  debtRelations?: DebtRelation[];
}

export interface MemberBalance {
  tripMemberId: number;
  nickname: string;
  balance: number;
  foreignCurrenciesUsed?: string[];
}

export interface DebtRelation {
  fromNickname: string;
  fromTripMemberId: number;
  toNickname: string;
  toTripMemberId: number;
  amount: number;
  equivalentOriginalAmount?: number;
  originalCurrencyCode?: string;
}

export interface TotalSettlementResponse {
  memberBalances: MemberBalance[];
  debtRelations: DebtRelation[];
  isExchangeRateApplied?: boolean;
}

export const settlementApi = {
  getDailySettlement: async (tripId: number, date: string): Promise<DailySettlementResponse> => {
    const response = await authenticatedAxios.get<ApiResponse<DailySettlementResponse>>(`/trips/${tripId}/settlements/daily`, {
      params: { date },
    });
    return response.data.data as DailySettlementResponse;
  },

  getTotalSettlement: async (tripId: number): Promise<TotalSettlementResponse> => {
    const response = await authenticatedAxios.get<ApiResponse<TotalSettlementResponse>>(`/trips/${tripId}/settlements/total`);
    return response.data.data as TotalSettlementResponse;
  },
};
