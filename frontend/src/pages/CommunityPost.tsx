import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Copy, Loader2, MoreVertical, Pencil, Trash2, UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { communityApi } from "@/api/community";
import { getMemberInfo, type MemberResponse } from "@/api/auth";
import { useToast } from "@/hooks/use-toast";
import { TripImportModal } from "@/components/TripImportModal";
import { PostEditModal } from "@/components/PostEditModal";
import { PostDeleteDialog } from "@/components/PostDeleteDialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const CommunityPost = () => {
  const { postId } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  const { data: currentUser } = useQuery<MemberResponse | null>({
    queryKey: ["user"],
    queryFn: getMemberInfo,
  });

  const { data: post, isLoading } = useQuery({
    queryKey: ["post", postId],
    queryFn: () => communityApi.getPostById(Number(postId)),
    enabled: !!postId,
  });

  const fallbackDates = useMemo(() => {
    const start = new Date();
    const end = new Date(start);
    end.setDate(end.getDate() + 2);

    return {
      startDate: start.toISOString().slice(0, 10),
      endDate: end.toISOString().slice(0, 10),
    };
  }, []);

  const isAuthor = !!currentUser && !!post && currentUser.memberId === post.authorId;

  const handleImport = () => {
    if (!currentUser) {
      toast({
        title: "로그인이 필요합니다",
        description: "여행을 복사하려면 먼저 로그인해주세요.",
        variant: "destructive",
      });
      return;
    }

    setIsImportModalOpen(true);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-subtle flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-8 h-8 mx-auto mb-4 animate-spin text-primary" />
          <p className="text-muted-foreground">게시글을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="min-h-screen bg-gradient-subtle flex items-center justify-center px-6">
        <Card className="w-full max-w-lg">
          <CardContent className="py-10 text-center space-y-4">
            <p className="text-muted-foreground">게시글을 찾을 수 없습니다.</p>
            <Button onClick={() => navigate("/community")}>커뮤니티로 돌아가기</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-subtle">
      <header className="bg-white shadow-soft">
        <div className="container mx-auto px-4 md:px-6 py-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <Button variant="ghost" onClick={() => navigate("/community")} size="sm">
              <ArrowLeft className="w-4 h-4 mr-2" />
              뒤로가기
            </Button>
            <div>
              <h1 className="text-xl md:text-2xl font-bold text-foreground">{post.title}</h1>
              <p className="text-sm text-muted-foreground">{post.country}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button onClick={handleImport} className="bg-gradient-primary hover:shadow-primary" size="sm">
              <Copy className="w-4 h-4 mr-2" />
              여행 복사하기
            </Button>

            {isAuthor && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon">
                    <MoreVertical className="w-4 h-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem onClick={() => setIsEditModalOpen(true)}>
                    <Pencil className="w-4 h-4 mr-2" />
                    수정
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    onClick={() => setIsDeleteDialogOpen(true)}
                    className="text-destructive focus:text-destructive"
                  >
                    <Trash2 className="w-4 h-4 mr-2" />
                    삭제
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 md:px-6 py-8">
        <div className="grid gap-6 lg:grid-cols-[minmax(0,2fr)_320px]">
          <Card className="overflow-hidden">
            {post.representativeImageUrl && (
              <img
                src={post.representativeImageUrl}
                alt={post.title}
                className="w-full h-72 object-cover border-b"
              />
            )}
            <CardHeader>
              <CardTitle className="text-2xl">{post.title}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-muted-foreground">
                현재 백엔드는 이 게시글에 예전처럼 일정 상세 payload를 포함하지 않습니다. 그래서 이 화면은
                게시글 본문 중심으로 재구성됐고, 여행 복사는 제목과 날짜를 다시 지정하는 방식으로 동작합니다.
              </p>
              <div className="prose prose-sm max-w-none whitespace-pre-wrap text-foreground">
                {post.content}
              </div>
            </CardContent>
          </Card>

          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">작성자</CardTitle>
              </CardHeader>
              <CardContent className="flex items-center gap-3">
                <Avatar
                  className="cursor-pointer"
                  onClick={() => navigate(`/profile/${post.authorId}`)}
                >
                  <AvatarFallback className="bg-gradient-primary text-white">
                    {post.authorNickname[0]?.toUpperCase() || <UserRound className="w-4 h-4" />}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <button
                    type="button"
                    onClick={() => navigate(`/profile/${post.authorId}`)}
                    className="font-medium hover:underline"
                  >
                    {post.authorNickname}
                  </button>
                  <p className="text-sm text-muted-foreground">{post.country}</p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">가져오기 안내</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <p>현재 백엔드 기준으로 게시글 상세에는 공유 일정 구조가 포함되지 않습니다.</p>
                <p>그래서 복사 시에는 게시글을 기준으로 새 여행을 만들고, 일정은 백엔드가 지원하는 범위에서 가져옵니다.</p>
                <Button onClick={handleImport} className="w-full bg-gradient-primary hover:shadow-primary">
                  이 게시글로 여행 만들기
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>

      <TripImportModal
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        postId={post.postId}
        originalTitle={post.title}
        originalStartDate={fallbackDates.startDate}
        originalEndDate={fallbackDates.endDate}
      />

      <PostEditModal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        postId={post.postId}
        initialTitle={post.title}
        initialContent={post.content}
        initialImageUrl={post.representativeImageUrl}
      />

      <PostDeleteDialog
        isOpen={isDeleteDialogOpen}
        onClose={() => setIsDeleteDialogOpen(false)}
        postId={post.postId}
        postTitle={post.title}
      />
    </div>
  );
};

export default CommunityPost;
