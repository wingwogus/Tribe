import type {KeyboardEvent, ReactNode} from "react";
import {Loader2, Plus, Search, X} from "lucide-react";
import {PlaceSearchResult} from "@/api/places";
import {Button} from "@/components/ui/button";
import {Card} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {PlacePreviewCard} from "@/components/trip-planner/PlacePreviewCard";

interface PlaceSearchPanelProps {
  query: string;
  submittedQuery?: string;
  onQueryChange: (query: string) => void;
  onSearchSubmit: () => void;
  results: PlaceSearchResult[];
  onAddPlace: (place: PlaceSearchResult) => void;
  onClose: () => void;
  isLoading?: boolean;
  addingExternalPlaceId?: string | null;
  errorMessage?: string | null;
}

export const PlaceSearchPanel = ({
  query,
  submittedQuery = "",
  onQueryChange,
  onSearchSubmit,
  results,
  onAddPlace,
  onClose,
  isLoading = false,
  addingExternalPlaceId = null,
  errorMessage = null,
}: PlaceSearchPanelProps) => {
  const hasQuery = query.trim().length > 0;
  const hasSubmittedQuery = submittedQuery.trim().length > 0;
  const handleSearchKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Enter") {
      event.preventDefault();
      onSearchSubmit();
    }
  };

  return (
    <Card className="flex h-full flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-2xl">
      <div className="flex-shrink-0 border-b border-gray-100 bg-white p-5 pb-4">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-xl font-bold tracking-tight text-slate-900">장소 검색</h2>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="h-8 w-8 rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-700"
            onClick={onClose}
          >
            <X className="h-5 w-5" />
            <span className="sr-only">장소 검색 닫기</span>
          </Button>
        </div>

        <div className="relative">
          <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <Input
            placeholder="오사카 맛집, 카페 검색..."
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            onKeyDown={handleSearchKeyDown}
            className="h-11 rounded-lg border-slate-200 bg-slate-50 pl-10 text-sm font-medium shadow-none placeholder:text-slate-500 focus-visible:bg-white"
          />
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto bg-slate-50/50 p-5 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {isLoading ? (
          <PanelMessage icon={<Loader2 className="h-7 w-7 animate-spin text-slate-300" />} title="검색 중..." />
        ) : errorMessage ? (
          <PanelMessage
            icon={<Search className="h-7 w-7 text-slate-300" />}
            title="검색에 실패했습니다"
            description={errorMessage}
          />
        ) : results.length > 0 ? (
          <div className="space-y-4">
            {results.map((place) => (
              <PlacePreviewCard
                key={place.externalPlaceId}
                place={{
                  title: place.placeName,
                  address: place.address,
                  placeTypeSummary: place.placeTypeSummary,
                  normalizedCategoryKey: place.normalizedCategoryKey,
                  placeDetailSummary: place.placeDetailSummary,
                  openingSummary: place.openingSummary,
                }}
                action={
                  <Button
                    type="button"
                    size="icon"
                    disabled={addingExternalPlaceId === place.externalPlaceId}
                    className="mt-1 h-9 w-9 rounded-full bg-primary text-white shadow-md shadow-primary/20 hover:bg-primary/90"
                    onClick={() => onAddPlace(place)}
                  >
                    {addingExternalPlaceId === place.externalPlaceId
                      ? <Loader2 className="h-4 w-4 animate-spin" />
                      : <Plus className="h-5 w-5" />}
                    <span className="sr-only">{place.placeName} 위시리스트에 추가</span>
                  </Button>
                }
              />
            ))}
          </div>
        ) : hasSubmittedQuery ? (
          <PanelMessage
            icon={<Search className="h-7 w-7 text-slate-300" />}
            title="검색 결과가 없습니다"
            description="다른 키워드를 입력한 뒤 Enter를 눌러보세요."
          />
        ) : hasQuery ? (
          <PanelMessage
            icon={<Search className="h-7 w-7 text-slate-300" />}
            title="Enter를 누르면 검색합니다"
            description="키워드를 다 입력한 뒤 Google Places에서 조회합니다."
          />
        ) : (
          <PanelMessage
            icon={<Search className="h-7 w-7 text-slate-300" />}
            title="장소를 검색해보세요"
            description="장소명이나 카테고리를 입력하고 Enter를 누르세요."
          />
        )}
      </div>
    </Card>
  );
};

const PanelMessage = ({
  icon,
  title,
  description,
}: {
  icon: ReactNode;
  title: string;
  description?: string;
}) => (
  <div className="flex h-full min-h-[240px] items-center justify-center text-center text-slate-500">
    <div>
      <div className="mb-2 flex justify-center">{icon}</div>
      <p className="text-sm font-semibold text-slate-700">{title}</p>
      {description && <p className="mt-1 text-xs">{description}</p>}
    </div>
  </div>
);
