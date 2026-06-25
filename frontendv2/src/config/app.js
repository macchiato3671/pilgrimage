export const APP_NAME = '필그리미지'
export const SEOUL_CENTER = { latitude: 37.5665, longitude: 126.978 }

export const PLAN_COLORS = [
  '#f35f45',
  '#f59e4a',
  '#d7ad31',
  '#4ba271',
  '#3e88d6',
  '#7568cc',
  '#ad5ca8',
]

export const MAP_MARKER_COLORS = {
  selected: '#111827',
  scene: '#d946ef',
  place: '#06b6d4',
  favorite: '#64748b',
  route: '#334155',
}

// 한국관광공사 ContentType ID 기준. 백엔드 분류가 바뀌면 이 목록만 수정합니다.
export const PLACE_CATEGORIES = [
  { id: null, label: '주변 전체', keyword: '' },
  { id: 39, label: '음식점', keyword: '' },
  { id: 39, label: '카페', keyword: '카페' },
  { id: 12, label: '관광지', keyword: '' },
  { id: 14, label: '문화시설', keyword: '' },
  { id: 32, label: '숙박', keyword: '' },
  { id: 38, label: '쇼핑', keyword: '' },
  { id: 28, label: '레포츠', keyword: '' },
]

export const STORAGE_KEYS = {
  auth: 'pilgrimage.auth',
  guest: 'pilgrimage.guest',
  colors: 'pilgrimage.plan-colors',
  editor: 'pilgrimage.editor-draft',
}

export const DEFAULT_PAGE_SIZE = 20
