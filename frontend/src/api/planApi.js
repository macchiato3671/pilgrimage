import { publicApiClient } from './apiClient'
import { authApiClient } from './apiClient'

export const makePlan = (requestBody) => {
  return authApiClient.post('/plans', requestBody);
};