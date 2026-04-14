import {forwardRef, useEffect, useImperativeHandle, useRef, useState} from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import {CategoryResponse} from '@/api/categories';
import {WishlistItem} from '@/api/wishlist';
import {getCountryCoordinates} from '@/lib/countryCoordinates';
import {getTripRegionCenter} from '@/lib/tripRegions';

export interface ItineraryMapHandle {
    flyToMarker: (itineraryId: number) => void;
    flyToWishlistMarker: (wishlistItemId: number) => void;
}

interface ItineraryMapProps {
    categories: CategoryResponse[];
    getCategoryColor: (categoryName: string) => { bg: string; text: string; marker: string };
    wishlistItems?: WishlistItem[];
    tripCountry?: string;
    tripRegionCode?: string | null;
    onAddToItinerary?: (wishlistItem: WishlistItem, categoryId: number) => void;
    onDeleteItinerary?: (itineraryId: number, categoryId: number) => void;
    onDeleteWishlist?: (wishlistItemId: number) => void;
}

export const ItineraryMap = forwardRef<ItineraryMapHandle, ItineraryMapProps>(
    ({categories, getCategoryColor, wishlistItems = [], tripCountry, tripRegionCode, onAddToItinerary, onDeleteItinerary, onDeleteWishlist}, ref) => {
        const mapContainer = useRef<HTMLDivElement>(null);
        const map = useRef<L.Map | null>(null);
        const markersMap = useRef<Map<number, L.Marker>>(new Map());
        const wishlistMarkersMap = useRef<Map<number, L.Marker>>(new Map());
        const polyline = useRef<L.Polyline | null>(null);
        const [hasInitialFit, setHasInitialFit] = useState(false);

        // Expose flyToMarker method to parent component
        useImperativeHandle(ref, () => ({
            flyToMarker: (itineraryId: number) => {
                const marker = markersMap.current.get(itineraryId);
                if (marker && map.current) {
                    const latLng = marker.getLatLng();
                    map.current.flyTo(latLng, 16, {
                        duration: 1.5,
                        easeLinearity: 0.5,
                    });
                    marker.openPopup();
                }
            },
            flyToWishlistMarker: (wishlistItemId: number) => {
                const marker = wishlistMarkersMap.current.get(wishlistItemId);
                if (marker && map.current) {
                    const latLng = marker.getLatLng();
                    map.current.flyTo(latLng, 16, {
                        duration: 1.5,
                        easeLinearity: 0.5,
                    });
                    marker.openPopup();
                }
            },
        }));

        // Sort categories by order, then flatten itineraries with category info
        const validItineraries = categories
            .sort((a, b) => a.order - b.order)
            .flatMap(category =>
                (category.itineraryItems || [])
                    .sort((a, b) => a.order - b.order)
                    .map(item => ({...item, categoryName: category.name}))
            )
            .filter(item => item.location && item.location.lat && item.location.lng);

    // Effect for map initialization and destruction
    useEffect(() => {
        if (mapContainer.current && !map.current) {
            // ISO 국가 코드 기반으로 초기 중심 설정
            const defaultCenter = tripRegionCode
                ? getTripRegionCenter(tripRegionCode) ?? (tripCountry ? getCountryCoordinates(tripCountry) : [37.5665, 126.9780] as [number, number])
                : tripCountry 
                ? getCountryCoordinates(tripCountry)
                : [37.5665, 126.9780] as [number, number];

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

        // Effect for updating markers based on itineraries
        useEffect(() => {
            if (!map.current) return;

            // Clear existing markers and polyline
            markersMap.current.forEach(marker => marker.remove());
            markersMap.current.clear();
            if (polyline.current) {
                polyline.current.remove();
                polyline.current = null;
            }

            if (validItineraries.length > 0) {
                const latLngs: L.LatLngExpression[] = [];

                validItineraries.forEach((item, index) => {
                    if (!item.location) return;

                    const latLng: L.LatLngExpression = [item.location.lat, item.location.lng];
                    latLngs.push(latLng);

                    // Get category color
                    const categoryColor = getCategoryColor(item.categoryName);

                    // Create custom icon with number and category color
                    const icon = L.divIcon({
                        className: 'custom-marker',
                        html: `
                          <div style="
                            width: 24px;
                            height: 24px;
                            border-radius: 50%;
                            background-color: ${categoryColor.marker};
                            border: 3px solid white;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            color: white;
                            font-weight: bold;
                            font-size: 12px;
                          ">
                            ${index + 1}
                          </div>
                        `,
                        iconSize: [24, 24],
                        iconAnchor: [12, 12],
                    });

                    const marker = L.marker(latLng, {icon})
                        .bindPopup(`
                            <div style="padding: 10px; min-width: 200px;">
                                            <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 8px;">
                                                <span style="font-size: 20px;">${index + 1}</span>
                                                <h3 style="font-weight: bold; font-size: 15px; margin: 0; flex: 1;">
                                                    ${item.name}
                                                </h3>
                                                ${item.time
                            ? `<p style="font-size: 12px; color: #666; margin: 0;">
                                        ${new Date(item.time).toLocaleTimeString('ko-KR', {
                                hour: '2-digit',
                                minute: '2-digit'
                            })}</p>` : ''}
                                                <button 
                                                    class="itinerary-delete-btn" 
                                                    data-itinerary-id="${item.itineraryId}"
                                                    data-category-id="${item.categoryId}"
                                                    style="
                                                        background: none;
                                                        border: none;
                                                        cursor: pointer;
                                                        padding: 4px;
                                                        display: flex;
                                                        align-items: center;
                                                        justify-content: center;
                                                        border-radius: 4px;
                                                        transition: all 0.2s;
                                                        color: #6b7280;
                                                    "
                                                    onmouseover="this.style.backgroundColor='#fee2e2'; this.style.color='#ef4444'"
                                                    onmouseout="this.style.backgroundColor='transparent'; this.style.color='#6b7280'"
                                                    title="삭제"
                                                >
                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                        <path d="M3 6h18"></path>
                                                        <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path>
                                                        <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path>
                                                        <line x1="10" y1="11" x2="10" y2="17"></line>
                                                        <line x1="14" y1="11" x2="14" y2="17"></line>
                                                    </svg>
                                                </button>
                                            </div>
                                            ${item.location.address ? `
                                                <p style="font-size: 12px; color: #666; margin-bottom: 8px;">
                                                    ${item.location.address}
                                                </p>
                                            ` : ''}
                            ${item.memo ? `<p style="font-size: 12px; color: #888; margin-bottom: 0;">${item.memo}</p>` : ''}
                                        </div>
                            
                        `)
                        .addTo(map.current!);

                    // 일정 마커 팝업 열릴 때 삭제 버튼 이벤트 추가
                    marker.on('popupopen', (e) => {
                        const popup = e.popup.getElement();
                        if (popup) {
                            const handleClick = (event: Event) => {
                                const target = event.target as HTMLElement;
                                const deleteBtn = target.closest('.itinerary-delete-btn') as HTMLElement;
                                
                                if (deleteBtn) {
                                    const itineraryId = parseInt(deleteBtn.getAttribute('data-itinerary-id')!);
                                    const categoryId = parseInt(deleteBtn.getAttribute('data-category-id')!);
                                    
                                    if (onDeleteItinerary) {
                                        onDeleteItinerary(itineraryId, categoryId);
                                    }
                                }
                            };
                            
                            popup.removeEventListener('click', handleClick);
                            popup.addEventListener('click', handleClick);
                        }
                    });

                    markersMap.current.set(item.itineraryId, marker);
                });

                // Draw lines between markers with gradient-like effect (using default color)
                if (latLngs.length > 1) {
                    polyline.current = L.polyline(latLngs, {
                        color: '#94A3B8',
                        weight: 3,
                        opacity: 0.5,
                        dashArray: '5, 10',
                    }).addTo(map.current!);

                    // Fit map to show all markers only on initial load
                    if (!hasInitialFit) {
                        map.current!.fitBounds(L.latLngBounds(latLngs), {padding: [50, 50]});
                        setHasInitialFit(true);
                    }
                } else if (latLngs.length === 1 && !hasInitialFit) {
                    map.current!.setView(latLngs[0], 13);
                    setHasInitialFit(true);
                }
            }
        }, [validItineraries, getCategoryColor]);

        // Effect for wishlist markers
        useEffect(() => {
            if (!map.current) return;

            // Clear existing wishlist markers
            wishlistMarkersMap.current.forEach(marker => marker.remove());
            wishlistMarkersMap.current.clear();

            if (wishlistItems && wishlistItems.length > 0) {
                wishlistItems.forEach((item) => {
                    const latLng: L.LatLngExpression = [item.latitude, item.longitude];

                    // Create wishlist star icon
                    const wishlistIcon = L.divIcon({
                        className: 'custom-wishlist-marker',
                        html: `
                            <svg width="24" height="24" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" class="text-primary">
                              <circle cx="50" cy="50" r="42" fill="currentColor" stroke="white" stroke-width="8"/>
                              <path d="M50 26 L58.96 41.68 L76.8 44.72 L64.64 58.04 L66.48 76.4 L50 69.52 L33.52 76.4 L35.36 58.04 L23.2 44.72 L41.04 41.68 Z" fill="#ffffff" />
                            </svg>
                    `,
                        iconSize: [24, 24],
                        iconAnchor: [12, 12],
                    });

                    // Create category dropdown HTML for popup
                    const categoriesHtml = `
                        <select 
                            class="wishlist-category-select" 
                            data-wishlist-id="${item.wishlistItemId}"
                            style="
                                width: 100%;
                                padding: 8px 12px;
                                border: 1px solid #e5e7eb;
                                border-radius: 6px;
                                font-size: 13px;
                                background-color: white;
                                cursor: pointer;
                                outline: none;
                            "
                            onfocus="this.style.borderColor='#3b82f6'"
                            onblur="this.style.borderColor='#e5e7eb'"
                        >
                            <option value="" selected disabled>일정을 선택하세요</option>
                            ${categories.map(cat => `
                                <option value="${cat.categoryId}">
                                    ${cat.name} (Day ${cat.day})
                                </option>
                            `).join('')}
                        </select>
                    `;

                    const wishlistMarker = L.marker(latLng, {
                        icon: wishlistIcon,
                        zIndexOffset: -100
                    })
                        .bindPopup(`
                        <div style="padding: 10px; min-width: 200px;">
                            <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 8px;">
                                <span style="font-size: 20px;">⭐</span>
                                <h3 style="font-weight: bold; font-size: 15px; margin: 0; color: #F59E0B; flex: 1;">
                                    ${item.name}
                                </h3>
                                <button 
                                    class="wishlist-delete-btn" 
                                    data-wishlist-id="${item.wishlistItemId}"
                                    style="
                                        background: none;
                                        border: none;
                                        cursor: pointer;
                                        padding: 4px;
                                        display: flex;
                                        align-items: center;
                                        justify-content: center;
                                        border-radius: 4px;
                                        transition: all 0.2s;
                                        color: #6b7280;
                                    "
                                    onmouseover="this.style.backgroundColor='#fee2e2'; this.style.color='#ef4444'"
                                    onmouseout="this.style.backgroundColor='transparent'; this.style.color='#6b7280'"
                                    title="삭제"
                                >
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M3 6h18"></path>
                                        <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path>
                                        <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path>
                                        <line x1="10" y1="11" x2="10" y2="17"></line>
                                        <line x1="14" y1="11" x2="14" y2="17"></line>
                                    </svg>
                                </button>
                            </div>
                            ${item.address ? `
                                <p style="font-size: 12px; color: #666; margin-bottom: 8px;">
                                    ${item.address}
                                </p>
                            ` : ''}
                            <p style="font-size: 11px; color: #888; margin-bottom: 12px;">
                                추가한 사람: ${item.adder.nickname}
                            </p>
                            <div style="border-top: 1px solid #e5e7eb; padding-top: 10px;">
                                <p style="font-size: 12px; font-weight: 600; margin-bottom: 6px; color: #374151;">
                                    일정에 추가:
                                </p>
                                <div style="max-height: 200px; overflow-y: auto;">
                                    ${categoriesHtml}
                                </div>
                            </div>
                        </div>
                    `, {
                            maxWidth: 300,
                            className: 'wishlist-popup'
                        })
                    .addTo(map.current!);

                    // 팝업이 열릴 때마다 이벤트 위임으로 리스너 추가
                    wishlistMarker.on('popupopen', (e) => {
                        const popup = e.popup.getElement();
                        if (popup) {
                            const handleEvent = (event: Event) => {
                                const target = event.target as HTMLElement;
                                
                                // 일정 선택 드롭다운
                                const selectElement = target as HTMLSelectElement;
                                if (selectElement.classList.contains('wishlist-category-select') && selectElement.value) {
                                    const wishlistId = parseInt(selectElement.getAttribute('data-wishlist-id')!);
                                    const categoryId = parseInt(selectElement.value);
                                    const wishlistItem = wishlistItems.find(item => item.wishlistItemId === wishlistId);

                                    if (wishlistItem && onAddToItinerary) {
                                        onAddToItinerary(wishlistItem, categoryId);
                                        // 추가 후 드롭다운 초기화
                                        selectElement.value = '';
                                    }
                                }
                                
                                // 위시리스트 삭제 버튼
                                const deleteBtn = target.closest('.wishlist-delete-btn') as HTMLElement;
                                if (deleteBtn) {
                                    const wishlistId = parseInt(deleteBtn.getAttribute('data-wishlist-id')!);
                                    
                                    if (onDeleteWishlist) {
                                        onDeleteWishlist(wishlistId);
                                    }
                                }
                            };
                            
                            // 기존 리스너 제거 후 추가 (중복 방지)
                            popup.removeEventListener('click', handleEvent);
                            popup.removeEventListener('change', handleEvent);
                            popup.addEventListener('click', handleEvent);
                            popup.addEventListener('change', handleEvent);
                        }
                    });

                wishlistMarkersMap.current.set(item.wishlistItemId, wishlistMarker);
            });
            }
        }, [wishlistItems, categories, onAddToItinerary]);

        return (
            <div className="relative w-full h-full rounded-lg overflow-hidden shadow-soft z-0">
                <div ref={mapContainer} className="absolute inset-0"/>
            </div>
        );
    }
);
