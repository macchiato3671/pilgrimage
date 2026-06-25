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

const BACKEND_MAX_PAGE_SIZE = 50

const get = async (url, config) => unwrap(await http.get(url, config))
const post = async (url, data, config) => unwrap(await http.post(url, data, config))
const put = async (url, data, config) => unwrap(await http.put(url, data, config))
const patch = async (url, data, config) => unwrap(await http.patch(url, data, config))
const remove = async (url, data, config = {}) =>
  unwrap(await http.delete(url, data === undefined ? config : { ...config, data }))

const dramaPage = (payload) => {
  const page = normalizePage(payload, ['dramas'])
  return { ...page, items: page.items.map(normalizeDrama) }
}

const placePage = (payload, listKeys = ['places']) => {
  const page = normalizePage(payload, listKeys)
  return { ...page, items: page.items.map(normalizePlace) }
}

const wishlistScenesPage = async (drama, params = {}) => {
  const page = normalizePage(
    await get(`/wishlist/dramas/${encodeURIComponent(drama.dramaId)}/scenes`, {
      params: { page: 0, size: BACKEND_MAX_PAGE_SIZE, ...params },
    }),
    ['scenes'],
  )
  return {
    ...page,
    items: page.items.map((scene) =>
      normalizeScene(
        {
          ...scene,
          dramaId: scene.dramaId ?? drama.dramaId,
          dramaTitle: scene.dramaTitle ?? drama.title,
        },
        drama,
      ),
    ),
  }
}

const collectWishlistScenes = async (drama) => {
  const scenes = []
  let page = 0
  let hasNext = true
  while (hasNext) {
    const response = await wishlistScenesPage(drama, { page, size: BACKEND_MAX_PAGE_SIZE })
    scenes.push(...response.items)
    hasNext = response.hasNext && response.items.length > 0
    page += 1
  }
  return scenes
}

const collectPlans = async () => {
  const plans = []
  let page = 1
  let shouldContinue = true
  while (shouldContinue) {
    const pageItems = arrayOf(await get('/plans', { params: { page, pageSize: BACKEND_MAX_PAGE_SIZE } }), [
      'travelPlans',
      'plans',
    ]).map(normalizePlan)
    plans.push(...pageItems)
    shouldContinue = pageItems.length === BACKEND_MAX_PAGE_SIZE
    page += 1
  }
  return plans
}

const requirePlanDetails = (details = []) => {
  if (details.length) return
  const error = new Error('세부 일정을 저장하려면 촬영지나 장소를 하나 이상 추가해 주세요.')
  error.code = 'INVALID_PLAN_DETAIL'
  throw error
}

export const authApi = {
  async login(credentials) {
    return tokenPayload(await post('/auth/login', credentials))
  },
  register: (member) => post('/members', member),
  async me() {
    return normalizeMember(await get('/me'))
  },
  updateMe: (member) => patch('/me', member),
  removeMe: (request = {}) => remove('/me', request),
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
    const scenes = []
    let drama = null
    let page = 0
    let hasNext = true

    while (hasNext) {
      const data = await get(`/dramas/${encodeURIComponent(dramaId)}`, {
        params: { page, size: BACKEND_MAX_PAGE_SIZE },
      })
      drama ||= normalizeDrama(data?.drama || data)
      const response = normalizePage(data, ['scenes', 'sceneList'])
      scenes.push(...response.items.map((raw) => normalizeScene(raw, drama)))
      hasNext = response.hasNext && response.items.length > 0
      page += 1
    }

    return { drama, scenes }
  },
  scene: async (sceneId) => normalizeScene(await get(`/scenes/${encodeURIComponent(sceneId)}`)),
}

export const placeApi = {
  async search(params = {}) {
    return placePage(await get('/places/search', { params }), ['places', 'attractions'])
  },
  detail: async (placeId) => normalizePlace(await get(`/places/${encodeURIComponent(placeId)}`)),
  nearby: async (sceneId, params = {}) => {
    const data = await get(`/scenes/${encodeURIComponent(sceneId)}/nearby-attractions`, { params })
    return placePage(data, ['attractions', 'places'])
  },
}

export const wishlistApi = {
  list: async () => (await Promise.all((await wishlistApi.dramas()).map(collectWishlistScenes))).flat(),
  add: (sceneId) => post(`/wishlist/${encodeURIComponent(sceneId)}`),
  remove: (sceneId) => remove(`/wishlist/${encodeURIComponent(sceneId)}`),
  dramas: async () => arrayOf(await get('/wishlist/dramas'), ['dramas']).map(normalizeDrama),
  scenesForDrama: async (dramaId, params = {}) =>
    (await wishlistScenesPage({ dramaId }, params)).items,
}

export const planApi = {
  list: collectPlans,
  detail: async (planId) => normalizePlan(await get(`/plans/${encodeURIComponent(planId)}`)),
  create: async (plan) => normalizePlan(await post('/plans', planPayload(plan))),
  update: async (planId, plan) => normalizePlan(await put(`/plans/${encodeURIComponent(planId)}`, planPayload(plan))),
  remove: (planId) => remove(`/plans/${encodeURIComponent(planId)}`),
  syncDetails: async (planId, details) => {
    requirePlanDetails(details)
    return arrayOf(
      await put(`/plans/${encodeURIComponent(planId)}/details`, {
        details: details.map(detailPayload),
      }),
      ['details'],
    )
  },
}
