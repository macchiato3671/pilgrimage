import { defineStore } from 'pinia'
import { toRaw } from 'vue'
import { STORAGE_KEYS } from '../config/app'
import { itemToDetail } from '../models/plan'
import { readStorage, writeStorage } from '../models/storage'
import { usePlansStore } from './plans'

const clone = (value) => (value == null ? value : JSON.parse(JSON.stringify(toRaw(value))))
const DAY_START = 9 * 60
const DEFAULT_GAP = 120
const EDGE_GAP = 60
const MAX_MINUTES = 23 * 60 + 59
const minutesOf = (time) => {
  const [hours = 9, minutes = 0] = String(time || '09:00').split(':').map(Number)
  return Math.max(0, Math.min(MAX_MINUTES, hours * 60 + minutes))
}
const timeOf = (minutes) => {
  const next = Math.max(0, Math.min(MAX_MINUTES, Math.round(minutes)))
  return `${String(Math.floor(next / 60)).padStart(2, '0')}:${String(next % 60).padStart(2, '0')}`
}

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
    orderedDayDetails(dayNo, excludeClientId = null) {
      return [...(this.activePlan?.details || [])]
        .filter((detail) => Number(detail.dayNo) === Number(dayNo) && detail.clientId !== excludeClientId)
        .sort((a, b) => minutesOf(a.beginTime) - minutesOf(b.beginTime))
    },
    suggestedTime(dayNo, beforeClientId = null, excludeClientId = null) {
      const details = this.orderedDayDetails(dayNo, excludeClientId)
      if (!details.length) return timeOf(DAY_START)
      if (!beforeClientId) return timeOf(minutesOf(details.at(-1).beginTime) + DEFAULT_GAP)
      const beforeIndex = details.findIndex((detail) => detail.clientId === beforeClientId)
      if (beforeIndex < 0) return timeOf(minutesOf(details.at(-1).beginTime) + DEFAULT_GAP)
      const before = minutesOf(details[beforeIndex].beginTime)
      const previous = beforeIndex > 0 ? minutesOf(details[beforeIndex - 1].beginTime) : null
      if (previous == null) return timeOf(before - EDGE_GAP)
      return timeOf(previous + Math.max(1, Math.floor((before - previous) / 2)))
    },
    addItem(item, dayNo, beforeClientId = null) {
      if (!this.activePlan) return
      const duplicate = this.activePlan.details.some(
        (detail) =>
          (item.kind === 'scene' && String(detail.sceneId) === String(item.sceneId)) ||
          (item.kind === 'place' && String(detail.placeId) === String(item.placeId)),
      )
      if (!duplicate) {
        const detail = itemToDetail(item, dayNo, this.suggestedTime(dayNo, beforeClientId))
        const before = this.activePlan.details.findIndex((candidate) => candidate.clientId === beforeClientId)
        this.activePlan.details.splice(before < 0 ? this.activePlan.details.length : before, 0, detail)
      }
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
      if (clientId === beforeClientId) return
      const index = this.activePlan.details.findIndex((detail) => detail.clientId === clientId)
      if (index < 0) return
      const [detail] = this.activePlan.details.splice(index, 1)
      detail.dayNo = Number(dayNo)
      detail.beginTime = this.suggestedTime(dayNo, beforeClientId, clientId)
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
