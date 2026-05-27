import {type KeyboardEvent, useCallback, useRef, useState} from "react";
import {Loader2, Plus, Search} from "lucide-react";
import {Dialog, DialogContent, DialogHeader, DialogTitle,} from "@/components/ui/dialog";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {placesApi, PlaceSearchResult} from "@/api/places";
import {useToast} from "@/hooks/use-toast";
import {buildPlaceSearchQuery, getCountryOptionByCode2, getTripRegionByCode, getTripRegionLabel} from "@/lib/tripRegions";
import {readApiErrorMessage} from "@/api/http";
import {PlacePreviewCard} from "@/components/trip-planner/PlacePreviewCard";

interface PlaceSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAddPlace: (place: PlaceSearchResult) => void;
  countryCode?: string;
  regionCode?: string | null;
}

export const PlaceSearchModal = ({ isOpen, onClose, onAddPlace, countryCode, regionCode }: PlaceSearchModalProps) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const [places, setPlaces] = useState<PlaceSearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const searchRequestIdRef = useRef(0);
  const { toast } = useToast();
  const region = getTripRegionByCode(regionCode);
  const regionLabel = getTripRegionLabel(regionCode);
  const resolvedCountryCode = getCountryOptionByCode2(countryCode)?.code2;

  const resetSearch = useCallback(() => {
    searchRequestIdRef.current += 1;
    setSearchQuery("");
    setSubmittedQuery("");
    setPlaces([]);
    setIsLoading(false);
    setErrorMessage(null);
  }, []);

  const searchPlaces = useCallback(async () => {
    const query = searchQuery.trim();
    if (!query) {
      setPlaces([]);
      setIsLoading(false);
      setSubmittedQuery("");
      setErrorMessage(null);
      searchRequestIdRef.current += 1;
      return;
    }
    
    setIsLoading(true);
    setSubmittedQuery(query);
    setErrorMessage(null);
    const requestId = searchRequestIdRef.current + 1;
    searchRequestIdRef.current = requestId;
    try {
      const [latitude, longitude] = region
        ? [region.centerLat, region.centerLng]
        : [undefined, undefined];
      const results = await placesApi.searchPlaces(
        buildPlaceSearchQuery(query, regionCode),
        resolvedCountryCode,
        latitude,
        longitude,
        50000,
        region ? `region:${region.code}` : resolvedCountryCode ? `country:${resolvedCountryCode}` : undefined,
      );
      if (searchRequestIdRef.current !== requestId) {
        return;
      }
      setPlaces(results);
    } catch (error) {
      if (searchRequestIdRef.current !== requestId) {
        return;
      }
      const message = readApiErrorMessage(error, "장소 검색 중 오류가 발생했습니다.");
      setPlaces([]);
      setErrorMessage(message);
      toast({
        title: "검색 실패",
        description: message,
        variant: "destructive",
      });
    } finally {
      if (searchRequestIdRef.current === requestId) {
        setIsLoading(false);
      }
    }
  }, [region, regionCode, resolvedCountryCode, searchQuery, toast]);

  const handleSearchQueryChange = (query: string) => {
    setSearchQuery(query);
    setErrorMessage(null);

    if (query.trim() !== submittedQuery.trim()) {
      setPlaces([]);
      setIsLoading(false);
      setSubmittedQuery("");
      searchRequestIdRef.current += 1;
    }
  };

  const handleSearchKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Enter") {
      event.preventDefault();
      void searchPlaces();
    }
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      resetSearch();
      onClose();
    }
  };

  const handleAddPlace = (place: PlaceSearchResult) => {
    onAddPlace(place);
    resetSearch();
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center">
            <Search className="w-5 h-5 mr-2 text-primary" />
            장소 검색
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 flex-1 overflow-hidden flex flex-col">
          {/* Search Input */}
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={regionLabel ? `${regionLabel} 근처 장소를 검색하세요...` : "장소명이나 카테고리를 검색하세요..."}
              value={searchQuery}
              onChange={(e) => handleSearchQueryChange(e.target.value)}
              onKeyDown={handleSearchKeyDown}
              className="pl-10"
            />
          </div>

          {/* Search Results */}
          <div className="flex-1 overflow-auto space-y-3">
            {isLoading && (
              <div className="text-center text-muted-foreground py-8">
                <Loader2 className="w-8 h-8 mx-auto mb-2 opacity-50 animate-spin" />
                <p>검색 중...</p>
              </div>
            )}

            {!isLoading && errorMessage && (
              <div className="text-center text-muted-foreground py-8">
                <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>검색에 실패했습니다</p>
                <p className="text-sm">{errorMessage}</p>
              </div>
            )}

            {!isLoading && places.map((place) => (
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
                    onClick={() => handleAddPlace(place)}
                    size="icon"
                    className="mt-1 h-9 w-9 rounded-full bg-primary text-white shadow-md shadow-primary/20 hover:bg-primary/90"
                  >
                    <Plus className="w-5 h-5" />
                    <span className="sr-only">{place.placeName} 위시리스트에 추가</span>
                  </Button>
                }
              />
            ))}
            
            {!isLoading && !errorMessage && submittedQuery && places.length === 0 && (
              <div className="text-center text-muted-foreground py-8">
                <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>검색 결과가 없습니다</p>
                <p className="text-sm">다른 키워드를 입력한 뒤 Enter를 눌러보세요</p>
              </div>
            )}

            {!isLoading && !submittedQuery && searchQuery && (
              <div className="text-center text-muted-foreground py-8">
                <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>Enter를 누르면 검색합니다</p>
                <p className="text-sm">키워드를 다 입력한 뒤 Google Places에서 조회합니다</p>
              </div>
            )}

            {!isLoading && !searchQuery && (
              <div className="text-center text-muted-foreground py-8">
                <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>장소를 검색해보세요</p>
                <p className="text-sm">장소명이나 주소를 입력하고 Enter를 누르세요</p>
              </div>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};
