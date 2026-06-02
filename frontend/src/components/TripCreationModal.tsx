import { useMemo, useState } from "react";
import { ko } from "date-fns/locale";
import { type DateRange } from "react-day-picker";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { ArrowLeft, ArrowRight, CalendarDays, Check, MapPin, Plane, X } from "lucide-react";

import { Country, CreateTripRequest } from "@/api/trips";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Dialog,
  DialogClose,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { useToast } from "@/hooks/use-toast";
import { cn } from "@/lib/utils";
import {
  getCountryOptionByCode2,
  TRIP_COUNTRY_OPTIONS,
  TRIP_REGIONS,
  type TripCountryOption,
  type TripRegionCatalogEntry,
} from "@/lib/tripRegions";

interface TripCreationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateTrip: (tripData: CreateTripRequest) => void;
}

type TripCreationStep = 0 | 1 | 2;

type DestinationOption = {
  id: string;
  label: string;
  searchText: string;
  country: Country;
  regionCode: string | null;
  type: "country" | "region";
};

const steps = ["장소", "날짜", "여행 이름"] as const;
const recommendedRegionCodes = ["JP_TOKYO", "JP_OSAKA_KYOTO", "VN_DA_NANG_HOI_AN", "KR_JEJU"];
const recommendedDestinationLabels: Record<string, string> = {
  JP_TOKYO: "도쿄, 일본",
  JP_OSAKA_KYOTO: "오사카, 일본",
  VN_DA_NANG_HOI_AN: "다낭, 베트남",
  KR_JEJU: "제주도, 한국",
};

const toDateInputValue = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const addDays = (date: Date, days: number) => {
  const nextDate = new Date(date);
  nextDate.setDate(nextDate.getDate() + days);
  return nextDate;
};

const getUpcomingWeekendRange = (): DateRange => {
  const today = new Date();
  const daysUntilSaturday = (6 - today.getDay() + 7) % 7;
  const from = addDays(today, daysUntilSaturday);
  return { from, to: addDays(from, 1) };
};

const getOneWeekRange = (baseDate?: Date): DateRange => {
  const from = baseDate ?? new Date();
  return { from, to: addDays(from, 6) };
};

const getDayCount = (range: DateRange) => {
  if (!range.from || !range.to) return null;
  const from = new Date(range.from.getFullYear(), range.from.getMonth(), range.from.getDate());
  const to = new Date(range.to.getFullYear(), range.to.getMonth(), range.to.getDate());
  const days = Math.round((to.getTime() - from.getTime()) / 86_400_000) + 1;
  return days > 0 ? days : null;
};

const formatKoreanDate = (date: Date) =>
  date.toLocaleDateString("ko-KR", {
    month: "numeric",
    day: "numeric",
    weekday: "short",
  });

const formatDateRangeSummary = (range: DateRange) => {
  if (!range.from || !range.to) {
    return "날짜를 선택해주세요";
  }

  return `${formatKoreanDate(range.from)} - ${formatKoreanDate(range.to)}`;
};

const createCountryDestinationOption = (country: TripCountryOption): DestinationOption => ({
  id: `country:${country.code}`,
  label: country.name,
  searchText: [country.name, country.displayName, country.code, country.code2].join(" ").toLowerCase(),
  country: country.code,
  regionCode: null,
  type: "country",
});

const createRegionDestinationOption = (region: TripRegionCatalogEntry): DestinationOption | null => {
  const country = getCountryOptionByCode2(region.countryCode);
  if (!country) return null;

  const label = `${region.label}, ${country.name}`;

  return {
    id: `region:${region.code}`,
    label,
    searchText: [label, region.code, region.type, ...region.searchHints, country.displayName, country.code2]
      .join(" ")
      .toLowerCase(),
    country: country.code,
    regionCode: region.code,
    type: "region",
  };
};

const destinationOptions = [
  ...TRIP_COUNTRY_OPTIONS.map(createCountryDestinationOption),
  ...TRIP_REGIONS.map(createRegionDestinationOption).filter(
    (option): option is DestinationOption => Boolean(option),
  ),
];

const recommendedDestinations = recommendedRegionCodes
  .map((code) => destinationOptions.find((option) => option.id === `region:${code}`))
  .filter((option): option is DestinationOption => Boolean(option));

export const TripCreationModal = ({ isOpen, onClose, onCreateTrip }: TripCreationModalProps) => {
  const { toast } = useToast();
  const [currentStep, setCurrentStep] = useState<TripCreationStep>(0);
  const [tripName, setTripName] = useState("");
  const [placeQuery, setPlaceQuery] = useState("");
  const [selectedDestination, setSelectedDestination] = useState<DestinationOption | null>(null);
  const [dateRange, setDateRange] = useState<DateRange>({ from: undefined, to: undefined });

  const filteredDestinationOptions = useMemo(() => {
    const normalizedQuery = placeQuery.trim().toLowerCase();
    if (!normalizedQuery) return [];

    return destinationOptions
      .filter((option) => option.searchText.includes(normalizedQuery))
      .slice(0, 8);
  }, [placeQuery]);

  const selectedDayCount = getDayCount(dateRange);
  const isDateRangeValid = Boolean(dateRange.from && dateRange.to && selectedDayCount);
  const canMoveNext =
    (currentStep === 0 && Boolean(selectedDestination)) ||
    (currentStep === 1 && isDateRangeValid) ||
    (currentStep === 2 && tripName.trim().length > 0 && tripName.trim().length <= 100);

  const resetForm = () => {
    setCurrentStep(0);
    setTripName("");
    setPlaceQuery("");
    setSelectedDestination(null);
    setDateRange({ from: undefined, to: undefined });
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      resetForm();
      onClose();
    }
  };

  const handlePlaceQueryChange = (value: string) => {
    setPlaceQuery(value);
    if (selectedDestination && selectedDestination.label !== value) {
      setSelectedDestination(null);
    }
  };

  const handleDestinationSelect = (destination: DestinationOption) => {
    setSelectedDestination(destination);
    setPlaceQuery(destination.label);
  };

  const goToNextStep = () => {
    if (!canMoveNext) return;

    if (currentStep < 2) {
      setCurrentStep((currentStep + 1) as TripCreationStep);
      return;
    }

    handleCreate();
  };

  const goToPreviousStep = () => {
    if (currentStep === 0) return;
    setCurrentStep((currentStep - 1) as TripCreationStep);
  };

  const handleCreate = () => {
    if (!selectedDestination) {
      toast({
        title: "장소를 선택해주세요",
        description: "검색 결과 또는 추천 여행지에서 국가/권역을 선택해야 합니다.",
        variant: "destructive",
      });
      return;
    }

    if (!dateRange.from || !dateRange.to || !selectedDayCount) {
      toast({
        title: "날짜를 선택해주세요",
        description: "여행 시작일과 종료일을 선택해야 합니다.",
        variant: "destructive",
      });
      return;
    }

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

    const tripData: CreateTripRequest = {
      title: tripName.trim(),
      country: selectedDestination.country,
      regionCode: selectedDestination.regionCode,
      startDate: toDateInputValue(dateRange.from),
      endDate: toDateInputValue(dateRange.to),
    };

    onCreateTrip(tripData);
    resetForm();
    onClose();
  };

  const primaryButtonLabel = "다음";
  const hasPreviousButton = currentStep === 2;
  const footerClassName = cn("flex w-full items-center gap-2", hasPreviousButton ? "justify-between" : "justify-end");

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogPortal>
        <DialogOverlay className="bg-black/40 backdrop-blur-sm" />
        <DialogPrimitive.Content
          className={cn(
            "fixed left-1/2 top-1/2 z-50 flex max-h-[calc(100vh-2rem)] w-[calc(100vw-2rem)] max-w-2xl -translate-x-1/2 -translate-y-1/2 flex-col overflow-y-auto rounded-[32px] border-0 bg-white shadow-xl outline-none duration-200",
            "data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95",
          )}
        >
          <DialogTitle className="sr-only">새 여행 만들기</DialogTitle>
          <DialogDescription className="sr-only">
            장소, 날짜, 여행 이름을 차례로 입력해서 새 여행을 만듭니다.
          </DialogDescription>
          <DialogClose className="absolute right-6 top-6 z-20 rounded-full p-2 text-slate-950 transition-colors hover:bg-[#f3f3fc] hover:text-[#5570f1] focus:outline-none focus:ring-0 focus:ring-offset-0 focus-visible:ring-0">
            <X className="h-6 w-6" />
            <span className="sr-only">닫기</span>
          </DialogClose>

          <div
            className={cn(
              "flex w-full flex-col items-center px-6",
              currentStep === 1 ? "py-8 sm:p-10" : "py-12 sm:p-16",
            )}
          >
            <StepIndicator currentStep={currentStep} isCompact={currentStep === 1} />

            <div className="w-full">
              {currentStep === 0 && (
                <PlaceStep
                  filteredOptions={filteredDestinationOptions}
                  onDestinationSelect={handleDestinationSelect}
                  onQueryChange={handlePlaceQueryChange}
                  placeQuery={placeQuery}
                  selectedDestination={selectedDestination}
                />
              )}

              {currentStep === 1 && (
                <DateStep
                  dateRange={dateRange}
                  selectedDayCount={selectedDayCount}
                  setDateRange={setDateRange}
                />
              )}

              {currentStep === 2 && (
                <NameStep
                  tripName={tripName}
                  onTripNameChange={setTripName}
                />
              )}
            </div>

            <div className={footerClassName}>
              {hasPreviousButton && (
                <Button
                  type="button"
                  variant="outline"
                  className="h-auto min-w-[108px] rounded-full border-[#c4c6d5] bg-white px-6 py-2.5 text-sm font-semibold text-[#1a1b22] shadow-none hover:bg-[#f3f3fc] hover:text-[#1a1b22] sm:min-w-[116px]"
                  onClick={goToPreviousStep}
                >
                  <ArrowLeft className="mr-1 h-4 w-4" />
                  이전
                </Button>
              )}

              <Button
                type="button"
                className="h-auto min-w-[108px] rounded-full bg-[#5773f1] px-6 py-2.5 text-sm font-semibold text-white shadow-md hover:bg-[#4f6aef] disabled:bg-[#5773f1] disabled:text-white disabled:opacity-100 sm:min-w-[116px]"
                onClick={goToNextStep}
                disabled={!canMoveNext}
              >
                {primaryButtonLabel}
                <ArrowRight className="ml-1 h-4 w-4" />
              </Button>
            </div>
          </div>
        </DialogPrimitive.Content>
      </DialogPortal>
    </Dialog>
  );
};

const StepIndicator = ({
  currentStep,
  isCompact,
}: {
  currentStep: TripCreationStep;
  isCompact: boolean;
}) => (
  <div
    className={cn(
      "flex w-full items-center justify-center gap-2 pr-10 text-xs text-[#64748b] sm:gap-3 sm:pr-0 sm:text-base",
      isCompact ? "mb-6" : "mb-8",
    )}
  >
    {steps.map((step, index) => {
      const isActive = currentStep === index;
      const isComplete = currentStep > index;

      return (
        <div key={step} className="flex items-center gap-2 sm:gap-3">
          <div className="flex items-center gap-1.5 sm:gap-2">
            <span
              className={cn(
                "flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[11px] font-bold sm:h-6 sm:w-6 sm:text-xs",
                isActive && "bg-[#5773f1] text-white",
                isComplete && "bg-[rgba(85,122,231,0.10)] text-[#5773f1]",
                !isActive && !isComplete && "bg-[#e8e7f1] text-[#64748b]",
              )}
            >
              {isComplete ? <Check className="h-3.5 w-3.5 sm:h-4 sm:w-4" /> : index + 1}
            </span>
            <span
              className={cn(
                "whitespace-nowrap font-medium",
                isActive && "font-semibold text-[#5773f1]",
                isComplete && "text-[#64748b]",
              )}
            >
              {step}
            </span>
          </div>
          {index < steps.length - 1 && <span className="h-px w-5 bg-[#c4c6d5] sm:w-8" />}
        </div>
      );
    })}
  </div>
);

const PlaceStep = ({
  filteredOptions,
  onDestinationSelect,
  onQueryChange,
  placeQuery,
  selectedDestination,
}: {
  filteredOptions: DestinationOption[];
  onDestinationSelect: (destination: DestinationOption) => void;
  onQueryChange: (value: string) => void;
  placeQuery: string;
  selectedDestination: DestinationOption | null;
}) => (
  <div>
    <h2 className="mb-8 break-keep text-center text-[30px] font-bold leading-tight tracking-normal text-[#1a1b22] sm:text-[32px]">
      어디로 떠나시나요?
    </h2>

    <div className="relative mb-8 w-full">
      <div className="relative">
        <MapPin className="pointer-events-none absolute left-4 top-1/2 h-6 w-6 -translate-y-1/2 text-[#747684]" />
        <Input
          aria-label="장소 검색"
          className="h-14 rounded-xl border-[#c4c6d5] bg-white pl-12 pr-4 text-base text-[#1a1b22] shadow-none placeholder:text-[#64748b] hover:border-[#747684] focus-visible:border-[#5773f1] focus-visible:ring-1 focus-visible:ring-[#5773f1] focus-visible:ring-offset-0"
          value={placeQuery}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="국가 또는 권역 검색"
        />
      </div>

      {placeQuery.trim().length > 0 && selectedDestination?.label !== placeQuery && (
        <div className="absolute left-0 right-0 top-[calc(100%+8px)] z-30 max-h-52 overflow-y-auto rounded-2xl border border-[#d8ddeb] bg-white p-2 shadow-[0_14px_28px_rgba(15,23,42,0.08)]">
          {filteredOptions.length > 0 ? (
            filteredOptions.map((option) => (
              <DestinationRow
                key={option.id}
                destination={option}
                isSelected={selectedDestination?.id === option.id}
                onSelect={onDestinationSelect}
              />
            ))
          ) : (
            <p className="px-4 py-5 text-sm font-medium text-[#6c7488]">
              선택 가능한 국가/권역이 없어요.
            </p>
          )}
        </div>
      )}
    </div>

    <div className="mb-12 flex w-full flex-col gap-4">
      <p className="text-base font-semibold text-[#434653]">추천 여행지</p>
      <div className="flex flex-wrap gap-3">
        {recommendedDestinations.map((destination) => (
          <button
            key={destination.id}
            type="button"
            className={cn(
              "inline-flex items-center gap-2 rounded-full border border-[#c4c6d5] bg-white px-4 py-3 text-base font-normal text-[#1a1b22] transition hover:border-[#747684] hover:bg-[#f3f3fc]",
              selectedDestination?.id === destination.id && "border-[#5773f1] bg-[rgba(85,122,231,0.10)] text-[#5773f1]",
            )}
            onClick={() => onDestinationSelect(destination)}
          >
            <Plane className="h-[18px] w-[18px] text-[#94a3b8]" />
            {destination.regionCode ? recommendedDestinationLabels[destination.regionCode] ?? destination.label : destination.label}
          </button>
        ))}
      </div>
    </div>
  </div>
);

const DestinationRow = ({
  destination,
  isSelected,
  onSelect,
}: {
  destination: DestinationOption;
  isSelected: boolean;
  onSelect: (destination: DestinationOption) => void;
}) => (
  <button
    type="button"
    className={cn(
      "flex w-full items-center justify-between rounded-xl px-4 py-3 text-left transition hover:bg-[#f5f7ff]",
      isSelected && "bg-[rgba(85,122,231,0.10)]",
    )}
    onClick={() => onSelect(destination)}
  >
    <span className="flex min-w-0 items-center gap-3">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#eef2ff] text-[#5570f1]">
        {destination.type === "region" ? <Plane className="h-5 w-5" /> : <MapPin className="h-5 w-5" />}
      </span>
      <span className="min-w-0">
        <span className="block truncate text-sm font-bold text-slate-950">{destination.label}</span>
        <span className="block text-xs font-medium text-[#6c7488]">
          {destination.type === "region" ? "권역" : "국가"}
        </span>
      </span>
    </span>
    {isSelected && <Check className="h-5 w-5 shrink-0 text-[#5570f1]" />}
  </button>
);

const DateStep = ({
  dateRange,
  selectedDayCount,
  setDateRange,
}: {
  dateRange: DateRange;
  selectedDayCount: number | null;
  setDateRange: (range: DateRange) => void;
}) => {
  const nights = selectedDayCount ? Math.max(selectedDayCount - 1, 0) : null;

  return (
    <div>
      <h2 className="mb-6 break-keep text-center text-[30px] font-bold leading-tight tracking-normal text-[#1a1b22] sm:text-[32px]">
        언제 떠나시나요?
      </h2>

      <div className="mb-5 flex justify-center gap-3">
        <Button
          type="button"
          variant="outline"
          className="h-auto rounded-full border-[#c4c6d5] bg-white px-6 py-2.5 text-base font-semibold text-[#1a1b22] shadow-none hover:bg-[#f3f3fc] hover:text-[#1a1b22]"
          onClick={() => setDateRange(getUpcomingWeekendRange())}
        >
          이번 주말
        </Button>
        <Button
          type="button"
          variant="outline"
          className="h-auto rounded-full border-[#c4c6d5] bg-white px-6 py-2.5 text-base font-semibold text-[#1a1b22] shadow-none hover:bg-[#f3f3fc] hover:text-[#1a1b22]"
          onClick={() => setDateRange(getOneWeekRange(dateRange.from))}
        >
          한 주
        </Button>
      </div>

      <Calendar
        mode="range"
        locale={ko}
        selected={dateRange}
        onSelect={(range) => setDateRange(range ?? { from: undefined, to: undefined })}
        numberOfMonths={1}
        className="mx-auto w-full p-0"
        classNames={{
          months: "block",
          month: "space-y-2",
          caption: "hidden",
          table: "w-full border-collapse",
          head_row: "grid grid-cols-7 gap-y-2",
          head_cell: "py-1.5 text-center text-base font-semibold text-[#64748b]",
          row: "grid grid-cols-7 gap-y-2",
          cell: "relative h-10 p-0 text-center",
          day: "h-10 w-full rounded-none p-0 text-base font-semibold text-[#1a1b22] hover:bg-[#f3f3fc]",
          day_outside: "text-[#64748b] opacity-30 aria-selected:opacity-100",
          day_selected: "bg-[#5773f1] text-white opacity-100 hover:bg-[#5773f1] hover:text-white focus:bg-[#5773f1] focus:text-white",
          day_range_start: "rounded-l-full bg-[#5773f1] text-white opacity-100 hover:bg-[#5773f1] hover:text-white",
          day_range_end: "rounded-r-full bg-[#5773f1] text-white opacity-100 hover:bg-[#5773f1] hover:text-white",
          day_range_middle: "!bg-[rgba(85,122,231,0.10)] !text-[#5773f1] !opacity-100 hover:!bg-[rgba(85,122,231,0.10)] hover:!text-[#5773f1]",
          day_today: "font-bold text-[#5773f1]",
        }}
      />

      <div className="mb-5 mt-5 flex min-h-12 items-center justify-between gap-1.5 rounded-2xl border border-[#c4c6d5]/30 bg-[#f3f3fc] px-4 py-2.5 text-[#5773f1] sm:min-h-14 sm:gap-2 sm:px-5 sm:py-3">
        <div className="flex min-w-0 items-center gap-2 sm:gap-3">
          <CalendarDays className="h-5 w-5 shrink-0 sm:h-6 sm:w-6" />
          <span className="truncate text-xs font-semibold leading-tight sm:text-sm">{formatDateRangeSummary(dateRange)}</span>
        </div>
        {nights !== null && selectedDayCount !== null && (
          <span className="shrink-0 whitespace-nowrap text-[11px] font-semibold leading-tight sm:text-xs">
            {nights}박 {selectedDayCount}일
          </span>
        )}
      </div>
    </div>
  );
};

const NameStep = ({
  onTripNameChange,
  tripName,
}: {
  onTripNameChange: (value: string) => void;
  tripName: string;
}) => (
  <div>
    <h2 className="mb-8 break-keep text-center text-[30px] font-bold leading-tight tracking-normal text-[#1a1b22] sm:text-[32px]">
      여행의 이름을 정해주세요
    </h2>

    <div className="mb-8 flex w-full flex-col gap-3">
      <Input
        aria-label="여행 이름"
        className="h-auto rounded-2xl border-[#c4c6d5] bg-white px-6 py-5 text-lg text-[#1a1b22] shadow-none placeholder:text-[#64748b] focus-visible:border-[#5773f1] focus-visible:ring-1 focus-visible:ring-[#5773f1] focus-visible:ring-offset-0"
        value={tripName}
        onChange={(event) => onTripNameChange(event.target.value)}
        placeholder="예: 도쿄 5박 6일 맛집 탐방"
        maxLength={100}
      />
      <p className="text-sm font-normal text-[#64748b]">나중에 언제든 변경할 수 있어요.</p>
    </div>
  </div>
);
