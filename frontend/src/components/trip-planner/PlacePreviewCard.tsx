import type {KeyboardEvent, ReactNode} from "react";
import {Star} from "lucide-react";
import type {
  NormalizedPlaceCategoryKey,
  OpeningSummary,
  PlaceDetailSummary,
  PlaceTypeSummary,
} from "@/api/placeMetadata";
import {
  getPlaceCategoryColor,
  getPlaceTypeLabel,
} from "@/lib/placePresentation";
import {cn} from "@/lib/utils";

export interface PlacePreviewCardPlace {
  title: string;
  address?: string | null;
  placeTypeSummary?: PlaceTypeSummary | null;
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null;
  placeDetailSummary?: PlaceDetailSummary | null;
  openingSummary?: OpeningSummary | null;
}

interface PlacePreviewCardProps {
  place: PlacePreviewCardPlace;
  action?: ReactNode;
  isDragging?: boolean;
  draggable?: boolean;
  onSelect?: () => void;
  onDragStart?: () => void;
  onDragEnd?: () => void;
  className?: string;
}

export const PlacePreviewCard = ({
  place,
  action,
  isDragging = false,
  draggable = false,
  onSelect,
  onDragStart,
  onDragEnd,
  className,
}: PlacePreviewCardProps) => {
  const categoryLabel = getPlaceTypeLabel(place.placeTypeSummary, place.normalizedCategoryKey);
  const categoryColor = getPlaceCategoryColor(place.placeTypeSummary, place.normalizedCategoryKey);
  const rating = place.placeDetailSummary?.rating;
  const reviewCount = place.placeDetailSummary?.userRatingCount;
  const opening = getPlaceOpeningDisplay(place.openingSummary, place.placeDetailSummary);
  const shortAddress = formatShortAddress(place.address);
  const isInteractive = Boolean(onSelect);

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (!onSelect) {
      return;
    }

    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onSelect();
    }
  };

  return (
    <div
      role={isInteractive ? "button" : undefined}
      tabIndex={isInteractive ? 0 : undefined}
      draggable={draggable}
      onClick={onSelect}
      onKeyDown={handleKeyDown}
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
      className={cn(
        "block min-h-[124px] w-full rounded-xl border border-slate-100 bg-white p-4 text-left shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-md",
        isInteractive && "cursor-pointer",
        isDragging && "scale-[0.98] opacity-60",
        className,
      )}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="mb-2 flex min-w-0 items-center gap-2">
            <h3 className="truncate text-base font-bold leading-tight text-slate-900">{place.title}</h3>
            {categoryLabel && (
              <span
                className="shrink-0 rounded border px-1.5 py-0.5 text-[10px] font-bold"
                style={{
                  borderColor: `${categoryColor}33`,
                  backgroundColor: `${categoryColor}12`,
                  color: categoryColor,
                }}
              >
                {categoryLabel}
              </span>
            )}
          </div>

          <p className="mb-3 line-clamp-2 text-sm font-semibold text-slate-500">{shortAddress}</p>

          {typeof rating === "number" && (
            <div className="mb-3 flex items-center gap-1.5">
              <Star className="h-3.5 w-3.5 fill-yellow-400 text-yellow-400" />
              <span className="text-sm font-bold text-slate-900">{rating.toFixed(1)}</span>
              {typeof reviewCount === "number" && (
                <span className="text-xs font-semibold text-slate-400">
                  ({reviewCount.toLocaleString()} Reviews)
                </span>
              )}
            </div>
          )}

          <div className={cn("flex items-center text-sm font-bold", opening.className)}>
            <span className={cn("mr-2 h-2 w-2 rounded-full", opening.dotClassName)} />
            {opening.label}
          </div>
        </div>

        {action && (
          <div className="shrink-0" onClick={(event) => event.stopPropagation()}>
            {action}
          </div>
        )}
      </div>
    </div>
  );
};

const formatShortAddress = (address?: string | null) => {
  const normalized = address?.trim();
  if (!normalized) {
    return "주소 정보 없음";
  }

  const segments = normalized
    .split(",")
    .map((segment) => segment.trim())
    .filter(Boolean)
    .filter((segment) => !/^(japan|south korea|republic of korea|대한민국|일본)$/i.test(segment))
    .filter((segment) => !segment.startsWith("〒"));

  if (segments.length >= 3) {
    return segments.slice(-3).join(", ");
  }

  return segments.join(", ") || normalized;
};

const getPlaceOpeningDisplay = (
  openingSummary?: OpeningSummary | null,
  detailSummary?: PlaceDetailSummary | null,
) => {
  if (openingSummary?.openNow === true) {
    const closeTime = formatKoreanTime(openingSummary.nextCloseTime);
    return {
      label: closeTime ? `영업 중 - ${closeTime}까지 영업` : "영업 중",
      className: "text-green-600",
      dotClassName: "bg-green-500",
    };
  }

  if (openingSummary?.openNow === false) {
    const openTime = formatKoreanTime(openingSummary.nextOpenTime);
    return {
      label: openTime ? `영업시간 외 - ${openTime} 개점` : "영업시간 외",
      className: "text-slate-500",
      dotClassName: "bg-slate-300",
    };
  }

  switch (detailSummary?.businessStatus) {
    case "CLOSED_TEMPORARILY":
      return {
        label: "임시 휴업",
        className: "text-rose-600",
        dotClassName: "bg-rose-500",
      };
    case "CLOSED_PERMANENTLY":
      return {
        label: "폐업",
        className: "text-rose-600",
        dotClassName: "bg-rose-500",
      };
    default:
      return {
        label: "영업시간 정보 없음",
        className: "text-slate-500",
        dotClassName: "bg-slate-300",
      };
  }
};

const formatKoreanTime = (value?: string | null) => {
  if (!value) {
    return null;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(date);
};
