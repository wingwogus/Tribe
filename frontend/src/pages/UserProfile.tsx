import {useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {useQuery} from "@tanstack/react-query";
import {ArrowLeft, Loader2} from "lucide-react";
import {Button} from "@/components/ui/button";
import {Card} from "@/components/ui/card";
import {Avatar, AvatarFallback, AvatarImage} from "@/components/ui/avatar";
import {Skeleton} from "@/components/ui/skeleton";
import {Header} from "@/components/Header";
import {PostCard} from "@/components/PostCard";
import {LoginModal} from "@/components/LoginModal";
import {NicknameEditModal} from "@/components/NicknameEditModal";
import {TripJoinModal} from "@/components/TripJoinModal";
import {authenticatedAxios, getMemberInfo, logout, MemberResponse} from "@/api/auth";
import {communityApi} from "@/api/community";
import {useToast} from "@/hooks/use-toast";

const UserProfile = () => {
  const { memberId } = useParams<{ memberId: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [page, setPage] = useState(0);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showJoinTripModal, setShowJoinTripModal] = useState(false);
  const [showNicknameEditModal, setShowNicknameEditModal] = useState(false);

  // Get current user info
  const { data: currentUser } = useQuery({
    queryKey: ['user'],
    queryFn: getMemberInfo,
    retry: false,
  });

  const isLoggedIn = !!currentUser;

  // Fetch member info
  const { data: memberData, isLoading: memberLoading, error: memberError } = useQuery({
    queryKey: ['member', memberId],
    queryFn: async () => {
      const response = await authenticatedAxios.get<{ data: MemberResponse }>(
        `/members/${memberId}`
      );
      return response.data.data;
    },
    enabled: !!memberId,
    retry: false,
  });

  // Fetch member's posts
  const { data: postsData, isLoading: postsLoading } = useQuery({
    queryKey: ['userPosts', memberId, page],
    queryFn: () => communityApi.getPostsByAuthorId(Number(memberId), page, 12),
    enabled: !!memberId,
  });

  const handleLoginClick = () => {
    setShowLoginModal(true);
  };

  const handleLogoutClick = async () => {
    try {
      await logout();
      toast({
        title: "로그아웃 완료",
      });
      window.location.href = '/';
    } catch (error) {
      toast({
        variant: "destructive",
        title: "로그아웃 실패",
      });
    }
  };

  const handleLoadMore = () => {
    setPage(prev => prev + 1);
  };

  if (memberLoading) {
    return (
      <div className="min-h-screen bg-background">
        <Header
          isLoggedIn={isLoggedIn}
          user={currentUser || null}
          onLoginClick={handleLoginClick}
          onLogoutClick={handleLogoutClick}
          onJoinTripClick={() => setShowJoinTripModal(true)}
          onEditNicknameClick={() => setShowNicknameEditModal(true)}
        />
        <div className="container mx-auto px-4 py-8">
          <Skeleton className="h-8 w-32 mb-8" />
          <div className="max-w-2xl mx-auto mb-12">
            <Card className="p-8 text-center">
              <Skeleton className="w-24 h-24 rounded-full mx-auto mb-4" />
              <Skeleton className="h-8 w-48 mx-auto mb-2" />
              <Skeleton className="h-4 w-64 mx-auto" />
            </Card>
          </div>
        </div>
      </div>
    );
  }

  if (memberError || !memberData) {
    return (
      <div className="min-h-screen bg-background">
        <Header
          isLoggedIn={isLoggedIn}
          user={currentUser || null}
          onLoginClick={handleLoginClick}
          onLogoutClick={handleLogoutClick}
          onJoinTripClick={() => setShowJoinTripModal(true)}
          onEditNicknameClick={() => setShowNicknameEditModal(true)}
        />
        <div className="container mx-auto px-4 py-8">
          <Button
            variant="ghost"
            onClick={() => navigate(-1)}
            className="mb-8"
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            뒤로가기
          </Button>
          <div className="max-w-2xl mx-auto">
            <Card className="p-8 text-center">
              <p className="text-muted-foreground mb-4">사용자를 찾을 수 없습니다</p>
              <Button onClick={() => navigate('/community')}>
                커뮤니티로 돌아가기
              </Button>
            </Card>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header
        isLoggedIn={isLoggedIn}
        user={currentUser || null}
        onLoginClick={handleLoginClick}
        onLogoutClick={handleLogoutClick}
        onJoinTripClick={() => setShowJoinTripModal(true)}
        onEditNicknameClick={() => setShowNicknameEditModal(true)}
      />

      <div className="container mx-auto px-4 py-8">
        {/* Back button */}
        <Button
          variant="ghost"
          onClick={() => navigate(-1)}
          className="mb-8"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          뒤로가기
        </Button>

        {/* Profile section */}
        <div className="max-w-2xl mx-auto mb-12">
          <Card className="p-8 text-center">
            <Avatar className="w-24 h-24 mx-auto mb-4">
              <AvatarImage
                  className="object-cover"
                  src={memberData.avatar || undefined} />
              <AvatarFallback className="text-2xl bg-gradient-primary text-white">
                {memberData.nickname[0]?.toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <h2 className="text-2xl font-bold text-foreground mb-2">
              {memberData.nickname}
            </h2>
            <p className="text-muted-foreground">
              {memberData.email}
            </p>
          </Card>
        </div>

        {/* Posts section */}
        <div className="mb-8">
          <h3 className="text-xl font-semibold text-foreground mb-6">작성한 게시글</h3>

          {postsLoading && page === 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {[1, 2, 3, 4].map((i) => (
                <Skeleton key={i} className="h-80 w-full" />
              ))}
            </div>
          ) : postsData && postsData.content.length > 0 ? (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {postsData.content.map((post) => (
                  <PostCard
                    key={post.postId}
                    post={post}
                    onClick={() => navigate(`/post/${post.postId}`)}
                  />
                ))}
              </div>

              {/* Load more button */}
              {postsData.content.length >= 12 && (
                <div className="flex justify-center mt-8">
                  <Button
                    onClick={handleLoadMore}
                    disabled={postsLoading}
                    className="bg-gradient-primary hover:shadow-primary"
                  >
                    {postsLoading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        로딩 중...
                      </>
                    ) : (
                      '더 보기'
                    )}
                  </Button>
                </div>
              )}
            </>
          ) : (
            <div className="text-center py-12">
              <p className="text-muted-foreground">아직 작성한 게시글이 없습니다</p>
            </div>
          )}
        </div>
      </div>

      {/* Modals */}
      <LoginModal 
        isOpen={showLoginModal} 
        onClose={() => setShowLoginModal(false)}
        onLoginSuccess={() => {
          setShowLoginModal(false);
          window.location.reload();
        }}
      />
      <TripJoinModal 
        isOpen={showJoinTripModal} 
        onClose={() => setShowJoinTripModal(false)}
        onJoinTrip={async () => {
          setShowJoinTripModal(false);
        }}
      />
      <NicknameEditModal
        isOpen={showNicknameEditModal}
        onClose={() => setShowNicknameEditModal(false)}
        onSuccess={() => {
          setShowNicknameEditModal(false);
          window.location.reload();
        }}
        currentNickname={currentUser?.nickname || ''}
      />
    </div>
  );
};

export default UserProfile;
