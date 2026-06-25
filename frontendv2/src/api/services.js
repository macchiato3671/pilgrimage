import http from './http'
import {
  arrayOf,
  normalizeDrama,
  normalizeMember,
  normalizePage,
  normalizePlace,
  normalizePlan,
  normalizeScene,
  tokenPayload,
  unwrap,
} from '../models/normalizers'
import { detailPayload, planPayload } from '../models/plan'

const get = async (url, config) => unwrap(await http.get(url, config))
const post = async (url, data, config) => unwrap(await http.post(url, data, config))
const put = async (url, data, config) => unwrap(await http.put(url, data, config))
const remove = async (url, config) => unwrap(await http.delete(url, config))

const dramaPage = (payload) => {
  const page = normalizePage(payload, ['dramas'])
  return { ...page, items: page.items.map(normalizeDrama) }
}

export const authApi = {
  async login(credentials) {
    return tokenPayload(await post('/auth/login', credentials))
  },
  register: (member) => post('/members', member),
  async me() {
    return normalizeMember(await get('/me'))
  },
  updateMe: (member) => put('/me', member),
  removeMe: () => remove('/me'),
}

// API-001, API-003, API-004는 취소된 명세이므로 호출하지 않습니다.
export const dramaApi = {
  years: async () => arrayOf(await get('/dramas/years'), ['years']).map(Number).filter(Number.isFinite),
  byYear: async (year, params = {}) => dramaPage(await get(`/dramas/years/${encodeURIComponent(year)}`, { params })),
  genres: async () =>
    arrayOf(await get('/dramas/genres'), ['genres']).map((raw) => ({
      genreId: raw.genreId ?? raw.genre_id ?? raw.id,
      name: raw.name || raw.genreName || String(raw),
    })),
  byGenre: async (genreId, params = {}) =>
    dramaPage(await get(`/dramas/genres/${encodeURIComponent(genreId)}`, { params })),
  search: async (keyword, params = {}) =>
    dramaPage(await get('/dramas/search', { params: { keyword, ...params } })),
  async scenes(dramaId) {
    const data = await get(`/dramas/${encodeURIComponent(dramaId)}`)
    const drama = normalizeDrama(data?.drama || data)
    return {
      drama,
      scenes: arrayOf(data, ['scenes', 'sceneList']).map((raw) => normalizeScene(raw, drama)),
    }
  },
  scene: async (sceneId) => normalizeScene(await get(`/scenes/${encodeURIComponent(sceneId)}`)),
}

export const placeApi = {
  async search(params = {}) {
    const page = normalizePage(await get('/places/search', { params }), ['places', 'attractions'])
    return { ...page, items: page.items.map(normalizePlace) }
  },
  detail: async (placeId) => normalizePlace(await get(`/places/${encodeURIComponent(placeId)}`)),
  nearby: async (sceneId, params = {}) => {
    const data = await get(`/scenes/${encodeURIComponent(sceneId)}/nearby-attractions`, { params })
    return arrayOf(data, ['places', 'attractions']).map(normalizePlace)
  },
}

export const wishlistApi = {
  list: async () => arrayOf(await get('/wishlist'), ['wishlist', 'scenes']).map(normalizeScene),
  add: (sceneId) => post(`/wishlist/${encodeURIComponent(sceneId)}`),
  remove: (sceneId) => remove(`/wishlist/${encodeURIComponent(sceneId)}`),
  dramas: async () => arrayOf(await get('/wishlist/dramas'), ['dramas']).map(normalizeDrama),
  scenesForDrama: async (dramaId) =>
    arrayOf(await get(`/wishlist/dramas/${encodeURIComponent(dramaId)}/scenes`), ['scenes']).map(normalizeScene),
}

export const planApi = {
  list: async () => arrayOf(await get('/plans'), ['plans']).map(normalizePlan),
  detail: async (planId) => normalizePlan(await get(`/plans/${encodeURIComponent(planId)}`)),
  create: async (plan) => normalizePlan(await post('/plans', planPayload(plan))),
  update: async (planId, plan) => normalizePlan(await put(`/plans/${encodeURIComponent(planId)}`, planPayload(plan))),
  remove: (planId) => remove(`/plans/${encodeURIComponent(planId)}`),
  syncDetails: async (planId, details) =>
    arrayOf(
      await put(`/plans/${encodeURIComponent(planId)}/details`, {
        details: details.map(detailPayload),
      }),
      ['details'],
    ),
}
