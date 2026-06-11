import { publicApiClient } from "./apiClient";

export const fetchSceneList = (dramaId) => publicApiClient.get(`/dramas/${dramaId}`);

export const getNearbyAttractions = (sceneId, params = {}) => {
  return publicApiClient.get(`/scenes/${sceneId}/nearby-attractions`, {
    params: {
      radiusKm: params.radiusKm ?? 3,
      page: params.page ?? 0,
      size: params.size ?? 10,
    },
  });
};