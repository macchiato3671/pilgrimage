import axios from 'axios'
import { STORAGE_KEYS } from '../config/app'
import { readStorage } from '../models/storage'

const http = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/$/, ''),
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const auth = readStorage(STORAGE_KEYS.auth)
  if (auth?.accessToken) config.headers.Authorization = `${auth.tokenType || 'Bearer'} ${auth.accessToken}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401) window.dispatchEvent(new CustomEvent('pilgrimage:unauthorized'))
    const body = error.response?.data
    const normalized = new Error(
      body?.message || body?.errorMessage || body?.error || error.message || '요청을 처리하지 못했습니다.',
    )
    normalized.status = status
    normalized.code = body?.code || body?.errorCode
    normalized.payload = body
    return Promise.reject(normalized)
  },
)

export default http
