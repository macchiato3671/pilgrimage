import { publicApiClient } from './apiClient'

export const signup = (requestBody) => {
  return publicApiClient.post('/members', requestBody)
}