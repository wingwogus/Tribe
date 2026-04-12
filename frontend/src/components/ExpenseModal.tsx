import {useEffect, useState} from "react";
import {FileImage, HelpCircle, Plus, Upload, X} from "lucide-react";
import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Checkbox} from "@/components/ui/checkbox";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select";
import {Tooltip, TooltipContent, TooltipTrigger} from "@/components/ui/tooltip";
import {expensesApi, type ItemDetailResponse} from "@/api/expenses";
import {useToast} from "@/hooks/use-toast";
import {getCurrencyInfo, SUPPORTED_CURRENCIES} from "@/lib/currency";
import {MemberInfo, tripApi} from "@/api/trips";
import {useQueryClient} from "@tanstack/react-query";
import { tripQueryKeys } from "@/lib/tripQueryKeys";

interface ExpenseModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (expenseData: any, imageFile?: File) => void;
  selectedItem: any;
  tripId: number;
  currency?: { code: string; name: string; currency: string; rate: number };
  tripMembers?: MemberInfo[];
  editMode?: boolean;
  existingExpense?: {
    expenseId: number;
    expenseTitle: string;
    totalAmount: number;
    items: ItemDetailResponse[];
    payerId: number;
    currency?: string;
  };
  onUpdate?: (expenseId: number, expenseData: any) => void;
  defaultCurrency?: string; // 추가: 기본 통화 코드
}

interface ExpenseItem {
  itemId?: number;
  name: string;
  price: number;
}

interface Participant {
  id: string;
  name: string;
  checked: boolean;
}

export const ExpenseModal = ({ 
  isOpen, 
  onClose, 
  onSave, 
  selectedItem,
  tripId,
  currency = { code: 'JP', name: '일본', currency: 'JPY', rate: 9.5 },
  tripMembers = [],
  editMode = false,
  existingExpense,
  onUpdate,
  defaultCurrency = 'KRW'
}: ExpenseModalProps) => {
  const [inputMethod, setInputMethod] = useState<'none' | 'receipt' | 'manual'>('none');
  const [receiptUploaded, setReceiptUploaded] = useState(false);
  const [expenses, setExpenses] = useState<ExpenseItem[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [uploadedFile, setUploadedFile] = useState<File | undefined>(undefined);
  const [selectedPayerId, setSelectedPayerId] = useState<number | null>(null);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [customParticipant, setCustomParticipant] = useState('');
  const [settlementMode, setSettlementMode] = useState<'items' | 'split'>('items');
  const [itemParticipants, setItemParticipants] = useState<Record<string, string[]>>({});
  const [splitParticipants, setSplitParticipants] = useState<string[]>([]);
  const [createdExpenseId, setCreatedExpenseId] = useState<number | null>(null);
  const [selectedCurrency, setSelectedCurrency] = useState<string>(defaultCurrency); // 추가: 선택된 통화
  const queryClient = useQueryClient();

  // Initialize participants when tripMembers change
  useEffect(() => {
    if (tripMembers.length > 0) {
      setParticipants(tripMembers.filter(m => m.tripMemberId !== null).map(member => ({
        id: String(member.tripMemberId),
        name: member.nickname,
        checked: true
      })));
      if (!selectedPayerId && tripMembers[0]?.tripMemberId) {
        setSelectedPayerId(tripMembers[0].tripMemberId);
      }
    }
  }, [tripMembers]);

  // Load existing expense data in edit mode
  useEffect(() => {
    if (editMode && existingExpense) {
      setExpenses(existingExpense.items.map(item => ({
        itemId: item.itemId,
        name: item.itemName,
        price: item.price
      })));
      setSelectedPayerId(existingExpense.payerId);
      setSelectedCurrency(existingExpense.currency || defaultCurrency);
      setInputMethod('manual');
      setCreatedExpenseId(existingExpense.expenseId);

      // 기존 분배 내역 복원
      const newItemParticipants: Record<string, string[]> = {};
      let baseParticipantSet: string[] | null = null;
      let isPureSplitMode = true;

      existingExpense.items.forEach((item, index) => {
        const key = `item-${index}`;
        const participantIds = (item.participants || []).map((p) => String(p.id));
        newItemParticipants[key] = participantIds;

        const sorted = [...participantIds].sort();
        if (!baseParticipantSet) {
          baseParticipantSet = sorted;
        } else {
          if (
            sorted.length !== baseParticipantSet.length ||
            sorted.some((id, i) => id !== baseParticipantSet![i])
          ) {
            isPureSplitMode = false;
          }
        }

        if (participantIds.length === 0) {
          isPureSplitMode = false;
        }
      });

      setItemParticipants(newItemParticipants);

      if (isPureSplitMode && baseParticipantSet && baseParticipantSet.length > 0) {
        setSettlementMode('split');
        setSplitParticipants(baseParticipantSet);
      } else {
        setSettlementMode('items');
        setSplitParticipants([]);
      }
    } else if (!editMode) {
      // Reset when not in edit mode
      setExpenses([]);
      setInputMethod('none');
      setCreatedExpenseId(null);
    }
  }, [editMode, existingExpense, defaultCurrency]);

  const addCustomParticipant = async () => {
    if (customParticipant.trim()) {
      try {
        const newGuest = await tripApi.addGuest(tripId, {
          name: customParticipant.trim()
        });
        
        setParticipants(prev => [...prev, {
          id: String(newGuest.id),
          name: newGuest.name,
          checked: true
        }]);
        
        setCustomParticipant('');
        
        queryClient.invalidateQueries({ queryKey: tripQueryKeys.trip(tripId) });
        
        toast({
          title: "게스트 추가 완료",
          description: `${newGuest.name}님이 추가되었습니다.`,
        });
      } catch (error) {
        console.error('Failed to add guest:', error);
        toast({
          title: "게스트 추가 실패",
          description: "게스트를 추가하는데 실패했습니다.",
          variant: "destructive",
        });
      }
    }
  };

  const removeParticipant = (id: string) => {
    if (participants.find(p => p.id === id && ['minjun', 'seoa'].includes(p.id))) {
      // 기본 멤버는 체크만 해제
      setParticipants(prev => prev.map(p => 
        p.id === id ? { ...p, checked: false } : p
      ));
    } else {
      // 커스텀 멤버는 삭제
      setParticipants(prev => prev.filter(p => p.id !== id));
    }
  };

  const toggleParticipant = (id: string) => {
    setParticipants(prev => prev.map(p => 
      p.id === id ? { ...p, checked: !p.checked } : p
    ));
  };

  const toggleItemParticipant = (itemIndex: number, participantId: string) => {
    const key = `item-${itemIndex}`;
    setItemParticipants(prev => {
      const current = prev[key] || [];
      const updated = current.includes(participantId)
        ? current.filter(id => id !== participantId)
        : [...current, participantId];
      return { ...prev, [key]: updated };
    });
  };

  const toggleSplitParticipant = (participantId: string) => {
    setSplitParticipants(prev => 
      prev.includes(participantId)
        ? prev.filter(id => id !== participantId)
        : [...prev, participantId]
    );
  };

  const calculateItemCosts = () => {
    const costs: Record<string, number> = {};
    
    if (settlementMode === 'split') {
      const perPerson = splitParticipants.length > 0 ? totalAmount / splitParticipants.length : 0;
      splitParticipants.forEach(id => {
        const participant = participants.find(p => p.id === id);
        if (participant) costs[participant.name] = perPerson;
      });
    } else {
      expenses.forEach((expense, index) => {
        const key = `item-${index}`;
        const participantIds = itemParticipants[key] || [];
        const costPerPerson = participantIds.length > 0 ? expense.price / participantIds.length : 0;
        
        participantIds.forEach(id => {
          const participant = participants.find(p => p.id === id);
          if (participant) {
            costs[participant.name] = (costs[participant.name] || 0) + costPerPerson;
          }
        });
      });
    }
    
    return costs;
  };

  const { toast } = useToast();

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !selectedPayerId) {
      if (!selectedPayerId) {
        toast({
          title: "결제자를 먼저 선택해주세요",
          variant: "destructive",
        });
      }
      return;
    }

    setIsProcessing(true);
    setUploadedFile(file);
    
    try {
      // Create expense with receipt image directly
      const request = {
        tripId: tripId,
        expenseTitle: selectedItem?.name || "영수증 정산",
        itineraryItemId: selectedItem?.itineraryId || 0,
        payerId: selectedPayerId,
        inputMethod: 'SCAN',
        items: [], // Items will be extracted by backend
        currency: selectedCurrency // 추가: 선택된 통화
      };

      const result = await expensesApi.createExpense(request, file);
      
      // Use items from response with itemId
      const convertedExpenses: ExpenseItem[] = result.items.map(item => ({
        itemId: item.itemId,
        name: item.itemName,
        price: item.price
      }));
      
      setExpenses(convertedExpenses);
      setCreatedExpenseId(result.expenseId);
      setReceiptUploaded(true);
      
      // Initialize participants (select all members)
      const defaultParticipants = participants.map(p => p.id);
      setSplitParticipants(defaultParticipants);
      
      toast({
        title: "영수증 분석 완료",
        description: `${result.items.length}개의 항목을 찾았습니다. 멤버 배분을 진행해주세요.`,
      });
    } catch (error) {
      console.error('Receipt processing failed:', error);
      toast({
        title: "영수증 처리 실패",
        description: "영수증을 처리하는데 실패했습니다. 다시 시도해주세요.",
        variant: "destructive",
      });
    } finally {
      setIsProcessing(false);
    }
  };

  const handleManualEntry = () => {
    setExpenses([{ name: '', price: 0 }]);
    setInputMethod('manual');
    const defaultParticipants = participants.map(p => p.id);
    setSplitParticipants(defaultParticipants);
  };

  const totalAmount = expenses.reduce((sum, item) => sum + item.price, 0);
  const selectedParticipants = participants.filter(p => p.checked);
  const perPersonAmount = selectedParticipants.length > 0 ? Math.round(totalAmount / selectedParticipants.length) : 0;

  const handleSave = async () => {
    if (!selectedPayerId) {
      alert("결제자를 선택해주세요.");
      return;
    }
    
    // Build participant assignments using actual itemId (for receipt mode) or index (for manual mode)
    const participantAssignmentsByIndex: Record<number, number[]> = {};
    
    if (settlementMode === 'items') {
      expenses.forEach((expense, index) => {
        const key = `item-${index}`;
        const participantIds = (itemParticipants[key] || [])
          .map(id => Number(id))
          .filter(id => !isNaN(id));
        
        if (participantIds.length > 0) {
          // Use actual itemId for receipt mode (when itemId exists), otherwise use index for manual mode
          const keyId = expense.itemId !== undefined ? expense.itemId : index;
          participantAssignmentsByIndex[keyId] = participantIds;
        }
      });
    } else if (settlementMode === 'split') {
      const participantIds = splitParticipants
        .map(id => Number(id))
        .filter(id => !isNaN(id));
      
      if (participantIds.length > 0) {
        expenses.forEach((expense, index) => {
          // Use actual itemId for receipt mode (when itemId exists), otherwise use index for manual mode
          const keyId = expense.itemId !== undefined ? expense.itemId : index;
          participantAssignmentsByIndex[keyId] = participantIds;
        });
      }
    }
    
    // Edit mode - update existing expense first
    if (editMode && existingExpense && onUpdate) {
      const expenseData = {
        description: selectedItem?.name || existingExpense.expenseTitle,
        totalAmount: totalAmount,
        items: expenses,
        settlementMode,
        inputMethod: 'HANDWRITE',
        payerId: selectedPayerId,
        date: new Date().toISOString().split('T')[0],
        participantAssignmentsByIndex,
        currency: selectedCurrency
      };
      onUpdate(existingExpense.expenseId, expenseData);
    }
    // If expense already created (receipt mode), just assign participants
    else if (createdExpenseId) {
      const expenseData = {
        expenseId: createdExpenseId,
        inputMethod: 'RECEIPT',
        participantAssignmentsByIndex
      };
      onSave(expenseData);
    }
    // Create new expense
    else {
      const expenseData = {
        description: selectedItem?.name || "비용 정산",
        totalAmount: totalAmount,
        items: expenses,
        settlementMode,
        inputMethod: 'HANDWRITE',
        payerId: selectedPayerId,
        date: new Date().toISOString().split('T')[0],
        participantAssignmentsByIndex,
        currency: selectedCurrency
      };
      onSave(expenseData, uploadedFile);
    }
    
    // 모달 초기화
    setInputMethod('none');
    setReceiptUploaded(false);
    setExpenses([]);
    setIsProcessing(false);
    setUploadedFile(undefined);
    setCreatedExpenseId(null);
    if (tripMembers.length > 0 && tripMembers[0]?.tripMemberId) {
      setSelectedPayerId(tripMembers[0].tripMemberId);
      setParticipants(tripMembers.filter(m => m.tripMemberId !== null).map(member => ({
        id: String(member.tripMemberId),
        name: member.nickname,
        checked: true
      })));
    } else {
      setSelectedPayerId(null);
      setParticipants([]);
    }
    setCustomParticipant('');
    setSettlementMode('items');
    setItemParticipants({});
    setSplitParticipants([]);
  };

  const handleClose = () => {
    setInputMethod('none');
    setReceiptUploaded(false);
    setExpenses([]);
    setIsProcessing(false);
    setUploadedFile(undefined);
    setCreatedExpenseId(null);
    if (tripMembers.length > 0 && tripMembers[0]?.tripMemberId) {
      setSelectedPayerId(tripMembers[0].tripMemberId);
      setParticipants(tripMembers.filter(m => m.tripMemberId !== null).map(member => ({
        id: String(member.tripMemberId),
        name: member.nickname,
        checked: true
      })));
    } else {
      setSelectedPayerId(null);
      setParticipants([]);
    }
    setCustomParticipant('');
    setSettlementMode('items');
    setItemParticipants({});
    setSplitParticipants([]);
    onClose();
  };

  return (
      <Dialog open={isOpen} onOpenChange={handleClose}>
        <DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center">
              <FileImage className="w-5 h-5 mr-2 text-primary"/>
              {editMode ? "비용 수정하기" : "비용 추가하기"}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-6">
            {inputMethod === 'none' && !editMode ? (
                <div className="space-y-4">
                  <div>
                    <h3 className="text-lg font-medium text-foreground mb-2">비용 추가 방법을 선택하세요</h3>
                    <p className="text-sm text-muted-foreground">
                      영수증을 업로드하거나 직접 입력할 수 있습니다
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <Button
                        onClick={() => setInputMethod('receipt')}
                        variant="outline"
                        className="h-24 flex-col space-y-2"
                    >
                      <FileImage className="w-8 h-8 text-primary"/>
                      <div className="text-center">
                        <div className="font-medium">📷 영수증으로 추가하기</div>
                        <div className="text-xs text-muted-foreground">AI가 자동으로 분석</div>
                      </div>
                    </Button>

                    <Button
                        onClick={handleManualEntry}
                        variant="outline"
                        className="h-24 flex-col space-y-2"
                    >
                      <Plus className="w-8 h-8 text-primary"/>
                      <div className="text-center">
                        <div className="font-medium">✍️ 직접 항목 추가하기</div>
                        <div className="text-xs text-muted-foreground">수동으로 입력</div>
                      </div>
                    </Button>
                  </div>
                </div>
            ) : inputMethod === 'receipt' && !receiptUploaded && !editMode ? (
                <div className="space-y-4">
                  {/* Currency Selection */}
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <Label>통화 선택</Label>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <button type="button" className="text-muted-foreground hover:text-foreground">
                            <HelpCircle className="w-3.5 h-3.5" />
                          </button>
                        </TooltipTrigger>
                        <TooltipContent side="right" className="max-w-xs">
                          <p>여행 국가의 통화로 지출을 입력하고, 자동으로 원화 환산됩니다</p>
                        </TooltipContent>
                      </Tooltip>
                    </div>
                    <Select value={selectedCurrency} onValueChange={setSelectedCurrency}>
                      <SelectTrigger>
                        <SelectValue placeholder="통화를 선택하세요"/>
                      </SelectTrigger>
                      <SelectContent className="bg-background z-50">
                        {SUPPORTED_CURRENCIES.map(currency => (
                            <SelectItem key={currency.code} value={currency.code}>
                              {currency.flag} {currency.name} ({currency.code})
                            </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <p className="text-xs text-muted-foreground">
                      선택한 통화로 금액을 입력하세요. 자동으로 원화로 환산됩니다.
                    </p>
                  </div>

                  {/* Payer Selection */}
                  <div className="space-y-2">
                    <label className="text-sm font-medium">결제자</label>
                    <select
                        value={selectedPayerId || ''}
                        onChange={(e) => setSelectedPayerId(Number(e.target.value))}
                        className="w-full p-2 border rounded-md"
                    >
                      <option value="">결제자를 선택하세요</option>
                      {tripMembers.filter(m => m.tripMemberId !== null).map((member) => (
                          <option key={member.tripMemberId} value={member.tripMemberId!}>
                            {member.nickname}
                          </option>
                      ))}
                    </select>
                  </div>

                  <div className="flex items-center space-x-2">
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setInputMethod('none')}
                    >
                      ← 뒤로
                    </Button>
                    <div>
                      <Label htmlFor="receipt-upload" className="text-base font-medium">
                        영수증 업로드
                      </Label>
                      <p className="text-sm text-muted-foreground mt-1">
                        영수증을 업로드하면 AI가 자동으로 항목과 가격을 분석해드립니다
                      </p>
                    </div>
                  </div>

                  <div className="border-2 border-dashed border-muted rounded-lg p-8 text-center">
                    {isProcessing ? (
                        <div className="space-y-4">
                          <div
                              className="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center mx-auto">
                            <div
                                className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
                          </div>
                          <div>
                            <h3 className="font-medium text-foreground">AI가 영수증을 분석 중입니다...</h3>
                            <p className="text-sm text-muted-foreground mt-1">
                              잠시만 기다려 주세요
                            </p>
                          </div>
                        </div>
                    ) : (
                        <div className="space-y-4">
                          <div
                              className="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center mx-auto">
                            <Upload className="w-6 h-6 text-primary"/>
                          </div>
                          <div>
                            <h3 className="font-medium text-foreground">영수증을 업로드하세요</h3>
                            <p className="text-sm text-muted-foreground mt-1">
                              JPG, PNG 파일을 선택해주세요
                            </p>
                          </div>
                          <input
                              id="receipt-upload"
                              type="file"
                              accept="image/*"
                              onChange={(e) => {
                                handleFileUpload(e);
                                setInputMethod('receipt');
                              }}
                              className="hidden"
                          />
                          <Button
                              onClick={() => document.getElementById('receipt-upload')?.click()}
                              className="bg-gradient-primary hover:shadow-primary"
                          >
                            파일 선택
                          </Button>
                        </div>
                    )}
                  </div>
                </div>
            ) : null}

            {/* Expense Details - shown for both receipt and manual input */}
            {(inputMethod === 'manual' || (inputMethod === 'receipt' && receiptUploaded) || editMode) && (
                <div className="space-y-4">
                  {/* Currency Selection */}
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <Label>통화 선택</Label>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <button type="button" className="text-muted-foreground hover:text-foreground">
                            <HelpCircle className="w-3.5 h-3.5" />
                          </button>
                        </TooltipTrigger>
                        <TooltipContent side="right" className="max-w-xs">
                          <p>여행 국가의 통화로 지출을 입력하고, 자동으로 원화 환산됩니다</p>
                        </TooltipContent>
                      </Tooltip>
                    </div>
                    <Select value={selectedCurrency} onValueChange={setSelectedCurrency}>
                      <SelectTrigger>
                        <SelectValue placeholder="통화를 선택하세요"/>
                      </SelectTrigger>
                      <SelectContent className="bg-background z-50">
                        {SUPPORTED_CURRENCIES.map(currency => (
                            <SelectItem key={currency.code} value={currency.code}>
                              {currency.flag} {currency.name} ({currency.code})
                            </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <p className="text-xs text-muted-foreground">
                      선택한 통화로 금액을 입력하세요. 자동으로 원화로 환산됩니다.
                    </p>
                  </div>

                  {/* Payer Selection */}
                  <div className="space-y-2">
                    <label className="text-sm font-medium">결제자</label>
                    <select
                        value={selectedPayerId || ''}
                        onChange={(e) => setSelectedPayerId(Number(e.target.value))}
                        className="w-full p-2 border rounded-md"
                    >
                      <option value="">결제자를 선택하세요</option>
                      {tripMembers.filter(m => m.tripMemberId !== null).map((member) => (
                          <option key={member.tripMemberId} value={member.tripMemberId!}>
                            {member.nickname}
                          </option>
                      ))}
                    </select>
                  </div>

                  <div className="bg-gradient-subtle rounded-lg p-4 space-y-3">

                    <div className="flex items-center">
                      <h4 className="font-medium text-foreground">구매 항목</h4>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <button className="text-muted-foreground hover:text-foreground h-7 w-7 flex items-center justify-center">
                            <HelpCircle className="w-3 h-3" />
                          </button>
                        </TooltipTrigger>
                        <TooltipContent side="right" className="max-w-s">
                          <p>항목별로 금액을 입력하고 참여자를 지정하여 정확한 정산이 가능합니다</p>
                        </TooltipContent>
                      </Tooltip>
                    </div>
                    <div className="space-y-2">
                      {expenses.map((item, idx) => (
                          <div key={idx} className="flex justify-between items-center">
                            <Input
                                value={item.name}
                                placeholder={"항목 입력"}
                                onChange={(e) => {
                                  const newExpenses = [...expenses];
                                  newExpenses[idx].name = e.target.value;
                                  setExpenses(newExpenses);
                                }}
                                className="flex-1 mr-3"
                            />
                            <div className="flex items-center space-x-2 min-w-[120px]">
                              <span className="text-sm font-medium">{getCurrencyInfo(selectedCurrency).symbol}</span>
                              <Input
                                  value={item.price}
                                  type="number"
                                  placeholder={"0"}
                                  onChange={(e) => {
                                    const newExpenses = [...expenses];
                                    newExpenses[idx].price = Number(e.target.value);
                                    setExpenses(newExpenses);
                                  }}
                                  className="w-20"
                              />
                              <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => {
                                    const newExpenses = expenses.filter((_, i) => i !== idx);
                                    setExpenses(newExpenses);
                                  }}
                              >
                                <X className="w-4 h-4"/>
                              </Button>
                            </div>
                          </div>
                      ))}
                      <Button
                          onClick={() => {
                            const newExpense: ExpenseItem = { name: '', price: 0 };
                            setExpenses([...expenses, newExpense]);
                          }}
                          variant="outline"
                          className="w-full"
                      >
                        항목 추가
                      </Button>
                    </div>


                    <div className="pt-3 border-t">
                      <div className="flex justify-between items-center">
                        <span className="font-semibold text-foreground">총액</span>
                        <div className="text-right">
                          <div className="font-semibold text-lg text-primary">
                            {getCurrencyInfo(selectedCurrency).symbol}{totalAmount.toLocaleString()} {selectedCurrency}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 정산 방식 선택 */}
                  <div className="space-y-4">
                    <div className="flex space-x-2">
                      <Button
                          variant={settlementMode === 'items' ? 'default' : 'outline'}
                          onClick={() => setSettlementMode('items')}
                          className="flex-1"
                      >
                        품목별 정산
                      </Button>
                      <Button
                          variant={settlementMode === 'split' ? 'default' : 'outline'}
                          onClick={() => setSettlementMode('split')}
                          className="flex-1"
                      >
                        💸 간편 1/N
                      </Button>
                    </div>

                    {settlementMode === 'items' ? (
                        <div className="space-y-4">
                          <Label className="text-base font-medium">참여자별 정산 내역</Label>
                          {participants.map((participant) => (
                              <div key={participant.id} className="border rounded-lg p-3 space-y-2">
                                <div className="flex items-center justify-between">
                                  <div className="flex items-center space-x-2">
                                    <span className="font-medium">{participant.name}</span>
                                    {!['minjun', 'seoa'].includes(participant.id) && (
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => removeParticipant(participant.id)}
                                        >
                                          <X className="w-4 h-4"/>
                                        </Button>
                                    )}
                                  </div>
                                  <div className="text-sm text-muted-foreground">
                                    {Math.round(calculateItemCosts()[participant.name] || 0).toLocaleString()} {selectedCurrency}
                                  </div>
                                </div>
                                <div className="space-y-2">
                                  <div className="text-sm text-muted-foreground">품목 선택:</div>
                                  <div className="flex flex-wrap gap-2">
                                    {expenses.map((expense, index) => {
                                      const isSelected = (itemParticipants[`item-${index}`] || []).includes(participant.id);
                                      const selectedCount = (itemParticipants[`item-${index}`] || []).length;
                                      const perPersonCost = selectedCount > 0 ? expense.price / selectedCount : expense.price;

                                      return (
                                          <Button
                                              key={index}
                                              variant={isSelected ? 'default' : 'outline'}
                                              size="sm"
                                              onClick={() => toggleItemParticipant(index, participant.id)}
                                              className="text-xs"
                                          >
                                             {expense.name}
                                             {isSelected && selectedCount > 1 && (
                                                 <span className="ml-1 text-xs opacity-70">
                                       ({getCurrencyInfo(selectedCurrency).symbol}{Math.round(perPersonCost).toLocaleString()})
                                     </span>
                                             )}
                                          </Button>
                                      );
                                    })}
                                  </div>
                                </div>
                              </div>
                          ))}

                          {/* 참여자 추가 */}
                          <div className="flex space-x-2">
                            <Input
                                placeholder="참여자 이름 (예: Tom)"
                                value={customParticipant}
                                onChange={(e) => setCustomParticipant(e.target.value)}
                                onKeyPress={(e) => e.key === 'Enter' && addCustomParticipant()}
                            />
                            <Button
                                variant="outline"
                                onClick={addCustomParticipant}
                                className="whitespace-nowrap"
                            >
                              <Plus className="w-4 h-4 mr-1"/>
                              참여자 추가
                            </Button>
                          </div>
                        </div>
                    ) : (
                        <div className="space-y-4">
                          <Label className="text-base font-medium">참여자 선택 (간편 1/N 정산)</Label>
                          <div className="space-y-2">
                            {participants.map((participant) => (
                                <div key={participant.id}
                                     className="flex items-center justify-between p-2 bg-muted rounded">
                                  <div className="flex items-center space-x-2">
                                    <Checkbox
                                        checked={splitParticipants.includes(participant.id)}
                                        onCheckedChange={() => toggleSplitParticipant(participant.id)}
                                    />
                                    <span className="text-sm">{participant.name}</span>
                                  </div>
                                  {!['minjun', 'seoa'].includes(participant.id) && (
                                      <Button
                                          variant="ghost"
                                          size="sm"
                                          onClick={() => removeParticipant(participant.id)}
                                      >
                                        <X className="w-4 h-4"/>
                                      </Button>
                                  )}
                                </div>
                            ))}
                          </div>

                          {/* 참여자 추가 */}
                          <div className="flex space-x-2">
                            <Input
                                placeholder="참여자 이름 (예: Tom)"
                                value={customParticipant}
                                onChange={(e) => setCustomParticipant(e.target.value)}
                                onKeyPress={(e) => e.key === 'Enter' && addCustomParticipant()}
                            />
                            <Button
                                variant="outline"
                                onClick={addCustomParticipant}
                                className="whitespace-nowrap"
                            >
                              <Plus className="w-4 h-4 mr-1"/>
                              참여자 추가
                            </Button>
                          </div>

                          {splitParticipants.length > 0 && (
                              <div className="text-sm bg-accent/10 p-3 rounded">
                                <div className="font-medium text-primary">
                                  총 {totalAmount.toLocaleString()} {selectedCurrency} ÷ {splitParticipants.length}명 = 1인당 {Math.round(totalAmount / splitParticipants.length).toLocaleString()} {selectedCurrency}
                                </div>
                                {/*<div className="text-muted-foreground mt-1">*/}
                                {/*  (약 {Math.round(totalAmount / splitParticipants.length * 9.5).toLocaleString()} {selectedCurrency})*/}
                                {/*</div>*/}
                              </div>
                          )}
                        </div>
                    )}
                  </div>
                </div>
            )}

            <div className="flex space-x-3 pt-4 border-t">
              <Button
                  variant="outline"
                  onClick={handleClose}
                  className="flex-1"
              >
                취소
              </Button>

              <Button
                  onClick={handleSave}
                  className="flex-1 bg-gradient-sunset hover:shadow-accent"
                  disabled={
                    settlementMode === 'split'
                        ? splitParticipants.length === 0
                        : Object.keys(calculateItemCosts()).length === 0
                  }
              >
                {editMode ? "수정하기" : createdExpenseId ? "멤버 배분 완료" : "저장하기"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
  );
};
