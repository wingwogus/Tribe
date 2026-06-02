import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Loader2, CheckCircle, XCircle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { tripApi } from "@/api/trips";
import { useToast } from "@/hooks/use-toast";

const getInviteJoinErrorMessage = (error: unknown) => {
  const status = typeof error === "object" && error !== null && "response" in error
    ? (error as { response?: { status?: number } }).response?.status
    : undefined;

  if (status === 401) {
    return "로그인이 필요합니다.";
  }

  if (status === 404) {
    return "유효하지 않은 초대 링크입니다.";
  }

  if (status === 400) {
    return "이미 참여한 여행이거나 만료된 초대 링크입니다.";
  }

  return "여행 참여에 실패했습니다. 다시 시도해주세요.";
};

const InviteCallback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState("");
  const [tripTitle, setTripTitle] = useState("");

  useEffect(() => {
    const token = searchParams.get('token');
    
    if (!token) {
      setStatus('error');
      setErrorMessage('초대 토큰이 유효하지 않습니다.');
      return;
    }

    let redirectTimer: ReturnType<typeof setTimeout> | undefined;

    const joinTrip = async () => {
      try {
        const joinedTrip = await tripApi.joinTrip(token);
        setTripTitle(joinedTrip.title);
        setStatus('success');

        toast({
          title: "여행 참여 완료",
          description: `${joinedTrip.title} 여행에 참여했습니다.`,
        });

        // 3초 후 여행 페이지로 이동
        redirectTimer = setTimeout(() => {
          navigate(`/trip/${joinedTrip.tripId}`);
        }, 3000);
      } catch (error) {
        console.error('Failed to join trip:', error);
        const message = getInviteJoinErrorMessage(error);
        setStatus('error');
        setErrorMessage(message);

        toast({
          title: "오류",
          description: message,
          variant: "destructive",
        });
      }
    };

    void joinTrip();

    return () => {
      if (redirectTimer) {
        clearTimeout(redirectTimer);
      }
    };
  }, [navigate, searchParams, toast]);

  return (
    <div className="min-h-screen bg-gradient-subtle flex items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-center">여행 초대</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col items-center justify-center py-8 space-y-4">
          {status === 'loading' && (
            <>
              <Loader2 className="w-16 h-16 text-primary animate-spin" />
              <p className="text-lg font-medium text-foreground">여행에 참여하는 중...</p>
              <p className="text-sm text-muted-foreground">잠시만 기다려주세요</p>
            </>
          )}

          {status === 'success' && (
            <>
              <CheckCircle className="w-16 h-16 text-green-500" />
              <p className="text-lg font-medium text-foreground">참여 완료!</p>
              <p className="text-sm text-muted-foreground text-center">
                {tripTitle} 여행에 참여했습니다.
                <br />
                곧 여행 페이지로 이동합니다...
              </p>
            </>
          )}

          {status === 'error' && (
            <>
              <XCircle className="w-16 h-16 text-destructive" />
              <p className="text-lg font-medium text-foreground">참여 실패</p>
              <p className="text-sm text-muted-foreground text-center">{errorMessage}</p>
              <div className="flex gap-2 mt-4">
                <Button 
                  onClick={() => navigate('/')}
                  variant="outline"
                >
                  홈으로
                </Button>
                <Button 
                  onClick={() => navigate('/')}
                  className="bg-gradient-primary"
                >
                  로그인하기
                </Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default InviteCallback;
