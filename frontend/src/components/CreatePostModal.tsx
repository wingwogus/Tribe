import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { communityApi, CreatePostRequest } from "@/api/community";
import { tripApi, SimpleTrip } from "@/api/trips";
import { useToast } from "@/hooks/use-toast";
import { Loader2 } from "lucide-react";

interface CreatePostModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const CreatePostModal = ({ isOpen, onClose }: CreatePostModalProps) => {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  
  const [selectedTripId, setSelectedTripId] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [imageFile, setImageFile] = useState<File | null>(null);

  // Fetch user's trips
  const { data: tripsData } = useQuery({
    queryKey: ['trips'],
    queryFn: () => tripApi.getTrips(0, 100),
    enabled: isOpen,
    staleTime: 0, // Always fetch fresh data when modal opens
  });

  // Create post mutation
  const createPostMutation = useMutation({
    mutationFn: (data: { request: CreatePostRequest; imageFile?: File }) => 
      communityApi.createPost(data.request, data.imageFile),
    onSuccess: () => {
      toast({
        title: "포스트 작성 완료",
        description: "커뮤니티에 여행이 공유되었습니다.",
      });
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      queryClient.invalidateQueries({ queryKey: ['trips'] });
      handleClose();
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "포스트 작성 실패",
        description: error.response?.data?.message || "포스트 작성에 실패했습니다.",
      });
    },
  });

  const handleClose = () => {
    setSelectedTripId(null);
    setTitle("");
    setContent("");
    setImageFile(null);
    onClose();
  };

  const handleSubmit = () => {
    if (!selectedTripId) {
      toast({
        variant: "destructive",
        title: "여행을 선택해주세요",
      });
      return;
    }
    if (!title.trim()) {
      toast({
        variant: "destructive",
        title: "제목을 입력해주세요",
      });
      return;
    }
    if (!content.trim()) {
      toast({
        variant: "destructive",
        title: "내용을 입력해주세요",
      });
      return;
    }

    createPostMutation.mutate({
      request: {
        tripId: selectedTripId,
        title: title.trim(),
        content: content.trim(),
      },
      imageFile: imageFile || undefined,
    });
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>포스트 작성</DialogTitle>
          <DialogDescription>
            여행 계획을 커뮤니티에 공유해보세요
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Trip selection */}
          <div className="space-y-2">
            <Label htmlFor="trip">여행 선택</Label>
            <Select 
              value={selectedTripId?.toString() || ""} 
              onValueChange={(value) => setSelectedTripId(Number(value))}
              disabled={!tripsData?.content || tripsData.content.length === 0}
            >
              <SelectTrigger>
                <SelectValue placeholder={
                  !tripsData ? "여행 목록을 불러오는 중..." :
                  tripsData.content?.length === 0 ? "생성된 여행이 없습니다" :
                  "공유할 여행을 선택하세요"
                } />
              </SelectTrigger>
              <SelectContent>
                {tripsData?.content?.map((trip: SimpleTrip) => (
                  <SelectItem key={trip.tripId} value={trip.tripId.toString()}>
                    {trip.title} ({trip.startDate} ~ {trip.endDate})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Post title */}
          <div className="space-y-2">
            <Label htmlFor="title">포스트 제목</Label>
            <Input
              id="title"
              placeholder="포스트 제목을 입력하세요"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={100}
            />
          </div>

          {/* Post content */}
          <div className="space-y-2">
            <Label htmlFor="content">여행 소개</Label>
            <Textarea
              id="content"
              placeholder="여행 소개 글을 작성해주세요"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={6}
              maxLength={1000}
            />
            <p className="text-xs text-muted-foreground text-right">
              {content.length} / 1000
            </p>
          </div>

          {/* Image upload */}
          <div className="space-y-2">
            <Label htmlFor="image">대표 이미지 (선택)</Label>
            <Input
              id="image"
              type="file"
              accept="image/*"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) {
                  setImageFile(file);
                }
              }}
            />
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={handleClose}>
            취소
          </Button>
          <Button 
            onClick={handleSubmit}
            disabled={createPostMutation.isPending}
          >
            {createPostMutation.isPending && (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            )}
            작성하기
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
