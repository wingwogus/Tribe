import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2 } from "lucide-react";

interface TripJoinModalProps {
  isOpen: boolean;
  onClose: () => void;
  onJoinTrip: (token: string) => void | Promise<void>;
}

export const TripJoinModal = ({ isOpen, onClose, onJoinTrip }: TripJoinModalProps) => {
  const [input, setInput] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Extract token from URL or return the input as-is
  const extractToken = (value: string): string => {
    const trimmed = value.trim();
    
    // Check if it's a URL
    try {
      const url = new URL(trimmed);
      const tokenParam = url.searchParams.get('token');
      if (tokenParam) {
        return tokenParam;
      }
    } catch {
      // Not a valid URL, treat as token
    }
    
    return trimmed;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;

    const token = extractToken(input);
    if (!token) return;

    setIsSubmitting(true);
    try {
      await onJoinTrip(token);
      setInput("");
      onClose();
    } catch (error) {
      // Error handled by parent
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>여행 참여하기</DialogTitle>
          <DialogDescription>
            초대 링크의 토큰을 입력하여 여행에 참여하세요
          </DialogDescription>
        </DialogHeader>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="token">초대 링크 또는 토큰</Label>
            <Input
              id="token"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="초대 링크 또는 토큰을 입력하세요"
              disabled={isSubmitting}
            />
            <p className="text-xs text-muted-foreground">
              전체 URL 또는 토큰만 입력 가능합니다
            </p>
          </div>

          <div className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isSubmitting}
            >
              취소
            </Button>
            <Button
              type="submit"
              disabled={!input.trim() || isSubmitting}
              className="bg-gradient-primary"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  참여 중...
                </>
              ) : (
                "참여하기"
              )}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
