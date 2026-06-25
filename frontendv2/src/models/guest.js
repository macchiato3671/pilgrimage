import { STORAGE_KEYS } from '../config/app'
import { readStorage, writeStorage } from './storage'

const EMPTY_GUEST = { wishlist: [], plans: [] }

export const readGuest = () => ({ ...EMPTY_GUEST, ...(readStorage(STORAGE_KEYS.guest, EMPTY_GUEST) || {}) })
export const writeGuest = (guest) => writeStorage(STORAGE_KEYS.guest, guest)
export const clearGuest = () => localStorage.removeItem(STORAGE_KEYS.guest)
export const hasGuestWork = () => {
  const guest = readGuest()
  return Boolean(guest.wishlist.length || guest.plans.length)
}
