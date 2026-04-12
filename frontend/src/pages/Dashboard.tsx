import {useState} from "react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {Calendar, MapPin, Plus, Settings, Trash2, Users} from "lucide-react";
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
import {tripApi, TripDetail, UpdateTripRequest} from "@/api/trips";
import {useToast} from "@/hooks/use-toast";
import {Button} from "@/components/ui/button";
import {getCountryEmoji} from "@/lib/utils";

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

  const formatDate = (dateString: string) => {
    return dateString.replace(/-/g, '.');
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
      />

      {/* Main Content */}
      <main className="container mx-auto px-4 md:px-6 py-6 md:py-8">
        <div className="mb-6 md:mb-8">
          <h2 className="text-2xl md:text-3xl font-bold text-foreground mb-2">나의 여행</h2>
          <p className="text-sm md:text-base text-muted-foreground">함께 만들어가는 특별한 여행을 시작해보세요</p>
        </div>

        {/* Trip Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {isLoadingTrips ? (
            <div className="col-span-full text-center py-12 text-muted-foreground">
              여행 목록을 불러오는 중...
            </div>
          ) : isBootstrappingSession ? (
            <div className="col-span-full text-center py-12 text-muted-foreground">
              세션을 확인하는 중...
            </div>
          ) : trips.length === 0 ? (
            <div className="col-span-full text-center py-12 text-muted-foreground">
              아직 생성된 여행이 없습니다. 새 여행을 만들어보세요!
            </div>
          ) : (
            trips.map((trip) => (
              <Card 
                key={trip.tripId}
                className="group relative transition-all duration-300 hover:shadow-primary hover:-translate-y-1"
              >
                <div className="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity z-10">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleEditTrip(trip.tripId);
                    }}
                    className="hover:bg-primary/10"
                  >
                    <Settings className="w-4 h-4" />
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={(e) => {
                      e.stopPropagation();
                      setDeletingTripId(trip.tripId);
                      setDeletingTripTitle(trip.title);
                    }}
                    className="hover:bg-destructive/10 hover:text-destructive"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
                <CardContent className="p-6 cursor-pointer" onClick={() => navigate(`/trip/${trip.tripId}`)}>
                  <div className="text-4xl mb-4 text-center">{getCountryEmoji(trip.country)}</div>
                  
                  <div className="space-y-3">
                    <div>
                      <h3 className="text-xl font-semibold text-foreground group-hover:text-primary transition-colors">
                        {trip.title}
                      </h3>
                      <div className="flex items-center text-muted-foreground text-sm mt-1">
                        <MapPin className="w-4 h-4 mr-1" />
                        {trip.country}
                      </div>
                    </div>

                    <div className="flex items-center text-muted-foreground text-sm">
                      <Calendar className="w-4 h-4 mr-1" />
                      {formatDate(trip.startDate)} - {formatDate(trip.endDate)}
                    </div>

                    <div className="flex items-center text-muted-foreground text-sm">
                      <Users className="w-4 h-4 mr-1" />
                      멤버 {trip.memberCount}명
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))
          )}

          {/* Add New Trip Card */}
          <Card 
            className="group cursor-pointer transition-all duration-300 hover:shadow-accent border-dashed border-2 border-muted"
            onClick={() => {
              if (!isLoggedIn) {
                setShowLoginModal(true);
              } else {
                setShowTripCreationModal(true);
              }
            }}
          >
            <CardContent className="p-6 flex flex-col items-center justify-center text-center h-full min-h-[280px]">
              <div className="w-16 h-16 bg-accent/10 rounded-full flex items-center justify-center mb-4 group-hover:bg-accent/20 transition-colors">
                <Plus className="w-8 h-8 text-accent" />
              </div>
              <h3 className="text-lg font-semibold text-foreground mb-2">새 여행 계획</h3>
              <p className="text-muted-foreground text-sm">
                친구들과 함께 새로운 여행을<br />
                계획해보세요
              </p>
            </CardContent>
          </Card>
        </div>
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
