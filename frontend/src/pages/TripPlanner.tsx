import {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {useNavigate, useParams} from "react-router-dom";
import {
  ArrowLeft,
  ArrowRightLeft,
  Calculator,
  Calendar,
  Check,
  ChevronDown,
  ChevronRight,
  ChevronUp,
  Clock,
  Copy,
  DollarSign,
  Edit3,
  ExternalLink,
  GripVertical,
  HelpCircle,
  Loader2,
  MoreVertical,
  MoveRight,
  Plus,
  Sparkles,
  Star,
  Trash2,
  Users,
  MessageCircle,
} from "lucide-react";
import {getGoogleMapsSearchUrl, openGoogleMaps} from "@/lib/googleMaps";
import {
  closestCenter,
  DndContext,
  DragEndEvent,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import {CSS} from '@dnd-kit/utilities';
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Button} from "@/components/ui/button";
import {Tabs, TabsContent, TabsList, TabsTrigger} from "@/components/ui/tabs";
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Drawer, DrawerContent} from "@/components/ui/drawer";
import {Input} from "@/components/ui/input";
import {Avatar, AvatarFallback, AvatarImage} from "@/components/ui/avatar";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "@/components/ui/tooltip";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import {PlaceSearchModal} from "@/components/PlaceSearchModal";
import {TripReviewModal} from "@/components/TripReviewModal";
import {ItineraryCreateModal} from "@/components/ItineraryCreateModal";
import {ExpenseModal} from "@/components/ExpenseModal";
import {ExpenseDetailModal} from "@/components/ExpenseDetailModal";
import {ExpenseListModal} from "@/components/ExpenseListModal";
import {DailySettlementModal} from "@/components/DailySettlementModal";
import {TotalSettlementModal} from "@/components/TotalSettlementModal";
import {TripMembersModal} from "@/components/TripMembersModal";
import {ItineraryMap, ItineraryMapHandle} from "@/components/ItineraryMap";
import {TripChatModal} from "@/components/TripChatModal";
import {tripApi} from "@/api/trips";
import {wishlistApi, WishlistItem} from "@/api/wishlist";
import {categoryApi} from "@/api/categories";
import {itineraryApi, ItineraryResponse, RouteDetails} from "@/api/itinerary";
import {
  CreateExpenseRequest,
  expensesApi,
  ExpenseSimpleResponse,
  ItemDetailResponse,
  ParticipantAssignRequest
} from "@/api/expenses";
import {Badge} from "@/components/ui/badge";
import {settlementApi} from "@/api/settlement";
import {PlaceSearchResult} from "@/api/places";
import {getMemberInfo} from "@/api/auth";
import {useToast} from "@/hooks/use-toast";
import {useIsMobile} from "@/hooks/use-mobile";
import {useTripWebSocket} from "@/hooks/useTripWebSocket";
import { tripQueryKeys } from "@/lib/tripQueryKeys";
import {getCountryTimezone} from "@/lib/utils";
import {RouteInfoCard} from "@/components/RouteInfoCard";
import {getDefaultCurrencyByCountry} from "@/lib/currency";
import {addDays, format} from "date-fns";

// Category color mapping
const getCategoryColor = (categoryName: string): { bg: string; text: string; marker: string } => {
  const lowerName = categoryName.toLowerCase();
  if (lowerName.includes('식사') || lowerName.includes('음식')) {
    return { bg: 'bg-rose-50 dark:bg-rose-950/50', text: 'text-rose-700 dark:text-rose-300', marker: '#FB7185' };
  }
  if (lowerName.includes('숙소') || lowerName.includes('호텔')) {
    return { bg: 'bg-amber-50 dark:bg-amber-950/50', text: 'text-amber-700 dark:text-amber-300', marker: '#FBBF24' };
  }
  if (lowerName.includes('관광') || lowerName.includes('명소')) {
    return { bg: 'bg-emerald-50 dark:bg-emerald-950/50', text: 'text-emerald-700 dark:text-emerald-300', marker: '#34D399' };
  }
  if (lowerName.includes('쇼핑')) {
    return { bg: 'bg-sky-50 dark:bg-sky-950/50', text: 'text-sky-700 dark:text-sky-300', marker: '#38BDF8' };
  }
  if (lowerName.includes('액티비티') || lowerName.includes('활동')) {
    return { bg: 'bg-purple-50 dark:bg-purple-950/50', text: 'text-purple-700 dark:text-purple-300', marker: '#A78BFA' };
  }
  // Default color
  return { bg: 'bg-slate-50 dark:bg-slate-950/50', text: 'text-slate-700 dark:text-slate-300', marker: '#94A3B8' };
};

// Sortable Category Component
interface SortableCategoryProps {
  category: any;
  categoryIndex: number;
  categoriesArray: any[];
  day: number;
  categories: any[];
  dropTargetCategoryId: number | null;
  draggedItinerary: any;
  dragOverItemId: number | null;
  mapRef: React.RefObject<ItineraryMapHandle>;
  findRoutesForItems: (item1: ItineraryResponse, item2: ItineraryResponse) => RouteDetails[];
  handleCategoryDragOver: (e: React.DragEvent, categoryId: number) => void;
  handleDragOver: (e: React.DragEvent, categoryId: number) => void;
  handleDragLeave: () => void;
  handleDrop: (e: React.DragEvent, categoryId: number) => void;
  handleUpdateCategoryName: (categoryId: number, currentName: string) => void;
  handleDeleteCategory: (categoryId: number) => void;
  handleItineraryDragStart: (item: ItineraryResponse, categoryId: number) => void;
  handleItineraryDragOver: (e: React.DragEvent, targetItem: ItineraryResponse, categoryId: number) => void;
  handleDragEnd: () => void;
  setExpenseModal: (modal: any) => void;
  setEditingItinerary: (editing: any) => void;
  deleteItineraryMutation: any;
  setExpenseDetailModal: (modal: any) => void;
  setExpenseListModal: (modal: any) => void;
  handleMoveToCategory: (itemId: number, fromCategoryId: number, toCategoryId: number) => Promise<void>;
  setIsItineraryDrawerOpen?: (open: boolean) => void;
  expensesByItinerary: Map<number, ExpenseSimpleResponse[]>;
  isMobile: boolean;
  onMoveItemUp: (categoryId: number, itemId: number) => void;
  onMoveItemDown: (categoryId: number, itemId: number) => void;
}

const SortableCategory = ({
  category,
  categoryIndex,
  categoriesArray,
  day,
  categories,
  dropTargetCategoryId,
  draggedItinerary,
  dragOverItemId,
  mapRef,
  findRoutesForItems,
  handleCategoryDragOver,
  handleDragOver,
  handleDragLeave,
  handleDrop,
  handleUpdateCategoryName,
  handleDeleteCategory,
  handleItineraryDragStart,
  handleItineraryDragOver,
  handleDragEnd,
  setExpenseModal,
  setEditingItinerary,
  deleteItineraryMutation,
  setExpenseDetailModal,
  setExpenseListModal,
  handleMoveToCategory,
  setIsItineraryDrawerOpen,
  expensesByItinerary,
  isMobile,
  onMoveItemUp,
  onMoveItemDown,
}: SortableCategoryProps) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: category.categoryId });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const categoryColors = getCategoryColor(category.name);
  
  // Find previous category's last item and current category's first item for inter-category routes
  const prevCategory = categoryIndex > 0 ? categoriesArray[categoryIndex - 1] : null;
  const prevCategoryLastItem = prevCategory 
    ? [...prevCategory.itineraryItems].sort((a, b) => a.order - b.order).pop()
    : null;
  const currentCategoryFirstItem = [...category.itineraryItems]
    .sort((a, b) => a.order - b.order)[0];

  return (
    <div ref={setNodeRef} style={style} key={category.categoryId}>
      {/* Inter-category route */}
      {prevCategoryLastItem && currentCategoryFirstItem && 
       prevCategoryLastItem.location && currentCategoryFirstItem.location && (() => {
        const routes = findRoutesForItems(prevCategoryLastItem, currentCategoryFirstItem);
        if (routes.length > 0) {
          return (
            <div className="mb-3">
              <RouteInfoCard
                routes={routes}
                originName={prevCategoryLastItem.name}
                destinationName={currentCategoryFirstItem.name}
              />
            </div>
          );
        }
        return null;
      })()}
      
      <div 
        className={`border rounded-lg p-3 md:p-4 transition-all duration-200 ${
          dropTargetCategoryId === category.categoryId
            ? 'bg-primary/10 border-primary border-2 scale-[1.02]'
            : categoryColors.bg
        }`}
        onDragOver={(e) => draggedItinerary ? handleCategoryDragOver(e, category.categoryId) : handleDragOver(e, category.categoryId)}
        onDragLeave={handleDragLeave}
        onDrop={(e) => handleDrop(e, category.categoryId)}
      >
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <button
              className="cursor-grab active:cursor-grabbing touch-none p-1 hover:bg-primary/10 rounded"
              {...attributes}
              {...listeners}
            >
              <GripVertical className="w-4 h-4 text-muted-foreground" />
            </button>
            <h3 className={`font-medium text-sm md:text-base ${categoryColors.text}`}>{category.name}</h3>
          </div>
          <div className="flex space-x-1 md:space-x-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleUpdateCategoryName(category.categoryId, category.name)}
              className="h-8 w-8 p-0"
            >
              <Edit3 className="w-3 h-3 md:w-4 md:h-4" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleDeleteCategory(category.categoryId)}
              className="text-destructive hover:text-destructive-foreground hover:bg-destructive h-8 w-8 p-0"
            >
              <Trash2 className="w-3 h-3 md:w-4 md:h-4" />
            </Button>
          </div>
        </div>
       
        {/* Itinerary items */}
        <div className="space-y-2">
          {category.itineraryItems.length === 0 ? (
            <div className={`text-sm text-center py-8 rounded-lg border-2 border-dashed transition-all ${
              dropTargetCategoryId === category.categoryId
                ? 'border-primary bg-primary/5 text-primary'
                : 'border-muted-foreground/30 text-muted-foreground'
            }`}>
              <div className="flex flex-col items-center gap-2">
                <Plus className="w-5 h-5" />
                <p>위시리스트에서 장소를 드래그하여 추가하세요</p>
              </div>
            </div>
          ) : (
            <>
               {category.itineraryItems
                 .sort((a, b) => a.order - b.order)
                 .map((item, itemIndex, sortedItems) => {
                   // Calculate pin number for items with location
                   let pinNumber: number | null = null;
                   if (item.location) {
                     // Count all items with location up to this point across all categories
                     let count = 0;
                     for (const cat of categories.filter(c => c.day === day).sort((a, b) => a.order - b.order)) {
                       for (const itItem of cat.itineraryItems.sort((a, b) => a.order - b.order)) {
                         if (itItem.location) {
                           count++;
                           if (itItem.itineraryId === item.itineraryId) {
                             pinNumber = count;
                             break;
                           }
                         }
                       }
                       if (pinNumber) break;
                     }
                   }
                   
                   return (
                   <div key={item.itineraryId}>
                     <div 
                       draggable
                       onDragStart={() => handleItineraryDragStart(item, category.categoryId)}
                       onDragOver={(e) => handleItineraryDragOver(e, item, category.categoryId)}
                       onDragEnd={handleDragEnd}
                       className={`bg-white p-3 rounded border cursor-move transition-all duration-200 ${
                         draggedItinerary?.item.itineraryId === item.itineraryId
                           ? 'opacity-50 scale-95'
                           : dragOverItemId === item.itineraryId
                           ? 'border-primary border-2 shadow-lg'
                           : 'hover:shadow-md'
                       }`}
                     >
                   <div className="flex-col items-start justify-between">
                      <div className="flex-1 ">
                         <div className="flex items-center gap-2">
                           {pinNumber && (
                              <div 
                                className="flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-xs font-semibold text-white cursor-pointer hover:scale-110 transition-transform"
                                style={{ backgroundColor: getCategoryColor(category.name).marker }}
                                onClick={() => {
                                  if (item.location && mapRef.current) {
                                    mapRef.current.flyToMarker(item.itineraryId);
                                    // Delay closing drawer to allow map animation to start
                                    setTimeout(() => {
                                      if (setIsItineraryDrawerOpen) {
                                        setIsItineraryDrawerOpen(false);
                                      }
                                    }, 150);
                                  }
                                }}
                              >
                                {pinNumber}
                              </div>
                            )}
                            <h4
                              className={`font-medium text-sm ${item.location ? 'cursor-pointer hover:text-primary transition-colors' : ''}`}
                              onClick={() => {
                                if (item.location && mapRef.current) {
                                  mapRef.current.flyToMarker(item.itineraryId);
                                  // Delay closing drawer to allow map animation to start
                                  setTimeout(() => {
                                    if (setIsItineraryDrawerOpen) {
                                      setIsItineraryDrawerOpen(false);
                                    }
                                  }, 150);
                                }
                              }}
                            >
                             {item.name || "장소명 없음"}
                           </h4>
                         </div>
                      {item.time && (
                          <p className="text-xs text-muted-foreground mt-1">{item.time.split('T')[1]?.slice(0, 5)}</p>
                      )}
                      {item.memo && (
                        <p className="text-xs text-muted-foreground mt-1">{item.memo}</p>
                      )}
                      </div>
                     <div className="flex space-x-1 justify-end">
                       {(() => {
                         const itemExpenses = expensesByItinerary.get(item.itineraryId) || [];
                         const hasExpense = itemExpenses.length > 0;
                         
                         return (
                           <div className="hidden md:flex items-center gap-0.5">
                             <Button
                                 variant="ghost"
                                 size="sm"
                                 onClick={() => setExpenseModal({
                                   isOpen: true,
                                   itineraryItem: item
                                 })}
                                 className="h-7 w-7 p-0"
                                 title="정산 내역 추가"
                             >
                               <DollarSign className="w-3 h-3" />
                             </Button>
                              {hasExpense && (
                                <Badge 
                                  variant="secondary" 
                                  className="h-5 px-1.5 text-[10px] bg-green-100 text-green-700 dark:bg-green-950/50 dark:text-green-300 cursor-pointer"
                                  onClick={() => {
                                    if (itemExpenses.length === 1) {
                                      setExpenseDetailModal({
                                        isOpen: true,
                                        expenseId: itemExpenses[0].expenseId
                                      });
                                    } else if (itemExpenses.length > 1) {
                                      setExpenseListModal({
                                        isOpen: true,
                                        expenses: itemExpenses,
                                        itineraryTitle: item.name || "장소명 없음",
                                      });
                                    }
                                  }}
                                >
                                  {itemExpenses.length}건
                                </Badge>
                              )}
                           </div>
                         );
                       })()}
                      {(() => {
                         const itemExpenses = expensesByItinerary.get(item.itineraryId) || [];
                         const hasExpense = itemExpenses.length > 0;
                         
                         return (
                           <>
                             <Button
                                 variant="ghost"
                                 size="sm"
                                 onClick={() => setExpenseModal({
                                   isOpen: true,
                                   itineraryItem: item
                                 })}
                                 className="md:hidden h-7 w-7 p-0"
                                 title="정산 내역 추가"
                             >
                               <DollarSign className="w-3 h-3" />
                             </Button>
                              {hasExpense && (
                                <Badge 
                                  variant="secondary" 
                                  className="md:hidden h-5 px-1.5 text-[10px] bg-green-100 text-green-700 dark:bg-green-950/50 dark:text-green-300 cursor-pointer"
                                  onClick={() => {
                                    if (itemExpenses.length === 1) {
                                      setExpenseDetailModal({
                                        isOpen: true,
                                        expenseId: itemExpenses[0].expenseId
                                      });
                                    } else if (itemExpenses.length > 1) {
                                      setExpenseListModal({
                                        isOpen: true,
                                        expenses: itemExpenses,
                                        itineraryTitle: item.name || "장소명 없음",
                                      });
                                    }
                                  }}
                                >
                                  {itemExpenses.length}건
                                </Badge>
                              )}
                           </>
                         );
                       })()}
                      <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setEditingItinerary({
                            itemId: item.itineraryId,
                            categoryId: category.categoryId,
                            memo: item.memo || "",
                            time: item.time || ""
                          })}
                          className="h-7 w-7 p-0"
                      >
                        <Edit3 className="w-3 h-3" />
                      </Button>
                      <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => deleteItineraryMutation.mutate({
                            categoryId: category.categoryId,
                            itemId: item.itineraryId
                          })}
                          className="h-7 w-7 p-0 text-destructive hover:text-destructive-foreground hover:bg-destructive"
                          title="삭제"
                      >
                        <Trash2 className="w-3 h-3" />
                      </Button>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                              variant="ghost"
                              size="sm"
                              className="h-7 w-7 p-0"
                          >
                            <MoreVertical className="w-3 h-3" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="bg-white">
                          <DropdownMenuSub>
                            <DropdownMenuSubTrigger>
                              <ArrowRightLeft className="w-4 h-4 mr-2" />
                              다른 카테고리로 이동
                            </DropdownMenuSubTrigger>
                            <DropdownMenuSubContent className="bg-white">
                              {categories
                                .filter(cat => cat.day === day && cat.categoryId !== category.categoryId)
                                .map(targetCategory => (
                                  <DropdownMenuItem
                                    key={targetCategory.categoryId}
                                    onClick={() => handleMoveToCategory(item.itineraryId, category.categoryId, targetCategory.categoryId)}
                                  >
                                    {targetCategory.name}
                                  </DropdownMenuItem>
                                ))}
                              {categories.filter(cat => cat.day === day && cat.categoryId !== category.categoryId).length === 0 && (
                                <DropdownMenuItem disabled>
                                  다른 카테고리가 없습니다
                                </DropdownMenuItem>
                              )}
                            </DropdownMenuSubContent>
                          </DropdownMenuSub>
                        </DropdownMenuContent>
                      </DropdownMenu>
                       {isMobile && (
                           <div className="flex flex-col items-center mr-1 md:hidden">
                             <Button
                                 variant="ghost"
                                 size="icon"
                                 className="h-6 w-6 p-0"
                                 disabled={itemIndex === 0}
                                 onClick={() => onMoveItemUp(category.categoryId, item.itineraryId)}
                             >
                               <ChevronUp className="w-3 h-3" />
                             </Button>
                             <Button
                                 variant="ghost"
                                 size="icon"
                                 className="h-6 w-6 p-0"
                                 disabled={itemIndex === sortedItems.length - 1}
                                 onClick={() => onMoveItemDown(category.categoryId, item.itineraryId)}
                             >
                               <ChevronDown className="w-3 h-3" />
                             </Button>
                           </div>
                       )}
                    </div>

                   </div>
                   </div>
                   
                   {/* Route between items within category */}
                   {itemIndex < sortedItems.length - 1 && (() => {
                     const nextItem = sortedItems[itemIndex + 1];
                     if (item.location && nextItem.location) {
                       const routes = findRoutesForItems(item, nextItem);
                       if (routes.length > 0) {
                         return (
                           <div className="my-2">
                               <RouteInfoCard
                                 routes={routes}
                                 originName={item.name}
                                 destinationName={nextItem.name}
                               />
                           </div>
                         );
                       }
                     }
                     return null;
                   })()}
                   </div>
                   );
                 })}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

const TripPlanner = () => {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const isMobile = useIsMobile();
  
  const [selectedDay, setSelectedDay] = useState(1);
  const [currentTime, setCurrentTime] = useState<string>('');
  const mapRef = useRef<ItineraryMapHandle>(null);
  const [showPlaceSearchModal, setShowPlaceSearchModal] = useState(false);
  const [showInviteDialog, setShowInviteDialog] = useState(false);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [inviteLink, setInviteLink] = useState("");
  const [isCopied, setIsCopied] = useState(false);
  const [editingItinerary, setEditingItinerary] = useState<{
    itemId: number;
    categoryId: number;
    memo: string;
    time: string;
  } | null>(null);
  const [draggedItem, setDraggedItem] = useState<WishlistItem | null>(null);
  const [dropTargetCategoryId, setDropTargetCategoryId] = useState<number | null>(null);
  const [dragOverItemId, setDragOverItemId] = useState<number | null>(null);
  const [draggedItinerary, setDraggedItinerary] = useState<{
    item: ItineraryResponse;
    categoryId: number;
  } | null>(null);
  const [createItineraryModal, setCreateItineraryModal] = useState<{
    isOpen: boolean;
    categoryId: number;
    categoryName: string;
  }>({
    isOpen: false,
    categoryId: 0,
    categoryName: "",
  });
  const [expenseModal, setExpenseModal] = useState<{
    isOpen: boolean;
    itineraryItem: ItineraryResponse | null;
    editingExpense?: {
      expenseId: number;
      expenseTitle: string;
      totalAmount: number;
      items: ItemDetailResponse[];
      payerId: number;
      currency?: string;
    };
  }>({
    isOpen: false,
    itineraryItem: null,
  });
  const [expenseDetailModal, setExpenseDetailModal] = useState<{
    isOpen: boolean;
    expenseId: number | null;
  }>({
    isOpen: false,
    expenseId: null,
  });
  const [expenseListModal, setExpenseListModal] = useState<{
    isOpen: boolean;
    expenses: ExpenseSimpleResponse[];
    itineraryTitle: string;
  }>({
    isOpen: false,
    expenses: [],
    itineraryTitle: '',
  });
  const [dailySettlementModal, setDailySettlementModal] = useState<{
    isOpen: boolean;
    date: string | null;
  }>({
    isOpen: false,
    date: null,
  });
  const [totalSettlementModal, setTotalSettlementModal] = useState(false);
  const [showMembersModal, setShowMembersModal] = useState(false);
  const [moveItemModal, setMoveItemModal] = useState<{
    isOpen: boolean;
    item: ItineraryResponse | null;
    sourceCategoryId: number | null;
  }>({
    isOpen: false,
    item: null,
    sourceCategoryId: null,
  });

  const [isWishlistOpen, setIsWishlistOpen] = useState(false);
  const [wishlistSearchInput, setWishlistSearchInput] = useState("");
  const [wishlistSearchQuery, setWishlistSearchQuery] = useState("");
  const [isItineraryDrawerOpen, setIsItineraryDrawerOpen] = useState(false);
  const [isWishlistDrawerOpen, setIsWishlistDrawerOpen] = useState(false);
  const [isMembersHovered, setIsMembersHovered] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  
  // Debounce wishlist search
  useEffect(() => {
    const timer = setTimeout(() => {
      setWishlistSearchQuery(wishlistSearchInput);
    }, 500);

    return () => clearTimeout(timer);
  }, [wishlistSearchInput]);

  // Member info query
  const { data: memberInfo } = useQuery({
    queryKey: ['memberInfo'],
    queryFn: getMemberInfo,
    staleTime: Infinity,
  });

  // Trip detail query
  const { data: tripDetail, isLoading: isLoadingTrip } = useQuery({
    queryKey: tripQueryKeys.trip(tripId ?? ""),
    queryFn: () => tripApi.getTripById(Number(tripId)),
    enabled: !!tripId,
  });

  const myTripMemberId = useMemo(() => {
    if (!tripDetail || !memberInfo) return null;
    const me = tripDetail.members.find((m) => m.memberId === memberInfo.memberId);
    return me?.tripMemberId ?? null;
  }, [tripDetail, memberInfo]);

  // WebSocket connection for real-time updates
  useTripWebSocket({
    tripId: tripId ?? "",
    currentUserId: memberInfo?.memberId,
    enabled: !!tripId && !!memberInfo,
  });

  // Wishlist query
  const { data: wishlistData } = useQuery({
    queryKey: tripQueryKeys.wishlist(tripId ?? "", wishlistSearchQuery),
    queryFn: () => wishlistApi.getWishlist(Number(tripId), wishlistSearchQuery || undefined),
    enabled: !!tripId,
  });

  // Categories query
  const { data: categories = [] } = useQuery({
    queryKey: tripQueryKeys.categories(tripId ?? ""),
    queryFn: () => categoryApi.getCategories(Number(tripId)),
    enabled: !!tripId,
  });

  const resolveExpenseDate = useCallback((categoryId?: number | null, fallbackDate?: string) => {
    if (!tripDetail?.startDate || !categoryId) {
      return fallbackDate || new Date().toISOString().slice(0, 10);
    }

    const category = categories.find((value) => value.categoryId === categoryId);
    if (!category) {
      return fallbackDate || new Date().toISOString().slice(0, 10);
    }

    const baseDate = new Date(`${tripDetail.startDate}T00:00:00`);
    baseDate.setDate(baseDate.getDate() + Math.max(category.day - 1, 0));
    return baseDate.toISOString().slice(0, 10);
  }, [categories, tripDetail?.startDate]);

  // Directions query
  const { data: directionsData = [] } = useQuery({
    queryKey: [...tripQueryKeys.directions(tripId ?? ""), 'WALKING', 'DRIVING', 'TRANSIT'],
    queryFn: async () => {
      if (!tripId) return [];
      
      // Fetch all three modes in parallel with error handling
      const results = await Promise.allSettled([
        itineraryApi.getAllDirections(Number(tripId), 'WALKING'),
        itineraryApi.getAllDirections(Number(tripId), 'DRIVING'),
        itineraryApi.getAllDirections(Number(tripId), 'TRANSIT'),
      ]);
      
      const allRoutes: RouteDetails[] = [];
      
      results.forEach((result, index) => {
        const mode = ['WALKING', 'DRIVING', 'TRANSIT'][index];
        if (result.status === 'fulfilled') {
          allRoutes.push(...result.value);
        } else {
          console.warn(`Failed to fetch ${mode} directions:`, result.reason);
        }
      });
      
      return allRoutes;
    },
    enabled: !!tripId && categories.length > 0,
  });

  // Expense detail query
  const { data: expenseDetail, isLoading: isLoadingExpense } = useQuery({
    queryKey: tripQueryKeys.expense(tripId ?? "", expenseDetailModal.expenseId),
    queryFn: () => expensesApi.getExpenseDetail(Number(tripId), expenseDetailModal.expenseId!),
    enabled: !!tripId && !!expenseDetailModal.expenseId && expenseDetailModal.isOpen,
  });

  // Daily settlement query
  const { data: dailySettlement, isLoading: isLoadingSettlement } = useQuery({
    queryKey: tripQueryKeys.dailySettlement(tripId ?? "", dailySettlementModal.date),
    queryFn: () => settlementApi.getDailySettlement(Number(tripId), dailySettlementModal.date!),
    enabled: !!tripId && !!dailySettlementModal.date && dailySettlementModal.isOpen,
  });

  // Total settlement query
  const { data: totalSettlement, isLoading: isLoadingTotalSettlement } = useQuery({
    queryKey: tripQueryKeys.totalSettlement(tripId ?? ""),
    queryFn: () => settlementApi.getTotalSettlement(Number(tripId)),
    enabled: !!tripId && totalSettlementModal,
  });

  // Trip expenses query (Simple)
  const { data: tripExpenses = [] } = useQuery({
    queryKey: tripQueryKeys.expenses(tripId ?? ""),
    queryFn: () => expensesApi.getTripExpenses(Number(tripId)),
    enabled: !!tripId,
  });

  // Group expenses by itineraryItemId
  const expensesByItinerary = useMemo(() => {
    const map = new Map<number, ExpenseSimpleResponse[]>();
    tripExpenses.forEach(expense => {
      const existing = map.get(expense.itineraryItemId) || [];
      map.set(expense.itineraryItemId, [...existing, expense]);
    });
    return map;
  }, [tripExpenses]);

  const wishlistItems = wishlistData?.content || [];

  // Add to wishlist mutation
  const addWishlistMutation = useMutation({
    mutationFn: (place: PlaceSearchResult) =>
      wishlistApi.addWishlist(Number(tripId), {
        externalPlaceId: place.externalPlaceId,
        placeName: place.placeName,
        address: place.address,
        latitude: place.latitude,
        longitude: place.longitude,
      }),
    onSuccess: (_, place) => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.wishlistRoot(tripId ?? "") });
      toast({
        title: "위시리스트에 추가됨",
        description: `${place.placeName}이(가) 추가되었습니다.`,
      });
    },
    onError: () => {
      toast({
        title: "추가 실패",
        description: "위시리스트 추가 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Delete wishlist mutation
  const deleteWishlistMutation = useMutation({
    mutationFn: (wishlistItemId: number) =>
      wishlistApi.deleteWishlistItems(Number(tripId), [wishlistItemId]),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.wishlistRoot(tripId ?? "") });
      toast({
        title: "삭제됨",
        description: "위시리스트에서 삭제되었습니다.",
      });
    },
    onError: () => {
      toast({
        title: "삭제 실패",
        description: "삭제 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Create category mutation
  const createCategoryMutation = useMutation({
    mutationFn: ({ day, name, order }: { day: number; name: string; order: number }) =>
      categoryApi.createCategory(Number(tripId), { name, day, order }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      toast({
        title: "카테고리 추가됨",
        description: `${variables.name} 카테고리가 추가되었습니다.`,
      });
    },
    onError: () => {
      toast({
        title: "추가 실패",
        description: "카테고리 추가 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Update category order mutation
  const updateCategoryOrderMutation = useMutation({
    mutationFn: (items: { categoryId: number; order: number }[]) =>
      categoryApi.updateCategoryOrder(Number(tripId), { items }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.directions(tripId ?? "") });
      toast({
        title: "순서 변경됨",
        description: "카테고리 순서가 변경되었습니다.",
      });
    },
    onError: () => {
      toast({
        title: "순서 변경 실패",
        description: "카테고리 순서 변경 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Sensors for drag and drop
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  // Update current time for trip country
  useEffect(() => {
    if (!tripDetail?.country) return;
    
    const updateTime = () => {
      const timezone = getCountryTimezone(tripDetail.country);
      const now = new Date();
      const formatter = new Intl.DateTimeFormat('ko-KR', {
        timeZone: timezone,
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      });
      setCurrentTime(formatter.format(now));
    };
    
    updateTime();
    const interval = setInterval(updateTime, 60000);
    
    return () => clearInterval(interval);
  }, [tripDetail?.country]);

  const handleAddCategory = (day: number) => {
    const categoryName = prompt(`Day ${day}에 추가할 카테고리 이름을 입력하세요:`);
    if (!categoryName) return;

    const dayCategories = categories.filter(cat => cat.day === day);
    const nextOrder = dayCategories.length > 0 ? Math.max(...dayCategories.map(cat => cat.order)) + 1 : 0;

    createCategoryMutation.mutate({ day, name: categoryName, order: nextOrder });
  };

  const handleQuickAddCategory = (day: number, categoryName: string) => {
    const dayCategories = categories.filter(cat => cat.day === day);
    const nextOrder = dayCategories.length > 0 ? Math.max(...dayCategories.map(cat => cat.order)) + 1 : 0;

    createCategoryMutation.mutate({ day, name: categoryName, order: nextOrder });
  };

  // Delete category mutation
  const deleteCategoryMutation = useMutation({
    mutationFn: (categoryId: number) =>
      categoryApi.deleteCategory(Number(tripId), categoryId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      toast({
        title: "카테고리 삭제됨",
        description: "카테고리가 삭제되었습니다.",
      });
    },
    onError: () => {
      toast({
        title: "삭제 실패",
        description: "카테고리 삭제 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  const handleDeleteCategory = (categoryId: number) => {
    if (!confirm("이 카테고리와 포함된 모든 일정을 삭제하시겠습니까?")) return;
    deleteCategoryMutation.mutate(categoryId);
  };

  // Update category mutation
  const updateCategoryMutation = useMutation({
    mutationFn: ({ categoryId, name }: { categoryId: number; name: string }) =>
      categoryApi.updateCategory(Number(tripId), categoryId, { name }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      toast({
        title: "카테고리 수정됨",
        description: "카테고리 이름이 수정되었습니다.",
      });
    },
    onError: () => {
      toast({
        title: "수정 실패",
        description: "카테고리 수정 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  const handleUpdateCategoryName = (categoryId: number, currentName: string) => {
    const newName = prompt("카테고리 이름을 입력하세요:", currentName);
    if (!newName || newName === currentName) return;
    updateCategoryMutation.mutate({ categoryId, name: newName });
  };

  // Generate invite link
  const handleGenerateInvite = async () => {
    if (!tripId) return;
    
    try {
      const response = await tripApi.generateInvite(Number(tripId));
      setInviteLink(response.inviteLink);
      setShowInviteDialog(true);
      setIsCopied(false);
    } catch (error) {
      toast({
        title: "초대 링크 생성 실패",
        description: "초대 링크를 생성하는데 실패했습니다.",
        variant: "destructive",
      });
    }
  };

  // Add itinerary from wishlist
  const addItineraryMutation = useMutation({
    mutationFn: ({ 
      categoryId, 
      data 
    }: { 
      categoryId: number; 
      data: {
        placeId?: number | null;
        title?: string | null;
        time?: string | null;
        memo?: string | null;
      }
    }) =>
      itineraryApi.createItinerary(Number(tripId), categoryId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.directions(tripId ?? "") });
      toast({
        title: "일정 추가됨",
        description: "위시리스트에서 일정으로 추가되었습니다.",
      });
    },
    onError: () => {
      toast({
        title: "추가 실패",
        description: "일정 추가 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Handle adding wishlist item to itinerary from map
  const handleAddWishlistToItinerary = async (wishlistItem: WishlistItem, categoryId: number) => {
    try {
      await itineraryApi.createItinerary(Number(tripId), categoryId, {
        placeId: wishlistItem.placeId,
        memo: null,
        time: null,
        title: null,
      });
      
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.directions(tripId ?? "") });
      
      toast({
        title: "일정에 추가되었습니다",
        description: `${wishlistItem.name}이(가) 일정에 추가되었습니다.`,
      });
    } catch (error) {
      toast({
        variant: "destructive",
        title: "일정 추가 실패",
        description: "일정 추가 중 오류가 발생했습니다.",
      });
    }
  };

  // Update itinerary
  const updateItineraryMutation = useMutation({
    mutationFn: ({ 
      categoryId, 
      itemId, 
      updates 
    }: { 
      categoryId: number; 
      itemId: number; 
      updates: { time?: string; memo?: string } 
    }) =>
      itineraryApi.updateItinerary(Number(tripId), categoryId, itemId, updates),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      setEditingItinerary(null);
      toast({
        title: "수정 완료",
        description: "일정이 수정되었습니다.",
      });
    },
    onError: () => {
      toast({
        title: "수정 실패",
        description: "일정 수정 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Delete itinerary
  const deleteItineraryMutation = useMutation({
    mutationFn: ({ categoryId, itemId }: { categoryId: number; itemId: number }) =>
      itineraryApi.deleteItinerary(Number(tripId), categoryId, itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.directions(tripId ?? "") });
      toast({
        title: "삭제 완료",
        description: "일정이 삭제되었습니다.",
      });
    },
    onError: () => {
      toast({
        title: "삭제 실패",
        description: "일정 삭제 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Delete itinerary from map
  const handleDeleteItineraryFromMap = (itineraryId: number, categoryId: number) => {
    if (window.confirm('이 일정을 삭제하시겠습니까?')) {
      deleteItineraryMutation.mutate({ categoryId, itemId: itineraryId });
    }
  };

  // Delete wishlist from map
  const handleDeleteWishlistFromMap = (wishlistItemId: number) => {
    if (window.confirm('위시리스트에서 삭제하시겠습니까?')) {
      deleteWishlistMutation.mutate(wishlistItemId);
    }
  };

  // Update itinerary order
  const updateItineraryOrderMutation = useMutation({
    mutationFn: (orderData: { items: { itemId: number; categoryId: number; order: number }[] }) =>
      itineraryApi.updateItineraryOrder(Number(tripId), orderData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.directions(tripId ?? "") });
      toast({
        title: "순서 변경 완료",
        description: "일정 순서가 변경되었습니다.",
      });
    },
    onError: () => {
      toast({
        title: "순서 변경 실패",
        description: "순서 변경 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Move itinerary item to another category (using same API as drag-and-drop)
  const handleMoveToCategory = async (
    itemId: number,
    fromCategoryId: number,
    toCategoryId: number
  ) => {
    try {
      if (!tripId) return;

      // Find the item details
      const sourceCategory = categories.find(cat => cat.categoryId === fromCategoryId);
      const itemToMove = sourceCategory?.itineraryItems.find(it => it.itineraryId === itemId);
      const targetCategory = categories.find(cat => cat.categoryId === toCategoryId);

      if (!itemToMove || !targetCategory || !sourceCategory) {
        throw new Error("항목을 찾을 수 없습니다");
      }

      // 1. Source category items (excluding the moved item) with recalculated order
      const sourceItems = sourceCategory.itineraryItems
        .filter(it => it.itineraryId !== itemId)
        .map((it, idx) => ({
          itemId: it.itineraryId,
          categoryId: fromCategoryId,
          order: idx
        }));

      // 2. Target category items with current order
      const targetItems = targetCategory.itineraryItems.map((it, idx) => ({
        itemId: it.itineraryId,
        categoryId: toCategoryId,
        order: idx
      }));

      // 3. Add moved item to the end of target category
      const movedItem = {
        itemId: itemId,
        categoryId: toCategoryId,
        order: targetItems.length
      };

      // 4. Call updateItineraryOrder API (same as drag-and-drop)
      await itineraryApi.updateItineraryOrder(Number(tripId), {
        items: [...sourceItems, ...targetItems, movedItem]
      });

      // Refetch data to ensure sync
      await queryClient.refetchQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      await queryClient.refetchQueries({ queryKey: tripQueryKeys.directions(tripId ?? "") });

      toast({
        title: "이동 완료",
        description: `${itemToMove.name}을(를) ${targetCategory.name}(으)로 이동했습니다.`,
      });
    } catch (error) {
      console.error('Error moving itinerary:', error);
      toast({
        title: "이동 실패",
        description: "일정을 이동하는 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    }
  };

  // Create/update expense
  const createExpenseMutation = useMutation({
    mutationFn: async ({ 
      expenseData,
      imageFile
    }: { 
      expenseData: any;
      imageFile?: File;
    }) => {
      // If expenseId exists, it means expense was already created (receipt mode)
      // Just assign participants
      if (expenseData.expenseId) {
        const items = Object.entries(expenseData.participantAssignmentsByIndex as Record<number, number[]>)
          .map(([itemId, participantIds]) => ({
            itemId: Number(itemId),
            participantIds
          }))
          .filter(item => item.participantIds.length > 0);
        
        if (items.length > 0) {
          const assignRequest: ParticipantAssignRequest = {
            tripId: Number(tripId),
            items
          };
          
          return await expensesApi.assignParticipants(expenseData.expenseId, assignRequest);
        }
        return null;
      }
      
      // Manual entry - create expense first
      const request: CreateExpenseRequest = {
        tripId: Number(tripId),
        expenseTitle: expenseData.description,
        totalAmount: expenseData.totalAmount,
        itineraryItemId: expenseModal.itineraryItem?.itineraryId || 0,
        payerId: expenseData.payerId,
        inputMethod: expenseData.inputMethod || 'HANDWRITE',
        items: expenseData.items.map((item: any) => ({
          itemName: item.name,
          price: item.price
        })),
        currency: expenseData.currency || 'KRW', // 추가: 통화 정보
        date: resolveExpenseDate(expenseModal.itineraryItem?.categoryId, expenseData.date),
      };

      const expenseResponse = await expensesApi.createExpense(request, imageFile);
      
      // Then assign participants
      if (expenseData.participantAssignmentsByIndex && Object.keys(expenseData.participantAssignmentsByIndex).length > 0) {
        const items = expenseResponse.items.map((item, index) => ({
          itemId: item.itemId,
          participantIds: expenseData.participantAssignmentsByIndex[index] || []
        })).filter(item => item.participantIds.length > 0);
        
        if (items.length > 0) {
          const assignRequest: ParticipantAssignRequest = {
            tripId: Number(tripId),
            items
          };
          
          await expensesApi.assignParticipants(expenseResponse.expenseId, assignRequest);
        }
      }
      
      return expenseResponse;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.dailySettlementRoot(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.totalSettlement(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.expenses(tripId ?? "") });
      setExpenseModal({ isOpen: false, itineraryItem: null });
      toast({
        title: "정산 완료",
        description: "비용이 추가되고 멤버 배분이 완료되었습니다.",
      });
    },
    onError: (error: any) => {
      console.error('Expense error:', error);
      toast({
        title: "정산 실패",
        description: error.response?.data?.message || "정산 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Update expense mutation
  const updateExpenseMutation = useMutation({
    mutationFn: async ({
      expenseId,
      expenseData,
    }: {
      expenseId: number;
      expenseData: any;
    }) => {
      // UpdateRequest 형식에 맞게 변환
      const request = {
        expenseTitle: expenseData.description,
        totalAmount: expenseData.totalAmount,
        tripId: Number(tripId),
        payerId: expenseData.payerId,
        currency: expenseData.currency || 'KRW',
        itineraryItemId: expenseModal.itineraryItem?.itineraryId || null,
        date: resolveExpenseDate(expenseModal.itineraryItem?.categoryId, expenseData.date),
        items: expenseData.items.map((item: any) => ({
          itemId: item.itemId || null, // itemId 포함 (null이면 새 항목)
          itemName: item.name,
          price: item.price
        }))
      };

      const expenseResponse = await expensesApi.updateExpense(expenseId, request);
      
      // Assign participants - itemId 기반으로 매칭
      if (expenseData.participantAssignmentsByIndex && 
          Object.keys(expenseData.participantAssignmentsByIndex).length > 0) {
        
        // itemId를 먼저 확인하고, 없으면 index로 fallback
        const items = expenseResponse.items
          .map((responseItem, index) => ({
            itemId: responseItem.itemId,
            participantIds: 
              expenseData.participantAssignmentsByIndex[responseItem.itemId] ||  // 영수증 모드 (itemId가 키)
              expenseData.participantAssignmentsByIndex[index] ||                 // 수동 입력 모드 (index가 키)
              []
          }))
          .filter(item => item.participantIds.length > 0);
        
        if (items.length > 0) {
          const assignRequest: ParticipantAssignRequest = {
            tripId: Number(tripId),
            items
          };
          
          await expensesApi.assignParticipants(expenseId, assignRequest);
        }
      }
      
      return expenseResponse;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.dailySettlementRoot(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.totalSettlement(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.expenses(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.expense(tripId ?? "", variables.expenseId) });
      setExpenseModal({ isOpen: false, itineraryItem: null });
      toast({
        title: "정산 수정 완료",
        description: "비용이 수정되었습니다.",
      });
    },
    onError: (error: any) => {
      console.error('Expense update error:', error);
      toast({
        title: "정산 수정 실패",
        description: error.response?.data?.message || "비용 수정 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Delete expense
  const deleteExpenseMutation = useMutation({
    mutationFn: (expenseId: number) => 
      expensesApi.deleteExpense(Number(tripId), expenseId),
    onSuccess: (_, expenseId) => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.dailySettlementRoot(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.totalSettlement(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.expenses(tripId ?? "") });
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.expense(tripId ?? "", expenseId) });
      toast({
        title: "정산 내역 삭제 완료",
        description: "비용이 삭제되었습니다.",
      });
    },
    onError: (error: any) => {
      console.error('Expense deletion error:', error);
      toast({
        title: "정산 내역 삭제 실패",
        description: error.response?.data?.message || "비용 삭제 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  // Drag and drop handlers for wishlist
  const handleDragStart = (item: WishlistItem) => {
    setDraggedItem(item);
  };

  const handleDragEnd = () => {
    setDraggedItem(null);
    setDraggedItinerary(null);
    setDropTargetCategoryId(null);
    setDragOverItemId(null);
  };

  const handleDragOver = (e: React.DragEvent, categoryId: number) => {
    e.preventDefault();
    setDropTargetCategoryId(categoryId);
  };

  const handleDragLeave = () => {
    setDropTargetCategoryId(null);
  };

  const handleDrop = (e: React.DragEvent, categoryId: number) => {
    e.preventDefault();
    
    if (draggedItem) {
      // Dropping wishlist item
      addItineraryMutation.mutate({
        categoryId,
        data: {
          placeId: Number(draggedItem.placeId) || null,
          title: null,
          time: null,
          memo: null,
        }
      });
    } else if (draggedItinerary) {
      // Handle itinerary item drop (within same category or cross-category)
      const sourceCategory = categories.find(c => c.categoryId === draggedItinerary.categoryId);
      const targetCategory = categories.find(c => c.categoryId === categoryId);
      
      if (sourceCategory && targetCategory) {
        const isSameCategory = sourceCategory.categoryId === targetCategory.categoryId;
        
        if (isSameCategory) {
          // Reordering within the same category
          const items = [...sourceCategory.itineraryItems].sort((a, b) => a.order - b.order);
          const draggedIndex = items.findIndex(item => item.itineraryId === draggedItinerary.item.itineraryId);
          
          if (draggedIndex !== -1) {
            const [removed] = items.splice(draggedIndex, 1);
            
            // Find target position based on dragOverItemId
            let dropIndex = items.length;
            if (dragOverItemId) {
              const targetIndex = items.findIndex(item => item.itineraryId === dragOverItemId);
              if (targetIndex !== -1) {
                dropIndex = targetIndex;
              }
            }
            
            items.splice(dropIndex, 0, removed);
            
            // Create order update request
            const orderData = {
              items: items.map((item, index) => ({
                itemId: item.itineraryId,
                categoryId: categoryId,
                order: index
              }))
            };
            
            updateItineraryOrderMutation.mutate(orderData);
          }
        } else {
          // Moving to a different category
          const sourceItems = [...sourceCategory.itineraryItems]
            .filter(item => item.itineraryId !== draggedItinerary.item.itineraryId)
            .sort((a, b) => a.order - b.order);
          
          const targetItems = [...targetCategory.itineraryItems].sort((a, b) => a.order - b.order);
          
          // Find target position based on dragOverItemId
          let dropIndex = targetItems.length;
          if (dragOverItemId) {
            const targetIndex = targetItems.findIndex(item => item.itineraryId === dragOverItemId);
            if (targetIndex !== -1) {
              dropIndex = targetIndex;
            }
          }
          
          targetItems.splice(dropIndex, 0, draggedItinerary.item);
          
          // Create order update request for both categories
          const allItems = [
            ...sourceItems.map((item, index) => ({
              itemId: item.itineraryId,
              categoryId: sourceCategory.categoryId,
              order: index
            })),
            ...targetItems.map((item, index) => ({
              itemId: item.itineraryId,
              categoryId: targetCategory.categoryId,
              order: index
            }))
          ];
          
          updateItineraryOrderMutation.mutate({ items: allItems });
        }
      }
    }
    
    setDraggedItem(null);
    setDraggedItinerary(null);
    setDropTargetCategoryId(null);
    setDragOverItemId(null);
  };

  // Drag handlers for itinerary items
  const handleItineraryDragStart = (item: ItineraryResponse, categoryId: number) => {
    setDraggedItinerary({ item, categoryId });
  };

  const handleItineraryDragOver = (e: React.DragEvent, targetItem: ItineraryResponse, categoryId: number) => {
    e.preventDefault();
    e.stopPropagation();
    
    if (!draggedItinerary) return;
    
    // Set the drag over item for visual feedback and drop position
    setDragOverItemId(targetItem.itineraryId);
    setDropTargetCategoryId(categoryId);
  };

  const handleCategoryDragOver = (e: React.DragEvent, categoryId: number) => {
    e.preventDefault();
    if (draggedItinerary) {
      setDropTargetCategoryId(categoryId);
      setDragOverItemId(null); // Clear item hover when over empty space
    }
  };

  // Handle category drag end for reordering
  const handleCategoryDragEnd = (event: DragEndEvent, day: number) => {
    const { active, over } = event;

    if (!over || active.id === over.id) return;

    const dayCategories = categories
      .filter(cat => cat.day === day)
      .sort((a, b) => a.order - b.order);

    const oldIndex = dayCategories.findIndex(cat => cat.categoryId === active.id);
    const newIndex = dayCategories.findIndex(cat => cat.categoryId === over.id);

    if (oldIndex === -1 || newIndex === -1) return;

    const reorderedCategories = arrayMove(dayCategories, oldIndex, newIndex);

    // Update order values
    const updatedOrders = reorderedCategories.map((cat, index) => ({
      categoryId: cat.categoryId,
      order: index
    }));

    updateCategoryOrderMutation.mutate(updatedOrders);
  };

  // Move itinerary item up/down within a category (used mainly on mobile)
  const handleMoveItemWithinCategory = (categoryId: number, itemId: number, direction: 'up' | 'down') => {
    const category = categories.find((c) => c.categoryId === categoryId);
    if (!category) return;

    const items = [...category.itineraryItems].sort((a, b) => a.order - b.order);
    const currentIndex = items.findIndex((item) => item.itineraryId === itemId);
    if (currentIndex === -1) return;

    const newIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1;
    if (newIndex < 0 || newIndex >= items.length) return;

    const reorderedItems = arrayMove(items, currentIndex, newIndex);

    const orderData = {
      items: reorderedItems.map((item, index) => ({
        itemId: item.itineraryId,
        categoryId,
        order: index,
      })),
    };

    updateItineraryOrderMutation.mutate(orderData);
  };

  const handleMoveItemUp = (categoryId: number, itemId: number) => {
    handleMoveItemWithinCategory(categoryId, itemId, 'up');
  };

  const handleMoveItemDown = (categoryId: number, itemId: number) => {
    handleMoveItemWithinCategory(categoryId, itemId, 'down');
  };

  // Copy invite link
  const handleCopyInviteLink = async () => {
    try {
      await navigator.clipboard.writeText(inviteLink);
      setIsCopied(true);
      toast({
        title: "복사 완료",
        description: "초대 링크가 클립보드에 복사되었습니다.",
      });
      setTimeout(() => setIsCopied(false), 2000);
    } catch (error) {
      toast({
        title: "복사 실패",
        description: "클립보드 복사에 실패했습니다.",
        variant: "destructive",
      });
    }
  };

  if (isLoadingTrip) {
    return (
      <div className="min-h-screen bg-gradient-subtle flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-8 h-8 mx-auto mb-4 animate-spin text-primary" />
          <p className="text-muted-foreground">로딩 중...</p>
        </div>
      </div>
    );
  }

  if (!tripDetail) {
    return (
      <div className="min-h-screen bg-gradient-subtle flex items-center justify-center">
        <div className="text-center">
          <p className="text-muted-foreground">여행 정보를 찾을 수 없습니다.</p>
          <Button onClick={() => navigate("/")} className="mt-4">
            돌아가기
          </Button>
        </div>
      </div>
    );
  }

  const totalDays = Math.ceil((new Date(tripDetail.endDate).getTime() - new Date(tripDetail.startDate).getTime()) / (1000 * 60 * 60 * 24)) + 1;

  // Helper function to get date for a day
  const getDateForDay = (day: number) => {
    const date = addDays(new Date(tripDetail.startDate), day - 1);
    return format(date, 'M.d');
  };

  // Helper function to find routes between two itinerary items
  const findRoutesForItems = (item1: ItineraryResponse, item2: ItineraryResponse): RouteDetails[] => {
    if (!item1.location || !item2.location || !directionsData.length) {
      return [];
    }

    return directionsData.filter(route => {
      const originMatch = route.originPlace.placeName === item1.name;
      const destMatch = route.destinationPlace.placeName === item2.name;
      return originMatch && destMatch;
    });
  };

  return (
    <div className="h-screen bg-gradient-subtle flex flex-col">
      {/* Header */}
      <header className="bg-white shadow-soft">
        <div className="container mx-auto px-4 md:px-6 py-3 md:py-4">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
            <div className="flex items-center space-x-2 md:space-x-4">
              <Button 
                variant="ghost" 
                onClick={() => navigate("/")}
                className="hover:bg-primary/10"
                size="sm"
              >
                <ArrowLeft className="w-4 h-4 md:mr-2" />
                <span className="hidden md:inline">뒤로가기</span>
              </Button>
              <div className="flex items-center gap-3">
                <div>
                  <h1 className="text-lg md:text-2xl font-bold text-foreground">{tripDetail.title}</h1>
                  <p className="text-xs md:text-sm text-muted-foreground">
                    {new Date(tripDetail.startDate).toLocaleDateString()} - {new Date(tripDetail.endDate).toLocaleDateString()}
                  </p>
                </div>
                {/* Current time display */}
                {currentTime && (
                  <div className="hidden md:flex items-center gap-2 px-4 py-2 bg-gradient-primary rounded-xl shadow-primary backdrop-blur-sm border border-white/20 transition-all duration-300 hover:scale-105 hover:shadow-lg">
                    <Clock className="w-5 h-5 text-white animate-pulse" />
                    <div className="flex flex-col">
                      <span className="text-[10px] font-medium text-white/80 leading-none mb-0.5">현지 시간</span>
                      <span className="text-base font-bold text-white leading-none">{currentTime}</span>
                    </div>
                  </div>
                )}
              </div>
              {/* Member Avatars */}
              <TooltipProvider>
                <div 
                  className="flex -space-x-2 transition-all duration-300"
                  onMouseEnter={() => setIsMembersHovered(true)}
                  onMouseLeave={() => setIsMembersHovered(false)}
                >
                  {(isMembersHovered || tripDetail.members.length <= 3
                    ? tripDetail.members 
                    : tripDetail.members.slice(0, 3)
                  ).map((member) => (
                    <Tooltip key={member.tripMemberId || member.nickname}>
                      <TooltipTrigger asChild>
                        <Avatar className="border-2 border-white w-8 h-8 cursor-pointer hover:z-10 transition-transform hover:scale-110">
                          <AvatarImage
                              src={member.avatar || undefined}
                              alt={member.nickname}
                              className="object-cover"
                          />
                          <AvatarFallback className="bg-primary text-primary-foreground text-xs">
                            {member.nickname.charAt(0).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                      </TooltipTrigger>
                      <TooltipContent>
                        <p>{member.nickname}</p>
                      </TooltipContent>
                    </Tooltip>
                  ))}
                  
                  {/* "+N" badge when there are more than 3 members and not hovering */}
                  {!isMembersHovered && tripDetail.members.length > 3 && (
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-primary/10 border-2 border-white text-xs font-semibold text-primary cursor-pointer hover:bg-primary/20 transition-colors transition-all">
                          +{tripDetail.members.length - 3}
                        </div>
                      </TooltipTrigger>
                      <TooltipContent>
                        <p>나머지 {tripDetail.members.length - 3}명 보기</p>
                      </TooltipContent>
                    </Tooltip>
                  )}
                </div>
              </TooltipProvider>
            </div>
            <div className="flex items-center space-x-2 md:space-x-3 overflow-x-auto md:pb-0">
              <Button 
                variant="outline" 
                onClick={() => setShowMembersModal(true)}
                className="border-primary text-primary hover:bg-primary hover:text-primary-foreground whitespace-nowrap"
                size="sm"
              >
                <Users className="w-4 h-4 md:mr-2" />
                <span className="sm:inline">멤버 관리</span>
              </Button>
              <Button 
                variant="outline" 
                onClick={() => setShowReviewModal(true)}
                className="border-primary text-primary hover:bg-primary hover:text-primary-foreground whitespace-nowrap"
                size="sm"
              >
                <Sparkles className="w-4 h-4 md:mr-2" />
                <span className="sm:inline">AI 분석</span>
              </Button>
              <div className="hidden md:flex items-center gap-1">

                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                        variant="outline"
                        onClick={() => setTotalSettlementModal(true)}
                        className="border-accent text-accent hover:bg-accent hover:text-accent-foreground whitespace-nowrap"
                        size="sm"
                    >
                      <Calculator className="w-4 h-4 md:mr-2" />
                      <span className="sm:inline">전체 정산</span>
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent side="bottom" className="max-w-xs">
                    <p>일별로 나눠 정산하거나 전체 여행 기간의 정산을 한번에 확인할 수 있습니다</p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Button 
                variant="outline" 
                onClick={() => setTotalSettlementModal(true)}
                className="md:hidden border-accent text-accent hover:bg-accent hover:text-accent-foreground whitespace-nowrap"
                size="sm"
              >
                <Calculator className="w-4 h-4 md:mr-2" />
                <span className="sm:inline">전체 정산</span>
              </Button>
              <div className="hidden md:flex items-center gap-1">
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                        className="bg-gradient-primary hover:shadow-primary whitespace-nowrap"
                        onClick={handleGenerateInvite}
                        size="sm"
                    >
                      <Users className="w-4 h-4 md:mr-2" />
                      <span className="sm:inline">친구 초대</span>
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent side="bottom" className="max-w-xs">
                    <p>초대 링크를 복사하여 친구들과 여행을 함께 계획하세요</p>
                  </TooltipContent>
                </Tooltip>
              </div>
              <Button 
                className="md:hidden bg-gradient-primary hover:shadow-primary whitespace-nowrap"
                onClick={handleGenerateInvite}
                size="sm"
              >
                <Users className="w-4 h-4 md:mr-2" />
                <span className="sm:inline">친구 초대</span>
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="relative flex flex-col md:flex-row w-full flex-1 min-h-0 overflow-hidden">
        {/* Mobile Drawer for Itinerary */}
        <Drawer open={isItineraryDrawerOpen} onOpenChange={setIsItineraryDrawerOpen}>
          <DrawerContent className="md:hidden h-[85vh] flex flex-col">
            <Card className="flex flex-col bg-white border-0 flex-1 overflow-hidden">
              <CardHeader className="flex-shrink-0 p-4 border-b">
                <CardTitle className="flex items-center justify-between text-base">
                  <div className="flex items-center gap-1">
                    일정 계획
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <button className="text-muted-foreground hover:text-foreground">
                          <HelpCircle className="w-3.5 h-3.5" />
                        </button>
                      </TooltipTrigger>
                      <TooltipContent side="bottom" className="max-w-xs">
                        <p>카테고리와 일정을 드래그하여 순서를 변경할 수 있습니다</p>
                      </TooltipContent>
                    </Tooltip>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setIsItineraryDrawerOpen(false);
                      setTimeout(() => {
                        setIsWishlistDrawerOpen(true);
                      }, 300);
                    }}
                    className="text-amber-500 hover:bg-amber-500/10 gap-1 h-fit"
                  >
                    <Star className="w-4 h-4" />
                    <span className="text-sm">위시리스트</span>
                  </Button>
                </CardTitle>
              </CardHeader>
              <CardContent className="flex-1 overflow-y-auto p-4 min-h-0">
                <Tabs value={String(selectedDay)} onValueChange={(val) => setSelectedDay(Number(val))}>
                  <TabsList className="grid justify-between overflow-x-scroll h-auto [&::-webkit-scrollbar]:hidden " style={{ gridTemplateColumns: `repeat(${totalDays}, minmax(60px, 1fr))` }}>
                    {Array.from({ length: totalDays }, (_, i) => i + 1).map(day => (
                      <TabsTrigger key={day} value={String(day)} className="text-xs flex flex-col items-center gap-0.5 py-1">
                        <span>Day {day}</span>
                        <span className="text-[10px] text-muted-foreground">{getDateForDay(day)}</span>
                      </TabsTrigger>
                    ))}
                  </TabsList>

                  {Array.from({ length: totalDays }, (_, i) => i + 1).map((day) => {
                    const dayCategories = categories.filter(cat => cat.day === day);

                    return (
                      <TabsContent key={day} value={String(day)} className="mt-3 space-y-3">
                        <DndContext
                          sensors={sensors}
                          collisionDetection={closestCenter}
                          onDragEnd={(event) => handleCategoryDragEnd(event, day)}
                        >
                          <SortableContext
                            items={dayCategories.map(cat => cat.categoryId)}
                            strategy={verticalListSortingStrategy}
                          >
            <div className="space-y-3">
              {dayCategories.map((category, categoryIndex) => (
                <SortableCategory
                  key={category.categoryId}
                  category={category}
                  categoryIndex={categoryIndex}
                  categoriesArray={dayCategories}
                  day={day}
                  categories={categories}
                  dropTargetCategoryId={dropTargetCategoryId}
                  draggedItinerary={draggedItinerary}
                  dragOverItemId={dragOverItemId}
                  mapRef={mapRef}
                  findRoutesForItems={findRoutesForItems}
                  handleCategoryDragOver={handleCategoryDragOver}
                  handleDragOver={handleDragOver}
                  handleDragLeave={handleDragLeave}
                  handleDrop={handleDrop}
                  handleUpdateCategoryName={handleUpdateCategoryName}
                  handleDeleteCategory={handleDeleteCategory}
                  handleItineraryDragStart={handleItineraryDragStart}
                  handleItineraryDragOver={handleItineraryDragOver}
                  handleDragEnd={handleDragEnd}
                  setExpenseModal={setExpenseModal}
                  setEditingItinerary={setEditingItinerary}
                  deleteItineraryMutation={deleteItineraryMutation}
                  setExpenseDetailModal={setExpenseDetailModal}
                  setExpenseListModal={setExpenseListModal}
                  handleMoveToCategory={handleMoveToCategory}
                  setIsItineraryDrawerOpen={setIsItineraryDrawerOpen}
                  expensesByItinerary={expensesByItinerary}
                  isMobile={isMobile}
                  onMoveItemUp={handleMoveItemUp}
                  onMoveItemDown={handleMoveItemDown}
                />
              ))}
            </div>
                          </SortableContext>
                        </DndContext>

                        <Button
                          variant="outline"
                          onClick={() => {
                            const date = new Date(tripDetail.startDate);
                            date.setDate(date.getDate() + (day - 1));
                            setDailySettlementModal({
                              isOpen: true,
                              date: date.toISOString().split('T')[0]
                            });
                          }}
                          className="w-full border-primary text-primary hover:bg-primary hover:text-primary-foreground"
                        >
                          <Calculator className="w-4 h-4 mr-2" />
                          일별 정산 보기
                        </Button>

                        <div className="space-y-2">
                          <p className="text-xs text-muted-foreground font-medium">빠른 카테고리 추가</p>
                          <div className="flex flex-wrap gap-2">
                            {[
                              { name: '식사', bgColor: 'bg-rose-100 hover:bg-rose-200 dark:bg-rose-950 dark:hover:bg-rose-900', textColor: 'text-rose-700 dark:text-rose-300' },
                              { name: '숙소', bgColor: 'bg-amber-100 hover:bg-amber-200 dark:bg-amber-950 dark:hover:bg-amber-900', textColor: 'text-amber-700 dark:text-amber-300' },
                              { name: '관광', bgColor: 'bg-emerald-100 hover:bg-emerald-200 dark:bg-emerald-950 dark:hover:bg-emerald-900', textColor: 'text-emerald-700 dark:text-emerald-300' },
                              { name: '쇼핑', bgColor: 'bg-sky-100 hover:bg-sky-200 dark:bg-sky-950 dark:hover:bg-sky-900', textColor: 'text-sky-700 dark:text-sky-300' },
                              { name: '액티비티', bgColor: 'bg-purple-100 hover:bg-purple-200 dark:bg-purple-950 dark:hover:bg-purple-900', textColor: 'text-purple-700 dark:text-purple-300' },
                            ].map(({ name, bgColor, textColor }) => (
                              <Button
                                key={name}
                                variant="ghost"
                                size="sm"
                                onClick={() => handleQuickAddCategory(day, name)}
                                className={`text-xs ${bgColor} ${textColor}`}
                              >
                                {name}
                              </Button>
                            ))}
                          </div>
                        </div>

                        <Button
                          variant="outline"
                          onClick={() => handleAddCategory(day)}
                          className="w-full"
                        >
                          <Plus className="w-4 h-4 mr-2" />
                          직접 입력
                        </Button>
                      </TabsContent>
                    );
                  })}
                </Tabs>
              </CardContent>
            </Card>
          </DrawerContent>
        </Drawer>

        {/* Desktop Itinerary Panel - Fixed Left */}
        <div className="hidden md:block md:w-96 md:flex-shrink-0 h-full">
          <Card className="flex flex-col bg-white shadow-2xl border-2 h-full">
            <CardHeader className="flex-shrink-0 p-4 md:ps-6 border-b">
              <CardTitle className="flex items-center justify-between text-base md:text-lg">
                <div className="flex items-center gap-1">
                  일정 계획
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <button className="text-muted-foreground hover:text-foreground">
                        <HelpCircle className="w-3.5 h-3.5" />
                      </button>
                    </TooltipTrigger>
                    <TooltipContent side="bottom" className="max-w-xs">
                      <p>카테고리와 일정을 드래그하여 순서를 변경할 수 있습니다</p>
                    </TooltipContent>
                  </Tooltip>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setIsWishlistOpen(!isWishlistOpen)}
                  className="text-primary hover:bg-primary/10 gap-0 h-fit"
                >
                  <Star className="w-2 h-2" />
                  <ChevronRight className="w-2 h-2" />
                </Button>
              </CardTitle>
            </CardHeader>
            <CardContent className="flex-1 overflow-y-auto p-4 md:p-6 min-h-0 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
              <Tabs value={String(selectedDay)} onValueChange={(val) => setSelectedDay(Number(val))}>
                <TabsList className="grid justify-between overflow-x-scroll h-auto [&::-webkit-scrollbar]:hidden" style={{ gridTemplateColumns: `repeat(${totalDays}, minmax(60px, 1fr))` }}>
                  {Array.from({ length: totalDays }, (_, i) => i + 1).map(day => (
                    <TabsTrigger key={day} value={String(day)} className="text-xs md:text-sm flex flex-col items-center gap-0.5 py-1">
                      <span>Day {day}</span>
                      <span className="text-[10px] md:text-xs text-muted-foreground">{getDateForDay(day)}</span>
                    </TabsTrigger>
                  ))}
                </TabsList>

                {Array.from({ length: totalDays }, (_, i) => i + 1).map((day) => {
                  const dayCategories = categories.filter(cat => cat.day === day);

                  return (
                    <TabsContent key={day} value={String(day)} className="mt-3 md:mt-4 space-y-3 md:space-y-4">
                      <DndContext
                        sensors={sensors}
                        collisionDetection={closestCenter}
                        onDragEnd={(event) => handleCategoryDragEnd(event, day)}
                      >
                        <SortableContext
                          items={dayCategories.map(cat => cat.categoryId)}
                          strategy={verticalListSortingStrategy}
                        >
          <div className="space-y-3 md:space-y-4">
            {dayCategories.map((category, categoryIndex) => (
              <SortableCategory
                key={category.categoryId}
                category={category}
                categoryIndex={categoryIndex}
                categoriesArray={dayCategories}
                day={day}
                categories={categories}
                dropTargetCategoryId={dropTargetCategoryId}
                draggedItinerary={draggedItinerary}
                dragOverItemId={dragOverItemId}
                mapRef={mapRef}
                findRoutesForItems={findRoutesForItems}
                handleCategoryDragOver={handleCategoryDragOver}
                handleDragOver={handleDragOver}
                handleDragLeave={handleDragLeave}
                handleDrop={handleDrop}
                handleUpdateCategoryName={handleUpdateCategoryName}
                handleDeleteCategory={handleDeleteCategory}
                handleItineraryDragStart={handleItineraryDragStart}
                handleItineraryDragOver={handleItineraryDragOver}
                handleDragEnd={handleDragEnd}
                setExpenseModal={setExpenseModal}
                setEditingItinerary={setEditingItinerary}
                deleteItineraryMutation={deleteItineraryMutation}
                setExpenseDetailModal={setExpenseDetailModal}
                setExpenseListModal={setExpenseListModal}
                handleMoveToCategory={handleMoveToCategory}
                expensesByItinerary={expensesByItinerary}
                isMobile={isMobile}
                onMoveItemUp={handleMoveItemUp}
                onMoveItemDown={handleMoveItemDown}
              />
            ))}
          </div>
                        </SortableContext>
                      </DndContext>

                      <Button
                        variant="outline"
                        onClick={() => {
                          const date = new Date(tripDetail.startDate);
                          date.setDate(date.getDate() + (day - 1));
                          setDailySettlementModal({
                            isOpen: true,
                            date: date.toISOString().split('T')[0]
                          });
                        }}
                        className="w-full border-primary text-primary hover:bg-primary hover:text-primary-foreground"
                      >
                        <Calculator className="w-4 h-4 mr-2" />
                        일별 정산 보기
                      </Button>

                      <div className="space-y-2">
                        <p className="text-xs text-muted-foreground font-medium">빠른 카테고리 추가</p>
                        <div className="flex flex-wrap gap-2">
                          {[
                            { name: '식사', bgColor: 'bg-rose-100 hover:bg-rose-200 dark:bg-rose-950 dark:hover:bg-rose-900', textColor: 'text-rose-700 dark:text-rose-300' },
                            { name: '숙소', bgColor: 'bg-amber-100 hover:bg-amber-200 dark:bg-amber-950 dark:hover:bg-amber-900', textColor: 'text-amber-700 dark:text-amber-300' },
                            { name: '관광', bgColor: 'bg-emerald-100 hover:bg-emerald-200 dark:bg-emerald-950 dark:hover:bg-emerald-900', textColor: 'text-emerald-700 dark:text-emerald-300' },
                            { name: '쇼핑', bgColor: 'bg-sky-100 hover:bg-sky-200 dark:bg-sky-950 dark:hover:bg-sky-900', textColor: 'text-sky-700 dark:text-sky-300' },
                            { name: '액티비티', bgColor: 'bg-purple-100 hover:bg-purple-200 dark:bg-purple-950 dark:hover:bg-purple-900', textColor: 'text-purple-700 dark:text-purple-300' },
                          ].map(({ name, bgColor, textColor }) => (
                            <Button
                              key={name}
                              variant="ghost"
                              size="sm"
                              onClick={() => handleQuickAddCategory(day, name)}
                              className={`text-xs ${bgColor} ${textColor}`}
                            >
                              {name}
                            </Button>
                          ))}
                        </div>
                      </div>

                      <Button
                        variant="outline"
                        onClick={() => handleAddCategory(day)}
                        className="w-full"
                      >
                        <Plus className="w-4 h-4 mr-2" />
                        직접 입력
                      </Button>
                    </TabsContent>
                  );
                })}
              </Tabs>
            </CardContent>
          </Card>
        </div>

        {/* Map - Right Side */}
        <div className="relative flex-1 h-full min-h-[300px]">
          <ItineraryMap
            key={selectedDay}
            ref={mapRef}
            categories={categories.filter(cat => cat.day === selectedDay)}
            getCategoryColor={getCategoryColor}
            wishlistItems={wishlistItems}
            tripCountry={tripDetail?.country}
            onAddToItinerary={handleAddWishlistToItinerary}
            onDeleteItinerary={handleDeleteItineraryFromMap}
            onDeleteWishlist={handleDeleteWishlistFromMap}
          />
          
          {/* Floating Button to Open Itinerary Drawer (Mobile Only) */}
          <Button
            onClick={() => setIsItineraryDrawerOpen(true)}
            className="md:hidden fixed bottom-4 left-1/2 -translate-x-1/2 z-20 shadow-lg"
            size="lg"
          >
            <Calendar className="w-5 h-5 mr-2" />
            일정 보기
          </Button>
        </div>

        {/* Mobile Wishlist Drawer */}
        <Drawer open={isWishlistDrawerOpen} onOpenChange={setIsWishlistDrawerOpen}>
          <DrawerContent className="md:hidden h-[85vh] flex flex-col">
            <Card className="flex flex-col bg-white border-0 flex-1 overflow-hidden">
              <CardHeader className="flex-shrink-0 p-4 border-b space-y-3">
                <CardTitle className="flex items-center justify-between text-base">
                  <div className="flex items-center gap-2">
                    <span>위시리스트</span>
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent className="flex-1 overflow-y-auto space-y-2 p-4 min-h-0">
                <div className="relative">
                  <Input
                      placeholder="내 위시리스트 검색..."
                      value={wishlistSearchInput}
                      onChange={(e) => setWishlistSearchInput(e.target.value)}
                      className="h-9 text-sm"
                  />
                </div>
                <Button
                  onClick={() => {
                    setIsWishlistDrawerOpen(false);
                    setShowPlaceSearchModal(true);
                  }}
                  className="w-full"
                  size="sm"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  새로운 장소 추가
                </Button>
                {wishlistItems.map((item) => (
                  <div
                    key={item.wishlistItemId}
                    draggable
                    onDragStart={() => handleDragStart(item)}
                    onDragEnd={handleDragEnd}
                    className={`p-2 bg-gradient-subtle rounded-lg border transition-all duration-200 cursor-pointer ${
                      draggedItem?.wishlistItemId === item.wishlistItemId
                        ? 'opacity-50 scale-95'
                        : 'hover:shadow-soft hover:scale-102'
                    }`}
                    onClick={() => {
                      mapRef.current?.flyToWishlistMarker(item.wishlistItemId);
                      setIsWishlistDrawerOpen(false);
                    }}
                  >
                    <div className="flex-col items-start justify-between gap-2">
                      <div className="flex items-center gap-2 mb-2">
                        <Avatar className="h-6 w-6 border">
                          <AvatarImage
                              className="object-cover"
                              src={item.adder.avatar || undefined}
                              alt={item.adder.nickname} />
                          <AvatarFallback className="text-xs bg-primary/10 text-primary">
                            {item.adder.nickname.charAt(0).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        <span className="text-xs text-muted-foreground">{item.adder.nickname}</span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <h5 className="font-medium text-foreground text-xs truncate">
                          {item.name}
                        </h5>
                        <div className="text-xs text-muted-foreground">
                          <p className="truncate">{item.address || "주소 정보 없음"}</p>
                        </div>
                      </div>
                      <div className="flex gap-1 flex-shrink-0 justify-end">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-8 w-8 p-0"
                          onClick={(e) => {
                            e.stopPropagation();
                            const url = getGoogleMapsSearchUrl(item.name);
                            openGoogleMaps(url);
                          }}
                          title="구글 맵에서 보기"
                        >
                          <ExternalLink className="w-3 h-3" />
                        </Button>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0"
                            >
                              <MoveRight className="w-3 h-3" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <div className="px-2 py-1.5 text-xs font-medium text-muted-foreground">
                              카테고리로 이동
                            </div>
                            <DropdownMenuSeparator />
                            <div className="overflow-auto max-h-[20vh]">
                              {categories
                                  .map(category => (
                                      <DropdownMenuItem
                                          key={category.categoryId}
                                          onClick={() => {
                                            addItineraryMutation.mutate({
                                              categoryId: category.categoryId,
                                              data: {
                                                placeId: Number(item.placeId) || null,
                                                title: null,
                                                time: null,
                                                memo: null,
                                              }
                                            });
                                          }}
                                      >
                                        <span>Day{category.day}: {category.name}</span>
                                      </DropdownMenuItem>
                                  ))}
                              {categories.length === 0 && (
                                  <div className="px-2 py-1.5 text-xs text-muted-foreground">
                                    카테고리를 먼저 추가하세요
                                  </div>
                              )}
                            </div>
                          </DropdownMenuContent>
                        </DropdownMenu>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteWishlistMutation.mutate(item.wishlistItemId);
                          }}
                          className="text-destructive hover:text-destructive-foreground hover:bg-destructive h-8 w-8 p-0"
                        >
                          <Trash2 className="w-3 h-3" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
                {wishlistItems.length === 0 && (
                  <div className="text-center text-muted-foreground py-6">
                    <Star className="w-6 h-6 mx-auto mb-2 opacity-50" />
                    <p className="text-sm">위시리스트가 비어있습니다</p>
                    <p className="text-xs">장소를 추가해보세요!</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </DrawerContent>
        </Drawer>

        {/* Desktop Wishlist Panel - Slides from Itinerary Panel */}
        <div className={`hidden md:block absolute left-96 top-0 bottom-0 h-full transition-all duration-300 ease-in-out overflow-hidden z-10 ${isWishlistOpen ? "w-80" : "w-0"}`}>
          <div className="w-80 h-full">
            <Card className="h-full flex flex-col bg-white shadow-2xl border-2">
              <CardHeader className="flex-shrink-0 ps-6 p-4 border-b space-y-3">
                <CardTitle className="flex items-center justify-between text-lg">
                  <div className="flex items-center gap-2">
                    <span>위시리스트</span>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <button className="text-muted-foreground hover:text-foreground">
                          <HelpCircle className="w-4 h-4" />
                        </button>
                      </TooltipTrigger>
                      <TooltipContent side="right" className="max-w-xs">
                        <p>위시리스트 항목을 왼쪽 일정 카드로 드래그하여 일정에 추가할 수 있습니다</p>
                      </TooltipContent>
                    </Tooltip>
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent className="flex-1 overflow-y-auto space-y-3 p-6 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
                <div className="relative">
                  <Input
                      placeholder="내 위시리스트 검색..."
                      value={wishlistSearchInput}
                      onChange={(e) => setWishlistSearchInput(e.target.value)}
                      className="h-9 text-sm"
                  />
                </div>
                <Button
                  onClick={() => setShowPlaceSearchModal(true)}
                  className="w-full"
                  size="sm"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  새로운 장소 추가
                </Button>
                {wishlistItems.map((item) => (
                  <div
                    key={item.wishlistItemId}
                    draggable
                    onDragStart={() => handleDragStart(item)}
                    onDragEnd={handleDragEnd}
                    className={`p-3 bg-gradient-subtle rounded-lg border transition-all duration-200 cursor-pointer ${
                      draggedItem?.wishlistItemId === item.wishlistItemId
                        ? 'opacity-50 scale-95'
                        : 'hover:shadow-soft hover:scale-102'
                    }`}
                    onClick={() => {
                      mapRef.current?.flyToWishlistMarker(item.wishlistItemId);
                    }}
                  >
                    <div className="flex-col items-start justify-between gap-2">
                      <div className="flex items-center gap-2 mb-2">
                        <Avatar className="h-6 w-6 border">
                          <AvatarImage
                              className="object-cover"
                              src={item.adder.avatar || undefined}
                              alt={item.adder.nickname} />
                          <AvatarFallback className="text-xs bg-primary/10 text-primary">
                            {item.adder.nickname.charAt(0).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        <span className="text-xs text-muted-foreground">{item.adder.nickname}</span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <h5 className="font-medium text-foreground text-sm mb-1 truncate">
                          {item.name}
                        </h5>
                        <div className="text-xs text-muted-foreground">
                          <p className="truncate">{item.address || "주소 정보 없음"}</p>
                        </div>
                      </div>
                      <div className="flex gap-1 flex-shrink-0 justify-end">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-8 w-8 p-0"
                          onClick={(e) => {
                            e.stopPropagation();
                            const url = getGoogleMapsSearchUrl(item.name);
                            openGoogleMaps(url);
                          }}
                          title="구글 맵에서 보기"
                        >
                          <ExternalLink className="w-4 h-4" />
                        </Button>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0"
                            >
                              <MoveRight className="w-4 h-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <div className="px-2 py-1.5 text-xs font-medium text-muted-foreground">
                              카테고리로 이동
                            </div>
                            <DropdownMenuSeparator />
                            <div className="overflow-auto max-h-[20vh]">
                              {categories
                                  .map(category => (
                                      <DropdownMenuItem
                                          key={category.categoryId}
                                          onClick={() => {
                                            addItineraryMutation.mutate({
                                              categoryId: category.categoryId,
                                              data: {
                                                placeId: Number(item.placeId) || null,
                                                title: null,
                                                time: null,
                                                memo: null,
                                              }
                                            });
                                          }}
                                      >
                                        <span>{category.name} (Day {category.day})</span>
                                      </DropdownMenuItem>
                                  ))}
                              {categories.length === 0 && (
                                  <div className="px-2 py-1.5 text-xs text-muted-foreground">
                                    카테고리를 먼저 추가하세요
                                  </div>
                              )}
                            </div>
                          </DropdownMenuContent>
                        </DropdownMenu>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteWishlistMutation.mutate(item.wishlistItemId);
                          }}
                          className="text-destructive hover:text-destructive-foreground hover:bg-destructive h-8 w-8 p-0"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
                {wishlistItems.length === 0 && (
                  <div className="text-center text-muted-foreground py-8">
                    <Star className="w-8 h-8 mx-auto mb-2 opacity-50" />
                    <p className="text-base">위시리스트가 비어있습니다</p>
                    <p className="text-xs">장소를 추가해보세요!</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </main>

      {/* Floating Trip Chat Button */}
      {tripDetail && memberInfo && (
        <Button
          size="icon"
          className="fixed bottom-5 right-5 z-30 rounded-full w-14 h-14 shadow-lg"
          onClick={() => setIsChatOpen(true)}
        >
          <MessageCircle className="w-6 h-6" />
          <span className="sr-only">채팅 열기</span>
        </Button>
      )}

      {/* Trip Chat Modal */}
      <TripChatModal
        open={isChatOpen}
        onOpenChange={setIsChatOpen}
        tripId={Number(tripId)}
        tripTitle={tripDetail?.title}
        currentMemberId={memberInfo?.memberId ?? null}
        myTripMemberId={myTripMemberId}
      />

      {/* Itinerary Create Modal */}
      <ItineraryCreateModal
        isOpen={createItineraryModal.isOpen}
        onClose={() => setCreateItineraryModal({ isOpen: false, categoryId: 0, categoryName: "" })}
        onCreateItinerary={(data) => {
          addItineraryMutation.mutate({
            categoryId: createItineraryModal.categoryId,
            data,
          });
        }}
        categoryName={createItineraryModal.categoryName}
        tripStartDate={tripDetail?.startDate || ""}
        currentDay={selectedDay}
      />

      {/* Expense Modal */}
      <ExpenseModal
        isOpen={expenseModal.isOpen}
        onClose={() => setExpenseModal({ isOpen: false, itineraryItem: null })}
        onSave={(expenseData: any, imageFile?: File) => {
          createExpenseMutation.mutate({ 
            expenseData,
            imageFile
          });
        }}
        onUpdate={(expenseId: number, expenseData: any) => {
          updateExpenseMutation.mutate({
            expenseId,
            expenseData
          });
        }}
        selectedItem={expenseModal.itineraryItem}
        tripId={Number(tripId)}
        tripMembers={tripDetail?.members || []}
        editMode={!!expenseModal.editingExpense}
        existingExpense={expenseModal.editingExpense}
        defaultCurrency={tripDetail ? getDefaultCurrencyByCountry(tripDetail.country) : 'KRW'}
      />

      {/* Place Search Modal */}
      <PlaceSearchModal
        isOpen={showPlaceSearchModal}
        onClose={() => setShowPlaceSearchModal(false)}
        onAddPlace={(place) => addWishlistMutation.mutate(place)}
        region={tripDetail.country}
      />

      {/* AI Review Modal */}
      <TripReviewModal
        open={showReviewModal}
        onOpenChange={setShowReviewModal}
        tripId={Number(tripId)}
      />

      {/* Expense Detail Modal */}
      <ExpenseDetailModal
        isOpen={expenseDetailModal.isOpen}
        onClose={() => setExpenseDetailModal({ isOpen: false, expenseId: null })}
        expense={expenseDetail || null}
        isLoading={isLoadingExpense}
        onEdit={(expense) => {
          // Close detail modal and open edit modal
          setExpenseDetailModal({ isOpen: false, expenseId: null });
          
          // Find the itinerary item for this expense by searching through categories
          const itineraryItem = categories
            .flatMap(cat => cat.itineraryItems)
            .find(item => item.itineraryId === expense.expenseId); // This needs to be adjusted based on actual data structure
          
          if (itineraryItem) {
            setExpenseModal({
              isOpen: true,
              itineraryItem: itineraryItem,
              editingExpense: {
                expenseId: expense.expenseId,
                expenseTitle: expense.expenseTitle,
                totalAmount: expense.totalAmount,
                items: expense.items,
                payerId: expense.payer.id,
                currency: expense.currency
              }
            });
          } else {
            // Fallback: create a minimal itinerary item for editing
            setExpenseModal({
              isOpen: true,
              itineraryItem: {
                itineraryId: expense.expenseId,
                name: expense.expenseTitle,
              } as any,
              editingExpense: {
                expenseId: expense.expenseId,
                expenseTitle: expense.expenseTitle,
                totalAmount: expense.totalAmount,
                items: expense.items,
                payerId: expense.payer.id,
                currency: expense.currency
              }
            });
          }
        }}
        onDelete={(expenseId) => {
          deleteExpenseMutation.mutate(expenseId);
        }}
      />

      {/* Expense List Modal */}
      <ExpenseListModal
        isOpen={expenseListModal.isOpen}
        onClose={() => setExpenseListModal({ isOpen: false, expenses: [], itineraryTitle: '' })}
        expenses={expenseListModal.expenses}
        itineraryTitle={expenseListModal.itineraryTitle}
        onSelectExpense={(expenseId) => {
          setExpenseListModal({ isOpen: false, expenses: [], itineraryTitle: '' });
          setExpenseDetailModal({ isOpen: true, expenseId });
        }}
      />

      {/* Daily Settlement Modal */}
      <DailySettlementModal
        isOpen={dailySettlementModal.isOpen}
        onClose={() => setDailySettlementModal({ isOpen: false, date: null })}
        settlement={dailySettlement || null}
        isLoading={isLoadingSettlement}
        onViewExpenseDetail={(expenseId) => {
          setExpenseDetailModal({ isOpen: true, expenseId });
        }}
      />

      {/* Total Settlement Modal */}
      <TotalSettlementModal
        isOpen={totalSettlementModal}
        onClose={() => setTotalSettlementModal(false)}
        settlement={totalSettlement || null}
        isLoading={isLoadingTotalSettlement}
      />

      {/* Edit Itinerary Dialog */}
      <Dialog open={!!editingItinerary} onOpenChange={(open) => !open && setEditingItinerary(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>일정 수정</DialogTitle>
            <DialogDescription>
              메모와 시간을 수정할 수 있습니다
            </DialogDescription>
          </DialogHeader>
          
          {editingItinerary && (
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium mb-2 block">시간</label>
                <Input
                  type="datetime-local"
                  value={editingItinerary.time}
                  onChange={(e) => setEditingItinerary({
                    ...editingItinerary,
                    time: e.target.value
                  })}
                />
              </div>
              <div>
                <label className="text-sm font-medium mb-2 block">메모</label>
                <Input
                  value={editingItinerary.memo}
                  onChange={(e) => setEditingItinerary({
                    ...editingItinerary,
                    memo: e.target.value
                  })}
                  placeholder="메모를 입력하세요"
                />
              </div>
              <div className="flex justify-end space-x-2">
                <Button
                  variant="outline"
                  onClick={() => setEditingItinerary(null)}
                >
                  취소
                </Button>
                <Button
                  onClick={() => {
                    updateItineraryMutation.mutate({
                      categoryId: editingItinerary.categoryId,
                      itemId: editingItinerary.itemId,
                      updates: {
                        time: editingItinerary.time || undefined,
                        memo: editingItinerary.memo || undefined
                      }
                    });
                  }}
                >
                  저장
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Invite Link Dialog */}
      <Dialog open={showInviteDialog} onOpenChange={setShowInviteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>친구 초대하기</DialogTitle>
            <DialogDescription>
              아래 링크를 복사하여 친구에게 공유하세요
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4">
            <div className="flex gap-2">
              <Input
                value={inviteLink}
                readOnly
                className="flex-1"
              />
              <Button
                onClick={handleCopyInviteLink}
                variant="outline"
                className="min-w-[100px]"
              >
                {isCopied ? (
                  <>
                    <Check className="w-4 h-4 mr-2" />
                    복사됨
                  </>
                ) : (
                  <>
                    <Copy className="w-4 h-4 mr-2" />
                    복사
                  </>
                )}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Trip Members Modal */}
      <TripMembersModal
        isOpen={showMembersModal}
        onClose={() => setShowMembersModal(false)}
        tripId={Number(tripId)}
      />
    </div>
  );
};

export default TripPlanner;
