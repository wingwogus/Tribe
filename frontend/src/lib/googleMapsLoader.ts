import {importLibrary, setOptions} from "@googlemaps/js-api-loader";

export interface GoogleMapsConfig {
  apiKey: string;
  mapId: string;
}

export interface GoogleMapsLibraries {
  Map: typeof google.maps.Map;
  OverlayView: typeof google.maps.OverlayView;
  Polyline: typeof google.maps.Polyline;
  AdvancedMarkerElement: typeof google.maps.marker.AdvancedMarkerElement;
}

export class GoogleMapsConfigurationError extends Error {
  constructor(message = "Google Maps configuration is missing.") {
    super(message);
    this.name = "GoogleMapsConfigurationError";
  }
}

let hasConfiguredLoader = false;
let librariesPromise: Promise<GoogleMapsLibraries> | null = null;

export const getGoogleMapsConfig = (): GoogleMapsConfig | null => {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY?.trim();
  const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID?.trim();

  if (!apiKey || !mapId) {
    return null;
  }

  return {apiKey, mapId};
};

export const loadGoogleMapsLibraries = (): Promise<GoogleMapsLibraries> => {
  if (librariesPromise) {
    return librariesPromise;
  }

  const config = getGoogleMapsConfig();
  if (!config) {
    return Promise.reject(new GoogleMapsConfigurationError());
  }

  if (!hasConfiguredLoader) {
    setOptions({
      key: config.apiKey,
      mapIds: [config.mapId],
      v: "quarterly",
    });
    hasConfiguredLoader = true;
  }

  librariesPromise = Promise.all([
    importLibrary("maps"),
    importLibrary("marker"),
  ]).then(([mapsLibrary, markerLibrary]) => ({
    Map: mapsLibrary.Map,
    OverlayView: mapsLibrary.OverlayView,
    Polyline: mapsLibrary.Polyline,
    AdvancedMarkerElement: markerLibrary.AdvancedMarkerElement,
  }));

  return librariesPromise;
};
