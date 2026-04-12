import {type ClassValue, clsx} from "clsx"
import {twMerge} from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function getCountryEmoji(country: string): string {
  const emojiMap: Record<string, string> = {
    '대한민국': '🇰🇷',
    '일본': '🇯🇵',
    '중국': '🇨🇳',
    '태국': '🇹🇭',
    '베트남': '🇻🇳',
    '필리핀': '🇵🇭',
    '싱가포르': '🇸🇬',
    '말레이시아': '🇲🇾',
    '인도네시아': '🇮🇩',
    '인도': '🇮🇳',
    '아랍에미리트': '🇦🇪',
    '터키': '🇹🇷',
    '이집트': '🇪🇬',
    '이탈리아': '🇮🇹',
    '프랑스': '🇫🇷',
    '스페인': '🇪🇸',
    '영국': '🇬🇧',
    '독일': '🇩🇪',
    '스위스': '🇨🇭',
    '네덜란드': '🇳🇱',
    '그리스': '🇬🇷',
    '미국': '🇺🇸',
    '캐나다': '🇨🇦',
    '호주': '🇦🇺',
    '뉴질랜드': '🇳🇿',
    '브라질': '🇧🇷',
    '아르헨티나': '🇦🇷',
    '멕시코': '🇲🇽',
    '남아프리카 공화국': '🇿🇦',
    '모로코': '🇲🇦',
  };
  return emojiMap[country] || '✈️';
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
