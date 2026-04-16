import {useCallback, useEffect, useState} from "react";
import {Loader2, Plus, Search} from "lucide-react";
import {Dialog, DialogContent, DialogHeader, DialogTitle,} from "@/components/ui/dialog";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {placesApi, PlaceSearchResult} from "@/api/places";
import {Badge} from "@/components/ui/badge";
import {useToast} from "@/hooks/use-toast";
import {buildPlaceSearchQuery, getCountryOptionByCode2, getTripRegionByCode, getTripRegionLabel} from "@/lib/tripRegions";
import {getPlacePhotoUrl, getPlaceTypeLabel} from "@/lib/placePresentation";
import {readApiErrorMessage} from "@/api/http";

interface PlaceSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAddPlace: (place: PlaceSearchResult) => void;
  countryCode?: string;
  regionCode?: string | null;
}

export const PlaceSearchModal = ({ isOpen, onClose, onAddPlace, countryCode, regionCode }: PlaceSearchModalProps) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [places, setPlaces] = useState<PlaceSearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const { toast } = useToast();
  const region = getTripRegionByCode(regionCode);
  const regionLabel = getTripRegionLabel(regionCode);
  const resolvedCountryCode = getCountryOptionByCode2(countryCode)?.code2;
  const searchPlaces = useCallback(async () => {
    if (!searchQuery.trim()) return;
    
    setIsLoading(true);
    try {
      const [latitude, longitude] = region
        ? [region.centerLat, region.centerLng]
        : [undefined, undefined];
      const results = await placesApi.searchPlaces(
        buildPlaceSearchQuery(searchQuery, regionCode),
        resolvedCountryCode,
        latitude,
        longitude,
        50000,
        region ? `region:${region.code}` : resolvedCountryCode ? `country:${resolvedCountryCode}` : undefined,
      );
      setPlaces(results);
    } catch (error) {
      toast({
        title: "검색 실패",
        description: readApiErrorMessage(error, "장소 검색 중 오류가 발생했습니다."),
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  }, [region, regionCode, resolvedCountryCode, searchQuery, toast]);

  useEffect(() => {
    if (searchQuery.trim()) {
      const timer = setTimeout(() => {
        searchPlaces();
      }, 500);
      return () => clearTimeout(timer);
    } else {
      setPlaces([]);
    }
  }, [searchPlaces, searchQuery]);

  const handleAddPlace = (place: PlaceSearchResult) => {
    onAddPlace(place);
    setSearchQuery("");
    setPlaces([]);
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
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
              onChange={(e) => setSearchQuery(e.target.value)}
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

            {!isLoading && places.map((place) => (
              <div
                key={place.externalPlaceId}
                className="p-4 bg-gradient-subtle rounded-lg border hover:shadow-soft transition-all duration-200"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex flex-1 gap-4 min-w-0">
                    {getPlacePhotoUrl(place.photoHint) && (
                      <img
                        src={getPlacePhotoUrl(place.photoHint) || undefined}
                        alt={place.placeName}
                        className="h-16 w-16 rounded-lg object-cover border shrink-0"
                      />
                    )}
                    <div className="min-w-0 flex-1">
                      <h5 className="font-medium text-foreground mb-2 truncate">{place.placeName}</h5>
                      <div className="flex flex-wrap items-center gap-2 mb-2">
                        {getPlaceTypeLabel(place.placeTypeSummary, place.normalizedCategoryKey) && (
                          <Badge variant="secondary" className="font-medium">
                            {getPlaceTypeLabel(place.placeTypeSummary, place.normalizedCategoryKey)}
                          </Badge>
                        )}
                        {typeof place.placeDetailSummary?.rating === "number" && (
                          <Badge variant="outline" className="font-medium">
                            평점 {place.placeDetailSummary.rating.toFixed(1)}
                          </Badge>
                        )}
                      </div>
                      <div className="text-sm text-muted-foreground">
                        <p className="line-clamp-2">{place.address}</p>
                        {place.placeDetailSummary?.editorialSummary && (
                          <p className="line-clamp-2 mt-1">{place.placeDetailSummary.editorialSummary}</p>
                        )}
                      </div>
                    </div>
                  </div>
                  <Button
                    onClick={() => handleAddPlace(place)}
                    className="bg-gradient-primary"
                  >
                    <Plus className="w-4 h-4 mr-2" />
                    추가
                  </Button>
                </div>
              </div>
            ))}
            
            {!isLoading && searchQuery && places.length === 0 && (
              <div className="text-center text-muted-foreground py-8">
                <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>검색 결과가 없습니다</p>
                <p className="text-sm">다른 키워드로 검색해보세요</p>
              </div>
            )}

            {!isLoading && !searchQuery && (
              <div className="text-center text-muted-foreground py-8">
                <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>장소를 검색해보세요</p>
                <p className="text-sm">장소명이나 주소를 입력하세요</p>
              </div>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};
