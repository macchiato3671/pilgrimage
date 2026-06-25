import { defineStore } from 'pinia'
import { STORAGE_KEYS } from '../config/app'
import { itemToDetail } from '../models/plan'
import { readStorage, writeStorage } from '../models/storage'
import { usePlansStore } from './plans'

const clone = (value) => structuredClone(value)

export const useEditorStore = defineStore('editor', {
  state: () => ({
    activePlan: null,
    minimized: false,
    dirty: false,
    dragging: false,
    saving: false,
  }),
  getters: {
    isOpen: (state) => Boolean(state.activePlan),
  },
  actions: {
    restore() {
      const draft = readStorage(STORAGE_KEYS.editor)
      if (draft?.activePlan) Object.assign(this, draft)
    },
    persist() {
      writeStorage(
        STORAGE_KEYS.editor,
        this.activePlan
          ? { activePlan: this.activePlan, minimized: this.minimized, dirty: this.dirty, dragging: false }
          : null,
      )
    },
    async open(plan) {
      const plans = usePlansStore()
      this.activePlan = clone((await plans.getById(plan.planId)) || plan)
      this.minimized = false
      this.dirty = false
      this.persist()
    },
    close() {
      this.activePlan = null
      this.minimized = false
      this.dirty = false
      this.persist()
    },
    toggleMinimize() {
      this.minimized = !this.minimized
      this.persist()
    },
    beginExternalDrag() {
      this.dragging = true
      if (this.activePlan) this.minimized = false
      this.persist()
    },
    endDrag() {
      this.dragging = false
    },
    markDirty() {
      this.dirty = true
      this.persist()
    },
    updatePlan(plan) {
      this.activePlan = clone(plan)
      this.markDirty()
    },
    addItem(item, dayNo) {
      if (!this.activePlan) return
      const duplicate = this.activePlan.details.some(
        (detail) =>
          (item.kind === 'scene' && String(detail.sceneId) === String(item.sceneId)) ||
          (item.kind === 'place' && String(detail.placeId) === String(item.placeId)),
      )
      if (!duplicate) this.activePlan.details.push(itemToDetail(item, dayNo))
      this.markDirty()
    },
    removeDetail(clientId) {
      this.activePlan.details = this.activePlan.details.filter((detail) => detail.clientId !== clientId)
      this.markDirty()
    },
    updateTime(clientId, beginTime) {
      const detail = this.activePlan.details.find((item) => item.clientId === clientId)
      if (detail) detail.beginTime = beginTime
      this.markDirty()
    },
    moveDetail(clientId, dayNo, beforeClientId = null) {
      const index = this.activePlan.details.findIndex((detail) => detail.clientId === clientId)
      if (index < 0) return
      const [detail] = this.activePlan.details.splice(index, 1)
      detail.dayNo = Number(dayNo)
      if (beforeClientId) {
        const before = this.activePlan.details.findIndex((item) => item.clientId === beforeClientId)
        this.activePlan.details.splice(before < 0 ? this.activePlan.details.length : before, 0, detail)
      } else this.activePlan.details.push(detail)
      this.markDirty()
    },
    async save() {
      if (!this.activePlan) return null
      this.saving = true
      try {
        const plans = usePlansStore()
        const saved = await plans.saveDetails(this.activePlan)
        this.activePlan = clone(saved)
        this.dirty = false
        this.persist()
        return saved
      } finally {
        this.saving = false
      }
    },
  },
})
