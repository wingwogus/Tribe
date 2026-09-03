import {useCallback, useEffect, useMemo, useRef, useState, type ReactNode} from "react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {Heart, Loader2, Search, Trash2} from "lucide-react";
import {Header} from "@/components/Header";
import {ItineraryMap, type ItineraryMapHandle} from "@/components/ItineraryMap";
import {LoginModal} from "@/components/LoginModal";
import {NicknameEditModal} from "@/components/NicknameEditModal";
import {NicknameModal} from "@/components/NicknameModal";
import {PlaceSearchPanel} from "@/components/place/PlaceSearchPanel";
import {PlacePreviewCard} from "@/components/place/PlacePreviewCard";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {getMemberInfo, logout} from "@/api/auth";
import {readApiErrorMessage} from "@/api/http";
import {placesApi, type PlaceSearchResult} from "@/api/places";
import {type MemberWishlistItem, type MemberWishlistSort, type WishlistItem, wishlistApi} from "@/api/wishlist";
import {useToast} from "@/hooks/use-toast";
import {wishlistQueryKeys} from "@/lib/wishlistQueryKeys";

const DEFAULT_MEMBER_WISHLIST_SORT: MemberWishlistSort = "review_count_desc";
const DESKTOP_PANEL_MARGIN = 16;
const DESKTOP_PANEL_GAP = 12;
const DESKTOP_SEARCH_PANEL_WIDTH = 360;
const DESKTOP_WISHLIST_PANEL_WIDTH = 376;
const DESKTOP_SEARCH_PANEL_LEFT = DESKTOP_PANEL_MARGIN;
const DESKTOP_WISHLIST_PANEL_LEFT = DESKTOP_SEARCH_PANEL_LEFT + DESKTOP_SEARCH_PANEL_WIDTH + DESKTOP_PANEL_GAP;
const DESKTOP_MAP_VISIBLE_LEFT_INSET_PX = (
  DESKTOP_WISHLIST_PANEL_LEFT + DESKTOP_WISHLIST_PANEL_WIDTH + DESKTOP_PANEL_GAP
);

const toWishlistPreviewPlace = (item: MemberWishlistItem) => ({
  title: item.name,
  address: item.address,
  placeTypeSummary: item.placeTypeSummary,
  normalizedCategoryKey: item.normalizedCategoryKey,
  placeDetailSummary: item.placeDetailSummary,
  openingSummary: item.openingSummary,
});

const MyWishlist = () => {
  const {toast} = useToast();
  const queryClient = useQueryClient();
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showNicknameModal, setShowNicknameModal] = useState(false);
  const [showNicknameEditModal, setShowNicknameEditModal] = useState(false);
  const [placeSearchInput, setPlaceSearchInput] = useState("");
  const [submittedPlaceSearchQuery, setSubmittedPlaceSearchQuery] = useState("");
  const [placeSearchResults, setPlaceSearchResults] = useState<PlaceSearchResult[]>([]);
  const [isPlaceSearchLoading, setIsPlaceSearchLoading] = useState(false);
  const [placeSearchErrorMessage, setPlaceSearchErrorMessage] = useState<string | null>(null);
  const [wishlistSearchInput, setWishlistSearchInput] = useState("");
  const [wishlistSearchQuery, setWishlistSearchQuery] = useState("");
  const [selectedSearchPlaceExternalId, setSelectedSearchPlaceExternalId] = useState<string | null>(null);
  const [selectedWishlistItemId, setSelectedWishlistItemId] = useState<number | null>(null);
  const placeSearchRequestIdRef = useRef(0);
  const mapRef = useRef<ItineraryMapHandle>(null);

  const {data: user, isLoading: isBootstrappingSession} = useQuery({
    queryKey: ["user"],
    queryFn: async () => {
      const userInfo = await getMemberInfo();
      if (userInfo?.isNewUser) {
        setShowNicknameModal(true);
      }
      return userInfo;
    },
    staleTime: 10 * 60 * 1000,
  });

  const isLoggedIn = !!user;

  useEffect(() => {
    const timer = setTimeout(() => {
      setWishlistSearchQuery(wishlistSearchInput);
    }, 350);

    return () => clearTimeout(timer);
  }, [wishlistSearchInput]);

  const {
    data: memberWishlistData,
    isLoading: isLoadingWishlist,
    isError: isWishlistError,
  } = useQuery({
    queryKey: wishlistQueryKeys.memberWishlist(wishlistSearchQuery, DEFAULT_MEMBER_WISHLIST_SORT),
    queryFn: () => wishlistApi.getMemberWishlist(
      wishlistSearchQuery || undefined,
      0,
      300,
      DEFAULT_MEMBER_WISHLIST_SORT,
    ),
    enabled: isLoggedIn,
  });

  const memberWishlistItems = useMemo(
    () => memberWishlistData?.content ?? [],
    [memberWishlistData?.content],
  );
  const mapWishlistItems = useMemo<WishlistItem[]>(
    () => memberWishlistItems.map((item) => ({
      ...item,
      wishlistItemId: item.memberWishlistItemId,
      adder: {
        tripMemberId: null,
        memberId: user?.memberId ?? null,
        nickname: user?.nickname || "나",
        avatar: user?.avatar ?? null,
        role: "MEMBER",
      },
      likeCount: 0,
      likedByMe: false,
    })),
    [memberWishlistItems, user?.avatar, user?.memberId, user?.nickname],
  );

  const openLoginGate = useCallback(() => {
    setShowLoginModal(true);
  }, []);

  const handleLoginSuccess = () => {
    queryClient.invalidateQueries({queryKey: ["user"]});
    queryClient.invalidateQueries({queryKey: wishlistQueryKeys.memberWishlistRoot()});
    window.location.reload();
  };

  const handleLogout = async () => {
    await logout();
    queryClient.clear();
    window.location.reload();
  };

  const handlePlaceSearchQueryChange = useCallback((nextQuery: string) => {
    setPlaceSearchInput(nextQuery);
    setPlaceSearchErrorMessage(null);

    if (nextQuery.trim() !== submittedPlaceSearchQuery.trim()) {
      setPlaceSearchResults([]);
      setIsPlaceSearchLoading(false);
      setSubmittedPlaceSearchQuery("");
      setSelectedSearchPlaceExternalId(null);
      placeSearchRequestIdRef.current += 1;
    }
  }, [submittedPlaceSearchQuery]);

  const submitPlaceSearch = useCallback(async () => {
    if (!isLoggedIn) {
      openLoginGate();
      return;
    }

    const query = placeSearchInput.trim();
    if (!query) {
      setPlaceSearchResults([]);
      setIsPlaceSearchLoading(false);
      setPlaceSearchErrorMessage(null);
      setSubmittedPlaceSearchQuery("");
      setSelectedSearchPlaceExternalId(null);
      placeSearchRequestIdRef.current += 1;
      return;
    }

    setIsPlaceSearchLoading(true);
    setPlaceSearchErrorMessage(null);
    setSubmittedPlaceSearchQuery(query);
    const requestId = placeSearchRequestIdRef.current + 1;
    placeSearchRequestIdRef.current = requestId;

    try {
      const results = await placesApi.searchPlaces(query);
      if (placeSearchRequestIdRef.current !== requestId) {
        return;
      }
      setPlaceSearchResults(results);
      setSelectedSearchPlaceExternalId(results[0]?.externalPlaceId ?? null);
      setSelectedWishlistItemId(null);
    } catch (error) {
      if (placeSearchRequestIdRef.current !== requestId) {
        return;
      }
      const errorMessage = readApiErrorMessage(error, "장소 검색 중 오류가 발생했습니다.");
      setPlaceSearchResults([]);
      setSelectedSearchPlaceExternalId(null);
      setPlaceSearchErrorMessage(errorMessage);
      toast({
        title: "검색 실패",
        description: errorMessage,
        variant: "destructive",
      });
    } finally {
      if (placeSearchRequestIdRef.current === requestId) {
        setIsPlaceSearchLoading(false);
      }
    }
  }, [isLoggedIn, openLoginGate, placeSearchInput, toast]);

  const addMemberWishlistMutation = useMutation({
    mutationFn: (place: PlaceSearchResult) =>
      wishlistApi.addMemberWishlist({
        externalPlaceId: place.externalPlaceId,
        placeName: place.placeName,
        address: place.address,
        latitude: place.latitude,
        longitude: place.longitude,
      }),
    onSuccess: (item, place) => {
      queryClient.invalidateQueries({queryKey: wishlistQueryKeys.memberWishlistRoot()});
      setSelectedWishlistItemId(item.memberWishlistItemId);
      setSelectedSearchPlaceExternalId(place.externalPlaceId);
      toast({
        title: "내 위시에 추가됨",
        description: `${place.placeName}이(가) 저장되었습니다.`,
      });
    },
    onError: (error) => {
      toast({
        title: "추가 실패",
        description: readApiErrorMessage(error, "내 위시 추가 중 오류가 발생했습니다."),
        variant: "destructive",
      });
    },
  });

  const deleteMemberWishlistMutation = useMutation({
    mutationFn: (item: MemberWishlistItem) =>
      wishlistApi.deleteMemberWishlistItems([item.memberWishlistItemId]),
    onSuccess: (_, item) => {
      queryClient.invalidateQueries({queryKey: wishlistQueryKeys.memberWishlistRoot()});
      if (selectedWishlistItemId === item.memberWishlistItemId) {
        setSelectedWishlistItemId(null);
      }
      toast({
        title: "내 위시에서 삭제됨",
        description: `${item.name}이(가) 삭제되었습니다.`,
      });
    },
    onError: (error) => {
      toast({
        title: "삭제 실패",
        description: readApiErrorMessage(error, "내 위시 삭제 중 오류가 발생했습니다."),
        variant: "destructive",
      });
    },
  });

  const handleSelectSearchPlace = useCallback((place: PlaceSearchResult) => {
    setSelectedSearchPlaceExternalId(place.externalPlaceId);
    setSelectedWishlistItemId(null);
    mapRef.current?.focusNearbyMarker(place.externalPlaceId, {
      visibleLeftInsetPx: DESKTOP_MAP_VISIBLE_LEFT_INSET_PX,
    });
  }, []);

  const handleSelectWishlistItem = useCallback((item: MemberWishlistItem) => {
    setSelectedWishlistItemId(item.memberWishlistItemId);
    setSelectedSearchPlaceExternalId(null);
    mapRef.current?.focusWishlistMarker(item.memberWishlistItemId, {
      visibleLeftInsetPx: DESKTOP_MAP_VISIBLE_LEFT_INSET_PX,
    });
  }, []);

  const handleSelectMapWishlistItem = useCallback((item: WishlistItem) => {
    const wishlistItem = memberWishlistItems.find(
      (memberItem) => memberItem.memberWishlistItemId === item.wishlistItemId,
    );
    if (wishlistItem) {
      handleSelectWishlistItem(wishlistItem);
    }
  }, [handleSelectWishlistItem, memberWishlistItems]);

  useEffect(() => {
    if (!selectedSearchPlaceExternalId) {
      return;
    }

    const timer = window.setTimeout(() => {
      mapRef.current?.focusNearbyMarker(selectedSearchPlaceExternalId, {
        visibleLeftInsetPx: DESKTOP_MAP_VISIBLE_LEFT_INSET_PX,
      });
    }, 0);

    return () => window.clearTimeout(timer);
  }, [selectedSearchPlaceExternalId, placeSearchResults]);

  useEffect(() => {
    if (!selectedWishlistItemId) {
      return;
    }

    const timer = window.setTimeout(() => {
      mapRef.current?.focusWishlistMarker(selectedWishlistItemId, {
        visibleLeftInsetPx: DESKTOP_MAP_VISIBLE_LEFT_INSET_PX,
      });
    }, 0);

    return () => window.clearTimeout(timer);
  }, [selectedWishlistItemId, mapWishlistItems]);

  const handleAddPlace = (place: PlaceSearchResult) => {
    if (!isLoggedIn) {
      openLoginGate();
      return;
    }

    addMemberWishlistMutation.mutate(place);
  };

  const handleDeleteItem = (item: MemberWishlistItem) => {
    if (!isLoggedIn) {
      openLoginGate();
      return;
    }

    deleteMemberWishlistMutation.mutate(item);
  };

  const clearPlaceSearch = () => {
    setPlaceSearchInput("");
    setSubmittedPlaceSearchQuery("");
    setPlaceSearchResults([]);
    setPlaceSearchErrorMessage(null);
    setIsPlaceSearchLoading(false);
    setSelectedSearchPlaceExternalId(null);
    placeSearchRequestIdRef.current += 1;
  };

  const renderWishlistContent = () => {
    if (!isLoggedIn && !isBootstrappingSession) {
      return (
        <PanelMessage
          icon={<Heart className="h-8 w-8 text-slate-300" />}
          title="로그인이 필요합니다"
          description="로그인 후 내 위시를 확인할 수 있습니다."
          action={<Button onClick={openLoginGate}>로그인</Button>}
        />
      );
    }

    if (isBootstrappingSession || isLoadingWishlist) {
      return (
        <PanelMessage
          icon={<Loader2 className="h-8 w-8 animate-spin text-slate-300" />}
          title="내 위시를 불러오는 중..."
        />
      );
    }

    if (isWishlistError) {
      return (
        <PanelMessage
          icon={<Heart className="h-8 w-8 text-slate-300" />}
          title="내 위시를 불러오지 못했습니다"
          description="잠시 후 다시 시도해주세요."
        />
      );
    }

    if (memberWishlistItems.length === 0) {
      return (
        <PanelMessage
          icon={<Heart className="h-8 w-8 text-slate-300" />}
          title={wishlistSearchQuery ? "검색 결과가 없습니다" : "저장한 장소가 없습니다"}
          description={wishlistSearchQuery ? "다른 검색어로 다시 확인해보세요." : "검색 결과에서 장소를 저장해보세요."}
        />
      );
    }

    return (
      <div className="space-y-4">
        {memberWishlistItems.map((item) => {
          const isDeleting = deleteMemberWishlistMutation.isPending
            && deleteMemberWishlistMutation.variables?.memberWishlistItemId === item.memberWishlistItemId;

          return (
            <PlacePreviewCard
              key={item.memberWishlistItemId}
              onSelect={() => handleSelectWishlistItem(item)}
              className={item.memberWishlistItemId === selectedWishlistItemId
                ? "border-primary/40 ring-2 ring-primary/30 shadow-lg"
                : undefined}
              place={toWishlistPreviewPlace(item)}
              action={
                <Button
                  type="button"
                  size="icon"
                  variant="ghost"
                  disabled={isDeleting}
                  className="mt-1 h-9 w-9 rounded-full text-slate-400 hover:bg-red-50 hover:text-red-600"
                  onClick={() => handleDeleteItem(item)}
                >
                  {isDeleting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                  <span className="sr-only">{item.name} 내 위시에서 삭제</span>
                </Button>
              }
            />
          );
        })}
      </div>
    );
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <Header
        isLoggedIn={isLoggedIn}
        user={user}
        onLoginClick={openLoginGate}
        onLogoutClick={handleLogout}
        onJoinTripClick={() => undefined}
        onEditNicknameClick={() => setShowNicknameEditModal(true)}
        showJoinTripAction={false}
      />

      <main className="relative flex w-full flex-1 min-h-0 flex-col overflow-hidden md:block">
        <div
          className="absolute bottom-4 top-4 z-20 hidden transition-[left,width] duration-300 ease-in-out md:block"
          style={{
            left: DESKTOP_SEARCH_PANEL_LEFT,
            width: DESKTOP_SEARCH_PANEL_WIDTH,
          }}
        >
          <div className="h-full w-full overflow-hidden">
            <PlaceSearchPanel
              query={placeSearchInput}
              submittedQuery={submittedPlaceSearchQuery}
              onQueryChange={handlePlaceSearchQueryChange}
              onSearchSubmit={submitPlaceSearch}
              results={placeSearchResults}
              isLoading={isPlaceSearchLoading}
              errorMessage={placeSearchErrorMessage}
              selectedExternalPlaceId={selectedSearchPlaceExternalId}
              addingExternalPlaceId={
                addMemberWishlistMutation.isPending
                  ? addMemberWishlistMutation.variables?.externalPlaceId ?? null
                  : null
              }
              onSelectPlace={handleSelectSearchPlace}
              onAddPlace={handleAddPlace}
              onClose={clearPlaceSearch}
            />
          </div>
        </div>

        <div
          className="absolute bottom-4 top-4 z-20 hidden transition-[left,width] duration-300 ease-in-out md:block"
          style={{
            left: DESKTOP_WISHLIST_PANEL_LEFT,
            width: DESKTOP_WISHLIST_PANEL_WIDTH,
          }}
        >
          <section className="flex h-full w-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white/95 shadow-2xl backdrop-blur">
            <div className="flex-shrink-0 border-b border-gray-100 bg-white/80 p-4 md:ps-6">
              <div className="mb-5 flex items-center justify-between gap-3">
                <h2 className="text-base font-bold tracking-tight text-slate-900 md:text-lg">저장한 장소</h2>
                {isLoggedIn && (
                  <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-bold text-primary">
                    {memberWishlistData?.totalElements ?? memberWishlistItems.length}
                  </span>
                )}
              </div>

              <div className="relative">
                <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input
                  value={wishlistSearchInput}
                  onChange={(event) => setWishlistSearchInput(event.target.value)}
                  disabled={!isLoggedIn}
                  placeholder="저장한 장소 검색"
                  className="h-11 rounded-lg border-slate-200 bg-slate-50 pl-10 text-sm font-medium shadow-none placeholder:text-slate-500 focus-visible:bg-white"
                />
              </div>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto bg-slate-50/50 p-4 md:p-6 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              {renderWishlistContent()}
            </div>
          </section>
        </div>

        <div className="relative h-full min-h-[300px] flex-1 md:absolute md:inset-0 md:min-h-0">
          <ItineraryMap
            ref={mapRef}
            items={[]}
            wishlistItems={mapWishlistItems}
            nearbyPlaces={placeSearchResults}
            selectedWishlistItemId={selectedWishlistItemId}
            selectedNearbyPlaceExternalId={selectedSearchPlaceExternalId}
            panelOffsetPx={0}
            visibleLeftInsetPx={DESKTOP_MAP_VISIBLE_LEFT_INSET_PX}
            onSelectWishlistMarker={handleSelectMapWishlistItem}
            onSelectNearbyPlace={handleSelectSearchPlace}
          />
        </div>

        <div className="pointer-events-none absolute inset-x-3 bottom-3 top-3 z-20 grid grid-rows-[minmax(0,1fr)_minmax(0,1fr)] gap-3 md:hidden">
          <section className="pointer-events-auto min-h-0">
            <PlaceSearchPanel
              query={placeSearchInput}
              submittedQuery={submittedPlaceSearchQuery}
              onQueryChange={handlePlaceSearchQueryChange}
              onSearchSubmit={submitPlaceSearch}
              results={placeSearchResults}
              isLoading={isPlaceSearchLoading}
              errorMessage={placeSearchErrorMessage}
              selectedExternalPlaceId={selectedSearchPlaceExternalId}
              addingExternalPlaceId={
                addMemberWishlistMutation.isPending
                  ? addMemberWishlistMutation.variables?.externalPlaceId ?? null
                  : null
              }
              onSelectPlace={handleSelectSearchPlace}
              onAddPlace={handleAddPlace}
              onClose={clearPlaceSearch}
            />
          </section>

          <section className="pointer-events-auto flex min-h-0 flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white/95 shadow-2xl backdrop-blur">
            <div className="border-b border-gray-100 bg-white/80 p-4">
              <div className="mb-4 flex items-center justify-between gap-3">
                <h2 className="text-base font-bold tracking-tight text-slate-900">저장한 장소</h2>
                {isLoggedIn && (
                  <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-bold text-primary">
                    {memberWishlistData?.totalElements ?? memberWishlistItems.length}
                  </span>
                )}
              </div>

              <div className="relative">
                <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input
                  value={wishlistSearchInput}
                  onChange={(event) => setWishlistSearchInput(event.target.value)}
                  disabled={!isLoggedIn}
                  placeholder="저장한 장소 검색"
                  className="h-11 rounded-lg border-slate-200 bg-slate-50 pl-10 text-sm font-medium shadow-none placeholder:text-slate-500 focus-visible:bg-white"
                />
              </div>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto bg-slate-50/50 p-4 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              {renderWishlistContent()}
            </div>
          </section>
        </div>
      </main>

      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onLoginSuccess={handleLoginSuccess}
      />

      <NicknameModal
        isOpen={showNicknameModal}
        onClose={() => setShowNicknameModal(false)}
        onSuccess={() => {
          queryClient.invalidateQueries({queryKey: ["user"]});
        }}
      />

      <NicknameEditModal
        isOpen={showNicknameEditModal}
        onClose={() => setShowNicknameEditModal(false)}
        onSuccess={() => {
          queryClient.invalidateQueries({queryKey: ["user"]});
        }}
        currentNickname={user?.nickname || ""}
      />

    </div>
  );
};

const PanelMessage = ({
  icon,
  title,
  description,
  action,
}: {
  icon: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) => (
  <div className="flex h-full min-h-[260px] items-center justify-center text-center text-slate-500">
    <div>
      <div className="mb-3 flex justify-center">{icon}</div>
      <p className="text-sm font-semibold text-slate-700">{title}</p>
      {description && <p className="mt-1 text-xs">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  </div>
);

export default MyWishlist;
