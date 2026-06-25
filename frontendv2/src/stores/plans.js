import { defineStore } from 'pinia'
import { toRaw } from 'vue'
import { dramaApi, placeApi, planApi } from '../api/services'
import { PLAN_COLORS, STORAGE_KEYS } from '../config/app'
import { readGuest, writeGuest } from '../models/guest'
import { makeId, readStorage, writeStorage } from '../models/storage'
import { useAuthStore } from './auth'

const clone = (value) => (value == null ? value : JSON.parse(JSON.stringify(toRaw(value))))

export const usePlansStore = defineStore('plans', {
  state: () => ({
    items: [],
    colors: readStorage(STORAGE_KEYS.colors, {}) || {},
    loading: false,
  }),
  getters: {
    colorFor: (state) => (planId, index = 0) => state.colors[planId] || PLAN_COLORS[index % PLAN_COLORS.length],
  },
  actions: {
    assignColor(planId, color) {
      if (!planId) return
      this.colors[planId] = color || this.colorFor(planId, Object.keys(this.colors).length)
      writeStorage(STORAGE_KEYS.colors, this.colors)
    },
    decorate(plan, index = 0) {
      const color = plan.color || this.colorFor(plan.planId, index)
      this.assignColor(plan.planId, color)
      return { ...plan, color }
    },
    persistGuest() {
      const guest = readGuest()
      writeGuest({ ...guest, plans: this.items })
    },
    replace(plan) {
      const index = this.items.findIndex((item) => String(item.planId) === String(plan.planId))
      if (index < 0) this.items.unshift(plan)
      else this.items[index] = plan
    },
    async load() {
      const auth = useAuthStore()
      this.loading = true
      try {
        const plans = auth.isAuthenticated ? await planApi.list() : readGuest().plans
        this.items = plans.map((plan, index) => this.decorate(plan, index))
      } finally {
        this.loading = false
      }
    },
    async hydrateDetails(plan) {
      const details = await Promise.all(
        plan.details.map(async (detail) => {
          if (detail.item) return detail
          try {
            const item = detail.sceneId
              ? await dramaApi.scene(detail.sceneId)
              : await placeApi.detail(detail.placeId)
            return { ...detail, item }
          } catch {
            return {
              ...detail,
              item: detail.sceneId
                ? { kind: 'scene', sceneId: detail.sceneId, name: `촬영지 #${detail.sceneId}`, images: [] }
                : { kind: 'place', placeId: detail.placeId, name: `장소 #${detail.placeId}`, images: [] },
            }
          }
        }),
      )
      return { ...plan, details }
    },
    async getById(planId, force = false) {
      const auth = useAuthStore()
      let plan = this.items.find((item) => String(item.planId) === String(planId))
      if (!auth.isAuthenticated) {
        plan ||= readGuest().plans.find((item) => String(item.planId) === String(planId))
      } else if (force || !plan?.details?.length) {
        plan = await planApi.detail(planId)
      }
      if (!plan) return null
      plan = this.decorate(await this.hydrateDetails(plan))
      this.replace(plan)
      return clone(plan)
    },
    async create(form) {
      const auth = useAuthStore()
      const color = form.color || PLAN_COLORS[this.items.length % PLAN_COLORS.length]
      let plan
      if (auth.isAuthenticated) {
        plan = await planApi.create(form)
      } else {
        plan = {
          ...form,
          planId: makeId('guest-plan'),
          details: [],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      }
      plan = this.decorate({ ...plan, color })
      this.items.unshift(plan)
      if (!auth.isAuthenticated) this.persistGuest()
      return clone(plan)
    },
    async updateInfo(planId, form) {
      const auth = useAuthStore()
      const current = this.items.find((item) => String(item.planId) === String(planId))
      let updated
      if (auth.isAuthenticated) updated = await planApi.update(planId, form)
      else updated = { ...current, ...form, updatedAt: new Date().toISOString() }
      updated = this.decorate({ ...current, ...updated, color: form.color || current?.color })
      this.assignColor(planId, updated.color)
      this.replace(updated)
      if (!auth.isAuthenticated) this.persistGuest()
      return clone(updated)
    },
    async remove(planId) {
      const auth = useAuthStore()
      if (auth.isAuthenticated) await planApi.remove(planId)
      this.items = this.items.filter((plan) => String(plan.planId) !== String(planId))
      delete this.colors[planId]
      writeStorage(STORAGE_KEYS.colors, this.colors)
      if (!auth.isAuthenticated) this.persistGuest()
    },
    async saveDetails(plan) {
      const auth = useAuthStore()
      if (!auth.isAuthenticated) {
        const saved = { ...plan, updatedAt: new Date().toISOString() }
        this.replace(saved)
        this.persistGuest()
        return clone(saved)
      }
      await planApi.syncDetails(plan.planId, plan.details)
      return this.getById(plan.planId, true)
    },
  },
})
