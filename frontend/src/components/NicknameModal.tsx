import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { updateNickname } from "@/api/auth";
import { useToast } from "@/hooks/use-toast";

interface NicknameModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const NicknameModal = ({ isOpen, onClose, onSuccess }: NicknameModalProps) => {
  const { toast } = useToast();
  const [nickname, setNickname] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!nickname.trim()) {
      toast({
        title: "닉네임을 입력해주세요",
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const result = await updateNickname({nickname});
      if (result.success) {
        toast({
          title: "닉네임 설정 완료",
          description: `닉네임이 ${nickname}로 변경되었습니다.`,
        });
        onSuccess();
        onClose();
      } else {
        toast({
          title: "닉네임 설정 실패",
          description: result.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "오류 발생",
        description: "닉네임 설정 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>닉네임 설정</DialogTitle>
          <DialogDescription>
            처음 로그인하셨네요! 사용하실 닉네임을 설정해주세요.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 mt-4">
          <div className="space-y-2">
            <Label htmlFor="nickname">닉네임</Label>
            <Input
              id="nickname"
              type="text"
              placeholder="닉네임을 입력하세요"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              required
            />
          </div>
          <Button type="submit" className="w-full" disabled={isLoading}>
            {isLoading ? "설정 중..." : "닉네임 설정"}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
};
