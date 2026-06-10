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

const assertValid = (condition, message) => {
  if (!condition) throw new Error(message)
}

const assertValidScene = (scene) => {
  assertValid(hasValue(scene.sceneId), 'sceneId is required')

  const hasSceneBody = ['name', 'latitude', 'longitude'].some((key) => hasValue(scene[key]))
  if (!hasSceneBody) return

  assertValid(hasValue(scene.name), 'scene.name is required')
  assertValid(Number.isFinite(Number(scene.latitude)), 'scene.latitude must be a number')
  assertValid(Number.isFinite(Number(scene.longitude)), 'scene.longitude must be a number')
}

const assertValidPlanDetail = (detail) => {
  const hasPlace = hasValue(detail.placeId)
  const hasScene = hasValue(detail.sceneId)

  assertValid(Number(detail.dayNo) >= 1, 'detail.dayNo must be greater than or equal to 1')
  assertValid(hasValue(detail.beginTime), 'detail.beginTime is required')
  assertValid(hasValue(detail.endTime), 'detail.endTime is required')
  assertValid(detail.beginTime <= detail.endTime, 'detail.beginTime must be before detail.endTime')
  assertValid(hasPlace !== hasScene, 'detail requires exactly one of placeId or sceneId')
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
  const details = body.details ?? []

  assertValid(hasValue(body.title), 'plan.title is required')
  assertValid(Array.isArray(details), 'plan.details must be an array')
  assertValid(!body.beginDate || !body.endDate || body.beginDate <= body.endDate, 'plan beginDate must be before endDate')
  details.forEach(assertValidPlanDetail)

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
  },

  async post(path, body) {
    const [resource, id] = parts(path)

    if (resource === 'wishlist') {
      const wishlist = createWishlist(id, body)
      localPostWishlists(wishlist)
      return wishlist
    }

    if (resource === 'plans') {
      const plan = createPlan(body)
      localPostPlan(plan)
      return plan
    }
  },

  async put(path, body) {
    const [resource, id] = parts(path)

    if (resource === 'plans') {
      const plan = createPlan(body, id)
      localPutPlan(plan.planId, plan)
      return plan
    }
  },

  async delete(path) {
    const [resource, id] = parts(path)

    if (resource === 'wishlist') {
      return localDeleteWishlists(id)
    }

    if (resource === 'plans') {
      return localDeletePlan(id)
    }
  },
}
