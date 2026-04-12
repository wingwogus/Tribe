import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Users, Receipt, CheckCircle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

interface ExpenseItem {
  id: string;
  title: string;
  date: string;
  paidBy: string;
  amount: {
    jpy: number;
    krw: number;
  };
  participants: string[];
  items: {
    name: string;
    price: number;
  }[];
}

const Settlement = () => {
  const { tripId } = useParams();
  const navigate = useNavigate();

  const expenses: ExpenseItem[] = [
    {
      id: "dotonbori-dinner",
      title: "도톤보리 저녁 식사",
      date: "2024.12.20",
      paidBy: "민준",
      amount: {
        jpy: 4800,
        krw: 45600
      },
      participants: ["민준", "서아"],
      items: [
        { name: "진저에일", price: 800 },
        { name: "생맥주(중)", price: 1200 },
        { name: "도톤보리 세트", price: 2800 }
      ]
    }
  ];

  const settlements = [
    {
      from: "서아",
      to: "민준",
      amount: 22800,
      reason: "도톤보리 저녁 식사 정산"
    }
  ];

  const totalAmount = expenses.reduce((sum, expense) => sum + expense.amount.krw, 0);

  return (
    <div className="min-h-screen bg-gradient-subtle">
      {/* Header */}
      <header className="bg-white shadow-soft">
        <div className="container mx-auto px-4 md:px-6 py-3 md:py-4">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
            <div className="flex items-center space-x-2 md:space-x-4">
              <Button 
                variant="ghost" 
                onClick={() => navigate(`/trip/${tripId}`)}
                className="hover:bg-primary/10"
                size="sm"
              >
                <ArrowLeft className="w-4 h-4 md:mr-2" />
                <span className="hidden md:inline">여행 계획으로</span>
              </Button>
              <div>
                <h1 className="text-lg md:text-2xl font-bold text-foreground">정산하기</h1>
                <p className="text-xs md:text-sm text-muted-foreground">일본 오사카 여행</p>
              </div>
            </div>
            <Badge variant="outline" className="border-success text-success text-xs md:text-sm">
              <CheckCircle className="w-3 h-3 md:w-4 md:h-4 mr-1" />
              정산 완료
            </Badge>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-4 md:px-6 py-4 md:py-6 space-y-4 md:space-y-6">
        {/* Settlement Summary */}
        <Card className="bg-gradient-primary text-primary-foreground">
          <CardHeader className="p-4 md:p-6">
            <CardTitle className="flex items-center text-white text-base md:text-lg">
              <Users className="w-4 h-4 md:w-5 md:h-5 mr-2" />
              정산 요약
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 md:space-y-4 p-4 md:p-6">
            <div className="text-center">
              <div className="text-2xl md:text-3xl font-bold mb-2">
                총 {totalAmount.toLocaleString()}원
              </div>
              <div className="text-sm md:text-base text-primary-glow">
                전체 여행 경비
              </div>
            </div>
            
            {settlements.map((settlement, idx) => (
              <div key={idx} className="bg-white/10 rounded-lg p-3 md:p-4">
                <div className="text-center">
                  <div className="text-base md:text-xl font-semibold">
                    {settlement.from}이 {settlement.to}에게
                  </div>
                  <div className="text-xl md:text-2xl font-bold text-accent-foreground mt-1">
                    {settlement.amount.toLocaleString()}원 보내기
                  </div>
                  <div className="text-xs md:text-sm text-primary-glow mt-2">
                    {settlement.reason}
                  </div>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Expense Details */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 md:gap-6">
          {/* Expense List */}
          <Card>
            <CardHeader className="p-4 md:p-6">
              <CardTitle className="flex items-center text-base md:text-lg">
                <Receipt className="w-4 h-4 md:w-5 md:h-5 mr-2 text-primary" />
                지출 내역
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 md:space-y-4 p-4 md:p-6">
              {expenses.map((expense) => (
                <div key={expense.id} className="border rounded-lg p-4 space-y-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-semibold text-foreground">{expense.title}</h3>
                      <div className="text-sm text-muted-foreground mt-1">
                        {expense.date} · {expense.paidBy}이 결제
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-semibold text-lg text-primary">
                        {expense.amount.krw.toLocaleString()}원
                      </div>
                      <div className="text-sm text-muted-foreground">
                        ¥{expense.amount.jpy.toLocaleString()}
                      </div>
                    </div>
                  </div>

                  <div className="pt-2 border-t">
                    <h4 className="text-sm font-medium text-foreground mb-2">구매 항목</h4>
                    <div className="space-y-1">
                      {expense.items.map((item, idx) => (
                        <div key={idx} className="flex justify-between text-sm">
                          <span className="text-muted-foreground">{item.name}</span>
                          <span className="text-foreground">¥{item.price.toLocaleString()}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="pt-2 border-t">
                    <div className="flex items-center space-x-2">
                      <span className="text-sm text-muted-foreground">참여자:</span>
                      {expense.participants.map((participant, idx) => (
                        <Badge key={idx} variant="secondary">{participant}</Badge>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          {/* Settlement Actions */}
          <Card>
            <CardHeader className="p-4 md:p-6">
              <CardTitle className="text-base md:text-lg">정산 방법</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 md:space-y-4 p-4 md:p-6">
              <div className="text-center p-6 bg-gradient-subtle rounded-lg">
                <div className="text-6xl mb-4">💳</div>
                <h3 className="text-lg font-semibold text-foreground mb-2">
                  간편 송금하기
                </h3>
                <p className="text-muted-foreground text-sm mb-4">
                  카카오페이, 토스 등으로<br />
                  간편하게 정산을 완료하세요
                </p>
                <Button className="w-full bg-gradient-sunset hover:shadow-accent">
                  송금 링크 공유
                </Button>
              </div>

              <div className="text-center p-6 bg-gradient-subtle rounded-lg">
                <div className="text-6xl mb-4">📊</div>
                <h3 className="text-lg font-semibold text-foreground mb-2">
                  정산 내역 공유
                </h3>
                <p className="text-muted-foreground text-sm mb-4">
                  모든 지출 내역과 정산 결과를<br />
                  그룹 채팅으로 공유하세요
                </p>
                <Button variant="outline" className="w-full">
                  내역 공유하기
                </Button>
              </div>

              <div className="text-center p-6 bg-gradient-subtle rounded-lg">
                <div className="text-6xl mb-4">📱</div>
                <h3 className="text-lg font-semibold text-foreground mb-2">
                  QR 코드 정산
                </h3>
                <p className="text-muted-foreground text-sm mb-4">
                  QR 코드를 스캔해서<br />
                  빠르게 정산하세요
                </p>
                <Button variant="outline" className="w-full">
                  QR 코드 생성
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
};

export default Settlement;