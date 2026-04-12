import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Country, CreateTripRequest } from "@/api/trips";
import { useToast } from "@/hooks/use-toast";
import { AlertCircle } from "lucide-react";

interface TripCreationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateTrip: (tripData: CreateTripRequest) => void;
}

const countries = [
  { code: Country.SOUTH_KOREA, name: '대한민국', displayName: 'SOUTH_KOREA' },
  { code: Country.JAPAN, name: '일본', displayName: 'JAPAN' },
  { code: Country.CHINA, name: '중국', displayName: 'CHINA' },
  { code: Country.THAILAND, name: '태국', displayName: 'THAILAND' },
  { code: Country.VIETNAM, name: '베트남', displayName: 'VIETNAM' },
  { code: Country.PHILIPPINES, name: '필리핀', displayName: 'PHILIPPINES' },
  { code: Country.SINGAPORE, name: '싱가포르', displayName: 'SINGAPORE' },
  { code: Country.MALAYSIA, name: '말레이시아', displayName: 'MALAYSIA' },
  { code: Country.INDONESIA, name: '인도네시아', displayName: 'INDONESIA' },
  { code: Country.INDIA, name: '인도', displayName: 'INDIA' },
  { code: Country.UAE, name: '아랍에미리트', displayName: 'UAE' },
  { code: Country.TURKEY, name: '터키', displayName: 'TURKEY' },
  { code: Country.EGYPT, name: '이집트', displayName: 'EGYPT' },
  { code: Country.ITALY, name: '이탈리아', displayName: 'ITALY' },
  { code: Country.FRANCE, name: '프랑스', displayName: 'FRANCE' },
  { code: Country.SPAIN, name: '스페인', displayName: 'SPAIN' },
  { code: Country.UK, name: '영국', displayName: 'UK' },
  { code: Country.GERMANY, name: '독일', displayName: 'GERMANY' },
  { code: Country.SWITZERLAND, name: '스위스', displayName: 'SWITZERLAND' },
  { code: Country.NETHERLANDS, name: '네덜란드', displayName: 'NETHERLANDS' },
  { code: Country.GREECE, name: '그리스', displayName: 'GREECE' },
  { code: Country.USA, name: '미국', displayName: 'USA' },
  { code: Country.CANADA, name: '캐나다', displayName: 'CANADA' },
  { code: Country.AUSTRALIA, name: '호주', displayName: 'AUSTRALIA' },
  { code: Country.NEW_ZEALAND, name: '뉴질랜드', displayName: 'NEW_ZEALAND' },
  { code: Country.BRAZIL, name: '브라질', displayName: 'BRAZIL' },
  { code: Country.ARGENTINA, name: '아르헨티나', displayName: 'ARGENTINA' },
  { code: Country.MEXICO, name: '멕시코', displayName: 'MEXICO' },
  { code: Country.SOUTH_AFRICA, name: '남아프리카 공화국', displayName: 'SOUTH_AFRICA' },
  { code: Country.MOROCCO, name: '모로코', displayName: 'MOROCCO' }
];

export const TripCreationModal = ({ isOpen, onClose, onCreateTrip }: TripCreationModalProps) => {
  const { toast } = useToast();
  const [tripName, setTripName] = useState("");
  const [selectedCountry, setSelectedCountry] = useState<Country | "">("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const handleCreate = () => {
    // 여행 이름 검증
    if (!tripName || tripName.trim().length === 0) {
      toast({
        title: "여행 이름을 입력해주세요",
        description: "여행 이름은 필수 항목입니다.",
        variant: "destructive",
      });
      return;
    }

    if (tripName.trim().length > 100) {
      toast({
        title: "여행 이름이 너무 깁니다",
        description: "여행 이름은 100자 이내로 입력해주세요.",
        variant: "destructive",
      });
      return;
    }

    // 국가 선택 검증
    if (!selectedCountry) {
      toast({
        title: "국가를 선택해주세요",
        description: "여행할 국가를 선택해야 합니다.",
        variant: "destructive",
      });
      return;
    }

    // 시작일 검증
    if (!startDate) {
      toast({
        title: "시작일을 선택해주세요",
        description: "여행 시작일은 필수 항목입니다.",
        variant: "destructive",
      });
      return;
    }

    // 종료일 검증
    if (!endDate) {
      toast({
        title: "종료일을 선택해주세요",
        description: "여행 종료일은 필수 항목입니다.",
        variant: "destructive",
      });
      return;
    }

    // 날짜 순서 검증
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    if (end < start) {
      toast({
        title: "날짜를 확인해주세요",
        description: "종료일은 시작일보다 이전일 수 없습니다.",
        variant: "destructive",
      });
      return;
    }

    const tripData: CreateTripRequest = {
      title: tripName.trim(),
      country: selectedCountry as Country,
      startDate,
      endDate
    };

    onCreateTrip(tripData);
    
    // Reset form
    setTripName("");
    setSelectedCountry("");
    setStartDate("");
    setEndDate("");
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>새 여행 만들기</DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="tripName">여행 이름</Label>
            <Input
              id="tripName"
              placeholder="예: 오사카 여행"
              value={tripName}
              onChange={(e) => setTripName(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="country">여행 국가</Label>
            <Select value={selectedCountry} onValueChange={(value) => setSelectedCountry(value as Country)}>
              <SelectTrigger>
                <SelectValue placeholder="국가를 선택하세요" />
              </SelectTrigger>
              <SelectContent className="bg-background z-50 max-h-[300px]">
                {countries.map((country) => (
                  <SelectItem key={country.code} value={country.code}>
                    {country.name} ({country.displayName})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="startDate">시작일</Label>
              <Input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="endDate">종료일</Label>
              <Input
                id="endDate"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                min={startDate}
              />
            </div>
          </div>

          <div className="flex justify-end space-x-2">
            <Button variant="outline" onClick={onClose}>
              취소
            </Button>
            <Button 
              onClick={handleCreate}
              disabled={!tripName || !selectedCountry || !startDate || !endDate}
            >
              여행 만들기
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};