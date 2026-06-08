import { publicApiClient } from "./apiClient";

export const fetchSceneList = (dramaId) => publicApiClient.get(`/dramas/${dramaId}`)