import { defineStore } from 'pinia'
import { authApi } from '../api/services'
import { STORAGE_KEYS } from '../config/app'
import { hasGuestWork } from '../models/guest'
import { readStorage, writeStorage } from '../models/storage'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    session: readStorage(STORAGE_KEYS.auth),
    busy: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.session?.accessToken),
    member: (state) => state.session?.member || null,
    hasPendingGuestWork: () => hasGuestWork(),
  },
  actions: {
    persist(session) {
      this.session = session
      writeStorage(STORAGE_KEYS.auth, session)
    },
    async login(credentials) {
      this.busy = true
      try {
        const session = await authApi.login(credentials)
        this.persist(session)
        if (!session.member?.memberId) await this.loadMe().catch(() => null)
        return this.session
      } finally {
        this.busy = false
      }
    },
    async register(form) {
      this.busy = true
      try {
        await authApi.register(form)
        return this.login({ email: form.email, password: form.password })
      } finally {
        this.busy = false
      }
    },
    async loadMe() {
      if (!this.isAuthenticated) return null
      const member = await authApi.me()
      this.persist({ ...this.session, member })
      return member
    },
    async updateMe(form) {
      if (!this.isAuthenticated) return null
      this.busy = true
      try {
        const member = await authApi.updateMe(form)
        this.persist({ ...this.session, member })
        return member
      } finally {
        this.busy = false
      }
    },
    async removeMe(request) {
      if (!this.isAuthenticated) return
      this.busy = true
      try {
        await authApi.removeMe(request)
        this.logout()
      } finally {
        this.busy = false
      }
    },
    logout() {
      this.session = null
      writeStorage(STORAGE_KEYS.auth, null)
    },
  },
})
