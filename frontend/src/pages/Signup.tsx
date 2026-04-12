import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useNavigate } from "react-router-dom";
import { sendVerificationEmail, verifyEmailCode, checkNickname, signup } from "@/api/auth";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Check } from "lucide-react";

const Signup = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [isNicknameVerified, setIsNicknameVerified] = useState(false);

  const handleSendVerificationEmail = async () => {
    if (!email) {
      toast({
        title: "이메일을 입력해주세요",
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const result = await sendVerificationEmail({ email });
      if (result.success) {
        toast({
          title: "인증 코드 전송",
          description: result.message,
        });
        setStep(2);
      } else {
        toast({
          title: "전송 실패",
          description: result.message,
          variant: "destructive",
        });
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleVerifyCode = async () => {
    if (!verificationCode || verificationCode.length !== 6) {
      toast({
        title: "6자리 인증 코드를 입력해주세요",
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const result = await verifyEmailCode({ email, code: verificationCode });
      if (result.success) {
        toast({
          title: "인증 성공",
          description: result.message,
        });
        setIsEmailVerified(true);
        setStep(3);
      } else {
        toast({
          title: "인증 실패",
          description: result.message,
          variant: "destructive",
        });
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleCheckNickname = async () => {
    if (!nickname) {
      toast({
        title: "닉네임을 입력해주세요",
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const result = await checkNickname({ nickname });
      if (result.available) {
        toast({
          title: "사용 가능",
          description: result.message,
        });
        setIsNicknameVerified(true);
        setStep(4);
      } else {
        toast({
          title: "사용 불가",
          description: result.message,
          variant: "destructive",
        });
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleSignup = async () => {
    if (!password || !passwordConfirm) {
      toast({
        title: "비밀번호를 입력해주세요",
        variant: "destructive",
      });
      return;
    }

    if (password !== passwordConfirm) {
      toast({
        title: "비밀번호가 일치하지 않습니다",
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const result = await signup({ email, nickname, password });
      if (result.success) {
        toast({
          title: "회원가입 성공",
          description: result.message,
        });
        navigate("/");
      } else {
        toast({
          title: "회원가입 실패",
          description: result.message,
          variant: "destructive",
        });
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-subtle flex items-center justify-center p-6">
      <div className="w-full max-w-md">
        <Button
          variant="ghost"
          className="mb-4"
          onClick={() => navigate("/")}
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          돌아가기
        </Button>

        <Card>
          <CardHeader>
            <CardTitle className="text-2xl">회원가입</CardTitle>
            <CardDescription>
              단계 {step}/4: {
                step === 1 ? "이메일 입력" :
                step === 2 ? "코드 인증" :
                step === 3 ? "닉네임 설정" :
                "비밀번호 설정"
              }
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Step 1: Email */}
            {step === 1 && (
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="email">이메일</Label>
                  <Input
                    id="email"
                    type="email"
                    placeholder="example@email.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </div>
                <Button 
                  className="w-full" 
                  onClick={handleSendVerificationEmail}
                  disabled={isLoading}
                >
                  {isLoading ? "전송 중..." : "인증 코드 전송"}
                </Button>
              </div>
            )}

            {/* Step 2: Verification Code */}
            {step === 2 && (
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="code">인증 코드</Label>
                  <Input
                    id="code"
                    type="text"
                    placeholder="6자리 코드"
                    maxLength={6}
                    value={verificationCode}
                    onChange={(e) => setVerificationCode(e.target.value)}
                  />
                  <p className="text-sm text-muted-foreground">
                    {email}로 전송된 코드를 입력해주세요
                  </p>
                </div>
                <Button 
                  className="w-full" 
                  onClick={handleVerifyCode}
                  disabled={isLoading}
                >
                  {isLoading ? "확인 중..." : "코드 확인"}
                </Button>
              </div>
            )}

            {/* Step 3: Nickname */}
            {step === 3 && (
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="nickname">닉네임</Label>
                  <Input
                    id="nickname"
                    type="text"
                    placeholder="닉네임을 입력하세요"
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                  />
                </div>
                <Button 
                  className="w-full" 
                  onClick={handleCheckNickname}
                  disabled={isLoading}
                >
                  {isLoading ? "확인 중..." : "중복 확인"}
                </Button>
              </div>
            )}

            {/* Step 4: Password */}
            {step === 4 && (
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="password">비밀번호</Label>
                  <Input
                    id="password"
                    type="password"
                    placeholder="비밀번호를 입력하세요"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="passwordConfirm">비밀번호 확인</Label>
                  <Input
                    id="passwordConfirm"
                    type="password"
                    placeholder="비밀번호를 다시 입력하세요"
                    value={passwordConfirm}
                    onChange={(e) => setPasswordConfirm(e.target.value)}
                  />
                </div>
                <Button 
                  className="w-full" 
                  onClick={handleSignup}
                  disabled={isLoading}
                >
                  {isLoading ? "처리 중..." : "회원가입 완료"}
                </Button>
              </div>
            )}

            {/* Progress Indicators */}
            <div className="flex justify-center gap-2 pt-4">
              {[1, 2, 3, 4].map((s) => (
                <div
                  key={s}
                  className={`w-2 h-2 rounded-full ${
                    s <= step ? "bg-primary" : "bg-muted"
                  }`}
                />
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Signup;
