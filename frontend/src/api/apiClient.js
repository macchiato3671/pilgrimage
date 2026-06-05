import axios from 'axios'

const baseConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
}

/**
 * publicApiClient: 인증 토큰 붙이지 않는 api, authApiClient: 인증 토큰 붙이는 api
 */
export const publicApiClient = axios.create(baseConfig)
export const authApiClient = axios.create(baseConfig)

/**
 * 인증이 필요한 요청에 accessToken 자동 추가
 */
authApiClient.interceptors.request.use(
  (config) => {
    const accessToken = localStorage.getItem('accessToken')

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * 공통 응답 처리
 * axios 응답 전체가 아니라 백엔드 response body만 반환
 */
const handleResponse = (response) => {
  return response.data
}

/**
 * 공통 에러 객체 생성
 */
const createApiError = (error) => {
  const status = error.response?.status
  const data = error.response?.data

  const message =
    data?.message ||
    error.message ||
    '요청 처리 중 오류가 발생했습니다.'

  return {
    status,
    message,
    data,
    originalError: error,
  }
}

/**
 * public API 에러 처리
 * 로그인, 회원가입, 공개 조회 API 등
 */
const handlePublicError = (error) => {
  const apiError = createApiError(error)

  console.error('[Public API Error]', apiError.status, apiError.message)

  return Promise.reject(apiError)
}

/**
 * auth API 에러 처리
 * 인증 필요한 API에서 401 발생 시 토큰 제거 후 로그인 페이지 이동
 */
const handleAuthError = (error) => {
  const apiError = createApiError(error)

  console.error('[Auth API Error]', apiError.status, apiError.message)

  if (apiError.status === 401) {
    localStorage.removeItem('accessToken')

    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  }

  return Promise.reject(apiError)
}

publicApiClient.interceptors.response.use(handleResponse, handlePublicError)
authApiClient.interceptors.response.use(handleResponse, handleAuthError)