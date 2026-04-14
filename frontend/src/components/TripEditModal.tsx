import {useEffect, useState} from "react";
import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select";
import {Country, TripDetail, UpdateTripRequest} from "@/api/trips";
import {useToast} from "@/hooks/use-toast";
import {getCountryOptionByCode2, getTripRegionsByCountryCode, TRIP_COUNTRY_OPTIONS} from "@/lib/tripRegions";

interface TripEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  trip: TripDetail;
  onUpdateTrip: (updates: UpdateTripRequest) => void;
}

const NO_REGION_VALUE = "__NONE__";

export const TripEditModal = ({ isOpen, onClose, trip, onUpdateTrip }: TripEditModalProps) => {
  const { toast } = useToast();
  const [title, setTitle] = useState(trip.title);
  const [startDate, setStartDate] = useState(trip.startDate);
  const [endDate, setEndDate] = useState(trip.endDate);
  const [country, setCountry] = useState<string>('');
  const [regionCode, setRegionCode] = useState("");
  const selectedCountryOption = TRIP_COUNTRY_OPTIONS.find((option) => option.code === country);
  const availableRegions = getTripRegionsByCountryCode(selectedCountryOption?.code2);

  useEffect(() => {
    if (trip) {
      setTitle(trip.title);
      setStartDate(trip.startDate);
      setEndDate(trip.endDate);
      const countryOption = getCountryOptionByCode2(trip.country);
      if (countryOption) {
        setCountry(countryOption.code);
      }
      setRegionCode(trip.regionCode ?? NO_REGION_VALUE);
    }
  }, [trip]);

  const handleCountryChange = (value: string) => {
    setCountry(value);
    setRegionCode("");
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // 여행 이름 검증
    if (!title || title.trim().length === 0) {
      toast({
        title: "여행 이름을 입력해주세요",
        description: "여행 이름은 필수 항목입니다.",
        variant: "destructive",
      });
      return;
    }

    if (title.trim().length > 100) {
      toast({
        title: "여행 이름이 너무 깁니다",
        description: "여행 이름은 100자 이내로 입력해주세요.",
        variant: "destructive",
      });
      return;
    }

    // 국가 선택 검증
    if (!country) {
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

    onUpdateTrip({
      title: title.trim(),
      startDate,
      endDate,
      country: country as Country,
      regionCode: regionCode === NO_REGION_VALUE ? "" : regionCode || null,
    });
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>여행 정보 수정</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title">여행 제목</Label>
            <Input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="여행 제목을 입력하세요"
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="country">국가</Label>
            <Select value={country} onValueChange={handleCountryChange}>
              <SelectTrigger>
                <SelectValue placeholder="국가를 선택하세요" />
              </SelectTrigger>
              <SelectContent className="bg-background z-50 max-h-[300px]">
                {TRIP_COUNTRY_OPTIONS.map((country) => (
                    <SelectItem key={country.code} value={country.code}>
                      {country.name} ({country.displayName})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {availableRegions.length > 0 && (
            <div className="space-y-2">
              <Label htmlFor="regionCode">여행 권역</Label>
              <Select value={regionCode} onValueChange={setRegionCode}>
                <SelectTrigger>
                  <SelectValue placeholder="권역을 선택하세요" />
                </SelectTrigger>
                <SelectContent className="bg-background z-50 max-h-[260px]">
                  <SelectItem value={NO_REGION_VALUE}>권역 없음</SelectItem>
                  {availableRegions.map((region) => (
                    <SelectItem key={region.code} value={region.code}>
                      {region.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="startDate">시작일</Label>
              <div className="relative">
                <Input
                  id="startDate"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="endDate">종료일</Label>
              <div className="relative">
                <Input
                  id="endDate"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                  min={startDate}
                />
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={onClose}>
              취소
            </Button>
            <Button type="submit" className="bg-gradient-primary">
              수정하기
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
