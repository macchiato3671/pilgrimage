import { publicApiClient } from './apiClient'
import { authApiClient } from './apiClient'

export const signup = (requestBody) => {
  return publicApiClient.post('/members', requestBody)
}

export const signin = (requestBody) => {
  return publicApiClient.post('/auth/login', requestBody)
}

export const getMyPage = (requestBody) => {
  return authApiClient.get('/me', requestBody);
};

export const updateMyPage = (requestBody) => {
  return authApiClient.patch('/me', requestBody);
};

export const deleteAccount = (requestBody) => {
  return authApiClient.delete('/me', requestBody);
};