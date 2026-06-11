import { authApiClient } from '@/api/apiClient'
import { localApiClient } from '@/api/localClient'

const resolveSceneId = (scene) => scene?.sceneId

const getWishlistStorage = ({ isLoggedIn }) => {
  if (isLoggedIn) {
    return {
      add: (scene) => authApiClient.post(`/wishlist/${resolveSceneId(scene)}`),
      fetch: () => authApiClient.get('/wishlist'),
      remove: (sceneId) => authApiClient.delete(`/wishlist/${sceneId}`),
    }
  }

  return {
    add: (scene) => localApiClient.post(`/wishlist/${resolveSceneId(scene)}`, scene),
    fetch: () => localApiClient.get('/wishlist'),
    remove: (sceneId) => localApiClient.delete(`/wishlist/${sceneId}`),
  }
}

export const wishlistService = {
  getIds(wishlists = []) {
    return wishlists
      .map((wish) => wish.scene?.sceneId)
      .filter((sceneId) => sceneId !== undefined && sceneId !== null)
      .map((sceneId) => String(sceneId))
  },

  fetch({ isLoggedIn }) {
    return getWishlistStorage({ isLoggedIn }).fetch()
  },

  add({ scene, isLoggedIn }) {
    return getWishlistStorage({ isLoggedIn }).add(scene)
  },

  remove({ sceneId, isLoggedIn }) {
    return getWishlistStorage({ isLoggedIn }).remove(sceneId)
  },
}
