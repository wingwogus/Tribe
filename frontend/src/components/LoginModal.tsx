import {useState} from "react";
import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {useNavigate} from "react-router-dom";
import {login} from "@/api/auth";
import {OAUTH_LOGIN_URL} from "@/api/http";
import {useToast} from "@/hooks/use-toast";
import kakaoLoginButton from "@/assets/kakao_login_medium_narrow.png";

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLoginSuccess: () => void;
}

export const LoginModal = ({ isOpen, onClose, onLoginSuccess }: LoginModalProps) => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const result = await login({ email, password });

      if (result.success) {
        toast({
          title: "로그인 성공",
          description: "환영합니다!",
        });
        onClose();
        onLoginSuccess();
      } else {
        toast({
          title: "로그인 실패",
          description: result.message || "이메일 또는 비밀번호를 확인해주세요.",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "오류 발생",
        description: "로그인 중 오류가 발생했습니다.",
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleKakaoLogin = () => {
    window.location.href = OAUTH_LOGIN_URL;
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold text-center">로그인</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleLogin} className="space-y-4 mt-4">
          <div className="space-y-2">
            <Label htmlFor="email">이메일</Label>
            <Input
              id="email"
              type="email"
              placeholder="example@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">비밀번호</Label>
            <Input
              id="password"
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <Button type="submit" className="w-full" disabled={isLoading}>
            {isLoading ? "로그인 중..." : "로그인"}
          </Button>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-background px-2 text-muted-foreground">또는</span>
            </div>
          </div>

          <Button
              type="button"
              variant="outline"
              className="w-full bg-[#FEE500] hover:bg-[#FEE500]/90 text-[#000000] border-[#FEE500]"
              onClick={handleKakaoLogin}
          >
            <img
                src={kakaoLoginButton}
                alt="카카오로 시작하기"
                className="h-10"
            />
          </Button>

          <div className="text-center text-sm text-muted-foreground">
            계정이 없으신가요?{" "}
            <button
              type="button"
              className="text-primary hover:underline"
              onClick={() => {
                onClose();
                navigate("/signup");
              }}
            >
              회원가입
            </button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
