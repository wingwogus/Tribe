import {useState} from "react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {CalendarDays, ChevronRight, Pencil, Plus, Trash2, Users} from "lucide-react";
import {Card, CardContent} from "@/components/ui/card";
import {useNavigate} from "react-router-dom";
import {TripCreationModal} from "@/components/TripCreationModal";
import {TripJoinModal} from "@/components/TripJoinModal";
import {LoginModal} from "@/components/LoginModal";
import {NicknameModal} from "@/components/NicknameModal";
import {NicknameEditModal} from "@/components/NicknameEditModal";
import {TripEditModal} from "@/components/TripEditModal";
import {TripDeleteDialog} from "@/components/TripDeleteDialog";
import {Header} from "@/components/Header";
import {getMemberInfo, logout} from "@/api/auth";
import {type MemberInfo, type SimpleTrip, tripApi, type TripDetail, type UpdateTripRequest} from "@/api/trips";
import {useToast} from "@/hooks/use-toast";
import {Button} from "@/components/ui/button";
import {Avatar, AvatarFallback, AvatarImage} from "@/components/ui/avatar";
import {formatTripDestination} from "@/lib/tripRegions";

const dayMs = 1000 * 60 * 60 * 24;

const tripImageByCountry: Record<string, string> = {
  GREECE: "https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?auto=format&fit=crop&w=900&q=80",
  JAPAN: "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=900&q=80",
  SWITZERLAND: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?auto=format&fit=crop&w=900&q=80",
  FRANCE: "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=900&q=80",
  ITALY: "https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?auto=format&fit=crop&w=900&q=80",
  SOUTH_KOREA: "https://images.unsplash.com/photo-1538485399081-7c8ed225ea34?auto=format&fit=crop&w=900&q=80",
  USA: "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80",
};

const fallbackTripImage =
  "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80";

const getTripImage = (country: string) => tripImageByCountry[country] ?? fallbackTripImage;

const toDateOnly = (dateString: string) => new Date(`${dateString}T00:00:00`);

const getTripTiming = (trip: SimpleTrip) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const startDate = toDateOnly(trip.startDate);
  const endDate = toDateOnly(trip.endDate);
  const daysUntilStart = Math.ceil((startDate.getTime() - today.getTime()) / dayMs);

  if (today > endDate) {
    return {badge: "완료", daysUntilStart, section: "completed" as const};
  }

  if (daysUntilStart <= 0) {
    return {badge: "진행 중", daysUntilStart, section: "upcoming" as const};
  }

  return {
    badge: `D-${daysUntilStart}`,
    daysUntilStart,
    section: "upcoming" as const,
  };
};

const formatDate = (dateString: string) => {
  const [, month, day] = dateString.split("-");
  return `${Number(month)}월 ${Number(day)}일`;
};

const getInitials = (nickname?: string | null) => {
  const trimmedNickname = nickname?.trim();

  if (!trimmedNickname) {
    return "ME";
  }

  return trimmedNickname.slice(0, 2).toUpperCase();
};

const Dashboard = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [showTripCreationModal, setShowTripCreationModal] = useState(false);
  const [showTripJoinModal, setShowTripJoinModal] = useState(false);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showNicknameModal, setShowNicknameModal] = useState(false);
  const [showNicknameEditModal, setShowNicknameEditModal] = useState(false);
  const [editingTrip, setEditingTrip] = useState<TripDetail | null>(null);
  const [deletingTripId, setDeletingTripId] = useState<number | null>(null);
  const [deletingTripTitle, setDeletingTripTitle] = useState<string>("");

  // User info query
  const { data: user, isLoading: isBootstrappingSession } = useQuery({
    queryKey: ['user'],
    queryFn: async () => {
      const userInfo = await getMemberInfo();
      if (userInfo?.isNewUser) {
        setShowNicknameModal(true);
      }
      return userInfo;
    },
    staleTime: 10 * 60 * 1000, // 10분
  });

  const isLoggedIn = !!user;

  // Trips query
  const { data: trips = [], isLoading: isLoadingTrips } = useQuery({
    queryKey: ['trips'],
    queryFn: async () => {
      const response = await tripApi.getTrips(0, 10);
      return response.content;
    },
    enabled: isLoggedIn,
  });

  const handleLoginSuccess = () => {
    queryClient.invalidateQueries({ queryKey: ['user'] });
    queryClient.invalidateQueries({ queryKey: ['trips'] });
    window.location.reload(); // Force reload to update login status
  };

  const handleLogout = async () => {
    await logout();
    queryClient.clear();
    window.location.reload(); // Force reload to clear all state
  };

  // Create trip mutation
  const createTripMutation = useMutation({
    mutationFn: (tripData: any) => tripApi.createTrip(tripData),
    onSuccess: (newTrip) => {
      queryClient.invalidateQueries({ queryKey: ['trips'] });
      toast({
        title: "여행 생성 완료",
        description: `${newTrip.title} 여행이 생성되었습니다.`,
      });
      navigate(`/trip/${newTrip.tripId}`);
    },
    onError: () => {
      toast({
        title: "오류",
        description: "여행 생성에 실패했습니다.",
        variant: "destructive",
      });
    },
  });

  // Update trip mutation
  const updateTripMutation = useMutation({
    mutationFn: ({ tripId, updates }: { tripId: number; updates: UpdateTripRequest }) =>
      tripApi.updateTrip(tripId, updates),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trips'] });
      toast({
        title: "수정 완료",
        description: "여행 정보가 수정되었습니다.",
      });
      setEditingTrip(null);
    },
    onError: () => {
      toast({
        title: "오류",
        description: "여행 수정에 실패했습니다.",
        variant: "destructive",
      });
    },
  });

  const handleEditTrip = async (tripId: number) => {
    try {
      const tripDetail = await tripApi.getTripById(tripId);
      setEditingTrip(tripDetail);
    } catch (error) {
      toast({
        title: "오류",
        description: "여행 정보를 불러오는데 실패했습니다.",
        variant: "destructive",
      });
    }
  };

  const handleUpdateTrip = (updates: UpdateTripRequest) => {
    if (!editingTrip) return;
    updateTripMutation.mutate({ tripId: editingTrip.tripId, updates });
  };

  // Delete trip mutation
  const deleteTripMutation = useMutation({
    mutationFn: (tripId: number) => tripApi.deleteTrip(tripId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trips'] });
      toast({
        title: "삭제 완료",
        description: "여행이 삭제되었습니다.",
      });
      setDeletingTripId(null);
      setDeletingTripTitle("");
    },
    onError: () => {
      toast({
        title: "오류",
        description: "여행 삭제에 실패했습니다.",
        variant: "destructive",
      });
    },
  });

  const handleDeleteTrip = () => {
    if (!deletingTripId) return;
    deleteTripMutation.mutate(deletingTripId);
  };

  // Join trip mutation
  const joinTripMutation = useMutation({
    mutationFn: (token: string) => tripApi.joinTrip(token),
    onSuccess: (joinedTrip) => {
      queryClient.invalidateQueries({ queryKey: ['trips'] });
      toast({
        title: "여행 참여 완료",
        description: `${joinedTrip.title} 여행에 참여했습니다.`,
      });
      navigate(`/trip/${joinedTrip.tripId}`);
    },
    onError: () => {
      toast({
        title: "오류",
        description: "여행 참여에 실패했습니다. 토큰을 확인해주세요.",
        variant: "destructive",
      });
      throw new Error("Join failed");
    },
  });

  const handleJoinTrip = (token: string) => {
    joinTripMutation.mutate(token);
  };

  const openCreateTrip = () => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }

    setShowTripCreationModal(true);
  };

  const openJoinTrip = () => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }

    setShowTripJoinModal(true);
  };

  const tripCards = trips.map((trip) => ({
    trip,
    timing: getTripTiming(trip),
  }));
  const upcomingTrips = tripCards.filter(({timing}) => timing.section === "upcoming");
  const completedTrips = tripCards.filter(({timing}) => timing.section === "completed");
  const hasTrips = trips.length > 0;

  const renderTripCard = ({trip, timing}: (typeof tripCards)[number]) => {
    const fallbackMember: MemberInfo | null = user
      ? {
        memberId: user.memberId,
        tripMemberId: null,
        nickname: user.nickname,
        avatar: user.avatar,
        role: "MEMBER",
      }
      : null;
    const visibleMembers = (trip.members.length > 0 ? trip.members : fallbackMember ? [fallbackMember] : []).slice(0, 3);
    const hiddenMemberCount = Math.max(trip.memberCount - visibleMembers.length, 0);

    return (
      <Card
        key={trip.tripId}
        className="group overflow-hidden rounded-[24px] border-0 bg-white shadow-[0_14px_34px_-24px_rgba(15,23,42,0.5)] transition-all duration-300 ease-out hover:-translate-y-1.5 hover:shadow-[0_24px_54px_-28px_rgba(37,99,235,0.45)] focus-within:-translate-y-1.5 focus-within:shadow-[0_24px_54px_-28px_rgba(37,99,235,0.45)]"
      >
        <CardContent
          className="cursor-pointer p-0"
          onClick={() => navigate(`/trip/${trip.tripId}`)}
        >
          <div className="relative h-36 overflow-hidden bg-gradient-ocean">
            <img
              src={getTripImage(trip.country)}
              alt={`${formatTripDestination(trip.country, trip.regionCode)} 여행 이미지`}
              loading="lazy"
              decoding="async"
              referrerPolicy="no-referrer"
              className="h-full w-full object-cover transition-transform duration-500 ease-out group-hover:scale-105 group-focus-within:scale-105"
              onError={(event) => {
                event.currentTarget.hidden = true;
              }}
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/10 via-transparent to-black/10" />
            <div className="absolute right-3 top-3 rounded-full bg-white px-3 py-1 text-xs font-semibold text-primary shadow-soft">
              {timing.badge}
            </div>
            <div className="absolute left-3 top-3 flex gap-1 opacity-100 transition-opacity duration-200 sm:opacity-0 sm:group-hover:opacity-100 sm:group-focus-within:opacity-100">
              <Button
                type="button"
                size="icon"
                variant="secondary"
                aria-label={`${trip.title} 수정`}
                className="h-7 w-7 rounded-full bg-white/90 text-slate-700 shadow-soft backdrop-blur hover:bg-white"
                onClick={(event) => {
                  event.stopPropagation();
                  handleEditTrip(trip.tripId);
                }}
              >
                <Pencil className="h-3.5 w-3.5" />
              </Button>
              <Button
                type="button"
                size="icon"
                variant="secondary"
                aria-label={`${trip.title} 삭제`}
                className="h-7 w-7 rounded-full bg-white/90 text-slate-700 shadow-soft backdrop-blur hover:bg-white hover:text-destructive"
                onClick={(event) => {
                  event.stopPropagation();
                  setDeletingTripId(trip.tripId);
                  setDeletingTripTitle(trip.title);
                }}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            </div>
          </div>

          <div className="space-y-3 p-4">
            <div className="space-y-2">
              <h3 className="line-clamp-1 text-base font-bold text-slate-950 transition-colors group-hover:text-primary">
                {trip.title}
              </h3>
              <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                <CalendarDays className="h-4 w-4 text-slate-400" />
                <span>
                  {formatDate(trip.startDate)} - {formatDate(trip.endDate)}
                </span>
              </div>
            </div>

            <div className="border-t border-slate-200 pt-3">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <div className="flex -space-x-2">
                    {visibleMembers.map((member) => (
                      <Avatar
                        key={member.tripMemberId ?? `member-${member.memberId ?? member.nickname}`}
                        className="h-6 w-6 ring-2 ring-white"
                      >
                        <AvatarImage
                          src={member.avatar || undefined}
                          alt={member.nickname}
                          className="object-cover"
                        />
                        <AvatarFallback className="bg-primary text-[10px] font-semibold text-white">
                          {getInitials(member.nickname)}
                        </AvatarFallback>
                      </Avatar>
                    ))}
                    {hiddenMemberCount > 0 && (
                      <div className="flex h-6 min-w-[1.5rem] items-center justify-center rounded-full bg-slate-100 px-2 text-[10px] font-semibold text-slate-600 ring-2 ring-white">
                        +{hiddenMemberCount}
                      </div>
                    )}
                  </div>
                  <span className="text-sm font-medium text-slate-500">
                    멤버 {trip.memberCount}명
                  </span>
                </div>
                <ChevronRight className="h-4 w-4 text-slate-500 transition-transform duration-300 group-hover:translate-x-1 group-hover:text-primary" />
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  };

  const renderSection = (
    title: string,
    items: typeof tripCards,
  ) => {
    if (items.length === 0) {
      return null;
    }

    return (
      <section className="space-y-4">
        <h3 className="text-xl font-bold tracking-normal text-slate-950 md:text-2xl">
          {title}
        </h3>
        <div className="grid max-w-[660px] grid-cols-1 gap-5 sm:grid-cols-2">
          {items.map(renderTripCard)}
        </div>
      </section>
    );
  };

  return (
    <div className="min-h-screen bg-background">
      <Header
        isLoggedIn={isLoggedIn}
        user={user}
        onLoginClick={() => setShowLoginModal(true)}
        onLogoutClick={handleLogout}
        onJoinTripClick={() => setShowTripJoinModal(true)}
        onEditNicknameClick={() => setShowNicknameEditModal(true)}
        showJoinTripAction={false}
      />

      {/* Main Content */}
      <main className="container mx-auto px-4 py-8 md:px-6 md:py-10">
        <div className="mb-8 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div>
            <h2 className="text-3xl font-bold tracking-normal text-slate-950 md:text-[32px]">
              내 여행
            </h2>
            <p className="mt-2 text-sm font-medium text-slate-500 md:text-base">
              함께 만들어가는 특별한 여행을 시작해보세요
            </p>
          </div>
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row">
            <Button
              type="button"
              variant="outline"
              className="h-10 w-full gap-2 rounded-full border-slate-950 bg-white px-5 text-sm font-semibold text-slate-950 shadow-none hover:bg-slate-50 hover:text-slate-950 sm:w-auto"
              onClick={openJoinTrip}
            >
              <Users className="h-4 w-4" />
              여행 참여하기
            </Button>
            <Button
              type="button"
              className="h-10 w-full gap-2 rounded-full bg-primary px-5 text-sm font-semibold text-primary-foreground shadow-none transition-all duration-300 hover:-translate-y-0.5 hover:bg-primary/90 sm:w-auto"
              onClick={openCreateTrip}
            >
              <Plus className="h-4 w-4" />
              새 여행 만들기
            </Button>
          </div>
        </div>

        {isLoadingTrips || isBootstrappingSession ? (
          <div className="grid max-w-[660px] grid-cols-1 gap-5 sm:grid-cols-2">
            {[0, 1].map((item) => (
              <div
                key={item}
                className="h-[288px] animate-pulse rounded-[24px] bg-slate-100"
              />
            ))}
          </div>
        ) : !hasTrips ? (
          <div className="flex min-h-[300px] w-full flex-col items-center justify-center rounded-[24px] border border-slate-200 bg-white px-6 py-12 text-center shadow-[0_10px_30px_-26px_rgba(15,23,42,0.45)]">
            <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-slate-100">
              <svg
                viewBox="0 0 50 50"
                aria-hidden="true"
                className="h-10 w-10 text-slate-600"
                fill="currentColor"
              >
                <path d="M41.5 11.5Q47 11 48.5 12Q50 13 50 14.5Q50 16 48.5 17.5Q47 19 41 22Q35 25 32 32.5Q29 40 27 42Q25 44 23 43.5Q21 43 22 36.5Q23 30 17.5 32Q12 34 8.5 34Q5 34 2.5 29.5Q0 25 0 23Q0 21 1 20.5Q2 20 4.5 21Q7 22 7.5 23Q8 24 10 24Q12 24 24 18Q36 12 41.5 11.5Z M22 17.5Q20 18 17 19Q14 20 11.5 18Q9 16 9 15Q9 14 13.5 13.5Q18 13 19.5 14Q21 15 23 15Q25 15 24.5 16Q24 17 22 17.5Z" />
              </svg>
            </div>
            <h3 className="text-xl font-bold tracking-normal text-slate-950">
              아직 계획된 여행이 없어요.
            </h3>
            <p className="mt-3 text-base font-medium text-slate-500">
              첫 번째 여행을 계획해보세요!
            </p>
            <Button
              type="button"
              className="mt-8 h-10 gap-2 rounded-full bg-primary px-6 text-sm font-semibold text-primary-foreground shadow-none transition-all duration-300 hover:-translate-y-0.5 hover:bg-primary/90"
              onClick={openCreateTrip}
            >
              <Plus className="h-4 w-4" />
              새 여행 추가
            </Button>
          </div>
        ) : (
          <div className="space-y-12">
            {renderSection("다가오는 여행", upcomingTrips)}
            {renderSection("완료된 여행", completedTrips)}
          </div>
        )}
      </main>
      
      <TripCreationModal
        isOpen={showTripCreationModal}
        onClose={() => setShowTripCreationModal(false)}
        onCreateTrip={(tripData) => createTripMutation.mutate(tripData)}
      />

      <TripJoinModal
        isOpen={showTripJoinModal}
        onClose={() => setShowTripJoinModal(false)}
        onJoinTrip={handleJoinTrip}
      />
      
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onLoginSuccess={handleLoginSuccess}
      />

      <NicknameModal
        isOpen={showNicknameModal}
        onClose={() => setShowNicknameModal(false)}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['user'] });
        }}
      />

      <NicknameEditModal
        isOpen={showNicknameEditModal}
        onClose={() => setShowNicknameEditModal(false)}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['user'] });
        }}
        currentNickname={user?.nickname || ""}
      />

      {editingTrip && (
        <TripEditModal
          isOpen={!!editingTrip}
          onClose={() => setEditingTrip(null)}
          trip={editingTrip}
          onUpdateTrip={handleUpdateTrip}
        />
      )}

      <TripDeleteDialog
        isOpen={!!deletingTripId}
        onClose={() => {
          setDeletingTripId(null);
          setDeletingTripTitle("");
        }}
        onConfirm={handleDeleteTrip}
        tripTitle={deletingTripTitle}
      />
    </div>
  );
};

export default Dashboard;
