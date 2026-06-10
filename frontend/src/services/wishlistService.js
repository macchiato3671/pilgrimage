import { addWishlist, deleteWishlist, getWishlist } from '@/api/wishlistApi'
import { localApiClient } from '@/api/localClient'

export const fetchWishlists = async ({ isLoggedIn }) => {
  if (isLoggedIn) {
    return getWishlist()
  }

  return localApiClient.get('/wishlist')
}

export const addSceneToWishlist = async ({ scene, isLoggedIn }) => {
  if (isLoggedIn) {
    return addWishlist(scene.sceneId)
  }

  return localApiClient.post(`/wishlist/${scene.sceneId}`, scene)
}

export const removeSceneFromWishlist = async ({ sceneId, isLoggedIn }) => {
  if (isLoggedIn) {
    return deleteWishlist(sceneId)
  }

  return localApiClient.delete(`/wishlist/${sceneId}`)
}

export const getWishlistSceneIds = (wishlists = []) => {
  return wishlists
    .map((wish) => wish.scene?.sceneId)
    .filter((sceneId) => sceneId !== undefined && sceneId !== null)
    .map((sceneId) => String(sceneId))
}
