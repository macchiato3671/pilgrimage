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

const createLocalApiError = (status, errorCode, message) => {
  const error = new Error(message)

  error.status = status
  error.errorCode = errorCode
  error.data = { errorCode, message }
  error.originalError = error

  return error
}

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

const toTimeValue = (value) => value.length === 5 ? `${value}:00` : value

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
  assertValid(hasValue(detail.endTime), 400, 'INVALID_PLAN_DETAIL', 'Invalid travel plan detail.')
  assertValid(isValidTime(detail.beginTime), 400, 'INVALID_PLAN_DETAIL_TIME', 'Invalid travel plan detail time range.')
  assertValid(isValidTime(detail.endTime), 400, 'INVALID_PLAN_DETAIL_TIME', 'Invalid travel plan detail time range.')
  assertValid(
    toTimeValue(detail.beginTime) <= toTimeValue(detail.endTime),
    400,
    'INVALID_PLAN_DETAIL_TIME',
    'Invalid travel plan detail time range.',
  )
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

export const localApiClient = {
  async get(path) {
    const [resource, id] = parts(path)

    if (resource === 'wishlist') {
      return localGetWishlists()
    }

    if (resource === 'plans' && id) {
      return localGetPlan(id)
    }

    if (resource === 'plans') {
      return localGetPlans()
    }

    throwEndpointNotFound()
  },

  async post(path, body) {
    const [resource, id] = parts(path)

    if (resource === 'wishlist') {
      assertValid(hasValue(id), 400, 'REQUIRED_FIELD_MISSING', 'Required field is missing.')

      const wishlist = createWishlist(id, body)
      const exists = localGetWishlists().wishlists.some((item) => {
        return String(item.scene?.sceneId) === String(wishlist.scene.sceneId)
      })

      if (exists) {
        throwLocalApiError(409, 'WISHLIST_ALREADY_EXISTS', '이미 위시리스트에 추가된 씬입니다.')
      }

      localPostWishlists(wishlist)
      return wishlist
    }

    if (resource === 'plans') {
      const plan = createPlan(body)
      localPostPlan(plan)
      return plan
    }

    throwEndpointNotFound()
  },

  async put(path, body) {
    const [resource, id] = parts(path)

    if (resource === 'plans') {
      assertValid(hasValue(id), 400, 'INVALID_PLAN_ID', 'Invalid travel plan ID.')

      const current = localGetPlan(id).plan
      assertValid(current, 404, 'TRAVEL_PLAN_NOT_FOUND', 'Travel plan not found.')

      const plan = createPlan(body, id)
      localPutPlan(plan.planId, plan)
      return plan
    }

    throwEndpointNotFound()
  },

  async delete(path) {
    const [resource, id] = parts(path)

    if (resource === 'wishlist') {
      return localDeleteWishlists(id)
    }

    if (resource === 'plans') {
      return localDeletePlan(id)
    }

    throwEndpointNotFound()
  },
}
