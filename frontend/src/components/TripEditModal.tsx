import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { ko } from "date-fns/locale";
import { type DateRange } from "react-day-picker";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { AlertTriangle, ArrowRight, CalendarDays, Check, MapPin, Plane, Save, X } from "lucide-react";

import {
  Country,
  getTripDateRangeDeletionConflict,
  type TripDateRangeDeletionConflictDetail,
  type TripDetail,
  type UpdateTripRequest,
} from "@/api/trips";
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
  getCountryOptionByCode,
  getCountryOptionByCode2,
  TRIP_COUNTRY_OPTIONS,
  TRIP_REGIONS,
  type TripCountryOption,
  type TripRegionCatalogEntry,
} from "@/lib/tripRegions";

interface TripEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  trip: TripDetail;
  onUpdateTrip: (updates: UpdateTripRequest) => Promise<TripDetail>;
  isSaving?: boolean;
}

type DestinationOption = {
  id: string;
  label: string;
  searchText: string;
  country: Country;
  regionCode: string | null;
  type: "country" | "region";
};

const toLocalDate = (value: string) => {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
};

const toDateInputValue = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
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
  if (!range.from || !range.to) return "날짜를 선택해주세요";
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

const getInitialDestination = (trip: TripDetail): DestinationOption | null => {
  if (trip.regionCode) {
    const regionDestination = destinationOptions.find((option) => option.id === `region:${trip.regionCode}`);
    if (regionDestination) return regionDestination;
  }

  const country = getCountryOptionByCode(trip.country) ?? getCountryOptionByCode2(trip.country);
  return country ? createCountryDestinationOption(country) : null;
};

export const TripEditModal = ({
  isOpen,
  isSaving = false,
  onClose,
  onUpdateTrip,
  trip,
}: TripEditModalProps) => {
  const { toast } = useToast();
  const [title, setTitle] = useState(trip.title);
  const [destinationQuery, setDestinationQuery] = useState("");
  const [selectedDestination, setSelectedDestination] = useState<DestinationOption | null>(null);
  const [dateRange, setDateRange] = useState<DateRange>({
    from: toLocalDate(trip.startDate),
    to: toLocalDate(trip.endDate),
  });
  const [pendingUpdates, setPendingUpdates] = useState<UpdateTripRequest | null>(null);
  const [conflictDetail, setConflictDetail] = useState<TripDateRangeDeletionConflictDetail | null>(null);

  const filteredDestinationOptions = useMemo(() => {
    const normalizedQuery = destinationQuery.trim().toLowerCase();
    if (!normalizedQuery) return [];

    return destinationOptions
      .filter((option) => option.searchText.includes(normalizedQuery))
      .slice(0, 8);
  }, [destinationQuery]);

  const selectedDayCount = getDayCount(dateRange);
  const nights = selectedDayCount ? Math.max(selectedDayCount - 1, 0) : null;
  const isConfirmOpen = Boolean(conflictDetail && pendingUpdates);

  const resetForm = useCallback(() => {
    const initialDestination = getInitialDestination(trip);

    setTitle(trip.title);
    setSelectedDestination(initialDestination);
    setDestinationQuery(initialDestination?.label ?? "");
    setDateRange({
      from: toLocalDate(trip.startDate),
      to: toLocalDate(trip.endDate),
    });
    setPendingUpdates(null);
    setConflictDetail(null);
  }, [trip]);

  useEffect(() => {
    if (isOpen) resetForm();
  }, [isOpen, resetForm]);

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      resetForm();
      onClose();
    }
  };

  const handleDestinationQueryChange = (value: string) => {
    setDestinationQuery(value);
    if (selectedDestination && selectedDestination.label !== value) {
      setSelectedDestination(null);
    }
  };

  const handleDestinationSelect = (destination: DestinationOption) => {
    setSelectedDestination(destination);
    setDestinationQuery(destination.label);
  };

  const buildUpdates = (deleteOutOfRangeItems = false): UpdateTripRequest | null => {
    const trimmedTitle = title.trim();

    if (!trimmedTitle) {
      toast({
        title: "여행 이름을 입력해주세요",
        description: "여행 이름은 필수 항목입니다.",
        variant: "destructive",
      });
      return null;
    }

    if (trimmedTitle.length > 100) {
      toast({
        title: "여행 이름이 너무 깁니다",
        description: "여행 이름은 100자 이내로 입력해주세요.",
        variant: "destructive",
      });
      return null;
    }

    if (!selectedDestination) {
      toast({
        title: "여행지를 선택해주세요",
        description: "검색 결과에서 국가 또는 권역을 선택해야 합니다.",
        variant: "destructive",
      });
      return null;
    }

    if (!dateRange.from || !dateRange.to || !selectedDayCount) {
      toast({
        title: "날짜를 선택해주세요",
        description: "여행 시작일과 종료일을 선택해야 합니다.",
        variant: "destructive",
      });
      return null;
    }

    return {
      title: trimmedTitle,
      country: selectedDestination.country,
      regionCode: selectedDestination.type === "country" ? "" : selectedDestination.regionCode,
      startDate: toDateInputValue(dateRange.from),
      endDate: toDateInputValue(dateRange.to),
      deleteOutOfRangeItems,
    };
  };

  const submitUpdates = async (updates: UpdateTripRequest) => {
    try {
      await onUpdateTrip(updates);
    } catch (error) {
      const dateShrinkConflict = getTripDateRangeDeletionConflict(error);
      if (!dateShrinkConflict) return;

      setPendingUpdates(updates);
      setConflictDetail(dateShrinkConflict);
    }
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const updates = buildUpdates(false);
    if (!updates) return;
    submitUpdates(updates);
  };

  const handleConfirmDelete = () => {
    if (!pendingUpdates) return;
    submitUpdates({ ...pendingUpdates, deleteOutOfRangeItems: true });
  };

  const handleCancelDelete = () => {
    setPendingUpdates(null);
    setConflictDetail(null);
  };

  return (
    <>
      <Dialog open={isOpen} onOpenChange={handleOpenChange}>
        <DialogPortal>
          <DialogOverlay className="bg-black/40 backdrop-blur-sm" />
          <DialogPrimitive.Content
            onOpenAutoFocus={(event) => event.preventDefault()}
            className={cn(
              "fixed left-1/2 top-1/2 z-50 flex max-h-[calc(100vh-2rem)] w-[calc(100vw-2rem)] max-w-2xl -translate-x-1/2 -translate-y-1/2 flex-col overflow-y-auto rounded-[32px] border-0 bg-white shadow-xl outline-none duration-200",
              "data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95",
            )}
          >
            <DialogTitle className="sr-only">여행 정보 수정</DialogTitle>
            <DialogDescription className="sr-only">여행 이름, 여행지, 날짜를 수정합니다.</DialogDescription>
            <DialogClose className="absolute right-6 top-6 z-20 rounded-full p-2 text-slate-950 transition-colors hover:bg-[#f3f3fc] hover:text-[#5570f1] focus:outline-none focus:ring-0 focus:ring-offset-0 focus-visible:ring-0">
              <X className="h-6 w-6" />
              <span className="sr-only">닫기</span>
            </DialogClose>

            <form className="flex w-full flex-col px-6 py-8 sm:p-12" onSubmit={handleSubmit}>
              <h2 className="mb-6 break-keep text-center text-[28px] font-bold leading-tight tracking-normal text-[#1a1b22] sm:mb-7 sm:text-[32px]">
                <span className="block sm:inline">여행을 다시</span>{" "}
                <span className="block sm:inline">다듬어볼까요?</span>
              </h2>

              <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(280px,0.95fr)] lg:gap-7">
                <div className="flex flex-col gap-5 sm:gap-6">
                  <div className="flex flex-col gap-3">
                    <span className="text-base font-semibold text-[#434653]">여행 이름</span>
                    <Input
                      aria-label="여행 이름"
                      className="h-auto rounded-2xl border-[#c4c6d5] bg-white px-5 py-4 text-base text-[#1a1b22] shadow-none placeholder:text-[#64748b] focus-visible:border-[#5773f1] focus-visible:ring-1 focus-visible:ring-[#5773f1] focus-visible:ring-offset-0"
                      maxLength={100}
                      onChange={(event) => setTitle(event.target.value)}
                      placeholder="예: 도쿄 5박 6일 맛집 탐방"
                      value={title}
                    />
                  </div>

                  <div className="flex flex-col gap-3">
                    <span className="text-base font-semibold text-[#434653]">여행지</span>
                    <div className="relative">
                      <MapPin className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#747684]" />
                      <Input
                        aria-label="여행지 검색"
                        className="h-auto rounded-2xl border-[#c4c6d5] bg-white px-5 py-4 pl-12 text-base text-[#1a1b22] shadow-none placeholder:text-[#64748b] focus-visible:border-[#5773f1] focus-visible:ring-1 focus-visible:ring-[#5773f1] focus-visible:ring-offset-0"
                        onChange={(event) => handleDestinationQueryChange(event.target.value)}
                        placeholder="국가 또는 권역 검색"
                        value={destinationQuery}
                      />

                      {destinationQuery.trim().length > 0 && selectedDestination?.label !== destinationQuery && (
                        <div className="absolute left-0 right-0 top-[calc(100%+8px)] z-30 max-h-60 overflow-y-auto rounded-2xl border border-[#d8ddeb] bg-white p-2 shadow-[0_14px_28px_rgba(15,23,42,0.08)]">
                          {filteredDestinationOptions.length > 0 ? (
                            filteredDestinationOptions.map((option) => (
                              <DestinationRow
                                key={option.id}
                                destination={option}
                                isSelected={selectedDestination?.id === option.id}
                                onSelect={handleDestinationSelect}
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
                  </div>

                </div>

                <div className="flex flex-col gap-4">
                  <span className="text-base font-semibold text-[#434653]">날짜</span>
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
                      head_row: "grid grid-cols-7 gap-y-1 sm:gap-y-2",
                      head_cell: "py-0.5 text-center text-sm font-semibold text-[#64748b] sm:py-1",
                      row: "grid grid-cols-7 gap-y-0.5 sm:gap-y-1",
                      cell: "relative h-8 p-0 text-center sm:h-9",
                      day: "h-8 w-full rounded-none p-0 text-sm font-semibold text-[#1a1b22] hover:bg-[#f3f3fc] sm:h-9",
                      day_outside: "text-[#64748b] opacity-30 aria-selected:opacity-100",
                      day_selected:
                        "bg-[#5773f1] text-white opacity-100 hover:bg-[#5773f1] hover:text-white focus:bg-[#5773f1] focus:text-white",
                      day_range_start:
                        "rounded-l-full bg-[#5773f1] text-white opacity-100 hover:bg-[#5773f1] hover:text-white",
                      day_range_end:
                        "rounded-r-full bg-[#5773f1] text-white opacity-100 hover:bg-[#5773f1] hover:text-white",
                      day_range_middle:
                        "!bg-[rgba(85,122,231,0.10)] !text-[#5773f1] !opacity-100 hover:!bg-[rgba(85,122,231,0.10)] hover:!text-[#5773f1]",
                      day_today: "font-bold text-[#5773f1]",
                    }}
                  />

                  <div className="flex min-h-11 items-center justify-between gap-1.5 rounded-2xl border border-[#c4c6d5]/30 bg-[#f3f3fc] px-3 py-2 text-[#5773f1] sm:min-h-12 sm:gap-2 sm:px-4 sm:py-2.5">
                    <div className="flex min-w-0 items-center gap-2">
                      <CalendarDays className="h-4 w-4 shrink-0 sm:h-5 sm:w-5" />
                      <span className="truncate text-[11px] font-semibold leading-tight sm:text-xs">
                        {formatDateRangeSummary(dateRange)}
                      </span>
                    </div>
                    {nights !== null && selectedDayCount !== null && (
                      <span className="shrink-0 whitespace-nowrap text-[11px] font-semibold leading-tight sm:text-xs">
                        {nights}박 {selectedDayCount}일
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="mt-4 flex items-center justify-between gap-2 sm:mt-6">
                <Button
                  type="button"
                  variant="outline"
                  className="h-auto min-w-[96px] rounded-full border-[#c4c6d5] bg-white px-5 py-2.5 text-sm font-semibold text-[#1a1b22] shadow-none hover:bg-[#f3f3fc] hover:text-[#1a1b22] sm:min-w-[104px]"
                  onClick={() => handleOpenChange(false)}
                >
                  취소
                </Button>
                <Button
                  type="submit"
                  className="h-auto min-w-[108px] rounded-full bg-[#5773f1] px-6 py-2.5 text-sm font-semibold text-white shadow-md hover:bg-[#4f6aef] disabled:bg-[#5773f1] disabled:text-white disabled:opacity-60 sm:min-w-[116px]"
                  disabled={isSaving}
                >
                  저장
                  {isSaving ? (
                    <Save className="ml-1.5 h-4 w-4 animate-pulse" />
                  ) : (
                    <ArrowRight className="ml-1.5 h-4 w-4" />
                  )}
                </Button>
              </div>
            </form>

            {isConfirmOpen && (
              <div className="absolute inset-0 z-30 flex items-center justify-center rounded-[32px] bg-black/35 p-6 backdrop-blur-sm">
                <div
                  role="alertdialog"
                  aria-modal="true"
                  aria-labelledby="trip-edit-delete-confirm-title"
                  className="w-full max-w-md rounded-[28px] border-0 bg-white px-7 py-8 shadow-xl outline-none sm:px-9"
                >
                  <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-full bg-[#fff3e8] text-[#e26b2d]">
                    <AlertTriangle className="h-6 w-6" />
                  </div>
                  <h3
                    id="trip-edit-delete-confirm-title"
                    className="mb-3 break-keep text-[26px] font-bold leading-tight text-[#1a1b22]"
                  >
                    일부 일정이 삭제돼요
                  </h3>
                  <p className="mb-5 break-keep text-base leading-7 text-[#64748b]">
                    새 날짜 범위 밖에 있는 일정 {conflictDetail?.outOfRangeItemCount ?? 0}개를 삭제하고 저장할까요?
                  </p>

                  {conflictDetail?.outOfRangeItems.length ? (
                    <div className="mb-6 max-h-28 overflow-y-auto rounded-2xl border border-[#eadfd9] bg-[#fffaf7] px-4 py-3">
                      {conflictDetail.outOfRangeItems.slice(0, 4).map((item) => (
                        <div key={`${item.itemId}-${item.visitDay}`} className="flex items-center justify-between gap-3 py-1">
                          <span className="min-w-0 truncate text-sm font-semibold text-[#1a1b22]">
                            {item.title ?? "이름 없는 일정"}
                          </span>
                          <span className="shrink-0 text-sm font-semibold text-[#e26b2d]">{item.visitDay}일차</span>
                        </div>
                      ))}
                    </div>
                  ) : null}

                  <div className="flex items-center justify-between gap-3">
                    <Button
                      type="button"
                      variant="outline"
                      className="h-auto rounded-full border-[#c4c6d5] bg-white px-6 py-3 text-base font-semibold text-[#1a1b22] shadow-none hover:bg-[#f3f3fc] hover:text-[#1a1b22]"
                      onClick={handleCancelDelete}
                    >
                      돌아가기
                    </Button>
                    <Button
                      type="button"
                      className="h-auto rounded-full bg-[#5773f1] px-6 py-3 text-base font-semibold text-white shadow-md hover:bg-[#4f6aef] disabled:bg-[#5773f1] disabled:text-white disabled:opacity-60"
                      disabled={isSaving}
                      onClick={handleConfirmDelete}
                    >
                      삭제하고 저장
                    </Button>
                  </div>
                </div>
              </div>
            )}
          </DialogPrimitive.Content>
        </DialogPortal>
      </Dialog>
    </>
  );
};

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
