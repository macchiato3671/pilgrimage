import {
  localDeletePlan,
  localDeleteWishlists,
  localGetPlan,
  localGetPlans,
  localGetWishlists,
  localPostPlan,
  localPostWishlists,
  localPutPlan,
} from '../utils/localStorageUtil.js'

const parts = (path) => path.split('/').filter(Boolean)

const hasValue = (value) => value !== undefined && value !== null && value !== ''

const hasText = (value) => typeof value === 'string' && value.trim() !== ''

const createLocalApiError = (status, errorCode, message, originalError = new Error(message)) => ({
  status,
  errorCode,
  message,
  data: { detail: { errorCode, message } },
  originalError,
})

const throwLocalApiError = (status, errorCode, message) => {
  throw createLocalApiError(status, errorCode, message)
}

const assertValid = (condition, status, errorCode, message) => {
  if (!condition) throwLocalApiError(status, errorCode, message)
}

const isValidDate = (value) => {
  if (typeof value !== 'string') return false

  const date = new Date(value)
  return /^\d{4}-\d{2}-\d{2}$/.test(value)
    && !Number.isNaN(date.getTime())
    && date.toISOString().slice(0, 10) === value
}

const isValidTime = (value) => {
  if (typeof value !== 'string') return false

  return /^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/.test(value)
}

const getTripDays = (beginDate, endDate) => {
  const begin = new Date(beginDate)
  const end = new Date(endDate)
  const dayMs = 24 * 60 * 60 * 1000

  return Math.floor((end - begin) / dayMs) + 1
}

const assertValidScene = (scene) => {
  assertValid(hasValue(scene.sceneId), 404, 'SCENE_NOT_FOUND', 'Scene not found.')

  const hasSceneBody = ['name', 'latitude', 'longitude'].some((key) => hasValue(scene[key]))
  if (!hasSceneBody) return

  assertValid(hasText(scene.name), 400, 'REQUIRED_FIELD_MISSING', 'Required field is missing.')
  assertValid(Number.isFinite(Number(scene.latitude)), 400, 'INVALID_PLAN_DETAIL', 'Invalid travel plan detail.')
  assertValid(Number.isFinite(Number(scene.longitude)), 400, 'INVALID_PLAN_DETAIL', 'Invalid travel plan detail.')
}

const assertValidPlanDetail = (detail) => {
  const hasPlace = hasValue(detail.placeId)
  const hasScene = hasValue(detail.sceneId)

  assertValid(hasValue(detail.dayNo), 400, 'INVALID_PLAN_DETAIL', 'Invalid travel plan detail.')
  assertValid(Number.isInteger(Number(detail.dayNo)), 400, 'INVALID_PLAN_DETAIL', 'Invalid travel plan detail.')
  assertValid(Number(detail.dayNo) >= 1, 400, 'INVALID_PLAN_DETAIL', 'Invalid travel plan detail.')
  assertValid(hasValue(detail.beginTime), 400, 'INVALID_PLAN_DETAIL', 'Invalid travel plan detail.')
  assertValid(isValidTime(detail.beginTime), 400, 'INVALID_PLAN_DETAIL_TIME', 'Invalid travel plan detail time range.')
  assertValid(hasPlace !== hasScene, 400, 'INVALID_PLAN_DETAIL_TARGET', 'Invalid travel plan detail target.')
}

const assertDetailsInTripRange = (details, beginDate, endDate) => {
  const tripDays = getTripDays(beginDate, endDate)
  const isInRange = details.every((detail) => Number(detail.dayNo) <= tripDays)

  assertValid(isInRange, 422, 'PLAN_DETAIL_OUT_OF_RANGE', 'Travel plan detail is out of the travel date range.')
}

const throwEndpointNotFound = () => {
  throwLocalApiError(404, 'ENDPOINT_NOT_FOUND', 'Endpoint not found.')
}

const createWishlist = (sceneId, body = {}) => {
  const scene = { ...(body.scene ?? body), sceneId }
  assertValidScene(scene)

  return {
    wishlistId: body.wishlistId ?? `local-wishlist-${sceneId}`,
    createdAt: body.createdAt ?? new Date().toISOString(),
    scene,
  }
}

const createPlan = (body = {}, planId = body.planId ?? body.localPlanId ?? `local-${Date.now()}`) => {
  const details = body.details

  assertValid(hasText(body.title), 400, 'INVALID_TRAVEL_PLAN_TITLE', 'Invalid travel plan title.')
  assertValid(hasValue(body.beginDate), 400, 'REQUIRED_FIELD_MISSING', 'Required field is missing.')
  assertValid(hasValue(body.endDate), 400, 'REQUIRED_FIELD_MISSING', 'Required field is missing.')
  assertValid(Array.isArray(details), 400, 'REQUIRED_FIELD_MISSING', 'Required field is missing.')
  assertValid(isValidDate(body.beginDate), 400, 'INVALID_TRAVEL_PLAN_DATE', 'Invalid travel plan date range.')
  assertValid(isValidDate(body.endDate), 400, 'INVALID_TRAVEL_PLAN_DATE', 'Invalid travel plan date range.')
  assertValid(body.beginDate <= body.endDate, 400, 'INVALID_TRAVEL_PLAN_DATE', 'Invalid travel plan date range.')
  details.forEach(assertValidPlanDetail)
  assertDetailsInTripRange(details, body.beginDate, body.endDate)

  return {
    ...body,
    planId,
    localPlanId: body.localPlanId ?? planId,
    details,
    createdAt: body.createdAt ?? new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
}

const getPlan = (id) => {
  const plan = localGetPlan(id).plan
  assertValid(plan, 404, 'TRAVEL_PLAN_NOT_FOUND', 'Travel plan not found.')

  return plan
}

const getPlans = ({ id }) => id ? getPlan(id) : localGetPlans()

const postWishlist = ({ id, body }) => {
  assertValid(hasValue(id), 400, 'REQUIRED_FIELD_MISSING', 'Required field is missing.')

  const wishlist = createWishlist(id, body)
  const existing = localGetWishlists().wishlists.find((item) => {
    return String(item.scene?.sceneId) === String(wishlist.scene.sceneId)
  })

  if (existing) return existing

  localPostWishlists(wishlist)
  return wishlist
}

const postPlan = ({ body }) => {
  const plan = createPlan(body)
  localPostPlan(plan)
  return plan
}

const putPlan = ({ id, body }) => {
  assertValid(hasValue(id), 400, 'INVALID_PLAN_ID', 'Invalid travel plan ID.')

  getPlan(id)

  const plan = createPlan(body, id)
  localPutPlan(plan.planId, plan)
  return plan
}

const deleteWishlist = ({ id }) => {
  assertValid(hasValue(id), 400, 'REQUIRED_FIELD_MISSING', 'Required field is missing.')

  const current = localGetWishlists().wishlists.find((wishlist) => {
    return String(wishlist.scene?.sceneId) === String(id)
  })

  assertValid(current, 404, 'WISHLIST_NOT_FOUND', 'Wishlist item not found.')

  localDeleteWishlists(id)
  return { sceneId: id, deleted: true }
}

const deletePlan = ({ id }) => {
  assertValid(hasValue(id), 400, 'INVALID_PLAN_ID', 'Invalid travel plan ID.')

  getPlan(id)
  localDeletePlan(id)

  return { planId: id, deleted: true }
}

const handlers = {
  get: {
    wishlist: localGetWishlists,
    plans: getPlans,
  },
  post: {
    wishlist: postWishlist,
    plans: postPlan,
  },
  put: {
    plans: putPlan,
  },
  delete: {
    wishlist: deleteWishlist,
    plans: deletePlan,
  },
}

const request = (method, path, body) => {
  const [resource, id] = parts(path)
  const handler = handlers[method]?.[resource]

  if (!handler) throwEndpointNotFound()

  try {
    return handler({ id, body })
  } catch (error) {
    if (error?.status) throw error

    throw createLocalApiError(500, 'LOCAL_STORAGE_ERROR', error?.message ?? 'Local storage error.', error)
  }
}

export const localApiClient = {
  get: async (path) => request('get', path),
  post: async (path, body) => request('post', path, body),
  put: async (path, body) => request('put', path, body),
  delete: async (path) => request('delete', path),
}
