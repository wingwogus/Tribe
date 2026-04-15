import {useEffect, useMemo, useState} from "react";
import {
  ArrowUpRight,
  Clock3,
  Globe2,
  MapPin,
  Phone,
  Star,
  Trash2,
  X,
} from "lucide-react";
import {Drawer, DrawerContent} from "@/components/ui/drawer";
import {Button} from "@/components/ui/button";
import {Badge} from "@/components/ui/badge";
import type {PlaceDetailResponse} from "@/api/places";
import type {NormalizedPlaceCategoryKey, PlaceDetailSummary, PlaceTypeSummary} from "@/api/placeMetadata";
import {
  getOpeningStatusLabel,
  getOpeningStatusTone,
  getPlaceCategoryColor,
  getPlaceTypeLabel,
} from "@/lib/placePresentation";

export interface PlaceDetailDayOption {
  visitDay: number;
  label: string;
}

export interface PlaceDetailPanelPlace {
  mode: "itinerary" | "wishlist";
  name: string;
  address?: string | null;
  time?: string | null;
  memo?: string | null;
  adderNickname?: string | null;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingStatusWarning?: string | null;
}

interface PlaceDetailPanelProps {
  open: boolean;
  isMobile: boolean;
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

const infoTileClass = "rounded-2xl border border-white/40 bg-white/75 p-3 backdrop-blur";

export const PlaceDetailPanel = ({
  open,
  isMobile,
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
  }, [currentDay, place?.mode, place?.name]);

  const categoryColor = getPlaceCategoryColor(place?.placeTypeSummary, place?.normalizedCategoryKey);
  const typeLabel = getPlaceTypeLabel(detail?.placeTypeSummary ?? place?.placeTypeSummary, detail?.normalizedCategoryKey ?? place?.normalizedCategoryKey);
  const openingLabel = getOpeningStatusLabel(place?.openingStatusWarning)
    || (detail?.placeDetailSummary?.businessStatus === "CLOSED_TEMPORARILY" ? "임시 휴무" : null);
  const rating = detail?.placeDetailSummary?.rating ?? place?.placeDetailSummary?.rating;
  const ratingCount = detail?.placeDetailSummary?.userRatingCount ?? place?.placeDetailSummary?.userRatingCount;
  const editorialSummary = detail?.placeDetailSummary?.editorialSummary ?? place?.placeDetailSummary?.editorialSummary;
  const openingRows = useMemo(() => parseOpeningHoursRows(detail), [detail]);
  const visitTime = formatVisitTime(place?.time);
  const address = detail?.address ?? place?.address;

  const content = place ? (
    <div className="flex h-full flex-col bg-white">
      <div className="shrink-0 p-4 pb-3">
        <div className="grid h-[180px] grid-cols-4 grid-rows-2 gap-1 overflow-hidden rounded-[28px] bg-slate-100 md:h-[220px]">
          <div
            className="col-span-2 row-span-2 flex flex-col justify-between p-5 text-white"
            style={{ background: `linear-gradient(140deg, ${categoryColor} 0%, #0f172a 100%)` }}
          >
            <div className="flex items-center justify-between">
              <Badge className="border-white/20 bg-white/15 text-white hover:bg-white/15">{typeLabel || "장소"}</Badge>
              <button
                type="button"
                onClick={onClose}
                className="rounded-full bg-black/15 p-2 text-white transition hover:bg-black/25"
                aria-label="장소 상세 닫기"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="space-y-2">
              <div className="text-[11px] font-medium uppercase tracking-[0.24em] text-white/70">place detail</div>
              <div className="line-clamp-3 text-2xl font-semibold leading-tight">{place.name}</div>
            </div>
          </div>
          <div className={infoTileClass}>
            <div className="text-[10px] font-semibold text-slate-400">상태</div>
            <div className="mt-2 line-clamp-2 text-sm font-semibold text-slate-800">{openingLabel || "정보 준비 중"}</div>
          </div>
          <div className={infoTileClass}>
            <div className="text-[10px] font-semibold text-slate-400">평점</div>
            <div className="mt-2 text-sm font-semibold text-slate-800">
              {typeof rating === "number" ? rating.toFixed(1) : "-"}
            </div>
            {typeof ratingCount === "number" && (
              <div className="mt-1 text-[11px] text-slate-500">{ratingCount}개 평가</div>
            )}
          </div>
          <div className={infoTileClass}>
            <div className="text-[10px] font-semibold text-slate-400">주소</div>
            <div className="mt-2 line-clamp-3 text-sm font-medium text-slate-700">{address || "주소 정보 없음"}</div>
          </div>
          <div className={infoTileClass}>
            <div className="text-[10px] font-semibold text-slate-400">
              {place.mode === "wishlist" ? "추가자" : "방문 예정"}
            </div>
            <div className="mt-2 line-clamp-2 text-sm font-semibold text-slate-800">
              {place.mode === "wishlist" ? place.adderNickname || "멤버" : visitTime || "시간 미정"}
            </div>
          </div>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-4 pb-4">
        <div className="space-y-6">
          <div className="space-y-3">
            <div className="flex flex-wrap items-center gap-2">
              {typeLabel && <Badge variant="secondary">{typeLabel}</Badge>}
              {openingLabel && (
                <Badge variant={getOpeningStatusTone(place.openingStatusWarning || (detail?.placeDetailSummary?.businessStatus === "CLOSED_TEMPORARILY" ? "TEMPORARILY_CLOSED" : undefined))}>
                  {openingLabel}
                </Badge>
              )}
              {typeof rating === "number" && (
                <Badge variant="outline">평점 {rating.toFixed(1)}{typeof ratingCount === "number" ? ` · ${ratingCount}` : ""}</Badge>
              )}
              {place.mode === "itinerary" && visitTime && (
                <Badge variant="outline">{visitTime}</Badge>
              )}
            </div>
            <div className="space-y-2">
              <h3 className="text-2xl font-semibold tracking-tight text-slate-950">{place.name}</h3>
              {editorialSummary && (
                <p className="text-sm leading-6 text-slate-600">{editorialSummary}</p>
              )}
              {place.memo && (
                <div className="rounded-2xl bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-900">
                  {place.memo}
                </div>
              )}
            </div>
          </div>

          <div className="space-y-3">
            {place.mode === "wishlist" && availableDays.length > 0 && onAddToItinerary && (
              <div className="flex flex-wrap gap-2">
                {availableDays.map((day) => (
                  <button
                    key={day.visitDay}
                    type="button"
                    onClick={() => setSelectedVisitDay(day.visitDay)}
                    className={`rounded-full px-3 py-1.5 text-xs font-medium transition ${
                      selectedVisitDay === day.visitDay
                        ? "bg-primary text-primary-foreground"
                        : "border border-slate-200 bg-white text-slate-600 hover:border-slate-300"
                    }`}
                  >
                    {day.label}
                  </button>
                ))}
              </div>
            )}

            <div className={`grid gap-3 ${place.mode === "wishlist" ? "grid-cols-[1.2fr_1fr]" : "grid-cols-2"}`}>
              {place.mode === "wishlist" && onAddToItinerary ? (
                <Button
                  onClick={() => onAddToItinerary(selectedVisitDay)}
                  className="h-12 rounded-2xl"
                >
                  Day {selectedVisitDay} 일정에 추가
                </Button>
              ) : (
                <Button
                  onClick={onOpenGoogleMaps}
                  className="h-12 rounded-2xl"
                >
                  구글 지도
                </Button>
              )}

              <Button
                variant={place.mode === "wishlist" ? "outline" : "destructive"}
                onClick={place.mode === "wishlist" ? onOpenGoogleMaps : onDelete}
                className="h-12 rounded-2xl"
              >
                {place.mode === "wishlist" ? "구글 지도" : "일정 삭제"}
              </Button>
            </div>

            {place.mode === "wishlist" && onDelete && (
              <Button
                variant="ghost"
                onClick={onDelete}
                className="w-full justify-center rounded-2xl text-destructive hover:bg-destructive/10 hover:text-destructive"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                위시리스트 삭제
              </Button>
            )}
          </div>

          <div className="rounded-[24px] border border-slate-200 bg-slate-50/80 p-4">
            <div className="space-y-4">
              <div className="flex items-start gap-3">
                <MapPin className="mt-0.5 h-4 w-4 text-slate-400" />
                <div className="min-w-0 flex-1">
                  <div className="text-xs font-semibold text-slate-500">주소</div>
                  <div className="mt-1 text-sm leading-6 text-slate-700">{address || "주소 정보 없음"}</div>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <Clock3 className="mt-0.5 h-4 w-4 text-slate-400" />
                <div className="min-w-0 flex-1">
                  <div className="text-xs font-semibold text-slate-500">운영 정보</div>
                  <div className="mt-1 text-sm leading-6 text-slate-700">
                    {openingLabel || "영업 상태 정보 없음"}
                  </div>
                </div>
              </div>

              {(detail?.formattedPhoneNumber || detail?.internationalPhoneNumber) && (
                <div className="flex items-start gap-3">
                  <Phone className="mt-0.5 h-4 w-4 text-slate-400" />
                  <div className="min-w-0 flex-1">
                    <div className="text-xs font-semibold text-slate-500">전화번호</div>
                    <div className="mt-1 text-sm leading-6 text-slate-700">
                      {detail?.formattedPhoneNumber || detail?.internationalPhoneNumber}
                    </div>
                  </div>
                </div>
              )}

              {(detail?.websiteUri || detail?.googleMapsUri) && (
                <div className="grid gap-3 sm:grid-cols-2">
                  {detail?.websiteUri && (
                    <a
                      href={detail.websiteUri}
                      target="_blank"
                      rel="noreferrer"
                      className="flex items-center justify-between rounded-2xl bg-white px-4 py-3 text-sm font-medium text-slate-700 shadow-sm ring-1 ring-slate-200 transition hover:ring-slate-300"
                    >
                      <span className="inline-flex items-center gap-2">
                        <Globe2 className="h-4 w-4 text-slate-400" />
                        웹사이트
                      </span>
                      <ArrowUpRight className="h-4 w-4 text-slate-400" />
                    </a>
                  )}
                  {detail?.googleMapsUri && (
                    <a
                      href={detail.googleMapsUri}
                      target="_blank"
                      rel="noreferrer"
                      className="flex items-center justify-between rounded-2xl bg-white px-4 py-3 text-sm font-medium text-slate-700 shadow-sm ring-1 ring-slate-200 transition hover:ring-slate-300"
                    >
                      <span className="inline-flex items-center gap-2">
                        <MapPin className="h-4 w-4 text-slate-400" />
                        구글 지도
                      </span>
                      <ArrowUpRight className="h-4 w-4 text-slate-400" />
                    </a>
                  )}
                </div>
              )}
            </div>
          </div>

          <div className="space-y-3 rounded-[24px] border border-slate-200 bg-white p-4">
            <div className="flex items-center gap-2">
              <Clock3 className="h-4 w-4 text-slate-400" />
              <h4 className="text-sm font-semibold text-slate-900">영업시간</h4>
            </div>
            {openingRows.length > 0 ? (
              <div className="space-y-3">
                {openingRows.map((row, index) => (
                  <div
                    key={`${row.day ?? "unknown"}-${index}`}
                    className="grid grid-cols-[74px_minmax(0,1fr)] gap-3 border-b border-slate-100 pb-3 last:border-b-0 last:pb-0"
                  >
                    <div className="text-xs font-semibold text-slate-500">{row.day || "안내"}</div>
                    <div className="text-sm leading-6 text-slate-700">{row.value}</div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="rounded-2xl bg-slate-50 px-4 py-5 text-sm text-slate-500">
                {isLoading ? "영업시간 정보를 불러오는 중입니다." : isError ? "영업시간 정보를 불러오지 못했습니다." : "영업시간 정보가 없습니다."}
              </div>
            )}
          </div>

          {isLoading && (
            <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
              장소 상세 정보를 불러오는 중입니다.
            </div>
          )}

          {isError && (
            <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">
              장소 상세 정보를 불러오지 못했습니다.
            </div>
          )}
        </div>
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

  return (
    <div className="absolute inset-y-0 right-0 z-20 hidden w-[400px] border-l border-slate-200 bg-white shadow-[0_20px_60px_rgba(15,23,42,0.18)] md:block">
      {content}
    </div>
  );
};
