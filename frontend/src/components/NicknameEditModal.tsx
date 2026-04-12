import { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { updateNickname } from "@/api/auth";
import { useToast } from "@/hooks/use-toast";
import { User, Sparkles } from "lucide-react";

interface NicknameEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  currentNickname: string;
}

export const NicknameEditModal = ({ isOpen, onClose, onSuccess, currentNickname }: NicknameEditModalProps) => {
  const { toast } = useToast();
  const [nickname, setNickname] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setNickname(currentNickname);
    }
  }, [isOpen, currentNickname]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!nickname.trim()) {
      toast({
        title: "닉네임을 입력해주세요",
        variant: "destructive",
      });
      return;
    }

    if (nickname === currentNickname) {
      toast({
        title: "동일한 닉네임입니다",
        description: "새로운 닉네임을 입력해주세요.",
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const result = await updateNickname({nickname});
      if (result.success) {
        toast({
          title: "닉네임 변경 완료",
          description: `닉네임이 ${nickname}로 변경되었습니다.`,
        });
        onSuccess();
        onClose();
      } else {
        toast({
          title: "닉네임 변경 실패",
          description: result.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "오류 발생",
        description: "닉네임 변경 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader className="space-y-3 pb-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary/20 to-primary/10 border border-primary/20 shadow-lg">
              <User className="w-6 h-6 text-primary" />
            </div>
            <DialogTitle className="text-2xl font-bold">닉네임 변경</DialogTitle>
          </div>
          <DialogDescription className="text-base">
            새로운 닉네임을 입력해주세요.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-6 mt-2">
          <div className="space-y-3">
            <Label htmlFor="nickname" className="text-base font-semibold">현재 닉네임</Label>
            <div className="relative overflow-hidden rounded-xl bg-gradient-to-br from-muted/50 to-muted/20 border border-border p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-lg bg-primary/10">
                  <User className="w-5 h-5 text-primary" />
                </div>
                <p className="text-base font-medium text-foreground">{currentNickname}</p>
              </div>
            </div>
          </div>
          
          <div className="space-y-3">
            <Label htmlFor="nickname" className="text-base font-semibold flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-primary" />
              새로운 닉네임
            </Label>
            <Input
              id="nickname"
              type="text"
              placeholder="새로운 닉네임을 입력하세요"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              className="h-12 text-base"
              required
            />
          </div>
          
          <div className="flex gap-3 pt-2">
            <Button 
              type="button" 
              variant="outline" 
              onClick={onClose}
              className="flex-1 h-11 text-base"
              disabled={isLoading}
            >
              취소
            </Button>
            <Button 
              type="submit" 
              className="flex-1 h-11 text-base font-semibold shadow-lg" 
              disabled={isLoading}
            >
              {isLoading ? (
                <>변경 중...</>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 mr-2" />
                  닉네임 변경
                </>
              )}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
