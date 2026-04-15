export type PlaceItineraryCreateData = {
  placeId: number | null;
  title: null;
  time: null;
  memo: null;
};

export type MinimalCreatedItinerary = {
  itineraryId: number;
  placeId?: number | null;
  visitDay: number;
  location?: {
    lat: number;
    lng: number;
  } | null;
};

export type SelectedPlacePanelState = {
  mode: "itinerary" | "wishlist";
  placeId: number;
  itineraryId?: number;
  wishlistItemId?: number;
  visitDay?: number | null;
  markerLatLng: { lat: number; lng: number };
};

export const buildPlaceItineraryCreateData = (placeId?: number | null): PlaceItineraryCreateData => ({
  placeId: placeId ?? null,
  title: null,
  time: null,
  memo: null,
});

export const toItineraryPanelSelection = (
  item: MinimalCreatedItinerary | null | undefined,
): SelectedPlacePanelState | null => {
  if (!item?.placeId || !item.location) {
    return null;
  }

  return {
    mode: "itinerary",
    placeId: item.placeId,
    itineraryId: item.itineraryId,
    visitDay: item.visitDay,
    markerLatLng: {
      lat: item.location.lat,
      lng: item.location.lng,
    },
  };
};

export const toWishlistPanelSelection = (item: {
  placeId: number;
  wishlistItemId: number;
  latitude: number;
  longitude: number;
}): SelectedPlacePanelState => ({
  mode: "wishlist",
  placeId: item.placeId,
  wishlistItemId: item.wishlistItemId,
  markerLatLng: {
    lat: item.latitude,
    lng: item.longitude,
  },
});

export const transitionWishlistSelectionAfterCreate = ({
  currentSelection,
  wishlistItemId,
  createdItem,
}: {
  currentSelection: SelectedPlacePanelState | null;
  wishlistItemId: number;
  createdItem: MinimalCreatedItinerary;
}): SelectedPlacePanelState | null => {
  const isMatchingWishlistSelection =
    currentSelection?.mode === "wishlist" &&
    currentSelection.wishlistItemId === wishlistItemId;

  if (!isMatchingWishlistSelection) {
    return currentSelection;
  }

  return toItineraryPanelSelection(createdItem);
};

export const runCreatePlaceItineraryFlow = async <T>(params: {
  visitDay: number;
  placeId?: number | null;
  create: (visitDay: number, data: PlaceItineraryCreateData) => Promise<T>;
  afterCreate?: (created: T) => Promise<void> | void;
  onSuccess: (created: T) => void;
  onError: (error: unknown) => void;
}): Promise<T | null> => {
  try {
    const created = await params.create(
      params.visitDay,
      buildPlaceItineraryCreateData(params.placeId),
    );
    await params.afterCreate?.(created);
    params.onSuccess(created);
    return created;
  } catch (error) {
    params.onError(error);
    return null;
  }
};
