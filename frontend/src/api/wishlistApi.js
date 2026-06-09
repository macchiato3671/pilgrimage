import { authApiClient } from './apiClient'

export const getWishlist = () => {
  return authApiClient.get('/wishlist');
};

export const addWishlist = (sceneId) => {
  return authApiClient.post(`/wishlist/${sceneId}`);
};

export const deleteWishlist = (sceneId) => {
  return authApiClient.delete(`/wishlist/${sceneId}`);
};
