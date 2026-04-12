import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { ExpenseSimpleResponse } from "@/api/expenses";
import { ChevronRight } from "lucide-react";

interface ExpenseListModalProps {
  isOpen: boolean;
  onClose: () => void;
  expenses: ExpenseSimpleResponse[];
  itineraryTitle: string;
  onSelectExpense: (expenseId: number) => void;
}

export const ExpenseListModal = ({
  isOpen,
  onClose,
  expenses,
  itineraryTitle,
  onSelectExpense,
}: ExpenseListModalProps) => {
  // Group expenses by currency
  const totalsByCurrency = expenses.reduce((acc, expense) => {
    const currency = expense.currency || 'KRW';
    acc[currency] = (acc[currency] || 0) + expense.totalAmount;
    return acc;
  }, {} as Record<string, number>);

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg">
            💰 {itineraryTitle} 지출 내역
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-2 max-h-[400px] overflow-y-auto">
          {expenses.map((expense) => (
            <button
              key={expense.expenseId}
              onClick={() => onSelectExpense(expense.expenseId)}
              className="w-full flex items-center justify-between p-4 rounded-lg border bg-card hover:bg-accent transition-colors text-left group"
            >
              <div className="flex flex-col">
                <span className="font-medium">
                  {expense.totalAmount.toLocaleString()} {expense.currency || 'KRW'}
                </span>
                <span className="text-sm text-muted-foreground">
                  결제: {expense.payer}
                </span>
              </div>
              <ChevronRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground transition-colors" />
            </button>
          ))}
        </div>

        <div className="pt-4 border-t space-y-2">
          <div className="text-sm text-muted-foreground">
            총 {expenses.length}건
          </div>
          {Object.entries(totalsByCurrency).map(([currency, total]) => (
            <div key={currency} className="flex justify-between text-sm font-medium">
              <span>합계 ({currency})</span>
              <span>{total.toLocaleString()} {currency}</span>
            </div>
          ))}
        </div>

        <div className="flex justify-end pt-2">
          <Button variant="outline" onClick={onClose}>
            닫기
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
