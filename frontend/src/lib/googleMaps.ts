/**
 * Google Maps URL generation utilities
 */

/**
 * Generate Google Maps directions URL using place names
 * @param originName Origin place name
 * @param destinationName Destination place name
 * @param travelMode Travel mode (walking, driving, transit)
 */
export const getGoogleMapsDirectionsUrl = (
  originName: string,
  destinationName: string,
  travelMode: 'walking' | 'driving' | 'transit' = 'driving'
): string => {
  const baseUrl = 'https://www.google.com/maps/dir/';
  const params = new URLSearchParams({
    api: '1',
    origin: originName,
    destination: destinationName,
    travelmode: travelMode,
  });
  
  return `${baseUrl}?${params.toString()}`;
};

/**
 * Generate Google Maps search URL for a place
 * @param placeName Place name
 */
export const getGoogleMapsSearchUrl = (placeName: string): string => {
  const baseUrl = 'https://www.google.com/maps/search/';
  const params = new URLSearchParams({
    api: '1',
    query: placeName,
  });
  
  return `${baseUrl}?${params.toString()}`;
};

/**
 * Open Google Maps link in a new tab
 */
export const openGoogleMaps = (url: string) => {
  window.open(url, '_blank', 'noopener,noreferrer');
};
