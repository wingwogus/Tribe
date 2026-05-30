import {forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState} from "react";
import {ItineraryResponse} from "@/api/itinerary";
import {WishlistItem} from "@/api/wishlist";
import {PlaceSearchResult} from "@/api/places";
import {getCountryCoordinates} from "@/lib/countryCoordinates";
import {getTripRegionCenter} from "@/lib/tripRegions";
import {getPlaceCategoryColor, getPlaceTypeKey} from "@/lib/placePresentation";
import {
    CalendarPlus,
    ExternalLink,
    Heart,
    X,
} from "lucide-react";
import {Button} from "@/components/ui/button";
import {
    getGoogleMapsConfig,
    GoogleMapsConfigurationError,
    loadGoogleMapsLibraries,
} from "@/lib/googleMapsLoader";
import type {GoogleMapsLibraries} from "@/lib/googleMapsLoader";

export interface ItineraryMapHandle {
    focusItineraryMarker: (itineraryId: number, options?: { offsetForPanel?: boolean; visibleLeftInsetPx?: number }) => void;
    focusWishlistMarker: (wishlistItemId: number, options?: { offsetForPanel?: boolean; visibleLeftInsetPx?: number }) => void;
    focusNearbyMarker: (externalPlaceId: string, options?: { offsetForPanel?: boolean; visibleLeftInsetPx?: number }) => void;
    getSearchArea: (options?: { visibleLeftInsetPx?: number }) => { latitude: number; longitude: number; radiusMeters: number } | null;
}

export interface GooglePoiSelection {
    externalPlaceId: string;
    latitude: number;
    longitude: number;
}

interface ItineraryMapProps {
    items?: ItineraryResponse[];
    days?: number[];
    wishlistItems?: WishlistItem[];
    nearbyPlaces?: PlaceSearchResult[];
    tripCountry?: string;
    tripRegionCode?: string | null;
    selectedItineraryId?: number | null;
    selectedWishlistItemId?: number | null;
    selectedNearbyPlaceExternalId?: string | null;
    panelOffsetPx?: number;
    visibleLeftInsetPx?: number;
    onSelectItineraryMarker?: (item: ItineraryResponse) => void;
    onSelectWishlistMarker?: (item: WishlistItem) => void;
    onSelectNearbyPlace?: (place: PlaceSearchResult) => void;
    onAddGooglePoiToWishlist?: (place: GooglePoiSelection) => void;
    onAddGooglePoiToItinerary?: (place: GooglePoiSelection) => void;
    isAddingGooglePoiToWishlist?: boolean;
    isAddingGooglePoiToItinerary?: boolean;
    onOpenGooglePoiInGoogleMaps?: (place: GooglePoiSelection) => void;
    onSearchAreaChange?: () => void;
}

type AdvancedMarker = google.maps.marker.AdvancedMarkerElement;
type MarkerRecord = {
    marker: AdvancedMarker;
    clickListener: google.maps.MapsEventListener;
};
type GooglePoiActionState = GooglePoiSelection & {
    anchor: {
        x: number;
        y: number;
    };
};
type NearbyMarkerIconKey =
    | "restaurant"
    | "cafe"
    | "bakery"
    | "bar"
    | "attraction"
    | "shopping"
    | "stay"
    | "park"
    | "museum"
    | "transport"
    | "place";
type NearbyMarkerIconNode = [
    tagName: "path" | "line" | "polygon" | "circle",
    attributes: Record<string, string>,
];

const MIN_NEARBY_SEARCH_RADIUS_METERS = 100;
const MAX_NEARBY_SEARCH_RADIUS_METERS = 5_000;
const MIN_VISIBLE_MAP_WIDTH_PX = 240;
const DEFAULT_CENTER: [number, number] = [37.5665, 126.9780];
const DEFAULT_PLACE_ZOOM = 12;
const WEB_MERCATOR_TILE_SIZE = 256;
const MAX_MERCATOR_LATITUDE = 85.05112878;
const MAX_MAP_ZOOM = 21;
const MAP_CAMERA_ANIMATION_DURATION_MS = 520;
const GOOGLE_POI_ACTION_DOCK_WIDTH_PX = 292;
const GOOGLE_POI_ACTION_DOCK_HEIGHT_PX = 48;
const GOOGLE_POI_ACTION_DOCK_MARGIN_PX = 16;
const GOOGLE_POI_ACTION_DOCK_OFFSET_PX = 22;
const NEARBY_MARKER_SIZE_PX = 32;
const NEARBY_MARKER_SELECTED_SIZE_PX = 38;
const cameraAnimationFrames = new WeakMap<google.maps.Map, number>();

const NEARBY_MARKER_ICON_NODES: Record<NearbyMarkerIconKey, NearbyMarkerIconNode[]> = {
    restaurant: [
        ["path", {d: "M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"}],
        ["path", {d: "M7 2v20"}],
        ["path", {d: "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7"}],
    ],
    cafe: [
        ["path", {d: "M10 2v2"}],
        ["path", {d: "M14 2v2"}],
        ["path", {d: "M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1"}],
        ["path", {d: "M6 2v2"}],
    ],
    bakery: [
        ["path", {d: "m4.6 13.11 5.79-3.21c1.89-1.05 4.79 1.78 3.71 3.71l-3.22 5.81C8.8 23.16.79 15.23 4.6 13.11Z"}],
        ["path", {d: "m10.5 9.5-1-2.29C9.2 6.48 8.8 6 8 6H4.5C2.79 6 2 6.5 2 8.5a7.71 7.71 0 0 0 2 4.83"}],
        ["path", {d: "M8 6c0-1.55.24-4-2-4-2 0-2.5 2.17-2.5 4"}],
        ["path", {d: "m14.5 13.5 2.29 1c.73.3 1.21.7 1.21 1.5v3.5c0 1.71-.5 2.5-2.5 2.5a7.71 7.71 0 0 1-4.83-2"}],
        ["path", {d: "M18 16c1.55 0 4-.24 4 2 0 2-2.17 2.5-4 2.5"}],
    ],
    bar: [
        ["path", {d: "M8 22h8"}],
        ["path", {d: "M7 10h10"}],
        ["path", {d: "M12 15v7"}],
        ["path", {d: "M12 15a5 5 0 0 0 5-5c0-2-.5-4-2-8H9c-1.5 4-2 6-2 8a5 5 0 0 0 5 5Z"}],
    ],
    attraction: [
        ["line", {x1: "3", x2: "21", y1: "22", y2: "22"}],
        ["line", {x1: "6", x2: "6", y1: "18", y2: "11"}],
        ["line", {x1: "10", x2: "10", y1: "18", y2: "11"}],
        ["line", {x1: "14", x2: "14", y1: "18", y2: "11"}],
        ["line", {x1: "18", x2: "18", y1: "18", y2: "11"}],
        ["polygon", {points: "12 2 20 7 4 7"}],
    ],
    shopping: [
        ["path", {d: "M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"}],
        ["path", {d: "M3 6h18"}],
        ["path", {d: "M16 10a4 4 0 0 1-8 0"}],
    ],
    stay: [
        ["path", {d: "M2 20v-8a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v8"}],
        ["path", {d: "M4 10V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v4"}],
        ["path", {d: "M12 4v6"}],
        ["path", {d: "M2 18h20"}],
    ],
    park: [
        ["path", {d: "m17 14 3 3.3a1 1 0 0 1-.7 1.7H4.7a1 1 0 0 1-.7-1.7L7 14h-.3a1 1 0 0 1-.7-1.7L9 9h-.2A1 1 0 0 1 8 7.3L12 3l4 4.3a1 1 0 0 1-.8 1.7H15l3 3.3a1 1 0 0 1-.7 1.7H17Z"}],
        ["path", {d: "M12 22v-3"}],
    ],
    museum: [
        ["line", {x1: "3", x2: "21", y1: "22", y2: "22"}],
        ["line", {x1: "6", x2: "6", y1: "18", y2: "11"}],
        ["line", {x1: "10", x2: "10", y1: "18", y2: "11"}],
        ["line", {x1: "14", x2: "14", y1: "18", y2: "11"}],
        ["line", {x1: "18", x2: "18", y1: "18", y2: "11"}],
        ["polygon", {points: "12 2 20 7 4 7"}],
    ],
    transport: [
        ["path", {d: "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"}],
        ["circle", {cx: "12", cy: "10", r: "3"}],
    ],
    place: [
        ["path", {d: "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"}],
        ["circle", {cx: "12", cy: "10", r: "3"}],
    ],
};

const NEARBY_MARKER_ICON_BY_TYPE: Record<string, NearbyMarkerIconKey> = {
    KOREAN_FOOD: "restaurant",
    JAPANESE_FOOD: "restaurant",
    CHINESE_FOOD: "restaurant",
    RESTAURANT: "restaurant",
    CAFE: "cafe",
    BAKERY: "bakery",
    BAR: "bar",
    ATTRACTION: "attraction",
    SHOPPING: "shopping",
    STAY: "stay",
    PARK: "park",
    MUSEUM: "museum",
    TRANSPORT: "transport",
    ETC: "place",
    restaurant: "restaurant",
    cafe: "cafe",
    bakery: "bakery",
    bar: "bar",
    tourist_attraction: "attraction",
    shopping_mall: "shopping",
    store: "shopping",
    convenience_store: "shopping",
    lodging: "stay",
    park: "park",
    museum: "museum",
    subway_station: "transport",
    train_station: "transport",
    airport: "transport",
    bus_station: "transport",
};

const toLatLngLiteral = ([lat, lng]: [number, number]): google.maps.LatLngLiteral => ({lat, lng});

const isFiniteCoordinate = (lat: number, lng: number) => Number.isFinite(lat) && Number.isFinite(lng);

const toGooglePoiSelection = (state: GooglePoiActionState): GooglePoiSelection => ({
    externalPlaceId: state.externalPlaceId,
    latitude: state.latitude,
    longitude: state.longitude,
});

const getSafeVisibleLeftInsetPx = (currentMap: google.maps.Map, visibleLeftInsetPx: number): number => {
    const mapWidth = currentMap.getDiv().clientWidth;
    return Math.min(
        Math.max(0, visibleLeftInsetPx),
        Math.max(0, mapWidth - MIN_VISIBLE_MAP_WIDTH_PX),
    );
};

const calculateDistanceMeters = (
    from: google.maps.LatLng | google.maps.LatLngLiteral,
    to: google.maps.LatLng | google.maps.LatLngLiteral,
): number => {
    const fromLat = typeof from.lat === "function" ? from.lat() : from.lat;
    const fromLng = typeof from.lng === "function" ? from.lng() : from.lng;
    const toLat = typeof to.lat === "function" ? to.lat() : to.lat;
    const toLng = typeof to.lng === "function" ? to.lng() : to.lng;
    const earthRadiusMeters = 6_371_000;
    const toRadians = (degrees: number) => degrees * Math.PI / 180;
    const deltaLat = toRadians(toLat - fromLat);
    const deltaLng = toRadians(toLng - fromLng);
    const a = Math.sin(deltaLat / 2) ** 2
        + Math.cos(toRadians(fromLat)) * Math.cos(toRadians(toLat)) * Math.sin(deltaLng / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return earthRadiusMeters * c;
};

const getVisibleMapCenter = (
    currentMap: google.maps.Map,
    projection: google.maps.MapCanvasProjection,
    visibleLeftInsetPx: number,
): google.maps.LatLng | null => {
    const mapDiv = currentMap.getDiv();
    const safeLeftInsetPx = getSafeVisibleLeftInsetPx(currentMap, visibleLeftInsetPx);
    const visibleCenterPoint = new google.maps.Point(
        safeLeftInsetPx + ((mapDiv.clientWidth - safeLeftInsetPx) / 2),
        mapDiv.clientHeight / 2,
    );

    return projection.fromContainerPixelToLatLng(visibleCenterPoint, true);
};

const calculateSearchRadiusMeters = (
    currentMap: google.maps.Map,
    projection: google.maps.MapCanvasProjection,
    visibleLeftInsetPx: number,
): number | null => {
    const mapDiv = currentMap.getDiv();
    const safeLeftInsetPx = getSafeVisibleLeftInsetPx(currentMap, visibleLeftInsetPx);
    const center = getVisibleMapCenter(currentMap, projection, safeLeftInsetPx);
    const topRight = projection.fromContainerPixelToLatLng(
        new google.maps.Point(mapDiv.clientWidth, 0),
        true,
    );
    const bottomLeft = projection.fromContainerPixelToLatLng(
        new google.maps.Point(safeLeftInsetPx, mapDiv.clientHeight),
        true,
    );

    if (!center || !topRight || !bottomLeft) {
        return null;
    }

    const visibleRadius = Math.max(
        calculateDistanceMeters(center, topRight),
        calculateDistanceMeters(center, bottomLeft),
    );

    return Math.round(Math.min(
        MAX_NEARBY_SEARCH_RADIUS_METERS,
        Math.max(MIN_NEARBY_SEARCH_RADIUS_METERS, visibleRadius),
    ));
};

const getMarkerLatLng = (marker: AdvancedMarker): google.maps.LatLng | null => {
    const position = marker.position;
    if (!position) {
        return null;
    }

    if (position instanceof google.maps.LatLng) {
        return position;
    }

    return new google.maps.LatLng(position.lat, position.lng);
};

type PixelPoint = {
    x: number;
    y: number;
};

const clampLatitudeForMercator = (latitude: number): number => Math.min(
    Math.max(latitude, -MAX_MERCATOR_LATITUDE),
    MAX_MERCATOR_LATITUDE,
);

const latLngToPixelPoint = (latLng: google.maps.LatLng, zoom: number): PixelPoint => {
    const scale = WEB_MERCATOR_TILE_SIZE * (2 ** zoom);
    const latitude = clampLatitudeForMercator(latLng.lat());
    const sinLatitude = Math.sin(latitude * Math.PI / 180);

    return {
        x: ((latLng.lng() + 180) / 360) * scale,
        y: (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)) * scale,
    };
};

const pixelPointToLatLng = (point: PixelPoint, zoom: number): google.maps.LatLngLiteral => {
    const scale = WEB_MERCATOR_TILE_SIZE * (2 ** zoom);
    const longitude = (point.x / scale) * 360 - 180;
    const mercatorY = 0.5 - (point.y / scale);
    const latitude = 90 - (360 * Math.atan(Math.exp(-mercatorY * 2 * Math.PI)) / Math.PI);

    return {
        lat: clampLatitudeForMercator(latitude),
        lng: longitude,
    };
};

const getOffsetCenter = (
    position: google.maps.LatLng,
    offsetX: number,
    zoom: number,
): google.maps.LatLngLiteral => {
    const point = latLngToPixelPoint(position, zoom);

    return pixelPointToLatLng(
        {
            x: point.x + offsetX,
            y: point.y,
        },
        zoom,
    );
};

const toLiteral = (latLng: google.maps.LatLng | google.maps.LatLngLiteral): google.maps.LatLngLiteral => {
    if (latLng instanceof google.maps.LatLng) {
        return {lat: latLng.lat(), lng: latLng.lng()};
    }

    return latLng;
};

const shouldReduceMotion = () =>
    typeof window !== "undefined"
    && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

const easeInOutCubic = (progress: number) =>
    progress < 0.5
        ? 4 * progress * progress * progress
        : 1 - ((-2 * progress + 2) ** 3) / 2;

const interpolateLongitude = (start: number, end: number, progress: number) => {
    const delta = ((end - start + 540) % 360) - 180;
    return start + delta * progress;
};

const cancelMapCameraAnimation = (currentMap: google.maps.Map) => {
    const frameId = cameraAnimationFrames.get(currentMap);
    if (frameId !== undefined) {
        cancelAnimationFrame(frameId);
        cameraAnimationFrames.delete(currentMap);
    }
};

const animateMapCamera = (
    currentMap: google.maps.Map,
    target: { center: google.maps.LatLng | google.maps.LatLngLiteral; zoom?: number },
    durationMs = MAP_CAMERA_ANIMATION_DURATION_MS,
) => {
    const targetCenter = toLiteral(target.center);
    const targetZoom = target.zoom ?? currentMap.getZoom() ?? DEFAULT_PLACE_ZOOM;
    const startCenter = currentMap.getCenter();
    const startZoom = currentMap.getZoom() ?? targetZoom;

    cancelMapCameraAnimation(currentMap);

    if (!startCenter || shouldReduceMotion() || durationMs <= 0) {
        currentMap.moveCamera({center: targetCenter, zoom: targetZoom});
        return;
    }

    const start = {
        lat: startCenter.lat(),
        lng: startCenter.lng(),
        zoom: startZoom,
    };
    const startTime = performance.now();

    const step = (now: number) => {
        const progress = Math.min(1, (now - startTime) / durationMs);
        const eased = easeInOutCubic(progress);
        const center = {
            lat: start.lat + (targetCenter.lat - start.lat) * eased,
            lng: interpolateLongitude(start.lng, targetCenter.lng, eased),
        };
        const zoom = start.zoom + (targetZoom - start.zoom) * eased;

        currentMap.moveCamera({center, zoom});

        if (progress < 1) {
            cameraAnimationFrames.set(currentMap, requestAnimationFrame(step));
        } else {
            cameraAnimationFrames.delete(currentMap);
        }
    };

    cameraAnimationFrames.set(currentMap, requestAnimationFrame(step));
};

const latRad = (latitude: number) => {
    const sin = Math.sin(latitude * Math.PI / 180);
    const radX2 = Math.log((1 + sin) / (1 - sin)) / 2;

    return Math.max(Math.min(radX2, Math.PI), -Math.PI) / 2;
};

const getBoundsZoom = (currentMap: google.maps.Map, bounds: google.maps.LatLngBounds, paddingPx: number) => {
    const mapDiv = currentMap.getDiv();
    const ne = bounds.getNorthEast();
    const sw = bounds.getSouthWest();
    const mapWidth = Math.max(1, mapDiv.clientWidth - paddingPx * 2);
    const mapHeight = Math.max(1, mapDiv.clientHeight - paddingPx * 2);
    const latFraction = Math.max(0.000001, (latRad(ne.lat()) - latRad(sw.lat())) / Math.PI);
    const lngDiff = ne.lng() - sw.lng();
    const lngFraction = Math.max(0.000001, (lngDiff < 0 ? lngDiff + 360 : lngDiff) / 360);
    const latZoom = Math.floor(Math.log(mapHeight / WEB_MERCATOR_TILE_SIZE / latFraction) / Math.LN2);
    const lngZoom = Math.floor(Math.log(mapWidth / WEB_MERCATOR_TILE_SIZE / lngFraction) / Math.LN2);

    return Math.max(0, Math.min(MAX_MAP_ZOOM, latZoom, lngZoom));
};

const smoothFitBounds = (currentMap: google.maps.Map, bounds: google.maps.LatLngBounds, paddingPx: number) => {
    animateMapCamera(currentMap, {
        center: bounds.getCenter(),
        zoom: getBoundsZoom(currentMap, bounds, paddingPx),
    });
};

const getGooglePoiActionAnchor = (
    currentMap: google.maps.Map,
    currentProjection: google.maps.MapCanvasProjection,
    latLng: google.maps.LatLng,
): GooglePoiActionState["anchor"] | null => {
    const point = currentProjection.fromLatLngToContainerPixel(latLng);
    if (!point) {
        return null;
    }

    const mapDiv = currentMap.getDiv();
    const minX = GOOGLE_POI_ACTION_DOCK_MARGIN_PX + GOOGLE_POI_ACTION_DOCK_WIDTH_PX / 2;
    const maxX = mapDiv.clientWidth - minX;
    const minY = GOOGLE_POI_ACTION_DOCK_MARGIN_PX;
    const maxY = mapDiv.clientHeight - GOOGLE_POI_ACTION_DOCK_HEIGHT_PX - GOOGLE_POI_ACTION_DOCK_MARGIN_PX;
    const belowPoiY = point.y + GOOGLE_POI_ACTION_DOCK_OFFSET_PX;

    return {
        x: Math.min(Math.max(point.x, minX), Math.max(minX, maxX)),
        y: Math.min(Math.max(belowPoiY, minY), Math.max(minY, maxY)),
    };
};

const focusMarker = (
    map: google.maps.Map,
    marker: AdvancedMarker,
    options: { offsetForPanel?: boolean; panelOffsetPx?: number; visibleLeftInsetPx?: number } = {},
) => {
    const latLng = getMarkerLatLng(marker);
    if (!latLng) {
        return;
    }

    const zoom = Math.max(map.getZoom() ?? 0, 16);
    const visibleLeftInsetPx = options.visibleLeftInsetPx ?? 0;
    let center: google.maps.LatLng | google.maps.LatLngLiteral = latLng;

    if (visibleLeftInsetPx > 0) {
        const safeLeftInsetPx = getSafeVisibleLeftInsetPx(map, visibleLeftInsetPx);
        center = getOffsetCenter(latLng, -safeLeftInsetPx / 2, zoom);
    } else if (options.offsetForPanel) {
        const offsetPx = Math.max(0, Math.round((options.panelOffsetPx ?? 380) / 2));
        center = getOffsetCenter(latLng, offsetPx, zoom);
    }

    animateMapCamera(map, {center, zoom});
};

const applyBaseMarkerStyles = (
    element: HTMLElement,
    size: number,
    color: string,
    shadow: string,
    borderRadius = "999px",
) => {
    Object.assign(element.style, {
        alignItems: "center",
        background: color,
        border: "3px solid white",
        borderRadius,
        boxShadow: shadow,
        color: "white",
        display: "flex",
        fontWeight: "700",
        height: `${size}px`,
        justifyContent: "center",
        transform: "translateZ(0)",
        width: `${size}px`,
    });
};

const buildItineraryMarkerContent = (index: number, color: string, isSelected: boolean) => {
    const size = isSelected ? 32 : 24;
    const element = document.createElement("div");
    applyBaseMarkerStyles(
        element,
        size,
        color,
        isSelected ? "0 0 0 6px rgba(255,255,255,0.8), 0 12px 28px rgba(15,23,42,0.25)" : "0 2px 8px rgba(0,0,0,0.3)",
    );
    element.style.borderWidth = isSelected ? "4px" : "3px";
    element.style.fontSize = isSelected ? "13px" : "12px";
    element.textContent = String(index + 1);

    return element;
};

const buildWishlistMarkerContent = (color: string, isSelected: boolean) => {
    const size = isSelected ? 30 : 24;
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("width", String(size));
    svg.setAttribute("height", String(size));
    svg.setAttribute("viewBox", "0 0 100 100");
    svg.style.filter = isSelected
        ? "drop-shadow(0 10px 18px rgba(15,23,42,0.28))"
        : "drop-shadow(0 4px 10px rgba(15,23,42,0.2))";

    const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
    circle.setAttribute("cx", "50");
    circle.setAttribute("cy", "50");
    circle.setAttribute("r", "42");
    circle.setAttribute("fill", color);
    circle.setAttribute("stroke", isSelected ? "rgba(255,255,255,0.95)" : "white");
    circle.setAttribute("stroke-width", "8");
    svg.appendChild(circle);

    const star = document.createElementNS("http://www.w3.org/2000/svg", "path");
    star.setAttribute(
        "d",
        "M50 26 L58.96 41.68 L76.8 44.72 L64.64 58.04 L66.48 76.4 L50 69.52 L33.52 76.4 L35.36 58.04 L23.2 44.72 L41.04 41.68 Z",
    );
    star.setAttribute("fill", "#ffffff");
    svg.appendChild(star);

    return svg;
};

const getNearbyMarkerSize = (isSelected: boolean) =>
    isSelected ? NEARBY_MARKER_SELECTED_SIZE_PX : NEARBY_MARKER_SIZE_PX;

const getNearbyMarkerAnchorLeft = (isSelected: boolean) =>
    `calc(-100% + ${getNearbyMarkerSize(isSelected) / 2}px)`;

const getNearbyMarkerIconKey = (place: PlaceSearchResult): NearbyMarkerIconKey => {
    const typeKey = getPlaceTypeKey(place.placeTypeSummary, place.normalizedCategoryKey);
    return (typeKey && NEARBY_MARKER_ICON_BY_TYPE[typeKey]) || "place";
};

const buildNearbyMarkerIcon = (iconKey: NearbyMarkerIconKey, size: number) => {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("width", String(Math.round(size * 0.56)));
    svg.setAttribute("height", String(Math.round(size * 0.56)));
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("fill", "none");
    svg.setAttribute("stroke", "#ffffff");
    svg.setAttribute("stroke-width", iconKey === "place" ? "2.5" : "2.25");
    svg.setAttribute("stroke-linecap", "round");
    svg.setAttribute("stroke-linejoin", "round");
    svg.setAttribute("aria-hidden", "true");

    NEARBY_MARKER_ICON_NODES[iconKey].forEach(([tagName, attributes]) => {
        const node = document.createElementNS("http://www.w3.org/2000/svg", tagName);
        Object.entries(attributes).forEach(([name, value]) => {
            node.setAttribute(name, value);
        });
        svg.appendChild(node);
    });

    return svg;
};

const buildNearbyMarkerContent = (label: string, color: string, isSelected: boolean, iconKey: NearbyMarkerIconKey) => {
    const size = getNearbyMarkerSize(isSelected);
    const root = document.createElement("div");
    const text = document.createElement("div");
    const marker = document.createElement("div");

    Object.assign(root.style, {
        alignItems: "center",
        display: "flex",
        gap: "6px",
        pointerEvents: "auto",
        transform: "translateZ(0)",
    });

    Object.assign(text.style, {
        color,
        fontSize: isSelected ? "15px" : "14px",
        fontWeight: "800",
        lineHeight: "1.15",
        maxWidth: "132px",
        overflow: "hidden",
        textAlign: "right",
        textOverflow: "ellipsis",
        textShadow: "0 1px 0 #ffffff, 1px 0 0 #ffffff, -1px 0 0 #ffffff, 0 -1px 0 #ffffff, 0 2px 6px rgba(15,23,42,0.18)",
        whiteSpace: "nowrap",
    });
    text.textContent = label;

    Object.assign(marker.style, {
        alignItems: "center",
        background: color,
        border: "4px solid #ffffff",
        borderRadius: "999px",
        boxShadow: isSelected
            ? "0 0 0 4px rgba(255,255,255,0.72), 0 12px 24px rgba(15,23,42,0.32)"
            : "0 2px 4px rgba(15,23,42,0.18), 0 5px 12px rgba(15,23,42,0.22)",
        display: "flex",
        height: `${size}px`,
        justifyContent: "center",
        width: `${size}px`,
    });
    marker.appendChild(buildNearbyMarkerIcon(iconKey, size));

    root.appendChild(text);
    root.appendChild(marker);

    return root;
};

const clearMarkerRecords = <T,>(records: Map<T, MarkerRecord>) => {
    records.forEach(({marker, clickListener}) => {
        clickListener.remove();
        marker.map = null;
    });
    records.clear();
};

const createProjectionOverlay = (
    OverlayView: typeof google.maps.OverlayView,
    onProjectionReady: (projection: google.maps.MapCanvasProjection) => void,
) => {
    class ProjectionOverlay extends OverlayView {
        onAdd() {
            onProjectionReady(this.getProjection());
        }

        draw() {
            onProjectionReady(this.getProjection());
        }

        onRemove() {}
    }

    return new ProjectionOverlay();
};

export const ItineraryMap = forwardRef<ItineraryMapHandle, ItineraryMapProps>(
    ({
        items = [],
        wishlistItems = [],
        nearbyPlaces = [],
        tripCountry,
        tripRegionCode,
        selectedItineraryId = null,
        selectedWishlistItemId = null,
        selectedNearbyPlaceExternalId = null,
        panelOffsetPx = 400,
        visibleLeftInsetPx = 0,
        onSelectItineraryMarker,
        onSelectWishlistMarker,
        onSelectNearbyPlace,
        onAddGooglePoiToWishlist,
        onAddGooglePoiToItinerary,
        isAddingGooglePoiToWishlist = false,
        isAddingGooglePoiToItinerary = false,
        onOpenGooglePoiInGoogleMaps,
        onSearchAreaChange,
    }, ref) => {
        const mapContainer = useRef<HTMLDivElement>(null);
        const map = useRef<google.maps.Map | null>(null);
        const googleLibraries = useRef<GoogleMapsLibraries | null>(null);
        const projectionOverlay = useRef<google.maps.OverlayView | null>(null);
        const projection = useRef<google.maps.MapCanvasProjection | null>(null);
        const markersMap = useRef<Map<number, MarkerRecord>>(new Map());
        const wishlistMarkersMap = useRef<Map<number, MarkerRecord>>(new Map());
        const nearbyMarkersMap = useRef<Map<string, MarkerRecord>>(new Map());
        const polyline = useRef<google.maps.Polyline | null>(null);
        const lastFittedItineraryKey = useRef<string | null>(null);
        const [isMapReady, setIsMapReady] = useState(false);
        const [isProjectionReady, setIsProjectionReady] = useState(false);
        const [loadError, setLoadError] = useState<string | null>(null);
        const [selectedGooglePoi, setSelectedGooglePoi] = useState<GooglePoiActionState | null>(null);

        const defaultCenter = useMemo(
            () => getTripRegionCenter(tripRegionCode)
                ?? (tripCountry ? getCountryCoordinates(tripCountry) : DEFAULT_CENTER),
            [tripCountry, tripRegionCode],
        );
        const defaultCenterRef = useRef(defaultCenter);

        useEffect(() => {
            defaultCenterRef.current = defaultCenter;
        }, [defaultCenter]);

        const validItineraries = useMemo(
            () => [...items]
                .sort((a, b) => a.visitDay - b.visitDay || a.itemOrder - b.itemOrder)
                .filter((item) => item.location && isFiniteCoordinate(item.location.lat, item.location.lng)),
            [items],
        );

        const itineraryCoordinatesKey = useMemo(
            () => validItineraries
                .map((item) => `${item.itineraryId}:${item.location!.lat.toFixed(6)},${item.location!.lng.toFixed(6)}`)
                .join("|"),
            [validItineraries],
        );

        useImperativeHandle(ref, () => ({
            focusItineraryMarker: (itineraryId, options) => {
                const record = markersMap.current.get(itineraryId);
                if (record && map.current) {
                    focusMarker(map.current, record.marker, {
                        offsetForPanel: options?.offsetForPanel,
                        panelOffsetPx,
                        visibleLeftInsetPx: options?.visibleLeftInsetPx ?? visibleLeftInsetPx,
                    });
                }
            },
            focusWishlistMarker: (wishlistItemId, options) => {
                const record = wishlistMarkersMap.current.get(wishlistItemId);
                if (record && map.current) {
                    focusMarker(map.current, record.marker, {
                        offsetForPanel: options?.offsetForPanel,
                        panelOffsetPx,
                        visibleLeftInsetPx: options?.visibleLeftInsetPx ?? visibleLeftInsetPx,
                    });
                }
            },
            focusNearbyMarker: (externalPlaceId, options) => {
                const record = nearbyMarkersMap.current.get(externalPlaceId);
                if (record && map.current) {
                    focusMarker(map.current, record.marker, {
                        offsetForPanel: options?.offsetForPanel,
                        panelOffsetPx,
                        visibleLeftInsetPx: options?.visibleLeftInsetPx ?? visibleLeftInsetPx,
                    });
                }
            },
            getSearchArea: (options) => {
                if (!map.current || !projection.current) {
                    return null;
                }

                const effectiveVisibleLeftInsetPx = options?.visibleLeftInsetPx ?? visibleLeftInsetPx;
                const center = getVisibleMapCenter(map.current, projection.current, effectiveVisibleLeftInsetPx);
                const radiusMeters = calculateSearchRadiusMeters(map.current, projection.current, effectiveVisibleLeftInsetPx);

                if (!center || radiusMeters === null) {
                    return null;
                }

                return {
                    latitude: center.lat(),
                    longitude: center.lng(),
                    radiusMeters,
                };
            },
        }), [panelOffsetPx, visibleLeftInsetPx]);

        useEffect(() => {
            let isCancelled = false;
            const itineraryMarkerRecords = markersMap.current;
            const wishlistMarkerRecords = wishlistMarkersMap.current;
            const nearbyMarkerRecords = nearbyMarkersMap.current;

            if (!mapContainer.current || map.current) {
                return;
            }

            const initializeMap = async () => {
                try {
                    const [libraries, config] = await Promise.all([
                        loadGoogleMapsLibraries(),
                        Promise.resolve(getGoogleMapsConfig()),
                    ]);

                    if (isCancelled || !mapContainer.current) {
                        return;
                    }

                    if (!config) {
                        throw new GoogleMapsConfigurationError();
                    }

                    googleLibraries.current = libraries;
                    const nextMap = new libraries.Map(mapContainer.current, {
                        center: toLatLngLiteral(defaultCenterRef.current),
                        clickableIcons: true,
                        fullscreenControl: false,
                        mapId: config.mapId,
                        mapTypeControl: false,
                        streetViewControl: false,
                        zoom: DEFAULT_PLACE_ZOOM,
                    });
                    const nextProjectionOverlay = createProjectionOverlay(libraries.OverlayView, (nextProjection) => {
                        projection.current = nextProjection;
                        setIsProjectionReady(true);
                    });

                    nextProjectionOverlay.setMap(nextMap);
                    projectionOverlay.current = nextProjectionOverlay;
                    map.current = nextMap;
                    setIsMapReady(true);
                } catch (error) {
                    if (isCancelled) {
                        return;
                    }

                    setLoadError(error instanceof GoogleMapsConfigurationError
                        ? "Google Maps 설정이 필요합니다."
                        : "지도를 불러오지 못했습니다.");
                }
            };

            void initializeMap();

            return () => {
                isCancelled = true;
                clearMarkerRecords(itineraryMarkerRecords);
                clearMarkerRecords(wishlistMarkerRecords);
                clearMarkerRecords(nearbyMarkerRecords);
                if (polyline.current) {
                    polyline.current.setMap(null);
                    polyline.current = null;
                }
                if (map.current) {
                    cancelMapCameraAnimation(map.current);
                }
                projectionOverlay.current?.setMap(null);
                projectionOverlay.current = null;
                projection.current = null;
                map.current = null;
            };
        }, []);

        useEffect(() => {
            if (!isMapReady || !isProjectionReady || !map.current) return;

            const currentMap = map.current;
            const handleSearchAreaChange = () => onSearchAreaChange?.();
            const idleListener = currentMap.addListener("idle", handleSearchAreaChange);
            handleSearchAreaChange();

            return () => {
                idleListener.remove();
            };
        }, [isMapReady, isProjectionReady, onSearchAreaChange]);

        useEffect(() => {
            if (!isMapReady || !isProjectionReady || !map.current) return;

            const currentMap = map.current;
            const clickListener = currentMap.addListener("click", (event: google.maps.MapMouseEvent) => {
                const iconEvent = event as google.maps.IconMouseEvent;
                if (!iconEvent.placeId) {
                    setSelectedGooglePoi(null);
                    return;
                }

                if (!iconEvent.latLng || !projection.current) {
                    setSelectedGooglePoi(null);
                    return;
                }

                const anchor = getGooglePoiActionAnchor(currentMap, projection.current, iconEvent.latLng);
                if (!anchor) {
                    setSelectedGooglePoi(null);
                    return;
                }

                setSelectedGooglePoi({
                    anchor,
                    externalPlaceId: iconEvent.placeId,
                    latitude: iconEvent.latLng.lat(),
                    longitude: iconEvent.latLng.lng(),
                });
            });
            return () => {
                clickListener.remove();
            };
        }, [isMapReady, isProjectionReady]);

        useEffect(() => {
            if (!isMapReady || !isProjectionReady || !map.current || !projection.current || !selectedGooglePoi) return;

            const currentMap = map.current;
            const updateActionAnchor = () => {
                if (!projection.current) {
                    return;
                }

                const anchor = getGooglePoiActionAnchor(
                    currentMap,
                    projection.current,
                    new google.maps.LatLng(selectedGooglePoi.latitude, selectedGooglePoi.longitude),
                );

                if (!anchor) {
                    return;
                }

                setSelectedGooglePoi((current) => {
                    if (!current || current.externalPlaceId !== selectedGooglePoi.externalPlaceId) {
                        return current;
                    }

                    if (Math.abs(current.anchor.x - anchor.x) < 0.5 && Math.abs(current.anchor.y - anchor.y) < 0.5) {
                        return current;
                    }

                    return {...current, anchor};
                });
            };

            const boundsChangedListener = currentMap.addListener("bounds_changed", updateActionAnchor);
            const idleListener = currentMap.addListener("idle", updateActionAnchor);
            updateActionAnchor();

            return () => {
                boundsChangedListener.remove();
                idleListener.remove();
            };
        }, [
            isMapReady,
            isProjectionReady,
            selectedGooglePoi,
        ]);

        useEffect(() => {
            if (!isMapReady || !map.current || validItineraries.length > 0) return;

            animateMapCamera(map.current, {
                center: toLatLngLiteral(defaultCenter),
                zoom: DEFAULT_PLACE_ZOOM,
            });
        }, [defaultCenter, isMapReady, validItineraries.length]);

        useEffect(() => {
            const libraries = googleLibraries.current;
            if (!isMapReady || !map.current || !libraries) return;

            clearMarkerRecords(markersMap.current);
            if (polyline.current) {
                polyline.current.setMap(null);
                polyline.current = null;
            }

            if (validItineraries.length === 0) {
                lastFittedItineraryKey.current = null;
                return;
            }

            const latLngs: google.maps.LatLngLiteral[] = [];

            validItineraries.forEach((item, index) => {
                if (!item.location) return;

                const position = {lat: item.location.lat, lng: item.location.lng};
                const markerColor = getPlaceCategoryColor(item.placeTypeSummary, item.normalizedCategoryKey);
                const isSelected = item.itineraryId === selectedItineraryId;
                const marker = new libraries.AdvancedMarkerElement({
                    anchorLeft: "-50%",
                    anchorTop: "-50%",
                    content: buildItineraryMarkerContent(index, markerColor, isSelected),
                    gmpClickable: true,
                    map: map.current,
                    position,
                    title: item.name,
                    zIndex: isSelected ? 200 : 0,
                });
                const clickListener = marker.addListener("click", () => onSelectItineraryMarker?.(item));

                markersMap.current.set(item.itineraryId, {marker, clickListener});
                latLngs.push(position);
            });

            if (latLngs.length > 1) {
                polyline.current = new libraries.Polyline({
                    icons: [{
                        icon: {
                            path: "M 0,-1 0,1",
                            scale: 3,
                            strokeColor: "#94A3B8",
                            strokeOpacity: 0.5,
                            strokeWeight: 3,
                        },
                        offset: "0",
                        repeat: "14px",
                    }],
                    map: map.current,
                    path: latLngs,
                    strokeOpacity: 0,
                    strokeWeight: 3,
                });

                if (lastFittedItineraryKey.current !== itineraryCoordinatesKey) {
                    const bounds = new google.maps.LatLngBounds();
                    latLngs.forEach((latLng) => bounds.extend(latLng));
                    smoothFitBounds(map.current, bounds, 50);
                    lastFittedItineraryKey.current = itineraryCoordinatesKey;
                }
            } else if (latLngs.length === 1 && lastFittedItineraryKey.current !== itineraryCoordinatesKey) {
                animateMapCamera(map.current, {
                    center: latLngs[0],
                    zoom: 13,
                });
                lastFittedItineraryKey.current = itineraryCoordinatesKey;
            }
        }, [isMapReady, itineraryCoordinatesKey, onSelectItineraryMarker, selectedItineraryId, validItineraries]);

        useEffect(() => {
            const libraries = googleLibraries.current;
            if (!isMapReady || !map.current || !libraries) return;

            clearMarkerRecords(wishlistMarkersMap.current);

            wishlistItems.forEach((item) => {
                if (!isFiniteCoordinate(item.latitude, item.longitude)) return;

                const markerColor = getPlaceCategoryColor(item.placeTypeSummary, item.normalizedCategoryKey);
                const isSelected = item.wishlistItemId === selectedWishlistItemId;
                const marker = new libraries.AdvancedMarkerElement({
                    anchorLeft: "-50%",
                    anchorTop: "-50%",
                    content: buildWishlistMarkerContent(markerColor, isSelected),
                    gmpClickable: true,
                    map: map.current,
                    position: {lat: item.latitude, lng: item.longitude},
                    title: item.name,
                    zIndex: isSelected ? 150 : -100,
                });
                const clickListener = marker.addListener("click", () => onSelectWishlistMarker?.(item));

                wishlistMarkersMap.current.set(item.wishlistItemId, {marker, clickListener});
            });
        }, [isMapReady, onSelectWishlistMarker, selectedWishlistItemId, wishlistItems]);

        useEffect(() => {
            const libraries = googleLibraries.current;
            if (!isMapReady || !map.current || !libraries) return;

            clearMarkerRecords(nearbyMarkersMap.current);

            nearbyPlaces.forEach((place) => {
                if (!isFiniteCoordinate(place.latitude, place.longitude)) return;

                const markerColor = getPlaceCategoryColor(place.placeTypeSummary, place.normalizedCategoryKey);
                const isSelected = place.externalPlaceId === selectedNearbyPlaceExternalId;
                const iconKey = getNearbyMarkerIconKey(place);
                const marker = new libraries.AdvancedMarkerElement({
                    anchorLeft: getNearbyMarkerAnchorLeft(isSelected),
                    anchorTop: "-50%",
                    content: buildNearbyMarkerContent(place.placeName, markerColor, isSelected, iconKey),
                    gmpClickable: true,
                    map: map.current,
                    position: {lat: place.latitude, lng: place.longitude},
                    title: place.placeName,
                    zIndex: isSelected ? 125 : -50,
                });
                const clickListener = marker.addListener("click", () => onSelectNearbyPlace?.(place));

                nearbyMarkersMap.current.set(place.externalPlaceId, {marker, clickListener});
            });
        }, [isMapReady, nearbyPlaces, onSelectNearbyPlace, selectedNearbyPlaceExternalId]);

        return (
            <div className="relative w-full h-full rounded-lg overflow-hidden shadow-soft z-0">
                <div ref={mapContainer} className="absolute inset-0"/>
                {selectedGooglePoi && (
                    <div
                        className="absolute z-30 flex w-[292px] max-w-[calc(100%-2rem)] items-center gap-1 rounded-full border border-slate-200 bg-white/95 p-1.5 text-slate-950 shadow-[0_14px_34px_-24px_rgba(15,23,42,0.5)] backdrop-blur"
                        style={{
                            left: selectedGooglePoi.anchor.x,
                            top: selectedGooglePoi.anchor.y,
                            transform: "translateX(-50%)",
                        }}
                        onPointerDown={(event) => event.stopPropagation()}
                        onClick={(event) => event.stopPropagation()}
                    >
                        <Button
                            type="button"
                            size="sm"
                            className="h-9 flex-1 rounded-full bg-primary px-3 text-sm font-semibold text-primary-foreground shadow-none transition-all duration-300 hover:-translate-y-0.5 hover:bg-primary/90"
                            onClick={() => onAddGooglePoiToItinerary?.(toGooglePoiSelection(selectedGooglePoi))}
                            disabled={!onAddGooglePoiToItinerary || isAddingGooglePoiToItinerary || isAddingGooglePoiToWishlist}
                        >
                            <CalendarPlus className="h-4 w-4" />
                            일정
                        </Button>
                        <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            className="h-9 flex-1 rounded-full border-slate-950 bg-white px-3 text-sm font-semibold text-slate-950 shadow-none hover:bg-slate-50 hover:text-slate-950"
                            onClick={() => onAddGooglePoiToWishlist?.(toGooglePoiSelection(selectedGooglePoi))}
                            disabled={!onAddGooglePoiToWishlist || isAddingGooglePoiToWishlist || isAddingGooglePoiToItinerary}
                        >
                            <Heart className="h-4 w-4" />
                            위시
                        </Button>
                        <Button
                            type="button"
                            size="icon"
                            variant="ghost"
                            className="h-9 w-9 shrink-0 rounded-full text-slate-500 hover:bg-transparent hover:text-slate-950"
                            onClick={() => onOpenGooglePoiInGoogleMaps?.(toGooglePoiSelection(selectedGooglePoi))}
                            disabled={!onOpenGooglePoiInGoogleMaps}
                            aria-label="Google 지도에서 보기"
                        >
                            <ExternalLink className="h-4 w-4" />
                        </Button>
                        <Button
                            type="button"
                            size="icon"
                            variant="ghost"
                            className="h-9 w-9 shrink-0 rounded-full text-slate-400 hover:bg-transparent hover:text-slate-950"
                            onClick={() => setSelectedGooglePoi(null)}
                            aria-label="장소 액션 닫기"
                        >
                            <X className="h-4 w-4" />
                        </Button>
                    </div>
                )}
                {loadError && (
                    <div className="absolute inset-0 flex items-center justify-center bg-slate-50 px-4 text-center text-sm text-slate-600">
                        {loadError}
                    </div>
                )}
            </div>
        );
    },
);
