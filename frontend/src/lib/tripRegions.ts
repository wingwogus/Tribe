import { Country } from "@/api/trips";

export interface TripCountryOption {
  code: Country;
  name: string;
  displayName: string;
  code2: string;
  emoji: string;
  defaultCenter: [number, number];
}

export interface TripRegionCatalogEntry {
  code: string;
  countryCode: string;
  label: string;
  type: "CITY" | "METRO" | "ISLAND" | "AREA";
  centerLat: number;
  centerLng: number;
  searchHints: string[];
}

export const TRIP_COUNTRY_OPTIONS: TripCountryOption[] = [
  { code: Country.SOUTH_KOREA, name: "대한민국", displayName: "SOUTH_KOREA", code2: "KR", emoji: "🇰🇷", defaultCenter: [37.5665, 126.978] },
  { code: Country.JAPAN, name: "일본", displayName: "JAPAN", code2: "JP", emoji: "🇯🇵", defaultCenter: [35.6762, 139.6503] },
  { code: Country.CHINA, name: "중국", displayName: "CHINA", code2: "CN", emoji: "🇨🇳", defaultCenter: [39.9042, 116.4074] },
  { code: Country.THAILAND, name: "태국", displayName: "THAILAND", code2: "TH", emoji: "🇹🇭", defaultCenter: [13.7563, 100.5018] },
  { code: Country.VIETNAM, name: "베트남", displayName: "VIETNAM", code2: "VN", emoji: "🇻🇳", defaultCenter: [21.0285, 105.8542] },
  { code: Country.PHILIPPINES, name: "필리핀", displayName: "PHILIPPINES", code2: "PH", emoji: "🇵🇭", defaultCenter: [14.5995, 120.9842] },
  { code: Country.SINGAPORE, name: "싱가포르", displayName: "SINGAPORE", code2: "SG", emoji: "🇸🇬", defaultCenter: [1.3521, 103.8198] },
  { code: Country.MALAYSIA, name: "말레이시아", displayName: "MALAYSIA", code2: "MY", emoji: "🇲🇾", defaultCenter: [3.139, 101.6869] },
  { code: Country.INDONESIA, name: "인도네시아", displayName: "INDONESIA", code2: "ID", emoji: "🇮🇩", defaultCenter: [-6.2088, 106.8456] },
  { code: Country.INDIA, name: "인도", displayName: "INDIA", code2: "IN", emoji: "🇮🇳", defaultCenter: [28.6139, 77.209] },
  { code: Country.UAE, name: "아랍에미리트", displayName: "UAE", code2: "AE", emoji: "🇦🇪", defaultCenter: [25.2048, 55.2708] },
  { code: Country.TURKEY, name: "터키", displayName: "TURKEY", code2: "TR", emoji: "🇹🇷", defaultCenter: [39.9334, 32.8597] },
  { code: Country.EGYPT, name: "이집트", displayName: "EGYPT", code2: "EG", emoji: "🇪🇬", defaultCenter: [30.0444, 31.2357] },
  { code: Country.ITALY, name: "이탈리아", displayName: "ITALY", code2: "IT", emoji: "🇮🇹", defaultCenter: [41.9028, 12.4964] },
  { code: Country.FRANCE, name: "프랑스", displayName: "FRANCE", code2: "FR", emoji: "🇫🇷", defaultCenter: [48.8566, 2.3522] },
  { code: Country.SPAIN, name: "스페인", displayName: "SPAIN", code2: "ES", emoji: "🇪🇸", defaultCenter: [40.4168, -3.7038] },
  { code: Country.UK, name: "영국", displayName: "UK", code2: "GB", emoji: "🇬🇧", defaultCenter: [51.5074, -0.1278] },
  { code: Country.GERMANY, name: "독일", displayName: "GERMANY", code2: "DE", emoji: "🇩🇪", defaultCenter: [52.52, 13.405] },
  { code: Country.SWITZERLAND, name: "스위스", displayName: "SWITZERLAND", code2: "CH", emoji: "🇨🇭", defaultCenter: [46.948, 7.4474] },
  { code: Country.NETHERLANDS, name: "네덜란드", displayName: "NETHERLANDS", code2: "NL", emoji: "🇳🇱", defaultCenter: [52.3676, 4.9041] },
  { code: Country.GREECE, name: "그리스", displayName: "GREECE", code2: "GR", emoji: "🇬🇷", defaultCenter: [37.9838, 23.7275] },
  { code: Country.USA, name: "미국", displayName: "USA", code2: "US", emoji: "🇺🇸", defaultCenter: [38.9072, -77.0369] },
  { code: Country.CANADA, name: "캐나다", displayName: "CANADA", code2: "CA", emoji: "🇨🇦", defaultCenter: [45.4215, -75.6972] },
  { code: Country.AUSTRALIA, name: "호주", displayName: "AUSTRALIA", code2: "AU", emoji: "🇦🇺", defaultCenter: [-35.2809, 149.13] },
  { code: Country.NEW_ZEALAND, name: "뉴질랜드", displayName: "NEW_ZEALAND", code2: "NZ", emoji: "🇳🇿", defaultCenter: [-41.2865, 174.7762] },
  { code: Country.BRAZIL, name: "브라질", displayName: "BRAZIL", code2: "BR", emoji: "🇧🇷", defaultCenter: [-15.8267, -47.9218] },
  { code: Country.ARGENTINA, name: "아르헨티나", displayName: "ARGENTINA", code2: "AR", emoji: "🇦🇷", defaultCenter: [-34.6037, -58.3816] },
  { code: Country.MEXICO, name: "멕시코", displayName: "MEXICO", code2: "MX", emoji: "🇲🇽", defaultCenter: [19.4326, -99.1332] },
  { code: Country.SOUTH_AFRICA, name: "남아프리카 공화국", displayName: "SOUTH_AFRICA", code2: "ZA", emoji: "🇿🇦", defaultCenter: [-25.7479, 28.2293] },
  { code: Country.MOROCCO, name: "모로코", displayName: "MOROCCO", code2: "MA", emoji: "🇲🇦", defaultCenter: [33.9716, -6.8498] },
];

export const TRIP_REGIONS: TripRegionCatalogEntry[] = [
  { code: "KR_SEOUL", countryCode: "KR", label: "서울", type: "CITY", centerLat: 37.5665, centerLng: 126.9780, searchHints: ["서울", "Seoul"] },
  { code: "KR_BUSAN", countryCode: "KR", label: "부산", type: "CITY", centerLat: 35.1796, centerLng: 129.0756, searchHints: ["부산", "Busan"] },
  { code: "KR_JEJU", countryCode: "KR", label: "제주", type: "ISLAND", centerLat: 33.4996, centerLng: 126.5312, searchHints: ["제주", "Jeju"] },
  { code: "KR_GANGNEUNG_SOKCHO", countryCode: "KR", label: "강릉/속초", type: "AREA", centerLat: 37.7519, centerLng: 128.8761, searchHints: ["강릉", "속초", "Gangneung", "Sokcho"] },
  { code: "JP_TOKYO", countryCode: "JP", label: "도쿄", type: "CITY", centerLat: 35.6762, centerLng: 139.6503, searchHints: ["도쿄", "Tokyo"] },
  { code: "JP_OSAKA_KYOTO", countryCode: "JP", label: "오사카/교토", type: "METRO", centerLat: 34.6937, centerLng: 135.5023, searchHints: ["오사카", "교토", "Osaka", "Kyoto"] },
  { code: "JP_HOKKAIDO", countryCode: "JP", label: "홋카이도", type: "AREA", centerLat: 43.0618, centerLng: 141.3545, searchHints: ["홋카이도", "삿포로", "Hokkaido", "Sapporo"] },
  { code: "JP_OKINAWA", countryCode: "JP", label: "오키나와", type: "ISLAND", centerLat: 26.2124, centerLng: 127.6809, searchHints: ["오키나와", "나하", "Okinawa", "Naha"] },
  { code: "JP_FUKUOKA", countryCode: "JP", label: "후쿠오카", type: "CITY", centerLat: 33.5902, centerLng: 130.4017, searchHints: ["후쿠오카", "Fukuoka"] },
  { code: "CN_BEIJING", countryCode: "CN", label: "베이징", type: "CITY", centerLat: 39.9042, centerLng: 116.4074, searchHints: ["베이징", "북경", "Beijing"] },
  { code: "CN_SHANGHAI", countryCode: "CN", label: "상하이", type: "CITY", centerLat: 31.2304, centerLng: 121.4737, searchHints: ["상하이", "Shanghai"] },
  { code: "CN_GUANGZHOU_SHENZHEN", countryCode: "CN", label: "광저우/선전", type: "METRO", centerLat: 23.1291, centerLng: 113.2644, searchHints: ["광저우", "선전", "Guangzhou", "Shenzhen"] },
  { code: "CN_CHENGDU_XIAN", countryCode: "CN", label: "청두/시안", type: "AREA", centerLat: 30.5728, centerLng: 104.0668, searchHints: ["청두", "시안", "Chengdu", "Xian"] },
  { code: "TH_BANGKOK", countryCode: "TH", label: "방콕", type: "CITY", centerLat: 13.7563, centerLng: 100.5018, searchHints: ["방콕", "Bangkok"] },
  { code: "TH_CHIANG_MAI", countryCode: "TH", label: "치앙마이", type: "CITY", centerLat: 18.7883, centerLng: 98.9853, searchHints: ["치앙마이", "Chiang Mai"] },
  { code: "TH_PHUKET_KRABI", countryCode: "TH", label: "푸켓/끄라비", type: "AREA", centerLat: 7.8804, centerLng: 98.3923, searchHints: ["푸켓", "끄라비", "Phuket", "Krabi"] },
  { code: "VN_HANOI", countryCode: "VN", label: "하노이", type: "CITY", centerLat: 21.0278, centerLng: 105.8342, searchHints: ["하노이", "Hanoi"] },
  { code: "VN_DA_NANG_HOI_AN", countryCode: "VN", label: "다낭/호이안", type: "AREA", centerLat: 16.0544, centerLng: 108.2022, searchHints: ["다낭", "호이안", "Da Nang", "Hoi An"] },
  { code: "VN_HO_CHI_MINH", countryCode: "VN", label: "호치민", type: "CITY", centerLat: 10.8231, centerLng: 106.6297, searchHints: ["호치민", "Ho Chi Minh", "Saigon"] },
  { code: "PH_MANILA", countryCode: "PH", label: "마닐라", type: "CITY", centerLat: 14.5995, centerLng: 120.9842, searchHints: ["마닐라", "Manila"] },
  { code: "PH_CEBU_BOHOL", countryCode: "PH", label: "세부/보홀", type: "AREA", centerLat: 10.3157, centerLng: 123.8854, searchHints: ["세부", "보홀", "Cebu", "Bohol"] },
  { code: "PH_PALAWAN_BORACAY", countryCode: "PH", label: "팔라완/보라카이", type: "AREA", centerLat: 11.9674, centerLng: 121.9248, searchHints: ["팔라완", "보라카이", "Palawan", "Boracay"] },
  { code: "SG_SINGAPORE", countryCode: "SG", label: "싱가포르 시티", type: "CITY", centerLat: 1.3521, centerLng: 103.8198, searchHints: ["싱가포르", "Singapore"] },
  { code: "MY_KUALA_LUMPUR", countryCode: "MY", label: "쿠알라룸푸르", type: "CITY", centerLat: 3.139, centerLng: 101.6869, searchHints: ["쿠알라룸푸르", "Kuala Lumpur"] },
  { code: "MY_PENANG", countryCode: "MY", label: "페낭", type: "AREA", centerLat: 5.4141, centerLng: 100.3288, searchHints: ["페낭", "Penang"] },
  { code: "MY_KOTA_KINABALU", countryCode: "MY", label: "코타키나발루", type: "CITY", centerLat: 5.9804, centerLng: 116.0735, searchHints: ["코타키나발루", "Kota Kinabalu"] },
  { code: "ID_JAKARTA", countryCode: "ID", label: "자카르타", type: "CITY", centerLat: -6.2088, centerLng: 106.8456, searchHints: ["자카르타", "Jakarta"] },
  { code: "ID_BALI", countryCode: "ID", label: "발리", type: "ISLAND", centerLat: -8.3405, centerLng: 115.092, searchHints: ["발리", "Bali"] },
  { code: "ID_YOGYAKARTA", countryCode: "ID", label: "족자카르타", type: "CITY", centerLat: -7.7956, centerLng: 110.3695, searchHints: ["족자카르타", "Yogyakarta"] },
  { code: "IN_DELHI_AGRA", countryCode: "IN", label: "델리/아그라", type: "AREA", centerLat: 28.6139, centerLng: 77.209, searchHints: ["델리", "아그라", "Delhi", "Agra"] },
  { code: "IN_MUMBAI", countryCode: "IN", label: "뭄바이", type: "CITY", centerLat: 19.076, centerLng: 72.8777, searchHints: ["뭄바이", "Mumbai"] },
  { code: "IN_GOA", countryCode: "IN", label: "고아", type: "AREA", centerLat: 15.2993, centerLng: 74.124, searchHints: ["고아", "Goa"] },
  { code: "IN_JAIPUR_UDAIPUR", countryCode: "IN", label: "자이푸르/우다이푸르", type: "AREA", centerLat: 26.9124, centerLng: 75.7873, searchHints: ["자이푸르", "우다이푸르", "Jaipur", "Udaipur"] },
  { code: "AE_DUBAI", countryCode: "AE", label: "두바이", type: "CITY", centerLat: 25.2048, centerLng: 55.2708, searchHints: ["두바이", "Dubai"] },
  { code: "AE_ABU_DHABI", countryCode: "AE", label: "아부다비", type: "CITY", centerLat: 24.4539, centerLng: 54.3773, searchHints: ["아부다비", "Abu Dhabi"] },
  { code: "AE_SHARJAH_NORTHERN", countryCode: "AE", label: "샤르자/북부 에미리트", type: "AREA", centerLat: 25.3463, centerLng: 55.4209, searchHints: ["샤르자", "Sharjah"] },
  { code: "TR_ISTANBUL", countryCode: "TR", label: "이스탄불", type: "CITY", centerLat: 41.0082, centerLng: 28.9784, searchHints: ["이스탄불", "Istanbul"] },
  { code: "TR_CAPPADOCIA", countryCode: "TR", label: "카파도키아", type: "AREA", centerLat: 38.6431, centerLng: 34.827, searchHints: ["카파도키아", "Cappadocia"] },
  { code: "TR_ANTALYA_IZMIR", countryCode: "TR", label: "안탈리아/이즈미르", type: "AREA", centerLat: 36.8969, centerLng: 30.7133, searchHints: ["안탈리아", "이즈미르", "Antalya", "Izmir"] },
  { code: "EG_CAIRO_GIZA", countryCode: "EG", label: "카이로/기자", type: "AREA", centerLat: 30.0444, centerLng: 31.2357, searchHints: ["카이로", "기자", "Cairo", "Giza"] },
  { code: "EG_LUXOR_ASWAN", countryCode: "EG", label: "룩소르/아스완", type: "AREA", centerLat: 25.6872, centerLng: 32.6396, searchHints: ["룩소르", "아스완", "Luxor", "Aswan"] },
  { code: "EG_SHARM_EL_SHEIKH", countryCode: "EG", label: "샤름엘셰이크", type: "CITY", centerLat: 27.9158, centerLng: 34.33, searchHints: ["샤름엘셰이크", "Sharm El Sheikh"] },
  { code: "IT_ROME", countryCode: "IT", label: "로마", type: "CITY", centerLat: 41.9028, centerLng: 12.4964, searchHints: ["로마", "Rome"] },
  { code: "IT_MILAN_LAKE_COMO", countryCode: "IT", label: "밀라노/코모호", type: "AREA", centerLat: 45.4642, centerLng: 9.19, searchHints: ["밀라노", "코모", "Milan", "Como"] },
  { code: "IT_VENICE_FLORENCE", countryCode: "IT", label: "베네치아/피렌체", type: "AREA", centerLat: 43.7696, centerLng: 11.2558, searchHints: ["베네치아", "피렌체", "Venice", "Florence"] },
  { code: "IT_NAPLES_AMALFI", countryCode: "IT", label: "나폴리/아말피", type: "AREA", centerLat: 40.8518, centerLng: 14.2681, searchHints: ["나폴리", "아말피", "Naples", "Amalfi"] },
  { code: "FR_PARIS", countryCode: "FR", label: "파리", type: "CITY", centerLat: 48.8566, centerLng: 2.3522, searchHints: ["파리", "Paris"] },
  { code: "FR_NICE_PROVENCE", countryCode: "FR", label: "니스/프로방스", type: "AREA", centerLat: 43.7102, centerLng: 7.262, searchHints: ["니스", "프로방스", "Nice", "Provence"] },
  { code: "FR_LYON_ALPS", countryCode: "FR", label: "리옹/알프스", type: "AREA", centerLat: 45.764, centerLng: 4.8357, searchHints: ["리옹", "알프스", "Lyon", "Alps"] },
  { code: "ES_MADRID", countryCode: "ES", label: "마드리드", type: "CITY", centerLat: 40.4168, centerLng: -3.7038, searchHints: ["마드리드", "Madrid"] },
  { code: "ES_BARCELONA", countryCode: "ES", label: "바르셀로나", type: "CITY", centerLat: 41.3874, centerLng: 2.1686, searchHints: ["바르셀로나", "Barcelona"] },
  { code: "ES_ANDALUSIA", countryCode: "ES", label: "안달루시아", type: "AREA", centerLat: 37.3891, centerLng: -5.9845, searchHints: ["세비야", "그라나다", "말라가", "Andalusia"] },
  { code: "ES_MALLORCA_IBIZA", countryCode: "ES", label: "마요르카/이비사", type: "ISLAND", centerLat: 39.5696, centerLng: 2.6502, searchHints: ["마요르카", "이비사", "Mallorca", "Ibiza"] },
  { code: "GB_LONDON", countryCode: "GB", label: "런던", type: "CITY", centerLat: 51.5074, centerLng: -0.1278, searchHints: ["런던", "London"] },
  { code: "GB_EDINBURGH_HIGHLANDS", countryCode: "GB", label: "에든버러/하이랜드", type: "AREA", centerLat: 55.9533, centerLng: -3.1883, searchHints: ["에든버러", "하이랜드", "Edinburgh", "Highlands"] },
  { code: "GB_MANCHESTER_LIVERPOOL", countryCode: "GB", label: "맨체스터/리버풀", type: "AREA", centerLat: 53.4808, centerLng: -2.2426, searchHints: ["맨체스터", "리버풀", "Manchester", "Liverpool"] },
  { code: "DE_BERLIN", countryCode: "DE", label: "베를린", type: "CITY", centerLat: 52.52, centerLng: 13.405, searchHints: ["베를린", "Berlin"] },
  { code: "DE_MUNICH_BAVARIA", countryCode: "DE", label: "뮌헨/바이에른", type: "AREA", centerLat: 48.1351, centerLng: 11.582, searchHints: ["뮌헨", "바이에른", "Munich", "Bavaria"] },
  { code: "DE_FRANKFURT_RHINE", countryCode: "DE", label: "프랑크푸르트/라인", type: "AREA", centerLat: 50.1109, centerLng: 8.6821, searchHints: ["프랑크푸르트", "라인", "Frankfurt", "Rhine"] },
  { code: "CH_ZURICH_LUCERNE", countryCode: "CH", label: "취리히/루체른", type: "AREA", centerLat: 47.3769, centerLng: 8.5417, searchHints: ["취리히", "루체른", "Zurich", "Lucerne"] },
  { code: "CH_INTERLAKEN_BERNESE", countryCode: "CH", label: "인터라켄/베르너오버란트", type: "AREA", centerLat: 46.6863, centerLng: 7.8632, searchHints: ["인터라켄", "융프라우", "Interlaken", "Bernese Oberland"] },
  { code: "CH_GENEVA_MONTREUX", countryCode: "CH", label: "제네바/몽트뢰", type: "AREA", centerLat: 46.2044, centerLng: 6.1432, searchHints: ["제네바", "몽트뢰", "Geneva", "Montreux"] },
  { code: "NL_AMSTERDAM", countryCode: "NL", label: "암스테르담", type: "CITY", centerLat: 52.3676, centerLng: 4.9041, searchHints: ["암스테르담", "Amsterdam"] },
  { code: "NL_ROTTERDAM_THE_HAGUE", countryCode: "NL", label: "로테르담/헤이그", type: "AREA", centerLat: 51.9244, centerLng: 4.4777, searchHints: ["로테르담", "헤이그", "Rotterdam", "The Hague"] },
  { code: "NL_UTRECHT_GIETHOORN", countryCode: "NL", label: "위트레흐트/히트호른", type: "AREA", centerLat: 52.0907, centerLng: 5.1214, searchHints: ["위트레흐트", "히트호른", "Utrecht", "Giethoorn"] },
  { code: "GR_ATHENS", countryCode: "GR", label: "아테네", type: "CITY", centerLat: 37.9838, centerLng: 23.7275, searchHints: ["아테네", "Athens"] },
  { code: "GR_SANTORINI_CYCLADES", countryCode: "GR", label: "산토리니/키클라데스", type: "ISLAND", centerLat: 36.3932, centerLng: 25.4615, searchHints: ["산토리니", "미코노스", "Santorini", "Cyclades"] },
  { code: "GR_CRETE", countryCode: "GR", label: "크레타", type: "ISLAND", centerLat: 35.2401, centerLng: 24.8093, searchHints: ["크레타", "Crete"] },
  { code: "US_NEW_YORK", countryCode: "US", label: "뉴욕", type: "CITY", centerLat: 40.7128, centerLng: -74.006, searchHints: ["뉴욕", "New York"] },
  { code: "US_CALIFORNIA_WEST", countryCode: "US", label: "캘리포니아 서부", type: "AREA", centerLat: 34.0522, centerLng: -118.2437, searchHints: ["로스앤젤레스", "샌프란시스코", "California", "Los Angeles", "San Francisco"] },
  { code: "US_HAWAII", countryCode: "US", label: "하와이", type: "ISLAND", centerLat: 21.3069, centerLng: -157.8583, searchHints: ["하와이", "호놀룰루", "Hawaii", "Honolulu"] },
  { code: "US_FLORIDA", countryCode: "US", label: "플로리다", type: "AREA", centerLat: 25.7617, centerLng: -80.1918, searchHints: ["마이애미", "올랜도", "Florida", "Miami", "Orlando"] },
  { code: "CA_TORONTO", countryCode: "CA", label: "토론토", type: "CITY", centerLat: 43.6532, centerLng: -79.3832, searchHints: ["토론토", "Toronto"] },
  { code: "CA_VANCOUVER", countryCode: "CA", label: "밴쿠버", type: "CITY", centerLat: 49.2827, centerLng: -123.1207, searchHints: ["밴쿠버", "Vancouver"] },
  { code: "CA_BANFF_JASPER", countryCode: "CA", label: "밴프/재스퍼", type: "AREA", centerLat: 51.1784, centerLng: -115.5708, searchHints: ["밴프", "재스퍼", "Banff", "Jasper"] },
  { code: "CA_MONTREAL_QUEBEC", countryCode: "CA", label: "몬트리올/퀘벡시티", type: "AREA", centerLat: 45.5017, centerLng: -73.5673, searchHints: ["몬트리올", "퀘벡", "Montreal", "Quebec City"] },
  { code: "AU_SYDNEY", countryCode: "AU", label: "시드니", type: "CITY", centerLat: -33.8688, centerLng: 151.2093, searchHints: ["시드니", "Sydney"] },
  { code: "AU_MELBOURNE_GREAT_OCEAN", countryCode: "AU", label: "멜버른/그레이트오션로드", type: "AREA", centerLat: -37.8136, centerLng: 144.9631, searchHints: ["멜버른", "그레이트오션로드", "Melbourne", "Great Ocean Road"] },
  { code: "AU_GOLD_COAST_CAIRNS", countryCode: "AU", label: "골드코스트/케언스", type: "AREA", centerLat: -28.0167, centerLng: 153.4, searchHints: ["골드코스트", "케언스", "Gold Coast", "Cairns"] },
  { code: "AU_PERTH", countryCode: "AU", label: "퍼스", type: "CITY", centerLat: -31.9505, centerLng: 115.8605, searchHints: ["퍼스", "Perth"] },
  { code: "NZ_AUCKLAND", countryCode: "NZ", label: "오클랜드", type: "CITY", centerLat: -36.8509, centerLng: 174.7645, searchHints: ["오클랜드", "Auckland"] },
  { code: "NZ_QUEENSTOWN", countryCode: "NZ", label: "퀸스타운", type: "CITY", centerLat: -45.0312, centerLng: 168.6626, searchHints: ["퀸스타운", "Queenstown"] },
  { code: "NZ_CHRISTCHURCH_TEKAPO", countryCode: "NZ", label: "크라이스트처치/테카포", type: "AREA", centerLat: -43.5321, centerLng: 172.6362, searchHints: ["크라이스트처치", "테카포", "Christchurch", "Tekapo"] },
  { code: "BR_RIO_DE_JANEIRO", countryCode: "BR", label: "리우데자네이루", type: "CITY", centerLat: -22.9068, centerLng: -43.1729, searchHints: ["리우데자네이루", "Rio de Janeiro"] },
  { code: "BR_SAO_PAULO", countryCode: "BR", label: "상파울루", type: "CITY", centerLat: -23.5505, centerLng: -46.6333, searchHints: ["상파울루", "Sao Paulo"] },
  { code: "BR_SALVADOR", countryCode: "BR", label: "살바도르", type: "CITY", centerLat: -12.9777, centerLng: -38.5016, searchHints: ["살바도르", "Salvador"] },
  { code: "BR_IGUAZU", countryCode: "BR", label: "이구아수", type: "AREA", centerLat: -25.6953, centerLng: -54.4367, searchHints: ["이구아수", "Foz do Iguacu", "Iguazu"] },
  { code: "AR_BUENOS_AIRES", countryCode: "AR", label: "부에노스아이레스", type: "CITY", centerLat: -34.6037, centerLng: -58.3816, searchHints: ["부에노스아이레스", "Buenos Aires"] },
  { code: "AR_PATAGONIA", countryCode: "AR", label: "파타고니아", type: "AREA", centerLat: -50.3379, centerLng: -72.2648, searchHints: ["파타고니아", "엘칼라파테", "Patagonia", "El Calafate"] },
  { code: "AR_MENDOZA", countryCode: "AR", label: "멘도사", type: "CITY", centerLat: -32.8895, centerLng: -68.8458, searchHints: ["멘도사", "Mendoza"] },
  { code: "AR_IGUAZU", countryCode: "AR", label: "이과수", type: "AREA", centerLat: -25.5972, centerLng: -54.5786, searchHints: ["이과수", "Iguazu"] },
  { code: "MX_MEXICO_CITY", countryCode: "MX", label: "멕시코시티", type: "CITY", centerLat: 19.4326, centerLng: -99.1332, searchHints: ["멕시코시티", "Mexico City"] },
  { code: "MX_CANCUN_RIVIERA_MAYA", countryCode: "MX", label: "칸쿤/리비에라 마야", type: "AREA", centerLat: 21.1619, centerLng: -86.8515, searchHints: ["칸쿤", "리비에라 마야", "Cancun", "Riviera Maya"] },
  { code: "MX_GUADALAJARA_PUERTO_VALLARTA", countryCode: "MX", label: "과달라하라/푸에르토바야르타", type: "AREA", centerLat: 20.6597, centerLng: -103.3496, searchHints: ["과달라하라", "푸에르토바야르타", "Guadalajara", "Puerto Vallarta"] },
  { code: "ZA_CAPE_TOWN", countryCode: "ZA", label: "케이프타운", type: "CITY", centerLat: -33.9249, centerLng: 18.4241, searchHints: ["케이프타운", "Cape Town"] },
  { code: "ZA_JOHANNESBURG", countryCode: "ZA", label: "요하네스버그", type: "CITY", centerLat: -26.2041, centerLng: 28.0473, searchHints: ["요하네스버그", "Johannesburg"] },
  { code: "ZA_GARDEN_ROUTE_DURBAN", countryCode: "ZA", label: "가든루트/더반", type: "AREA", centerLat: -29.8587, centerLng: 31.0218, searchHints: ["가든루트", "더반", "Garden Route", "Durban"] },
  { code: "MA_MARRAKECH", countryCode: "MA", label: "마라케시", type: "CITY", centerLat: 31.6295, centerLng: -7.9811, searchHints: ["마라케시", "Marrakech"] },
  { code: "MA_CASABLANCA_RABAT", countryCode: "MA", label: "카사블랑카/라바트", type: "AREA", centerLat: 33.5731, centerLng: -7.5898, searchHints: ["카사블랑카", "라바트", "Casablanca", "Rabat"] },
  { code: "MA_FES_CHEFCHAOUEN", countryCode: "MA", label: "페스/셰프샤우엔", type: "AREA", centerLat: 34.0181, centerLng: -5.0078, searchHints: ["페스", "셰프샤우엔", "Fes", "Chefchaouen"] },
];

export const getCountryOptionByCode = (country: Country | string | undefined) =>
  TRIP_COUNTRY_OPTIONS.find((option) => option.code === country);

export const getCountryOptionByCode2 = (countryCode2: string | undefined | null) =>
  TRIP_COUNTRY_OPTIONS.find((option) => option.code2 === countryCode2);

export const getCountryLabelByCode2 = (countryCode2: string | undefined | null) =>
  getCountryOptionByCode2(countryCode2)?.name ?? countryCode2 ?? "";

export const getCountryEmojiByCode2 = (countryCode2: string | undefined | null) =>
  getCountryOptionByCode2(countryCode2)?.emoji ?? "✈️";

export const getCountryEmojiByValue = (country: string | undefined | null) =>
  getCountryOptionByCode2(country)?.emoji
  ?? TRIP_COUNTRY_OPTIONS.find((option) => option.name === country)?.emoji
  ?? "✈️";

export const getTripRegionsByCountryCode = (countryCode: string | undefined | null) =>
  TRIP_REGIONS.filter((region) => region.countryCode === countryCode);

export const getTripRegionByCode = (regionCode: string | undefined | null) =>
  TRIP_REGIONS.find((region) => region.code === regionCode);

export const getTripRegionLabel = (regionCode: string | undefined | null) =>
  getTripRegionByCode(regionCode)?.label ?? null;

export const formatTripDestination = (countryOrLabel: string, regionCode?: string | null) => {
  const countryLabel = getCountryLabelByCode2(countryOrLabel) || countryOrLabel;
  const regionLabel = getTripRegionLabel(regionCode);
  return regionLabel ? `${countryLabel} · ${regionLabel}` : countryLabel;
};

export const getTripRegionCenter = (regionCode?: string | null): [number, number] | null => {
  const region = getTripRegionByCode(regionCode);
  return region ? [region.centerLat, region.centerLng] : null;
};

export const getCountryCoordinatesByCode2 = (countryCode2: string | undefined | null): [number, number] =>
  getCountryOptionByCode2(countryCode2)?.defaultCenter ?? [37.5665, 126.978];

export const buildPlaceSearchQuery = (query: string, regionCode?: string | null) => {
  const normalizedQuery = query.trim();
  if (!normalizedQuery) return normalizedQuery;

  const region = getTripRegionByCode(regionCode);
  if (!region) return normalizedQuery;

  const queryLower = normalizedQuery.toLowerCase();
  const hasHint = [region.label, ...region.searchHints].some((hint) =>
    queryLower.includes(hint.toLowerCase()),
  );

  return hasHint ? normalizedQuery : `${region.label} ${normalizedQuery}`;
};
