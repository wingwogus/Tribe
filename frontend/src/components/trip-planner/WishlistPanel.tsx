import type {ReactNode} from "react";
import {ChevronDown, Globe2, Loader2, Plus, Search, Star, X} from "lucide-react";
import type {MemberWishlistItem, TripWishlistSort, WishlistItem} from "@/api/wishlist";
import {Button} from "@/components/ui/button";
import {Card} from "@/components/ui/card";
import {PlacePreviewCard} from "@/components/place/PlacePreviewCard";
import {getPlaceTypeLabelFromKey} from "@/lib/placePresentation";
import {cn} from "@/lib/utils";

export type WishlistSource = "trip" | "member";

export type WishlistPanelItem =
  | { source: "trip"; item: WishlistItem }
  | { source: "member"; item: MemberWishlistItem };

interface WishlistPanelProps {
  source: WishlistSource;
  onSourceChange: (source: WishlistSource) => void;
  items: WishlistPanelItem[];
  searchValue: string;
  onSearchChange: (value: string) => void;
  filterKeys: string[];
  selectedFilter: string;
  onFilterChange: (filter: string) => void;
  sort: TripWishlistSort;
  onSortChange: (sort: TripWishlistSort) => void;
  draggedItemId?: number | null;
  isSearchOpen?: boolean;
  isLoading?: boolean;
  errorMessage?: string | null;
  addingMemberWishlistItemId?: number | null;
  onOpenSearch: () => void;
  onCloseSearch: () => void;
  onSelectTripItem: (item: WishlistItem) => void;
  onDragStartTripItem: (item: WishlistItem) => void;
  onDragEnd: () => void;
  onAddMemberItem: (item: MemberWishlistItem) => void;
}

export const WishlistPanel = ({
  source,
  onSourceChange,
  items,
  searchValue,
  onSearchChange,
  filterKeys,
  selectedFilter,
  onFilterChange,
  sort,
  onSortChange,
  draggedItemId,
  isSearchOpen = false,
  isLoading = false,
  errorMessage = null,
  addingMemberWishlistItemId = null,
  onOpenSearch,
  onCloseSearch,
  onSelectTripItem,
  onDragStartTripItem,
  onDragEnd,
  onAddMemberItem,
}: WishlistPanelProps) => (
  <Card className="flex h-full flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-2xl">
    <div className="flex-shrink-0 border-b border-gray-100 bg-white p-5 pb-4">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-xl font-bold tracking-tight text-slate-900">위시리스트</h2>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-700"
          onClick={isSearchOpen ? onCloseSearch : onOpenSearch}
        >
          {isSearchOpen ? <X className="h-5 w-5" /> : <Search className="h-4 w-4" />}
          <span className="sr-only">{isSearchOpen ? "장소 검색 닫기" : "장소 검색 열기"}</span>
        </Button>
      </div>

      <div className="mb-5 flex rounded-full bg-slate-100 p-1 shadow-inner">
        <WishlistSourceButton
          isActive={source === "trip"}
          onClick={() => onSourceChange("trip")}
        >
          여행 위시리스트
        </WishlistSourceButton>
        <WishlistSourceButton
          isActive={source === "member"}
          onClick={() => onSourceChange("member")}
          icon={<Globe2 className="h-4 w-4" />}
        >
          내 위시리스트
        </WishlistSourceButton>
      </div>

      <label className="sr-only" htmlFor="wishlist-panel-search">
        위시리스트 검색
      </label>
      <input
        id="wishlist-panel-search"
        value={searchValue}
        onChange={(event) => onSearchChange(event.target.value)}
        className="sr-only"
        tabIndex={-1}
      />

      <div className="relative">
        <select
          value={selectedFilter}
          onChange={(event) => onFilterChange(event.target.value)}
          className="h-11 w-full appearance-none rounded-lg border border-slate-200 bg-white px-4 pr-10 text-sm font-medium text-slate-800 shadow-none outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
        >
          {(filterKeys.length > 0 ? filterKeys : ["ALL"]).map((filterKey) => (
            <option key={filterKey} value={filterKey}>
              {filterKey === "ALL" ? "카테고리: 전체" : `카테고리: ${getPlaceTypeLabelFromKey(filterKey)}`}
            </option>
          ))}
        </select>
        <ChevronDown className="pointer-events-none absolute right-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
      </div>

      <div className="mt-4 flex gap-2">
        <SortButton
          isActive={sort === "review_count_desc"}
          onClick={() => onSortChange("review_count_desc")}
        >
          리뷰 많은순
        </SortButton>
        <SortButton
          isActive={sort === "like_count_desc"}
          disabled={source === "member"}
          onClick={() => onSortChange("like_count_desc")}
        >
          좋아요 많은순
        </SortButton>
      </div>
    </div>

    <div className="min-h-0 flex-1 space-y-4 overflow-y-auto bg-slate-50/50 p-5 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      {isLoading ? (
        <PanelMessage icon={<Loader2 className="h-7 w-7 animate-spin text-slate-300" />} title="위시리스트 불러오는 중..." />
      ) : errorMessage ? (
        <PanelMessage icon={<Star className="h-7 w-7 text-slate-300" />} title="위시리스트를 불러오지 못했습니다" description={errorMessage} />
      ) : items.length > 0 ? (
        items.map((entry) => {
          if (entry.source === "trip") {
            const item = entry.item;
            return (
              <PlacePreviewCard
                key={`trip-${item.wishlistItemId}`}
                place={toPreviewPlace(item)}
                draggable
                isDragging={draggedItemId === item.wishlistItemId}
                onSelect={() => onSelectTripItem(item)}
                onDragStart={() => onDragStartTripItem(item)}
                onDragEnd={onDragEnd}
              />
            );
          }

          const item = entry.item;
          const isAdding = addingMemberWishlistItemId === item.memberWishlistItemId;
          return (
            <PlacePreviewCard
              key={`member-${item.memberWishlistItemId}`}
              place={toPreviewPlace(item)}
              action={
                <Button
                  type="button"
                  size="icon"
                  disabled={isAdding}
                  className="mt-1 h-9 w-9 rounded-full bg-primary text-white shadow-md shadow-primary/20 hover:bg-primary/90"
                  onClick={() => onAddMemberItem(item)}
                >
                  {isAdding ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-5 w-5" />}
                  <span className="sr-only">{item.name} 여행 위시리스트에 추가</span>
                </Button>
              }
            />
          );
        })
      ) : (
        <PanelMessage
          icon={<Star className="h-7 w-7 text-slate-300" />}
          title={source === "trip" ? "여행 위시리스트가 비어있습니다" : "내 위시리스트가 비어있습니다"}
          description={source === "trip" ? "장소 검색에서 새 장소를 추가해보세요." : "저장한 개인 장소가 여기에 표시됩니다."}
        />
      )}
    </div>
  </Card>
);

const WishlistSourceButton = ({
  isActive,
  onClick,
  icon,
  children,
}: {
  isActive: boolean;
  onClick: () => void;
  icon?: ReactNode;
  children: ReactNode;
}) => (
  <button
    type="button"
    className={cn(
      "flex flex-1 items-center justify-center gap-1.5 rounded-full px-4 py-2 text-sm font-semibold transition-colors",
      isActive
        ? "bg-primary font-bold text-white shadow-md"
        : "text-slate-500 hover:bg-white/70",
    )}
    onClick={onClick}
  >
    {icon}
    {children}
  </button>
);

const SortButton = ({
  isActive,
  disabled,
  onClick,
  children,
}: {
  isActive: boolean;
  disabled?: boolean;
  onClick: () => void;
  children: ReactNode;
}) => (
  <Button
    type="button"
    variant="outline"
    disabled={disabled}
    className={cn(
      "h-9 rounded-full px-4 text-xs font-semibold",
      isActive
        ? "border-primary/20 bg-primary/10 text-primary hover:bg-primary/15 hover:text-primary"
        : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50",
      disabled && "opacity-45",
    )}
    onClick={onClick}
  >
    {children}
  </Button>
);

const PanelMessage = ({
  icon,
  title,
  description,
}: {
  icon: ReactNode;
  title: string;
  description?: string;
}) => (
  <div className="rounded-xl border border-dashed border-slate-200 bg-white px-5 py-10 text-center text-slate-500">
    <div className="mb-2 flex justify-center">{icon}</div>
    <p className="text-sm font-semibold text-slate-700">{title}</p>
    {description && <p className="mt-1 text-xs">{description}</p>}
  </div>
);

const toPreviewPlace = (item: WishlistItem | MemberWishlistItem) => ({
  title: item.name,
  address: item.address,
  placeTypeSummary: item.placeTypeSummary,
  normalizedCategoryKey: item.normalizedCategoryKey,
  placeDetailSummary: item.placeDetailSummary,
  openingSummary: item.openingSummary,
});
