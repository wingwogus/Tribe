import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Upload, X } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { communityApi, UpdatePostRequest } from "@/api/community";

interface PostEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  postId: number;
  initialTitle: string;
  initialContent: string;
  initialImageUrl: string | null;
}

const editPostSchema = z.object({
  title: z.string().min(1, "제목을 입력해주세요").max(100, "제목은 100자 이하로 입력해주세요"),
  content: z.string().min(1, "내용을 입력해주세요").max(5000, "내용은 5000자 이하로 입력해주세요"),
});

type EditPostForm = z.infer<typeof editPostSchema>;

export const PostEditModal = ({
  isOpen,
  onClose,
  postId,
  initialTitle,
  initialContent,
  initialImageUrl,
}: PostEditModalProps) => {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(initialImageUrl);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<EditPostForm>({
    resolver: zodResolver(editPostSchema),
    defaultValues: {
      title: initialTitle,
      content: initialContent,
    },
  });

  useEffect(() => {
    if (isOpen) {
      reset({
        title: initialTitle,
        content: initialContent,
      });
      setImagePreview(initialImageUrl);
      setImageFile(null);
    }
  }, [isOpen, initialTitle, initialContent, initialImageUrl, reset]);

  const mutation = useMutation({
    mutationFn: async (data: EditPostForm) => {
      const request: UpdatePostRequest = {
        title: data.title,
        content: data.content,
      };
      return communityApi.updatePost(postId, request, imageFile || undefined);
    },
    onSuccess: () => {
      toast({
        title: "게시글 수정 완료",
        description: "게시글이 성공적으로 수정되었습니다.",
      });
      queryClient.invalidateQueries({ queryKey: ['post', postId] });
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      onClose();
    },
    onError: (error: any) => {
      const errorMessage = error.response?.data?.message || "게시글 수정에 실패했습니다.";
      toast({
        title: "수정 실패",
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        toast({
          title: "파일 크기 초과",
          description: "이미지 파일은 10MB 이하로 업로드해주세요.",
          variant: "destructive",
        });
        return;
      }
      setImageFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    setImagePreview(null);
  };

  const onSubmit = (data: EditPostForm) => {
    mutation.mutate(data);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>게시글 수정</DialogTitle>
          <DialogDescription>
            게시글의 제목, 내용, 대표 이미지를 수정할 수 있습니다.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-4">
          {/* Title */}
          <div>
            <Label htmlFor="title">제목 *</Label>
            <Input
              id="title"
              {...register("title")}
              placeholder="여행 제목을 입력하세요"
              className="mt-1"
            />
            {errors.title && (
              <p className="text-sm text-destructive mt-1">{errors.title.message}</p>
            )}
          </div>

          {/* Content */}
          <div>
            <Label htmlFor="content">내용 *</Label>
            <Textarea
              id="content"
              {...register("content")}
              placeholder="여행 이야기를 공유해주세요"
              className="mt-1 min-h-[200px]"
            />
            {errors.content && (
              <p className="text-sm text-destructive mt-1">{errors.content.message}</p>
            )}
          </div>

          {/* Image */}
          <div>
            <Label htmlFor="image">대표 이미지</Label>
            <div className="mt-2 space-y-3">
              {imagePreview && (
                <div className="relative inline-block">
                  <img
                    src={imagePreview}
                    alt="Preview"
                    className="w-full max-w-md h-48 object-cover rounded-lg border"
                  />
                  <Button
                    type="button"
                    variant="destructive"
                    size="icon"
                    className="absolute top-2 right-2"
                    onClick={handleRemoveImage}
                  >
                    <X className="w-4 h-4" />
                  </Button>
                </div>
              )}
              <div>
                <Input
                  id="image"
                  type="file"
                  accept="image/*"
                  onChange={handleImageChange}
                  className="hidden"
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => document.getElementById("image")?.click()}
                >
                  <Upload className="w-4 h-4 mr-2" />
                  {imagePreview ? "이미지 변경" : "이미지 업로드"}
                </Button>
                <p className="text-xs text-muted-foreground mt-1">
                  최대 10MB, JPG/PNG 형식
                </p>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end space-x-2 pt-4">
            <Button type="button" variant="outline" onClick={onClose} disabled={mutation.isPending}>
              취소
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
              수정하기
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
