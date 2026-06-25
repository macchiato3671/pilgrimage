const WRAPPER_KEYS = ['data', 'result', 'body', 'payload', 'response']

export const unwrap = (value) => {
  let current = value?.data ?? value
  for (let i = 0; i < 5 && current && typeof current === 'object' && !Array.isArray(current); i += 1) {
    const key = WRAPPER_KEYS.find((candidate) => current[candidate] !== undefined)
    if (!key) break
    current = current[key]
  }
  return current
}

export const arrayOf = (value, keys = []) => {
  const data = unwrap(value)
  if (Array.isArray(data)) return data
  for (const key of [...keys, 'content', 'items', 'list']) {
    if (Array.isArray(data?.[key])) return data[key]
  }
  return []
}

const numberOrNull = (value) => {
  if (value === '' || value == null) return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}
const validId = (value) => {
  if (value === '' || value == null) return null
  const number = Number(value)
  if (Number.isFinite(number) && number === 0) return null
  return value
}
const firstValidId = (...values) => values.map(validId).find((value) => value != null) ?? null

const imageUrls = (raw = {}) => {
  const candidates = [
    raw.imageUrls,
    raw.imgUrls,
    raw.images,
    raw.sceneImgs,
    raw.placeImgs,
    raw.dramaImgs,
    raw.imageList,
    raw.urls,
  ]
  const values = candidates.find(Array.isArray) || []
  const many = values
    .map((item) => (typeof item === 'string' ? item : item?.url || item?.imageUrl || item?.imgUrl))
    .filter(Boolean)
  const single = raw.posterUrl || raw.thumbnailUrl || raw.imageUrl || raw.imgUrl
  return [...new Set(single ? [single, ...many] : many)]
}

const genreNames = (raw) =>
  arrayOf(raw).map((genre) => (typeof genre === 'string' ? genre : genre?.name || genre?.genreName)).filter(Boolean)

export const normalizeDrama = (raw = {}) => {
  const source = raw.drama || raw
  const releasedAt = source.releasedAt || source.releaseDate || source.released_at || ''
  return {
    dramaId: source.dramaId ?? source.drama_id ?? source.id,
    title: source.title || source.name || '제목 미정',
    description: source.description || source.summary || '',
    releasedAt,
    year: Number(source.year || String(releasedAt).slice(0, 4)) || null,
    sceneCount: Number(source.sceneCount ?? source.scenesCount ?? source.count ?? source.scene_count ?? 0),
    genres: genreNames(source.genres || source.genreList),
    images: imageUrls(source),
  }
}

export const normalizeScene = (raw = {}, drama = {}) => {
  const source = raw.scene || raw
  return {
    kind: 'scene',
    sceneId: source.sceneId ?? source.scene_id ?? source.id,
    dramaId: firstValidId(source.dramaId, source.drama_id, source.drama?.dramaId, raw.dramaId, drama.dramaId),
    dramaTitle: source.dramaTitle || source.drama?.title || raw.dramaTitle || raw.titleOfDrama || drama.title || '',
    name: source.name || source.sceneName || source.title || '이름 없는 촬영지',
    description: source.description || source.sceneDescription || '',
    address: source.address || source.addr1 || '',
    latitude: numberOrNull(source.latitude ?? source.lat ?? source.mapY),
    longitude: numberOrNull(source.longitude ?? source.lng ?? source.lon ?? source.mapX),
    tips: source.tips || source.detailedTips || source.visitTip || source.detail || '',
    images: imageUrls(source),
    wishlistId: raw.wishlistId ?? raw.wishlist_id ?? source.wishlistId ?? null,
  }
}

export const normalizePlace = (raw = {}) => {
  const source = raw.place || raw
  return {
    kind: 'place',
    placeId: source.placeId ?? source.place_id ?? source.id,
    contentId: source.contentId ?? source.content_id,
    contentTypeId: Number(source.contentTypeId ?? source.content_type_id) || null,
    contentTypeName: source.contentTypeName || source.categoryName || source.typeName || '장소',
    name: source.name || source.title || '이름 없는 장소',
    description: source.description || source.overview || '',
    address: source.address || source.addr1 || '',
    latitude: numberOrNull(source.latitude ?? source.lat ?? source.mapY),
    longitude: numberOrNull(source.longitude ?? source.lng ?? source.lon ?? source.mapX),
    tips: source.tips || source.detailedTips || source.detail || '',
    images: imageUrls(source),
  }
}

export const normalizeDetail = (raw = {}) => ({
  detailId: raw.detailId ?? raw.detail_id ?? null,
  clientId: raw.clientId || `detail-${raw.detailId ?? globalThis.crypto?.randomUUID?.() ?? Math.random()}`,
  dayNo: Number(raw.dayNo ?? raw.day_no ?? 1),
  beginTime: String(raw.beginTime ?? raw.begin_time ?? '09:00').slice(0, 5),
  sceneId: raw.sceneId ?? raw.scene_id ?? raw.scene?.sceneId ?? null,
  placeId: raw.placeId ?? raw.place_id ?? raw.place?.placeId ?? null,
  item: raw.item || (raw.scene ? normalizeScene(raw.scene) : raw.place ? normalizePlace(raw.place) : null),
})

export const normalizePlan = (raw = {}) => ({
  planId: raw.planId ?? raw.plan_id ?? raw.id,
  title: raw.title || '이름 없는 여행',
  beginDate: raw.beginDate || raw.begin_date || '',
  endDate: raw.endDate || raw.end_date || '',
  memo: raw.memo || '',
  color: raw.color || null,
  details: arrayOf(raw.details).map(normalizeDetail),
  createdAt: raw.createdAt || raw.created_at || '',
  updatedAt: raw.updatedAt || raw.updated_at || '',
})

export const normalizeMember = (raw = {}) => ({
  memberId: raw.memberId ?? raw.member_id ?? raw.id,
  email: raw.email || '',
  nickname: raw.nickname || raw.name || '',
  role: raw.role || raw.roleName || 'USER',
  status: raw.status || 'ACTIVE',
})

export const normalizePage = (value, listKeys = []) => {
  const data = unwrap(value) || {}
  const items = arrayOf(data, listKeys)
  const page = data.page ?? data.pagination ?? {}
  const pageNumber = typeof page === 'number' ? page : page.number
  const number = Number(pageNumber ?? data.number ?? 0)
  const size = Number((typeof page === 'object' ? page.size : undefined) ?? data.size ?? items.length)
  const totalElements = Number(
    (typeof page === 'object' ? page.totalElements : undefined) ?? data.totalElements ?? items.length,
  )
  const totalPages = Number(
    (typeof page === 'object' ? page.totalPages : undefined) ??
      data.totalPages ??
      (totalElements && size ? Math.ceil(totalElements / size) : 1),
  )
  const hasNext = typeof page === 'object' ? page.hasNext : data.hasNext
  return {
    items,
    page: number,
    size,
    totalElements,
    totalPages,
    hasNext: Boolean(hasNext ?? number + 1 < totalPages),
  }
}

export const tokenPayload = (value) => {
  const data = unwrap(value) || {}
  const member = data.member || data.user || {}
  return {
    accessToken: data.accessToken || data.access_token || data.token || '',
    refreshToken: data.refreshToken || data.refresh_token || '',
    tokenType: data.tokenType || data.token_type || 'Bearer',
    expiresIn: data.expiresIn || data.expires_in || null,
    member: normalizeMember(member),
  }
}
