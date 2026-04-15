import test from "node:test";
import assert from "node:assert/strict";

import {
  buildPlaceItineraryCreateData,
  runCreatePlaceItineraryFlow,
  toItineraryPanelSelection,
  toWishlistPanelSelection,
  transitionWishlistSelectionAfterCreate,
} from "./itineraryCreateFlow.ts";

test("buildPlaceItineraryCreateData normalizes place itinerary payload", () => {
  assert.deepEqual(buildPlaceItineraryCreateData(12), {
    placeId: 12,
    title: null,
    time: null,
    memo: null,
  });

  assert.deepEqual(buildPlaceItineraryCreateData(undefined), {
    placeId: null,
    title: null,
    time: null,
    memo: null,
  });
});

test("toItineraryPanelSelection returns null when place selection is incomplete", () => {
  assert.equal(toItineraryPanelSelection(null), null);
  assert.equal(
    toItineraryPanelSelection({
      itineraryId: 1,
      placeId: 2,
      visitDay: 1,
      location: null,
    }),
    null,
  );
});

test("transitionWishlistSelectionAfterCreate promotes matching wishlist selection to itinerary", () => {
  const result = transitionWishlistSelectionAfterCreate({
    currentSelection: toWishlistPanelSelection({
      placeId: 5,
      wishlistItemId: 7,
      latitude: 37.5,
      longitude: 127.0,
    }),
    wishlistItemId: 7,
    createdItem: {
      itineraryId: 99,
      placeId: 5,
      visitDay: 2,
      location: {
        lat: 37.5,
        lng: 127.0,
      },
    },
  });

  assert.deepEqual(result, {
    mode: "itinerary",
    placeId: 5,
    itineraryId: 99,
    visitDay: 2,
    markerLatLng: {
      lat: 37.5,
      lng: 127.0,
    },
  });
});

test("runCreatePlaceItineraryFlow calls shared success path once", async () => {
  const calls: string[] = [];

  const result = await runCreatePlaceItineraryFlow({
    visitDay: 3,
    placeId: 42,
    create: async (visitDay, data) => {
      calls.push(`create:${visitDay}:${data.placeId}`);
      return {
        itineraryId: 11,
        placeId: 42,
        visitDay,
      };
    },
    afterCreate: async () => {
      calls.push("after");
    },
    onSuccess: () => {
      calls.push("success");
    },
    onError: () => {
      calls.push("error");
    },
  });

  assert.deepEqual(calls, ["create:3:42", "after", "success"]);
  assert.deepEqual(result, {
    itineraryId: 11,
    placeId: 42,
    visitDay: 3,
  });
});

test("runCreatePlaceItineraryFlow calls shared error path once", async () => {
  const calls: string[] = [];

  const result = await runCreatePlaceItineraryFlow({
    visitDay: 1,
    placeId: 9,
    create: async () => {
      throw new Error("boom");
    },
    onSuccess: () => {
      calls.push("success");
    },
    onError: () => {
      calls.push("error");
    },
  });

  assert.equal(result, null);
  assert.deepEqual(calls, ["error"]);
});
