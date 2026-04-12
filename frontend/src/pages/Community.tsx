import {useState} from "react";
import {useQueries, useQuery} from "@tanstack/react-query";
import {useNavigate} from "react-router-dom";
import {Button} from "@/components/ui/button";
import {ScrollArea, ScrollBar} from "@/components/ui/scroll-area";
import {Header} from "@/components/Header";
import {PostCard} from "@/components/PostCard";
import {CreatePostModal} from "@/components/CreatePostModal";
import {LoginModal} from "@/components/LoginModal";
import {NicknameEditModal} from "@/components/NicknameEditModal";
import {TripJoinModal} from "@/components/TripJoinModal";
import {communityApi} from "@/api/community";
import {getMemberInfo, logout, MemberResponse} from "@/api/auth";
import {Loader2, Plus} from "lucide-react";
import {useToast} from "@/hooks/use-toast";

// Major countries to display
const FEATURED_COUNTRIES = [
    {code: 'SOUTH_KOREA', name: '대한민국', emoji: '🇰🇷'},
    {code: 'JAPAN', name: '일본', emoji: '🇯🇵'},
    {code: 'CHINA', name: '중국', emoji: '🇨🇳'},
    {code: 'THAILAND', name: '태국', emoji: '🇹🇭'},
    {code: 'VIETNAM', name: '베트남', emoji: '🇻🇳'},
    {code: 'PHILIPPINES', name: '필리핀', emoji: '🇵🇭'},
    {code: 'SINGAPORE', name: '싱가포르', emoji: '🇸🇬'},
    {code: 'MALAYSIA', name: '말레이시아', emoji: '🇲🇾'},
    {code: 'INDONESIA', name: '인도네시아', emoji: '🇮🇩'},
    {code: 'INDIA', name: '인도', emoji: '🇮🇳'},
    {code: 'UAE', name: '아랍에미리트', emoji: '🇦🇪'},
    {code: 'TURKEY', name: '터키', emoji: '🇹🇷'},
    {code: 'EGYPT', name: '이집트', emoji: '🇪🇬'},
    {code: 'ITALY', name: '이탈리아', emoji: '🇮🇹'},
    {code: 'FRANCE', name: '프랑스', emoji: '🇫🇷'},
    {code: 'SPAIN', name: '스페인', emoji: '🇪🇸'},
    {code: 'UK', name: '영국', emoji: '🇬🇧'},
    {code: 'GERMANY', name: '독일', emoji: '🇩🇪'},
    {code: 'SWITZERLAND', name: '스위스', emoji: '🇨🇭'},
    {code: 'NETHERLANDS', name: '네덜란드', emoji: '🇳🇱'},
    {code: 'GREECE', name: '그리스', emoji: '🇬🇷'},
    {code: 'USA', name: '미국', emoji: '🇺🇸'},
    {code: 'CANADA', name: '캐나다', emoji: '🇨🇦'},
    {code: 'AUSTRALIA', name: '호주', emoji: '🇦🇺'},
    {code: 'NEW_ZEALAND', name: '뉴질랜드', emoji: '🇳🇿'},
    {code: 'BRAZIL', name: '브라질', emoji: '🇧🇷'},
    {code: 'ARGENTINA', name: '아르헨티나', emoji: '🇦🇷'},
    {code: 'MEXICO', name: '멕시코', emoji: '🇲🇽'},
    {code: 'SOUTH_AFRICA', name: '남아프리카 공화국', emoji: '🇿🇦'},
    {code: 'MOROCCO', name: '모로코', emoji: '🇲🇦'},
];

const Community = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  
  const [showCreatePostModal, setShowCreatePostModal] = useState(false);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showJoinTripModal, setShowJoinTripModal] = useState(false);
  const [showNicknameEditModal, setShowNicknameEditModal] = useState(false);
  // Get user info
  const { data: userInfo } = useQuery({
    queryKey: ['user'],
    queryFn: getMemberInfo,
    retry: false,
  });

  const isLoggedIn = !!userInfo;

  // Fetch posts for each country
  const countryQueries = useQueries({
    queries: FEATURED_COUNTRIES.map((country) => ({
      queryKey: ['posts', country.code],
      queryFn: () => communityApi.getPosts(country.code, 0, 10),
    })),
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

  const handleCreatePostClick = () => {
    if (!isLoggedIn) {
      toast({
        variant: "destructive",
        title: "로그인이 필요합니다",
      });
      setShowLoginModal(true);
      return;
    }
    setShowCreatePostModal(true);
  };

  return (
    <div className="min-h-screen bg-background">
      <Header
        isLoggedIn={isLoggedIn}
        user={userInfo || null}
        onLoginClick={handleLoginClick}
        onLogoutClick={handleLogoutClick}
        onJoinTripClick={() => setShowJoinTripModal(true)}
        onEditNicknameClick={() => setShowNicknameEditModal(true)}
      />

      <div className="container mx-auto px-4 py-8">
        {/* Page header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl md:text-3xl font-bold text-foreground mb-2">여행 커뮤니티</h1>
            <p className="text-sm md:text-base text-muted-foreground">다른 여행자들의 여행 계획을 둘러보세요</p>
          </div>
          <Button 
            onClick={handleCreatePostClick}
            className="bg-gradient-primary hover:shadow-primary"
          >
            <Plus className="w-4 h-4 mr-2" />
            포스트 작성
          </Button>
        </div>

        {/* Country sections */}
        <div className="space-y-8">
          {FEATURED_COUNTRIES.map((country, index) => {
            const query = countryQueries[index];
            const posts = query.data?.content || [];

            // Skip if no posts
            if (!query.isLoading && posts.length === 0) {
              return null;
            }

            return (
              <div key={country.code}>
                <div className="flex items-center gap-2 mb-4">
                  <span className="text-3xl">{country.emoji}</span>
                  <h2 className="text-2xl font-semibold">{country.name}</h2>
                  {query.isLoading && <Loader2 className="w-4 h-4 animate-spin text-muted-foreground" />}
                </div>

                {query.isLoading ? (
                  <div className="flex gap-4">
                    {[1, 2, 3].map((i) => (
                      <div key={i} className="w-80 h-64 bg-muted animate-pulse rounded-lg" />
                    ))}
                  </div>
                ) : (
                  <ScrollArea className="w-full whitespace-nowrap rounded-lg">
                    <div className="flex gap-4 pb-4">
                      {posts.map((post) => (
                        <PostCard
                          key={post.postId}
                          post={post}
                          onClick={() => navigate(`/post/${post.postId}`)}
                        />
                      ))}
                    </div>
                    <ScrollBar orientation="horizontal" />
                  </ScrollArea>
                )}
              </div>
            );
          })}
        </div>

        {/* Empty state */}
        {countryQueries.every(q => !q.isLoading) && 
         countryQueries.every(q => (q.data?.content.length || 0) === 0) && (
          <div className="text-center py-20">
            <p className="text-muted-foreground text-lg mb-4">
              아직 공유된 여행이 없습니다
            </p>
            <Button onClick={handleCreatePostClick}>
              첫 번째 여행을 공유해보세요
            </Button>
          </div>
        )}
      </div>

      {/* Modals */}
      <CreatePostModal
        isOpen={showCreatePostModal}
        onClose={() => setShowCreatePostModal(false)}
      />
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
        onJoinTrip={() => {
          setShowJoinTripModal(false);
          toast({ title: "여행에 참여했습니다" });
        }}
      />
      <NicknameEditModal
        isOpen={showNicknameEditModal}
        onClose={() => setShowNicknameEditModal(false)}
        currentNickname={(userInfo as MemberResponse | null)?.nickname || ''}
        onSuccess={() => {
          setShowNicknameEditModal(false);
          toast({ title: "닉네임이 변경되었습니다" });
        }}
      />
    </div>
  );
};

export default Community;
