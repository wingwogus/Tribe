import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Country, CreateTripRequest } from "@/api/trips";
import { useToast } from "@/hooks/use-toast";
import { getCountryOptionByCode, getTripRegionsByCountryCode, TRIP_COUNTRY_OPTIONS } from "@/lib/tripRegions";

interface TripCreationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateTrip: (tripData: CreateTripRequest) => void;
}

export const TripCreationModal = ({ isOpen, onClose, onCreateTrip }: TripCreationModalProps) => {
  const { toast } = useToast();
  const [tripName, setTripName] = useState("");
  const [selectedCountry, setSelectedCountry] = useState<Country | "">("");
  const [selectedRegionCode, setSelectedRegionCode] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const selectedCountryOption = getCountryOptionByCode(selectedCountry);
  const availableRegions = getTripRegionsByCountryCode(selectedCountryOption?.code2);

  const handleCountryChange = (value: string) => {
    setSelectedCountry(value as Country);
    setSelectedRegionCode("");
  };

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
      regionCode: selectedRegionCode || null,
      startDate,
      endDate
    };

    onCreateTrip(tripData);
    
    // Reset form
    setTripName("");
    setSelectedCountry("");
    setSelectedRegionCode("");
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
            <Select value={selectedCountry} onValueChange={handleCountryChange}>
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
              <Select value={selectedRegionCode} onValueChange={setSelectedRegionCode}>
                <SelectTrigger>
                  <SelectValue placeholder="권역을 선택하세요" />
                </SelectTrigger>
                <SelectContent className="bg-background z-50 max-h-[260px]">
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
