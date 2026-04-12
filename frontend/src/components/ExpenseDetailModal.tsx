import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Card, CardContent} from "@/components/ui/card";
import {Separator} from "@/components/ui/separator";
import {Edit, FileImage, Receipt, Trash2, User, Users} from "lucide-react";
import {ExpenseDetailResponse} from "@/api/expenses";
import {formatCurrency} from "@/lib/currency";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {useState} from "react";

interface ExpenseDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  expense: ExpenseDetailResponse | null;
  isLoading?: boolean;
  onEdit?: (expense: ExpenseDetailResponse) => void;
  onDelete?: (expenseId: number) => void;
}

export const ExpenseDetailModal = ({
  isOpen,
  onClose,
  expense,
  isLoading = false,
  onEdit,
  onDelete,
}: ExpenseDetailModalProps) => {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  if (!expense && !isLoading) {
    return null;
  }

  const handleEdit = () => {
    if (expense && onEdit) {
      onEdit(expense);
      onClose();
    }
  };

  const handleDelete = () => {
    if (expense && onDelete) {
      onDelete(expense.expenseId);
      setShowDeleteDialog(false);
      onClose();
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center">
            <Receipt className="w-5 h-5 mr-2 text-primary" />
            정산 내역 상세
          </DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <div className="flex items-center justify-center py-8">
            <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
        ) : expense ? (
          <div className="space-y-4">
            {/* Expense Title */}
            <div>
              <h3 className="text-lg font-semibold text-foreground">{expense.expenseTitle}</h3>
            </div>

            {/* Payer Info */}
            <Card className="bg-primary/5">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <User className="w-4 h-4 text-primary" />
                    <span className="text-sm font-medium">결제자</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <span className="font-semibold">{expense.payer.name}</span>
                    {expense.payer.isGuest && (
                      <span className="text-xs text-muted-foreground">(게스트)</span>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Receipt Image (영수증 이미지가 있는 경우) */}
            {expense.receiptImageUrl && (
              <div className="space-y-2">
                <h4 className="font-medium flex items-center">
                  <FileImage className="w-4 h-4 mr-2" />
                  영수증 이미지
                </h4>
                <Card>
                  <CardContent className="p-4">
                    <img 
                      src={expense.receiptImageUrl} 
                      alt="영수증" 
                      className="w-full max-h-[300px] object-contain rounded-md"
                    />
                  </CardContent>
                </Card>
              </div>
            )}

            {/* Items */}
            <div>
              <h4 className="font-medium mb-2 flex items-center">
                <Receipt className="w-4 h-4 mr-2" />
                구매 항목
              </h4>
              <Card>
                <CardContent className="p-4 space-y-2">
                  {expense.items.map((item) => (
                    <div key={item.itemId}>
                      <div className="flex items-center justify-between">
                        <span className="text-sm">{item.itemName}</span>
                        <span className="font-semibold">{formatCurrency(item.price, expense.currency || 'KRW')}</span>
                      </div>
                      
                      {/* Participants */}
                      {item.participants.length > 0 && (
                        <div className="mt-1 pl-4 space-y-1">
                          {item.participants.map((participant) => {
                            const shareAmount = item.price / item.participants.length;
                            return (
                              <div 
                                key={participant.id}
                                className="flex items-center justify-between text-xs text-muted-foreground"
                              >
                                <span className="flex items-center">
                                  <Users className="w-3 h-3 mr-1" />
                                  {participant.name}
                                  {participant.isGuest && ' (게스트)'}
                                </span>
                                <span>{formatCurrency(shareAmount, expense.currency || 'KRW')}</span>
                              </div>
                            );
                          })}
                        </div>
                      )}
                      
                      {item.itemId !== expense.items[expense.items.length - 1].itemId && (
                        <Separator className="mt-2" />
                      )}
                    </div>
                  ))}
                </CardContent>
              </Card>
            </div>

            {/* Total Amount */}
            <Card className="bg-gradient-primary text-white">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <span className="font-semibold">총 금액</span>
                  <span className="text-xl font-bold">{formatCurrency(expense.totalAmount, expense.currency || 'KRW')}</span>
                </div>
              </CardContent>
            </Card>

            {/* Action Buttons */}
            <div className="flex space-x-2">
              {onEdit && (
                <Button
                  onClick={handleEdit}
                  variant="outline"
                  className="flex-1"
                >
                  <Edit className="w-4 h-4 mr-2" />
                  수정
                </Button>
              )}
              {onDelete && (
                <Button
                  onClick={() => setShowDeleteDialog(true)}
                  variant="outline"
                  className="flex-1 text-destructive hover:text-destructive"
                >
                  <Trash2 className="w-4 h-4 mr-2" />
                  삭제
                </Button>
              )}
              <Button onClick={onClose} className="flex-1">
                닫기
              </Button>
            </div>
          </div>
        ) : null}
      </DialogContent>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>정산 내역을 삭제하시겠습니까?</AlertDialogTitle>
            <AlertDialogDescription>
              이 작업은 되돌릴 수 없습니다. 정산 내역이 영구적으로 삭제됩니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Dialog>
  );
};
