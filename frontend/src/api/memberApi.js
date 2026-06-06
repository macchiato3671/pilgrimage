import { publicApiClient } from './apiClient'

export const signup = (requestBody) => {
  return publicApiClient.post('/members', requestBody)
}

export const signin = (requestBody) => {
  return publicApiClient.post('/auth/login', requestBody)
}