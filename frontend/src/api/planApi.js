import { publicApiClient } from './apiClient'
import { authApiClient } from './apiClient'

export const makePlan = (requestBody) => {
  return authApiClient.post('/plans', requestBody);
};

export const getPlans = () => {
  return authApiClient.get('/plans');
};

export const deletePlan = (planId) => {
  return authApiClient.delete(`/plans/${planId}`);
};