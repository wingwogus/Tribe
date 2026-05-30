import {useEffect, useMemo, useState} from "react";
import {
  ArrowUpRight,
  Clock3,
  CircleDollarSign,
  Globe2,
  MapPin,
  Phone,
  Plus,
  Star,
  Trash2,
  X,
} from "lucide-react";
import {Drawer, DrawerContent} from "@/components/ui/drawer";
import {Button} from "@/components/ui/button";
import {Badge} from "@/components/ui/badge";
import {Avatar, AvatarFallback, AvatarImage} from "@/components/ui/avatar";
import {Tooltip, TooltipContent, TooltipTrigger} from "@/components/ui/tooltip";
import type {PlaceDetailResponse} from "@/api/places";
import type {NormalizedPlaceCategoryKey, PlaceDetailSummary, PlaceTypeSummary} from "@/api/placeMetadata";
import {
  getOpeningStatusLabel,
  getOpeningStatusTone,
  getPlaceTypeLabel,
} from "@/lib/placePresentation";

export interface PlaceDetailDayOption {
  visitDay: number;
  label: string;
}

export interface PlaceDetailPanelPlace {
  mode: "itinerary" | "wishlist" | "nearby";
  name: string;
  address?: string | null;
  visitDay?: number | null;
  time?: string | null;
  memo?: string | null;
  adderNickname?: string | null;
  adderAvatar?: string | null;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingStatusWarning?: string | null;
}

interface PlaceDetailPanelProps {
  open: boolean;
  isMobile: boolean;
  desktopMode?: "overlay" | "inline";
  place: PlaceDetailPanelPlace | null;
  detail?: PlaceDetailResponse | null;
  isLoading?: boolean;
  isError?: boolean;
  currentDay: number;
  availableDays: PlaceDetailDayOption[];
  onClose: () => void;
  onOpenGoogleMaps: () => void;
  onDelete?: () => void;
  onAddToItinerary?: (visitDay: number) => void;
}

const WEEKDAY_KO_LABELS: Record<string, string> = {
  Monday: "월요일",
  Mon: "월요일",
  Tuesday: "화요일",
  Tue: "화요일",
  Wednesday: "수요일",
  Wed: "수요일",
  Thursday: "목요일",
  Thu: "목요일",
  Friday: "금요일",
  Fri: "금요일",
  Saturday: "토요일",
  Sat: "토요일",
  Sunday: "일요일",
  Sun: "일요일",
  월요일: "월요일",
  화요일: "화요일",
  수요일: "수요일",
  목요일: "목요일",
  금요일: "금요일",
  토요일: "토요일",
  일요일: "일요일",
};

const localizeOpeningHoursText = (value: string) =>
  value
    .replace(/[\u00A0\u2007\u202F]/g, " ")
    .replace(/\s*[–—]\s*/g, " - ")
    .replace(/\bOpen 24 hours\b/gi, "24시간 영업")
    .replace(/\bTemporarily closed\b/gi, "임시 휴무")
    .replace(/\bClosed\b/gi, "휴무")
    .replace(/\bAM\b/g, "오전")
    .replace(/\bPM\b/g, "오후")
    .replace(/\s+/g, " ")
    .trim();

const normalizeOpeningDayLabel = (value: string) => {
  const normalized = value.replace(/\./g, "").trim();
  return WEEKDAY_KO_LABELS[normalized] ?? normalized;
};

const parseOpeningHoursRows = (detail?: PlaceDetailResponse | null) => {
  const parse = (json?: string | null) => {
    if (!json) return [];

    try {
      const parsed = JSON.parse(json) as { weekdayDescriptions?: string[] };
      return Array.isArray(parsed.weekdayDescriptions) ? parsed.weekdayDescriptions.filter(Boolean) : [];
    } catch {
      return [];
    }
  };

  const currentRows = parse(detail?.currentOpeningHoursJson);
  const regularRows = parse(detail?.regularOpeningHoursJson);
  const source = currentRows.length > 0 ? currentRows : regularRows;

  return source.slice(0, 7).map((line) => {
    const localized = localizeOpeningHoursText(line);
    const match = localized.match(/^([^:]+):\s*(.+)$/);

    if (!match) {
      return { day: null, value: localized };
    }

    return {
      day: normalizeOpeningDayLabel(match[1]),
      value: match[2],
    };
  });
};

const formatVisitTime = (time?: string | null) => time?.split("T")[1]?.slice(0, 5) ?? null;

const formatPriceLevel = (priceLevel?: number | null) => {
  if (typeof priceLevel !== "number") {
    return null;
  }

  if (priceLevel <= 0) {
    return "무료";
  }

  return "₩".repeat(Math.min(Math.max(priceLevel, 1), 4));
};

const detailRowClass = "flex items-start gap-3 px-4 py-3";
const actionButtonClass = "h-11 rounded-full px-4 font-semibold transition hover:-translate-y-0.5";
const primaryActionButtonClass = `${actionButtonClass} bg-primary text-primary-foreground hover:bg-primary/90`;
const secondaryActionButtonClass = `${actionButtonClass} border-foreground bg-background text-foreground hover:bg-slate-50 hover:text-foreground`;
const destructiveActionButtonClass = `${actionButtonClass} border-destructive/30 bg-background text-destructive hover:bg-destructive/10 hover:text-destructive`;

const getInitial = (value?: string | null) => value?.trim().charAt(0).toUpperCase() || "?";

export const PlaceDetailPanel = ({
  open,
  isMobile,
  desktopMode = "overlay",
  place,
  detail,
  isLoading = false,
  isError = false,
  currentDay,
  availableDays,
  onClose,
  onOpenGoogleMaps,
  onDelete,
  onAddToItinerary,
}: PlaceDetailPanelProps) => {
  const [selectedVisitDay, setSelectedVisitDay] = useState(currentDay);

  useEffect(() => {
    if (!place) {
      return;
    }

    setSelectedVisitDay(currentDay);
  }, [currentDay, place]);

  const placeTypeSummary = detail?.placeTypeSummary ?? place?.placeTypeSummary;
  const normalizedCategoryKey = detail?.normalizedCategoryKey ?? place?.normalizedCategoryKey;
  const typeLabel = getPlaceTypeLabel(placeTypeSummary, normalizedCategoryKey);
  const openingStatusKey = place?.openingStatusWarning
    || (detail?.placeDetailSummary?.businessStatus === "CLOSED_TEMPORARILY" ? "TEMPORARILY_CLOSED" : null);
  const openingLabel = getOpeningStatusLabel(openingStatusKey)
    || (detail?.placeDetailSummary?.businessStatus === "CLOSED_TEMPORARILY" ? "임시 휴무" : null);
  const isOpeningStatusDestructive = getOpeningStatusTone(openingStatusKey) === "destructive";
  const rating = detail?.placeDetailSummary?.rating ?? place?.placeDetailSummary?.rating;
  const ratingCount = detail?.placeDetailSummary?.userRatingCount ?? place?.placeDetailSummary?.userRatingCount;
  const openingRows = useMemo(() => parseOpeningHoursRows(detail), [detail]);
  const visitTime = formatVisitTime(place?.time);
  const address = detail?.address ?? place?.address;
  const displayName = detail?.placeName ?? place?.name;
  const priceLabel = formatPriceLevel(detail?.priceLevel);
  const phoneNumber = detail?.formattedPhoneNumber || detail?.internationalPhoneNumber;
  const primaryOpeningRow = openingRows[0] ?? null;
  const primaryOpeningText = openingLabel
    || (primaryOpeningRow ? [primaryOpeningRow.day, primaryOpeningRow.value].filter(Boolean).join(" ") : null);
  const itineraryDayLabel = place?.visitDay ? `Day ${place.visitDay}` : null;
  const selectedVisitDayLabel =
    availableDays.find((day) => day.visitDay === selectedVisitDay)?.label ?? `Day ${selectedVisitDay}`;
  const shouldShowItineraryMeta = place?.mode === "itinerary" && Boolean(visitTime || place.memo);
  const shouldShowWishlistAdder = place?.mode === "wishlist" && Boolean(place.adderNickname);

  const content = place ? (
    <div className="flex h-full flex-col bg-white">
      <div className="shrink-0 border-b border-slate-200 bg-white">
        <div className="relative px-4 pb-4 pt-4">
          <button
            type="button"
            onClick={onClose}
            className="absolute right-3 top-3 rounded-full p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-900"
            aria-label="장소 상세 닫기"
          >
            <X className="h-5 w-5" />
          </button>

          <div className="flex gap-3 pr-12">
            <div className="min-w-0 flex-1">
              <h2 className="line-clamp-2 text-2xl font-semibold leading-7 text-slate-950">
                {displayName}
              </h2>
              {detail?.placeName && detail.placeName !== place.name && (
                <div className="mt-1 line-clamp-1 text-sm text-slate-500">{place.name}</div>
              )}
              {typeof rating === "number" && (
                <div className="mt-3 flex items-center gap-2 text-sm text-slate-700">
                  <Star className="h-5 w-5 fill-amber-400 text-amber-400" />
                  <span className="text-base font-semibold text-slate-950">{rating.toFixed(1)}</span>
                  {typeof ratingCount === "number" && (
                    <span className="text-sm text-muted-foreground">({ratingCount} 리뷰)</span>
                  )}
                </div>
              )}
              {(itineraryDayLabel || typeLabel || openingLabel) && (
                <div className="mt-3 flex min-w-0 flex-wrap items-center gap-2">
                  {itineraryDayLabel && (
                    <Badge className="bg-primary text-primary-foreground hover:bg-primary">
                      {itineraryDayLabel}
                    </Badge>
                  )}
                  {typeLabel && (
                    <Badge className="bg-primary/10 text-primary hover:bg-primary/10">
                      {typeLabel}
                    </Badge>
                  )}
                  {openingLabel && (
                    <span className={`inline-flex items-center gap-1 text-sm font-semibold ${
                      isOpeningStatusDestructive ? "text-destructive" : "text-emerald-600"
                    }`}>
                      <span className={`h-2 w-2 rounded-full ${
                        isOpeningStatusDestructive ? "bg-destructive" : "bg-emerald-500"
                      }`} />
                      {openingLabel}
                    </span>
                  )}
                </div>
              )}
              {shouldShowItineraryMeta && (
                <div className="mt-3 space-y-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700">
                  {visitTime && (
                    <div className="inline-flex items-center gap-2 font-semibold text-slate-900">
                      <Clock3 className="h-4 w-4 text-slate-500" />
                      가는 시간 {visitTime}
                    </div>
                  )}
                  {place.memo && (
                    <div className="line-clamp-2 leading-6 text-slate-700">{place.memo}</div>
                  )}
                </div>
              )}
              {priceLabel && (
                <div className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-slate-800">
                  <CircleDollarSign className="h-4 w-4 text-slate-500" />
                  예상 {priceLabel}/인
                </div>
              )}
            </div>

            {shouldShowWishlistAdder && (
              <Tooltip>
                <TooltipTrigger asChild>
                  <span
                    tabIndex={0}
                    className="mt-1 inline-flex h-9 w-9 shrink-0 rounded-full ring-2 ring-background transition hover:ring-primary/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    aria-label={`${place.adderNickname}님이 위시리스트에 추가`}
                  >
                    <Avatar className="h-9 w-9 border border-border">
                      <AvatarImage
                        src={place.adderAvatar || undefined}
                        alt={place.adderNickname || "위시리스트 추가자"}
                      />
                      <AvatarFallback className="bg-primary/10 text-xs font-semibold text-primary">
                        {getInitial(place.adderNickname)}
                      </AvatarFallback>
                    </Avatar>
                  </span>
                </TooltipTrigger>
                <TooltipContent side="bottom" align="end">
                  <p>{place.adderNickname}님이 추가</p>
                </TooltipContent>
              </Tooltip>
            )}
          </div>
        </div>

      </div>

      <div className="min-h-0 flex-1 overflow-y-auto pb-28">
        <div className="space-y-3 px-4 py-4">
          <div className="divide-y divide-slate-100">
            <div className={detailRowClass}>
              <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" />
              <div className="min-w-0 flex-1">
                <div className="text-sm leading-6 text-slate-900">{address || "주소 정보 없음"}</div>
                <button
                  type="button"
                  onClick={onOpenGoogleMaps}
                  className="mt-0.5 text-left text-xs font-medium text-primary hover:underline"
                >
                  지도에서 보기
                </button>
              </div>
            </div>

            <div className={detailRowClass}>
              <Clock3 className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" />
              <div className="min-w-0 flex-1">
                <div className={`text-sm leading-6 ${
                  isOpeningStatusDestructive ? "font-semibold text-destructive" : "text-slate-900"
                }`}>
                  {primaryOpeningText || "영업 정보 없음"}
                </div>
                {openingRows.length > 1 && (
                  <div className="mt-1 space-y-1">
                    {openingRows.slice(1, 7).map((row, index) => (
                      <div key={`${row.day ?? "unknown"}-${index}`} className="grid grid-cols-[64px_minmax(0,1fr)] gap-2 text-xs leading-5 text-slate-500">
                        <span className="font-medium">{row.day || "안내"}</span>
                        <span>{row.value}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {phoneNumber && (
              <div className={detailRowClass}>
                <Phone className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" />
                <div className="min-w-0 flex-1 text-sm leading-6 text-slate-900">{phoneNumber}</div>
              </div>
            )}

            {detail?.websiteUri && (
              <a
                href={detail.websiteUri}
                target="_blank"
                rel="noreferrer"
                className={detailRowClass}
              >
                <Globe2 className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" />
                <span className="min-w-0 flex-1 truncate text-sm leading-6 text-primary">{detail.websiteUri.replace(/^https?:\/\//, "")}</span>
                <ArrowUpRight className="mt-1 h-4 w-4 shrink-0 text-slate-400" />
              </a>
            )}
          </div>

          {isLoading && (
            <div className="rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-500">
              장소 상세 정보를 불러오는 중입니다.
            </div>
          )}

          {isError && (
            <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">
              장소 상세 정보를 불러오지 못했습니다.
            </div>
          )}
        </div>
      </div>

      <div className="shrink-0 border-t border-slate-200 bg-white p-3">
        {place.mode === "wishlist" && onAddToItinerary ? (
          <div className={onDelete ? "grid grid-cols-[0.9fr_1.6fr] gap-2" : "grid gap-2"}>
            {onDelete && (
              <Button
                variant="outline"
                onClick={onDelete}
                className={secondaryActionButtonClass}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                위시 삭제
              </Button>
            )}
            <div className="group/add-itinerary relative min-w-0">
              <Button
                onClick={() => onAddToItinerary(selectedVisitDay)}
                aria-label={`${selectedVisitDayLabel}에 일정 추가`}
                className={`w-full ${primaryActionButtonClass}`}
              >
                <Plus className="mr-2 h-4 w-4" />
                일정에 추가
              </Button>
              {availableDays.length > 0 && (
                <div className="pointer-events-none absolute bottom-full left-0 right-0 z-20 min-w-[180px] translate-y-1 rounded-lg border border-border bg-background p-2 opacity-0 shadow-lg transition-all group-hover/add-itinerary:pointer-events-auto group-hover/add-itinerary:translate-y-0 group-hover/add-itinerary:opacity-100 group-focus-within/add-itinerary:pointer-events-auto group-focus-within/add-itinerary:translate-y-0 group-focus-within/add-itinerary:opacity-100">
                  <div className="mb-2 px-1 text-xs font-semibold text-muted-foreground">추가할 Day</div>
                  <div className="grid max-h-44 gap-1 overflow-y-auto overscroll-contain pr-1">
                    {availableDays.map((day) => (
                      <button
                        key={day.visitDay}
                        type="button"
                        onClick={() => setSelectedVisitDay(day.visitDay)}
                        aria-pressed={selectedVisitDay === day.visitDay}
                        className={`rounded-md px-3 py-2 text-left text-sm font-medium transition ${
                          selectedVisitDay === day.visitDay
                            ? "bg-primary text-primary-foreground"
                            : "text-foreground hover:bg-slate-50"
                        }`}
                      >
                        {day.label}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        ) : place.mode === "itinerary" ? (
          <div className={onDelete ? "grid grid-cols-2 gap-2" : "grid gap-2"}>
            <Button
              onClick={onOpenGoogleMaps}
              className={primaryActionButtonClass}
            >
              <MapPin className="mr-2 h-4 w-4" />
              구글 지도
            </Button>
            {onDelete && (
              <Button
                variant="outline"
                onClick={onDelete}
                className={destructiveActionButtonClass}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                일정 삭제
              </Button>
            )}
          </div>
        ) : (
          <Button
            onClick={onOpenGoogleMaps}
            className={`w-full ${primaryActionButtonClass}`}
          >
            <MapPin className="mr-2 h-4 w-4" />
            구글 지도
          </Button>
        )}
      </div>
    </div>
  ) : null;

  if (!open || !place) {
    return null;
  }

  if (isMobile) {
    return (
      <Drawer open={open} onOpenChange={(next) => !next && onClose()}>
        <DrawerContent className="h-[88vh] p-0 md:hidden">
          {content}
        </DrawerContent>
      </Drawer>
    );
  }

  if (desktopMode === "inline") {
    return (
      <div className="hidden h-full w-full bg-white md:block">
        {content}
      </div>
    );
  }

  return (
    <div className="absolute inset-y-0 right-0 z-20 hidden w-[400px] border-l border-slate-200 bg-white shadow-[0_20px_60px_rgba(15,23,42,0.18)] md:block">
      {content}
    </div>
  );
};
