// Supported currencies
export const SUPPORTED_CURRENCIES = [
  { code: 'KRW', name: '원', symbol: '₩', flag: '🇰🇷' },
  { code: 'JPY', name: '엔', symbol: '¥', flag: '🇯🇵' },
  { code: 'USD', name: '달러', symbol: '$', flag: '🇺🇸' },
  { code: 'EUR', name: '유로', symbol: '€', flag: '🇪🇺' },
  { code: 'GBP', name: '파운드', symbol: '£', flag: '🇬🇧' },
  { code: 'CNH', name: '위안', symbol: '¥', flag: '🇨🇳' },
  { code: 'AUD', name: '호주 달러', symbol: '$', flag: '🇦🇺' },
  { code: 'CAD', name: '캐나다 달러', symbol: '$', flag: '🇨🇦' },
  { code: 'CHF', name: '스위스 프랑', symbol: 'Fr', flag: '🇨🇭' },
  { code: 'HKD', name: '홍콩 달러', symbol: '$', flag: '🇭🇰' },
  { code: 'SGD', name: '싱가포르 달러', symbol: '$', flag: '🇸🇬' },
  { code: 'THB', name: '바트', symbol: '฿', flag: '🇹🇭' },
  { code: 'AED', name: '디르함', symbol: 'د.إ', flag: '🇦🇪' },
  { code: 'BHD', name: '디나르', symbol: '.د.ب', flag: '🇧🇭' },
  { code: 'BND', name: '브루나이 달러', symbol: '$', flag: '🇧🇳' },
  { code: 'DKK', name: '덴마크 크로네', symbol: 'kr', flag: '🇩🇰' },
  { code: 'IDR', name: '루피아', symbol: 'Rp', flag: '🇮🇩' },
  { code: 'KWD', name: '쿠웨이트 디나르', symbol: 'د.ك', flag: '🇰🇼' },
  { code: 'MYR', name: '링깃', symbol: 'RM', flag: '🇲🇾' },
  { code: 'NOK', name: '노르웨이 크로네', symbol: 'kr', flag: '🇳🇴' },
  { code: 'NZD', name: '뉴질랜드 달러', symbol: '$', flag: '🇳🇿' },
  { code: 'SAR', name: '리얄', symbol: 'ر.س', flag: '🇸🇦' },
  { code: 'SEK', name: '스웨덴 크로나', symbol: 'kr', flag: '🇸🇪' },
];

// Get currency info by code
export const getCurrencyInfo = (code: string) => {
  return SUPPORTED_CURRENCIES.find(c => c.code === code) || SUPPORTED_CURRENCIES[0];
};

// Format amount with currency symbol
export const formatCurrency = (amount: number, currencyCode: string): string => {
  const currency = getCurrencyInfo(currencyCode);
  return `${currency.symbol}${amount.toLocaleString()}`;
};

// Format with currency code and flag
export const formatCurrencyWithCode = (amount: number, currencyCode: string): string => {
  const currency = getCurrencyInfo(currencyCode);
  return `${currency.flag} ${formatCurrency(amount, currencyCode)} ${currencyCode}`;
};

// Get default currency by country name
export const getDefaultCurrencyByCountry = (country: string): string => {
  const countryToCurrency: Record<string, string> = {
    '일본': 'JPY',
    '미국': 'USD',
    '영국': 'GBP',
    '프랑스': 'EUR',
    '독일': 'EUR',
    '이탈리아': 'EUR',
    '스페인': 'EUR',
    '중국': 'CNH',
    '홍콩': 'HKD',
    '싱가포르': 'SGD',
    '태국': 'THB',
    '호주': 'AUD',
    '캐나다': 'CAD',
    '스위스': 'CHF',
    'UAE': 'AED',
    '아랍에미리트': 'AED',
    '바레인': 'BHD',
    '브루나이': 'BND',
    '덴마크': 'DKK',
    '인도네시아': 'IDR',
    '쿠웨이트': 'KWD',
    '말레이시아': 'MYR',
    '노르웨이': 'NOK',
    '뉴질랜드': 'NZD',
    '사우디아라비아': 'SAR',
    '사우디': 'SAR',
    '스웨덴': 'SEK',
  };
  
  return countryToCurrency[country] || 'KRW';
};
