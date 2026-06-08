import { publicApiClient } from './apiClient'
import { authApiClient } from './apiClient'

export const makePlan = (requestBody) => {
  return authApiClient.post('/plans', requestBody);
};

export const getPlans = (requestBody) => {
  return authApiClient.get('/plans', requestBody);
};

export const deletePlan = (planId) => {
  return authApiClient.delete(`/plans/${planId}`);
};