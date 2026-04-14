import {type ClassValue, clsx} from "clsx"
import {twMerge} from "tailwind-merge"
import { getCountryEmojiByValue } from "@/lib/tripRegions";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function getCountryEmoji(country: string): string {
  return getCountryEmojiByValue(country);
}

export function getCountryTimezone(country: string): string {
  const timezoneMap: Record<string, string> = {
    'KR': 'Asia/Seoul',
    'JP': 'Asia/Tokyo',
    'CN': 'Asia/Shanghai',
    'TH': 'Asia/Bangkok',
    'VN': 'Asia/Ho_Chi_Minh',
    'PH': 'Asia/Manila',
    'SG': 'Asia/Singapore',
    'MY': 'Asia/Kuala_Lumpur',
    'ID': 'Asia/Jakarta',
    'IN': 'Asia/Kolkata',
    'AE': 'Asia/Dubai',
    'TR': 'Europe/Istanbul',
    'EG': 'Africa/Cairo',
    'IT': 'Europe/Rome',
    'FR': 'Europe/Paris',
    'ES': 'Europe/Madrid',
    'GB': 'Europe/London',
    'DE': 'Europe/Berlin',
    'CH': 'Europe/Zurich',
    'NL': 'Europe/Amsterdam',
    'GR': 'Europe/Athens',
    'US': 'America/New_York',
    'CA': 'America/Toronto',
    'AU': 'Australia/Sydney',
    'NZ': 'Pacific/Auckland',
    'BR': 'America/Sao_Paulo',
    'AR': 'America/Buenos_Aires',
    'MX': 'America/Mexico_City',
    'ZA': 'Africa/Johannesburg',
    'MA': 'Africa/Casablanca',
  };
  return timezoneMap[country] || 'UTC';
}
