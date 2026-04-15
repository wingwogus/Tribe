import {forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState} from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import {ItineraryResponse} from '@/api/itinerary';
import {WishlistItem} from '@/api/wishlist';
import {getCountryCoordinates} from '@/lib/countryCoordinates';
import {getTripRegionCenter} from '@/lib/tripRegions';
import {getPlaceCategoryColor} from '@/lib/placePresentation';

export interface ItineraryMapHandle {
    focusItineraryMarker: (itineraryId: number, options?: { offsetForPanel?: boolean }) => void;
    focusWishlistMarker: (wishlistItemId: number, options?: { offsetForPanel?: boolean }) => void;
}

interface ItineraryMapProps {
    items?: ItineraryResponse[];
    days?: number[];
    wishlistItems?: WishlistItem[];
    tripCountry?: string;
    tripRegionCode?: string | null;
    selectedItineraryId?: number | null;
    selectedWishlistItemId?: number | null;
    panelOffsetPx?: number;
    onSelectItineraryMarker?: (item: ItineraryResponse) => void;
    onSelectWishlistMarker?: (item: WishlistItem) => void;
}

const buildItineraryIcon = (index: number, color: string, isSelected: boolean) => {
    const size = isSelected ? 32 : 24;
    const borderWidth = isSelected ? 4 : 3;
    const fontSize = isSelected ? 13 : 12;
    const glow = isSelected ? "0 0 0 6px rgba(255,255,255,0.8), 0 12px 28px rgba(15,23,42,0.25)" : "0 2px 8px rgba(0,0,0,0.3)";

    return L.divIcon({
        className: 'custom-marker',
        html: `
          <div style="
            width: ${size}px;
            height: ${size}px;
            border-radius: 999px;
            background-color: ${color};
            border: ${borderWidth}px solid white;
            box-shadow: ${glow};
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: 700;
            font-size: ${fontSize}px;
            transform: translateZ(0);
          ">
            ${index + 1}
          </div>
        `,
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
    });
};

const buildWishlistIcon = (color: string, isSelected: boolean) => {
    const ring = isSelected ? 'rgba(255,255,255,0.95)' : 'white';
    const shadow = isSelected ? 'drop-shadow(0 10px 18px rgba(15,23,42,0.28))' : 'drop-shadow(0 4px 10px rgba(15,23,42,0.2))';
    const size = isSelected ? 30 : 24;

    return L.divIcon({
        className: 'custom-wishlist-marker',
        html: `
            <svg width="${size}" height="${size}" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" style="filter:${shadow}">
              <circle cx="50" cy="50" r="42" fill="${color}" stroke="${ring}" stroke-width="8"/>
              <path d="M50 26 L58.96 41.68 L76.8 44.72 L64.64 58.04 L66.48 76.4 L50 69.52 L33.52 76.4 L35.36 58.04 L23.2 44.72 L41.04 41.68 Z" fill="#ffffff" />
            </svg>
        `,
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
    });
};

const focusMarker = (
    map: L.Map,
    marker: L.Marker,
    options: { offsetForPanel?: boolean; panelOffsetPx?: number } = {},
) => {
    const latLng = marker.getLatLng();
    const zoom = Math.max(map.getZoom(), 16);

    if (!options.offsetForPanel) {
        map.flyTo(latLng, zoom, {
            duration: 1.2,
            easeLinearity: 0.5,
        });
        return;
    }

    const offsetPx = Math.max(0, Math.round((options.panelOffsetPx ?? 380) / 2));
    const projected = map.project(latLng, zoom).add([offsetPx, 0]);
    const adjustedCenter = map.unproject(projected, zoom);

    map.flyTo(adjustedCenter, zoom, {
        duration: 1.2,
        easeLinearity: 0.5,
    });
};

export const ItineraryMap = forwardRef<ItineraryMapHandle, ItineraryMapProps>(
    ({
        items = [],
        wishlistItems = [],
        tripCountry,
        tripRegionCode,
        selectedItineraryId = null,
        selectedWishlistItemId = null,
        panelOffsetPx = 400,
        onSelectItineraryMarker,
        onSelectWishlistMarker,
    }, ref) => {
        const mapContainer = useRef<HTMLDivElement>(null);
        const map = useRef<L.Map | null>(null);
        const markersMap = useRef<Map<number, L.Marker>>(new Map());
        const wishlistMarkersMap = useRef<Map<number, L.Marker>>(new Map());
        const polyline = useRef<L.Polyline | null>(null);
        const [hasInitialFit, setHasInitialFit] = useState(false);

        const validItineraries = useMemo(
            () => [...items]
                .sort((a, b) => a.visitDay - b.visitDay || a.itemOrder - b.itemOrder)
                .filter((item) => item.location && item.location.lat && item.location.lng),
            [items],
        );

        useImperativeHandle(ref, () => ({
            focusItineraryMarker: (itineraryId, options) => {
                const marker = markersMap.current.get(itineraryId);
                if (marker && map.current) {
                    focusMarker(map.current, marker, {
                        offsetForPanel: options?.offsetForPanel,
                        panelOffsetPx,
                    });
                }
            },
            focusWishlistMarker: (wishlistItemId, options) => {
                const marker = wishlistMarkersMap.current.get(wishlistItemId);
                if (marker && map.current) {
                    focusMarker(map.current, marker, {
                        offsetForPanel: options?.offsetForPanel,
                        panelOffsetPx,
                    });
                }
            },
        }), [panelOffsetPx]);

        useEffect(() => {
            if (mapContainer.current && !map.current) {
                const defaultCenter = getTripRegionCenter(tripRegionCode)
                    ?? (tripCountry ? getCountryCoordinates(tripCountry) : [37.5665, 126.9780] as [number, number]);

                map.current = L.map(mapContainer.current, {
                    center: defaultCenter,
                    zoom: 10,
                });

                L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{@2x}.png', {
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
                    maxZoom: 20,
                }).addTo(map.current);
            }

            return () => {
                if (map.current) {
                    map.current.remove();
                    map.current = null;
                }
            };
        }, [tripCountry, tripRegionCode]);

        useEffect(() => {
            if (!map.current) return;

            markersMap.current.forEach((marker) => marker.remove());
            markersMap.current.clear();
            if (polyline.current) {
                polyline.current.remove();
                polyline.current = null;
            }

            if (validItineraries.length === 0) {
                return;
            }

            const latLngs: L.LatLngExpression[] = [];

            validItineraries.forEach((item, index) => {
                if (!item.location) return;

                const markerColor = getPlaceCategoryColor(item.placeTypeSummary, item.normalizedCategoryKey);
                const isSelected = item.itineraryId === selectedItineraryId;
                const marker = L.marker([item.location.lat, item.location.lng], {
                    icon: buildItineraryIcon(index, markerColor, isSelected),
                    zIndexOffset: isSelected ? 200 : 0,
                }).addTo(map.current!);

                marker.on('click', () => onSelectItineraryMarker?.(item));
                markersMap.current.set(item.itineraryId, marker);
                latLngs.push([item.location.lat, item.location.lng]);
            });

            if (latLngs.length > 1) {
                polyline.current = L.polyline(latLngs, {
                    color: '#94A3B8',
                    weight: 3,
                    opacity: 0.5,
                    dashArray: '5, 10',
                }).addTo(map.current!);

                if (!hasInitialFit) {
                    map.current.fitBounds(L.latLngBounds(latLngs), {padding: [50, 50]});
                    setHasInitialFit(true);
                }
            } else if (latLngs.length === 1 && !hasInitialFit) {
                map.current.setView(latLngs[0], 13);
                setHasInitialFit(true);
            }
        }, [hasInitialFit, onSelectItineraryMarker, selectedItineraryId, validItineraries]);

        useEffect(() => {
            if (!map.current) return;

            wishlistMarkersMap.current.forEach((marker) => marker.remove());
            wishlistMarkersMap.current.clear();

            wishlistItems.forEach((item) => {
                const markerColor = getPlaceCategoryColor(item.placeTypeSummary, item.normalizedCategoryKey);
                const isSelected = item.wishlistItemId === selectedWishlistItemId;
                const marker = L.marker([item.latitude, item.longitude], {
                    icon: buildWishlistIcon(markerColor, isSelected),
                    zIndexOffset: isSelected ? 150 : -100,
                }).addTo(map.current!);

                marker.on('click', () => onSelectWishlistMarker?.(item));
                wishlistMarkersMap.current.set(item.wishlistItemId, marker);
            });
        }, [onSelectWishlistMarker, selectedWishlistItemId, wishlistItems]);

        return (
            <div className="relative w-full h-full rounded-lg overflow-hidden shadow-soft z-0">
                <div ref={mapContainer} className="absolute inset-0"/>
            </div>
        );
    },
);
