import {useEffect, useState} from "react";
import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select";
import {Country, TripDetail, UpdateTripRequest} from "@/api/trips";
import {useToast} from "@/hooks/use-toast";

interface TripEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  trip: TripDetail;
  onUpdateTrip: (updates: UpdateTripRequest) => void;
}

const countryOptions = [
  { code: Country.SOUTH_KOREA, name: '대한민국', displayName: 'SOUTH_KOREA', code2: "KR" },
  { code: Country.JAPAN, name: '일본', displayName: 'JAPAN', code2: "JP" },
  { code: Country.CHINA, name: '중국', displayName: 'CHINA', code2: "CN"  },
  { code: Country.THAILAND, name: '태국', displayName: 'THAILAND', code2: "TH"  },
  { code: Country.VIETNAM, name: '베트남', displayName: 'VIETNAM', code2: "VN"  },
  { code: Country.PHILIPPINES, name: '필리핀', displayName: 'PHILIPPINES', code2: "PH"  },
  { code: Country.SINGAPORE, name: '싱가포르', displayName: 'SINGAPORE', code2: "SG"  },
  { code: Country.MALAYSIA, name: '말레이시아', displayName: 'MALAYSIA', code2: "MY"  },
  { code: Country.INDONESIA, name: '인도네시아', displayName: 'INDONESIA', code2: "ID"  },
  { code: Country.INDIA, name: '인도', displayName: 'INDIA', code2: "IN"  },
  { code: Country.UAE, name: '아랍에미리트', displayName: 'UAE', code2: "AE"  },
  { code: Country.TURKEY, name: '터키', displayName: 'TURKEY', code2: "TR"  },
  { code: Country.EGYPT, name: '이집트', displayName: 'EGYPT', code2: "EG"  },
  { code: Country.ITALY, name: '이탈리아', displayName: 'ITALY', code2: "IT"  },
  { code: Country.FRANCE, name: '프랑스', displayName: 'FRANCE', code2: "FR"  },
  { code: Country.SPAIN, name: '스페인', displayName: 'SPAIN', code2: "ES"  },
  { code: Country.UK, name: '영국', displayName: 'UK', code2: "GB"  },
  { code: Country.GERMANY, name: '독일', displayName: 'GERMANY', code2: "DE"  },
  { code: Country.SWITZERLAND, name: '스위스', displayName: 'SWITZERLAND', code2: "CH"  },
  { code: Country.NETHERLANDS, name: '네덜란드', displayName: 'NETHERLANDS', code2: "NL"  },
  { code: Country.GREECE, name: '그리스', displayName: 'GREECE', code2: "GR"  },
  { code: Country.USA, name: '미국', displayName: 'USA', code2: "US"  },
  { code: Country.CANADA, name: '캐나다', displayName: 'CANADA', code2: "CA"  },
  { code: Country.AUSTRALIA, name: '호주', displayName: 'AUSTRALIA', code2: "AU"  },
  { code: Country.NEW_ZEALAND, name: '뉴질랜드', displayName: 'NEW_ZEALAND', code2: "NZ"  },
  { code: Country.BRAZIL, name: '브라질', displayName: 'BRAZIL', code2: "BR"  },
  { code: Country.ARGENTINA, name: '아르헨티나', displayName: 'ARGENTINA', code2: "AR"  },
  { code: Country.MEXICO, name: '멕시코', displayName: 'MEXICO', code2: "MX"  },
  { code: Country.SOUTH_AFRICA, name: '남아프리카 공화국', displayName: 'SOUTH_AFRICA', code2: "ZA"  },
  { code: Country.MOROCCO, name: '모로코', displayName: 'MOROCCO', code2: "MA"  }
];

export const TripEditModal = ({ isOpen, onClose, trip, onUpdateTrip }: TripEditModalProps) => {
  const { toast } = useToast();
  const [title, setTitle] = useState(trip.title);
  const [startDate, setStartDate] = useState(trip.startDate);
  const [endDate, setEndDate] = useState(trip.endDate);
  const [country, setCountry] = useState<string>('');

  useEffect(() => {
    if (trip) {
      setTitle(trip.title);
      setStartDate(trip.startDate);
      setEndDate(trip.endDate);
      const countryOption = countryOptions.find(opt => opt.code2 == trip.country);
      if (countryOption) {
        setCountry(countryOption.code);
      }
    }
  }, [trip]);

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
            <Select value={country} onValueChange={setCountry}>
              <SelectTrigger>
                <SelectValue placeholder="국가를 선택하세요" />
              </SelectTrigger>
              <SelectContent className="bg-background z-50 max-h-[300px]">
                {countryOptions.map((country) => (
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
