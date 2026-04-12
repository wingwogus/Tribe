import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Card, CardContent, CardHeader} from "@/components/ui/card";
import {Separator} from "@/components/ui/separator";
import {Badge} from "@/components/ui/badge";
import {ArrowRight, Calculator, CheckCircle2, Info, Users, ChevronDown} from "lucide-react";
import {TotalSettlementResponse} from "@/api/settlement";
import {getCurrencyInfo} from "@/lib/currency";
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible";
import {useMemo, useState} from "react";

interface TotalSettlementModalProps {
  isOpen: boolean;
  onClose: () => void;
  settlement: TotalSettlementResponse | null;
  isLoading?: boolean;
}

export const TotalSettlementModal = ({
  isOpen,
  onClose,
  settlement,
  isLoading = false,
}: TotalSettlementModalProps) => {
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
              <Calculator className="w-6 h-6 text-primary" />
            </div>
            전체 정산 내역
          </DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="w-10 h-10 border-3 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
        ) : settlement ? (
          <div className="space-y-6 animate-fade-in">
            {/* Debt Relations */}
            {settlement.debtRelations.length > 0 && (
              <div>
                <h4 className="font-semibold mb-4 flex items-center text-lg">
                  <ArrowRight className="w-5 h-5 mr-2 text-primary" />
                  정산 방법
                </h4>
                {settlement.isExchangeRateApplied && (
                  <div className="mb-3 text-sm text-muted-foreground flex items-center gap-1 p-3 bg-blue-50 dark:bg-blue-950/30 rounded-lg border border-blue-200 dark:border-blue-900">
                    <Info className="w-4 h-4 text-blue-600 dark:text-blue-500" />
                    <span>실시간 환율이 적용된 금액입니다</span>
                  </div>
                )}
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
                                <span className="font-bold text-xl text-blue-600 dark:text-blue-500">
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
                                    <ArrowRight className="w-4 h-4 text-blue-600 dark:text-blue-500" />
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
            )}

            {settlement.debtRelations.length === 0 && (
              <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-green-50 via-green-50/50 to-background dark:from-green-950/30 dark:via-green-950/10 dark:to-background border-2 border-green-200 dark:border-green-900 p-8 text-center shadow-lg animate-fade-in">
                <div className="absolute top-0 right-0 w-32 h-32 bg-green-200/20 rounded-full -mr-16 -mt-16" />
                <div className="absolute bottom-0 left-0 w-24 h-24 bg-green-200/20 rounded-full -ml-12 -mb-12" />
                <div className="relative">
                  <div className="inline-flex p-4 rounded-full bg-green-100 dark:bg-green-900/30 mb-4">
                    <CheckCircle2 className="w-12 h-12 text-green-600 dark:text-green-500" />
                  </div>
                  <p className="text-lg font-semibold text-green-700 dark:text-green-400">모든 멤버의 정산이 완료되었습니다!</p>
                </div>
              </div>
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
