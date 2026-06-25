export const readStorage = (key, fallback = null) => {
  try {
    const raw = localStorage.getItem(key)
    return raw == null ? fallback : JSON.parse(raw)
  } catch {
    return fallback
  }
}

export const writeStorage = (key, value) => {
  if (value == null) localStorage.removeItem(key)
  else localStorage.setItem(key, JSON.stringify(value))
}

export const makeId = (prefix = 'local') =>
  `${prefix}-${globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`}`
