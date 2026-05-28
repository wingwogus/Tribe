import test from "node:test";
import assert from "node:assert/strict";

import { getTripDateStringForDay } from "./tripDates.ts";

test("getTripDateStringForDay derives trip dates by day number", () => {
  assert.equal(getTripDateStringForDay("2026-04-30", 1), "2026-04-30");
  assert.equal(getTripDateStringForDay("2026-04-30", 2), "2026-05-01");
  assert.equal(getTripDateStringForDay("2026-04-30", 3), "2026-05-02");
});

test("getTripDateStringForDay accepts datetime-like trip start values", () => {
  assert.equal(getTripDateStringForDay("2026-04-30T15:00:00Z", 2), "2026-05-01");
});
