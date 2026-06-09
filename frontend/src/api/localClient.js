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

const createWishlist = (sceneId, body = {}) => ({
  wishlistId: body.wishlistId ?? `local-wishlist-${sceneId}`,
  scene: { ...(body.scene ?? body), sceneId },
})

const createPlan = (body = {}, planId = body.planId ?? body.localPlanId ?? `local-${Date.now()}`) => ({
  ...body,
  planId,
  localPlanId: body.localPlanId ?? planId,
  details: body.details ?? [],
  createdAt: body.createdAt ?? new Date().toISOString(),
  updatedAt: new Date().toISOString(),
})

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
