let sdkPromise

export const loadKakaoMaps = () => {
  if (window.kakao?.maps) return Promise.resolve(window.kakao)
  if (sdkPromise) return sdkPromise
  const appKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY
  if (!appKey) return Promise.reject(new Error('KAKAO_KEY_MISSING'))

  sdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.async = true
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false`
    script.onload = () => window.kakao.maps.load(() => resolve(window.kakao))
    script.onerror = () => reject(new Error('KAKAO_SDK_LOAD_FAILED'))
    document.head.appendChild(script)
  })
  return sdkPromise
}
