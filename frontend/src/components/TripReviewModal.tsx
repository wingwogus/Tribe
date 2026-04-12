import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2, Sparkles, History, Plus, Calendar, ArrowRight, CheckCircle2, MapPin, Star, ListPlus } from "lucide-react";
import { reviewApi, TripReview } from "@/api/reviews";
import { toast } from "@/hooks/use-toast";
import ReactMarkdown from "react-markdown";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { wishlistApi } from "@/api/wishlist";
import { categoryApi } from "@/api/categories";
import { itineraryApi } from "@/api/itinerary";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { PlaceSearchResult } from "@/api/places";
import { tripQueryKeys } from "@/lib/tripQueryKeys";

interface TripReviewModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tripId: number;
}

export const TripReviewModal = ({ open, onOpenChange, tripId }: TripReviewModalProps) => {
  const queryClient = useQueryClient();
  const [concept, setConcept] = useState("");
  const [activeTab, setActiveTab] = useState("new");
  const [selectedReviewId, setSelectedReviewId] = useState<number | null>(null);
  const [newReview, setNewReview] = useState<TripReview | null>(null);
  const [addingToItinerary, setAddingToItinerary] = useState<PlaceSearchResult | null>(null);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>("");

  // 카테고리 목록 조회
  const { data: categories = [] } = useQuery({
    queryKey: tripQueryKeys.categories(tripId),
    queryFn: () => categoryApi.getCategories(tripId),
    enabled: open,
  });

  // 리뷰 목록 조회 (캐싱)
  const { data: reviews = [], isLoading: isLoadingReviews } = useQuery({
    queryKey: tripQueryKeys.reviews(tripId),
    queryFn: () => reviewApi.getAllReviews(tripId),
    enabled: open,
  });

  // 단건 리뷰 조회 (캐싱)
  const { data: selectedReview, isLoading: isLoadingDetail } = useQuery({
    queryKey: tripQueryKeys.review(tripId, selectedReviewId),
    queryFn: () => reviewApi.getReview(tripId, selectedReviewId!),
    enabled: !!selectedReviewId,
  });

  // 리뷰 생성 Mutation
  const createReviewMutation = useMutation({
    mutationFn: (request: { concept?: string }) => 
      reviewApi.createReview(tripId, request),
    onSuccess: (data) => {
      setNewReview(data);
      // 리뷰 목록 캐시 무효화하여 자동 리페치
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.reviews(tripId) });
      toast({
        title: "AI 리뷰 완료",
        description: "여행 일정에 대한 AI 분석이 완료되었습니다.",
      });
    },
    onError: (error) => {
      console.error("Failed to create review:", error);
      toast({
        title: "리뷰 생성 실패",
        description: "AI 리뷰를 생성하는 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setNewReview(null);
    createReviewMutation.mutate({
      concept: concept.trim() || undefined,
    });
  };

  const handleReviewClick = (reviewId: number) => {
    setSelectedReviewId(reviewId);
  };

  const handleClose = () => {
    setConcept("");
    setNewReview(null);
    setSelectedReviewId(null);
    setActiveTab("new");
    setAddingToItinerary(null);
    setSelectedCategoryId("");
    onOpenChange(false);
  };

  // 위시리스트 추가 Mutation
  const addToWishlistMutation = useMutation({
    mutationFn: (place: PlaceSearchResult) => 
      wishlistApi.addWishlist(tripId, {
        externalPlaceId: place.externalPlaceId,
        placeName: place.placeName,
        address: place.address,
        latitude: place.latitude,
        longitude: place.longitude,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.wishlistRoot(tripId) });
      toast({
        title: "위시리스트에 추가됨",
        description: "장소가 위시리스트에 추가되었습니다.",
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

  // 일정에 추가 Mutation
  const addToItineraryMutation = useMutation({
    mutationFn: ({ categoryId, placeId }: { categoryId: number; placeId?: number }) =>
      itineraryApi.createItinerary(tripId, categoryId, {
        placeId: placeId || null,
        title: null,
        time: null,
        memo: null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tripQueryKeys.categories(tripId) });
      toast({
        title: "일정에 추가됨",
        description: "장소가 일정에 추가되었습니다.",
      });
      setAddingToItinerary(null);
      setSelectedCategoryId("");
    },
    onError: () => {
      toast({
        title: "추가 실패",
        description: "일정 추가 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    },
  });

  const handleAddToWishlist = (place: PlaceSearchResult) => {
    addToWishlistMutation.mutate(place);
  };

  const handleAddToItinerary = (place: PlaceSearchResult) => {
    setAddingToItinerary(place);
  };

  const handleConfirmAddToItinerary = () => {
    if (!addingToItinerary || !selectedCategoryId) return;
    addToItineraryMutation.mutate({
      categoryId: parseInt(selectedCategoryId),
      placeId: addingToItinerary.placeId,
    });
  };

  const renderReviewContent = (reviewData: TripReview) => (
    <div className="space-y-6 animate-fade-in">
      {reviewData.concept && (
        <div className="relative overflow-hidden rounded-xl bg-gradient-to-br from-primary/10 via-primary/5 to-transparent border border-primary/20 p-6">
          <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl" />
          <div className="relative flex items-start gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <Sparkles className="w-5 h-5 text-primary" />
            </div>
            <div className="flex-1">
              <p className="text-sm font-medium text-muted-foreground mb-1">분석 콘셉트</p>
              <p className="text-lg font-semibold text-foreground">{reviewData.concept}</p>
            </div>
          </div>
        </div>
      )}

      <div className="prose prose-sm dark:prose-invert max-w-none">
        <ReactMarkdown
          components={{
            h2: ({ children }) => (
              <h2 className="text-2xl font-bold mt-8 mb-4 text-foreground flex items-center gap-2 pb-2 border-b border-border/50">
                <CheckCircle2 className="w-5 h-5 text-primary" />
                {children}
              </h2>
            ),
            h3: ({ children }) => (
              <h3 className="text-xl font-semibold mt-6 mb-3 text-foreground flex items-center gap-2">
                <ArrowRight className="w-4 h-4 text-primary" />
                {children}
              </h3>
            ),
            p: ({ children }) => (
              <p className="mb-4 text-foreground/90 leading-relaxed text-[15px]">{children}</p>
            ),
            ul: ({ children }) => (
              <ul className="mb-4 space-y-2 text-foreground/90">{children}</ul>
            ),
            li: ({ children }) => (
              <li className="flex items-start gap-2 ml-4">
                <span className="text-primary mt-1.5">•</span>
                <span className="flex-1">{children}</span>
              </li>
            ),
            ol: ({ children }) => (
              <ol className="mb-4 space-y-2 text-foreground/90 list-decimal list-inside">{children}</ol>
            ),
            strong: ({ children }) => (
              <strong className="font-semibold text-foreground bg-primary/10 px-1 rounded">{children}</strong>
            ),
            a: ({ href, children }) => (
              <a
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary hover:underline inline-flex items-center gap-1 font-medium"
              >
                {children}
              </a>
            ),
          }}
        >
          {reviewData.content}
        </ReactMarkdown>
      </div>

      {/* 추천 장소 섹션 */}
      {reviewData.recommendedPlaces && reviewData.recommendedPlaces.length > 0 && (
        <div className="mt-8 pt-6 border-t">
          <div className="flex items-center gap-2 mb-4">
            <MapPin className="w-5 h-5 text-primary" />
            <h3 className="text-xl font-bold text-foreground">AI 추천 장소</h3>
          </div>
          <div className="grid gap-3">
            {reviewData.recommendedPlaces.map((place, index) => (
              <Card key={`${place.externalPlaceId}-${index}`} className="overflow-hidden border-2 hover:border-primary/30 transition-all">
                <CardContent className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <h4 className="font-semibold text-foreground mb-1 flex items-center gap-2">
                        <Star className="w-4 h-4 text-amber-500 flex-shrink-0" />
                        {place.placeName}
                      </h4>
                      <p className="text-sm text-muted-foreground truncate">{place.address}</p>
                    </div>
                    <div className="flex gap-2 flex-shrink-0">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleAddToWishlist(place)}
                        disabled={addToWishlistMutation.isPending}
                        className="whitespace-nowrap"
                      >
                        <Star className="w-3.5 h-3.5 mr-1" />
                        위시리스트
                      </Button>
                      <Button
                        size="sm"
                        onClick={() => handleAddToItinerary(place)}
                        disabled={addToItineraryMutation.isPending}
                        className="whitespace-nowrap"
                      >
                        <ListPlus className="w-3.5 h-3.5 mr-1" />
                        일정 추가
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}
    </div>
  );

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-4xl max-h-[85vh] overflow-y-auto">
        <DialogHeader className="space-y-3 pb-6">
          <DialogTitle className="flex items-center gap-3 text-2xl">
            <div className="p-2 rounded-xl bg-gradient-to-br from-primary to-primary/80 shadow-lg">
              <Sparkles className="w-6 h-6 text-primary-foreground" />
            </div>
            AI 여행 일정 분석
          </DialogTitle>
          <DialogDescription className="text-base">
            여행 콘셉트를 입력하면 AI가 현재 일정을 분석하고 개선 방안을 제안합니다.
          </DialogDescription>
        </DialogHeader>

        {!newReview && !selectedReview ? (
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="new" className="flex items-center gap-2">
                <Plus className="w-4 h-4" />
                새 분석
              </TabsTrigger>
              <TabsTrigger value="history" className="flex items-center gap-2">
                <History className="w-4 h-4" />
                이전 분석
              </TabsTrigger>
            </TabsList>

            <TabsContent value="new" className="space-y-6 mt-6">
              <form onSubmit={handleSubmit} className="space-y-6">
                <div className="space-y-3">
                  <Label htmlFor="concept" className="text-base font-medium">여행 콘셉트 (선택사항)</Label>
                  <Input
                    id="concept"
                    value={concept}
                    onChange={(e) => setConcept(e.target.value)}
                    placeholder="예: 혼자 식도락 여행, 가족 힐링 여행"
                    disabled={createReviewMutation.isPending}
                    className="h-12 text-base"
                  />
                  <p className="text-sm text-muted-foreground flex items-center gap-2">
                    <Sparkles className="w-4 h-4" />
                    콘셉트를 입력하지 않으면 현재 일정을 기반으로 분석합니다.
                  </p>
                </div>

                <Button 
                  type="submit" 
                  disabled={createReviewMutation.isPending} 
                  className="w-full h-12 text-base font-semibold shadow-lg hover:shadow-xl transition-all"
                  size="lg"
                >
                  {createReviewMutation.isPending ? (
                    <>
                      <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                      AI 분석 중...
                    </>
                  ) : (
                    <>
                      <Sparkles className="mr-2 h-5 w-5" />
                      AI 리뷰 받기
                    </>
                  )}
                </Button>
              </form>
            </TabsContent>

            <TabsContent value="history" className="space-y-4 mt-6">
              {isLoadingReviews ? (
                <div className="flex flex-col items-center justify-center py-12">
                  <Loader2 className="w-8 h-8 animate-spin text-primary mb-3" />
                  <p className="text-sm text-muted-foreground">리뷰 목록을 불러오는 중...</p>
                </div>
              ) : reviews.length === 0 ? (
                <div className="text-center py-12">
                  <div className="inline-flex p-4 rounded-full bg-muted/50 mb-4">
                    <History className="w-12 h-12 text-muted-foreground" />
                  </div>
                  <p className="text-lg font-medium mb-2">아직 생성된 리뷰가 없습니다</p>
                  <p className="text-sm text-muted-foreground">새 분석을 받아보세요!</p>
                </div>
              ) : (
                <div className="space-y-3 max-h-[50vh] overflow-y-auto pr-2">
                  {reviews.map((r, index) => (
                    <Card
                      key={r.reviewId}
                      className="group cursor-pointer hover:shadow-md transition-all duration-200 border-2 hover:border-primary/20 animate-fade-in"
                      style={{ animationDelay: `${index * 0.05}s` }}
                      onClick={() => handleReviewClick(r.reviewId)}
                    >
                      <CardContent className="p-5">
                        <div className="flex items-start gap-4">
                          <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary/10 to-primary/5 group-hover:from-primary/20 group-hover:to-primary/10 transition-colors">
                            <Sparkles className="w-5 h-5 text-primary" />
                          </div>
                          <div className="flex-1 min-w-0">
                            {r.concept && (
                              <Badge variant="secondary" className="mb-2 font-medium">
                                {r.concept}
                              </Badge>
                            )}
                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                              <Calendar className="w-3.5 h-3.5" />
                              <span>
                                {new Date(r.createdAt).toLocaleString("ko-KR", {
                                  year: "numeric",
                                  month: "long",
                                  day: "numeric",
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })}
                              </span>
                            </div>
                          </div>
                          <ArrowRight className="w-5 h-5 text-muted-foreground group-hover:text-primary group-hover:translate-x-1 transition-all flex-shrink-0" />
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </TabsContent>
          </Tabs>
        ) : isLoadingDetail ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        ) : (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-muted/50 to-muted/20 rounded-xl p-6 border">
              {renderReviewContent(newReview || selectedReview!)}
            </div>

            <div className="flex gap-3 pt-4 border-t">
              <Button
                onClick={() => {
                  setNewReview(null);
                  setSelectedReviewId(null);
                }}
                variant="outline"
                className="flex-1 h-11"
                size="lg"
              >
                <History className="w-4 h-4 mr-2" />
                목록으로
              </Button>
              <Button
                onClick={() => {
                  setNewReview(null);
                  setSelectedReviewId(null);
                  setConcept("");
                  setActiveTab("new");
                }}
                className="flex-1 h-11 shadow-lg"
                size="lg"
              >
                <Sparkles className="w-4 h-4 mr-2" />
                새로운 분석 받기
              </Button>
            </div>
          </div>
        )}

        {/* 일정 추가 카테고리 선택 모달 */}
        <Dialog open={!!addingToItinerary} onOpenChange={() => {
          setAddingToItinerary(null);
          setSelectedCategoryId("");
        }}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>일정에 추가</DialogTitle>
              <DialogDescription>
                {addingToItinerary?.placeName}을(를) 추가할 날짜를 선택하세요
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label>날짜 선택</Label>
                <Select value={selectedCategoryId} onValueChange={setSelectedCategoryId}>
                  <SelectTrigger>
                    <SelectValue placeholder="날짜를 선택하세요" />
                  </SelectTrigger>
                  <SelectContent className="max-h-48 overflow-y-auto">
              {categories.map((category) => (
                <SelectItem key={category.categoryId} value={category.categoryId.toString()}>
                  {category.name} (Day {category.day})
                </SelectItem>
              ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setAddingToItinerary(null);
                  setSelectedCategoryId("");
                }}
              >
                취소
              </Button>
              <Button
                onClick={handleConfirmAddToItinerary}
                disabled={!selectedCategoryId || addToItineraryMutation.isPending}
              >
                {addToItineraryMutation.isPending ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    추가 중...
                  </>
                ) : (
                  <>
                    <Plus className="w-4 h-4 mr-2" />
                    추가
                  </>
                )}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </DialogContent>
    </Dialog>
  );
};
