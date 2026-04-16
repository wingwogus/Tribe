import { getCountryCoordinatesByCode2 } from "@/lib/tripRegions";

export const getCountryCoordinates = (countryCode: string): [number, number] =>
  getCountryCoordinatesByCode2(countryCode);
