import http from './http'

export const translateScreenTexts = async ({ targetLanguage, texts }) => {
  const { data } = await http.post('/translate', { targetLanguage, texts }, { timeout: 45000 })
  return Array.isArray(data?.translations) ? data.translations : []
}
