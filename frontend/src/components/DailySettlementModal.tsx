import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Separator} from "@/components/ui/separator";
import {Badge} from "@/components/ui/badge";
import {Calendar, Eye, Receipt, TrendingUp, Wallet, ChevronDown} from "lucide-react";
import {DailySettlementResponse} from "@/api/settlement";
import {formatCurrency, getCurrencyInfo} from "@/lib/currency";
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible";
import {useMemo, useState} from "react";

interface DailySettlementModalProps {
  isOpen: boolean;
  onClose: () => void;
  settlement: DailySettlementResponse | null;
  isLoading?: boolean;
  onViewExpenseDetail?: (expenseId: number) => void;
}

export const DailySettlementModal = ({
  isOpen,
  onClose,
  settlement,
  isLoading = false,
  onViewExpenseDetail,
}: DailySettlementModalProps) => {
  const [openGroups, setOpenGroups] = useState<Set<string>>(new Set());

  const groupedDebtRelations = useMemo(() => {
    if (!settlement?.debtRelations) return {};
    return settlement.debtRelations.reduce((acc, relation) => {
      const key = relation.fromNickname;
      if (!acc[key]) {
        acc[key] = [];
      }
      acc[key].push(relation);
      return acc;
    }, {} as Record<string, typeof settlement.debtRelations>);
  }, [settlement?.debtRelations]);

  const toggleGroup = (name: string) => {
    setOpenGroups(prev => {
      const newSet = new Set(prev);
      if (newSet.has(name)) {
        newSet.delete(name);
      } else {
        newSet.add(name);
      }
      return newSet;
    });
  };

  if (!settlement && !isLoading) {
    return null;
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[650px] max-h-[85vh] overflow-y-auto">
        <DialogHeader className="space-y-3 pb-6">
          <DialogTitle className="flex items-center text-2xl">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary/20 to-primary/10 mr-3 shadow-lg">
              <Receipt className="w-6 h-6 text-primary" />
            </div>
            일별 정산 내역
          </DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="w-10 h-10 border-3 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
        ) : settlement ? (
          <div className="space-y-6 animate-fade-in">
            {/* Date and Total */}
            <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary/10 via-primary/5 to-background border-2 border-primary/20 p-6 shadow-lg">
              <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16" />
              <div className="absolute bottom-0 left-0 w-24 h-24 bg-primary/5 rounded-full -ml-12 -mb-12" />
              <div className="relative text-center">
                <div className="flex items-center justify-center mb-3">
                  <Calendar className="w-4 h-4 mr-2 text-primary" />
                  <p className="text-base font-medium text-muted-foreground">
                    {new Date(settlement.date).toLocaleDateString('ko-KR', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                      weekday: 'long'
                    })}
                  </p>
                </div>
                <div className="flex items-center justify-center space-x-2 mb-2">
                  <Wallet className="w-5 h-5 text-primary" />
                  <p className="text-sm font-medium text-muted-foreground">총 지출</p>
                </div>
                <p className="text-4xl font-bold bg-gradient-to-r from-primary to-primary/70 bg-clip-text text-transparent">
                  ₩{settlement.dailyTotalAmount.toLocaleString()}
                </p>
              </div>
            </div>

            <Separator />

            {/* Expenses List */}
            {settlement.expenses.length > 0 && (
              <div>
                <h4 className="font-semibold mb-4 flex items-center text-lg">
                  <Receipt className="w-5 h-5 mr-2 text-primary" />
                  지출 내역
                </h4>
                <div className="space-y-3">
                  {settlement.expenses.map((expense, index) => (
                    <Card 
                      key={expense.expenseId} 
                      className="group hover:shadow-lg hover:border-primary/30 transition-all duration-200 border-2 animate-fade-in"
                      style={{ animationDelay: `${index * 50}ms` }}
                    >
                      <CardContent className="p-4">
                        <div className="flex items-center justify-between">
                          <div className="flex-1">
                            <p className="font-semibold text-base mb-1">{expense.title}</p>
                            <div className="flex items-center">
                              <Badge variant="secondary" className="text-xs">
                                {expense.payerName}
                              </Badge>
                            </div>
                          </div>
                          <div className="flex items-center space-x-3">
                            <div className="text-right">
                              <span className="font-bold text-lg text-primary">
                                ₩{expense.totalAmount.toLocaleString()}
                              </span>
                              {expense.currencyCode && expense.currencyCode !== 'KRW' && expense.originalAmount && (
                                <div className="text-sm text-muted-foreground mt-1">
                                  {formatCurrency(expense.originalAmount, expense.currencyCode)}
                                </div>
                              )}
                            </div>
                            {onViewExpenseDetail && (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => onViewExpenseDetail(expense.expenseId)}
                                className="h-9 w-9 p-0 hover:bg-primary/10 hover:scale-110 transition-transform"
                                title="상세 보기"
                              >
                                <Eye className="w-4 h-4" />
                              </Button>
                            )}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            )}

            <Separator />

            {/* Debt Relations */}
            {settlement.debtRelations && settlement.debtRelations.length > 0 && (
              <>
                <Separator />
                <div>
                  <h4 className="font-semibold mb-4 flex items-center text-lg">
                    <TrendingUp className="w-5 h-5 mr-2 text-primary" />
                    정산 방법
                  </h4>
                  <div className="space-y-3">
                    {Object.entries(groupedDebtRelations).map(([fromNickname, debts], groupIndex) => {
                      const isOpen = openGroups.has(fromNickname);
                      const totalAmount = debts.reduce((sum, d) => sum + d.amount, 0);
                      
                      return (
                        <Collapsible
                          key={fromNickname}
                          open={isOpen}
                          onOpenChange={() => toggleGroup(fromNickname)}
                        >
                          <Card 
                            className="overflow-hidden border-2 border-blue-200 dark:border-blue-900 animate-fade-in"
                            style={{ animationDelay: `${groupIndex * 50}ms` }}
                          >
                            <CollapsibleTrigger asChild>
                              <CardHeader className="cursor-pointer hover:bg-blue-50/50 dark:hover:bg-blue-950/20 transition-colors p-4">
                                <div className="flex items-center justify-between">
                                  <div className="flex items-center gap-2">
                                    <ChevronDown className={`w-5 h-5 text-primary transition-transform ${isOpen ? 'rotate-180' : ''}`} />
                                    <span className="font-semibold text-lg">{fromNickname}님이 보내야 할 금액</span>
                                  </div>
                                  <span className="font-bold text-lg text-blue-600 dark:text-blue-500">
                                    ₩{totalAmount.toLocaleString()}
                                  </span>
                                </div>
                              </CardHeader>
                            </CollapsibleTrigger>
                            <CollapsibleContent>
                              <CardContent className="pt-0 pb-4 px-4 space-y-2">
                                {debts.map((debt, debtIndex) => (
                                  <div 
                                    key={debtIndex}
                                    className="flex items-center justify-between py-3 px-4 rounded-lg bg-gradient-to-r from-blue-50 to-blue-50/30 dark:from-blue-950/30 dark:to-blue-950/10"
                                  >
                                    <div className="flex items-center gap-2">
                                      <TrendingUp className="w-4 h-4 text-blue-600 dark:text-blue-500" />
                                      <span className="font-medium">{debt.toNickname}에게</span>
                                    </div>
                                    <div className="text-right">
                                      <span className="font-semibold text-blue-600 dark:text-blue-500">
                                        ₩{debt.amount.toLocaleString()}
                                      </span>
                                      {debt.originalCurrencyCode && debt.originalCurrencyCode !== 'KRW' && debt.equivalentOriginalAmount && (
                                        <div className="text-sm text-muted-foreground">
                                          {getCurrencyInfo(debt.originalCurrencyCode).symbol}{debt.equivalentOriginalAmount.toLocaleString()}
                                        </div>
                                      )}
                                    </div>
                                  </div>
                                ))}
                              </CardContent>
                            </CollapsibleContent>
                          </Card>
                        </Collapsible>
                      );
                    })}
                  </div>
                </div>
              </>
            )}

            {/* Close Button */}
            <Button onClick={onClose} className="w-full h-12 text-base font-semibold shadow-lg">
              닫기
            </Button>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  );
};
