import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { useToast } from "@/hooks/use-toast";

const OAuthCallback = () => {
  const { toast } = useToast();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const accessToken = searchParams.get("accessToken");

    if (accessToken) {
      localStorage.setItem("accessToken", accessToken);

      toast({
        title: "로그인 성공",
        description: "카카오 로그인이 완료되었습니다.",
      });

      // Navigate to home
      window.location.replace("/");
    } else {
      toast({
        title: "로그인 실패",
        description: "로그인 정보를 받아오는데 실패했습니다.",
        variant: "destructive",
      });
      window.location.replace("/");
    }
  }, [searchParams, toast]);

  return (
    <div className="min-h-screen bg-gradient-subtle flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
        <p className="text-lg text-muted-foreground">로그인 처리 중...</p>
      </div>
    </div>
  );
};

export default OAuthCallback;
