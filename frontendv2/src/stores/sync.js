import { defineStore } from 'pinia'
import { planApi, wishlistApi } from '../api/services'
import { clearGuest, readGuest, writeGuest } from '../models/guest'
import { usePlansStore } from './plans'
import { useEditorStore } from './editor'
import { useWishlistStore } from './wishlist'

export const useSyncStore = defineStore('sync', {
  state: () => ({
    running: false,
    current: 0,
    total: 0,
    logs: [],
    error: '',
    snapshot: readGuest(),
    planIdMap: {},
  }),
  getters: {
    progress: (state) => (state.total ? Math.round((state.current / state.total) * 100) : 0),
  },
  actions: {
    refresh() {
      this.snapshot = readGuest()
    },
    resolveNextPath(path = '/plans') {
      const next = String(path || '/plans')
      const match = next.match(/^\/plans\/([^/?#]+)/)
      if (!match) return next
      const oldPlanId = decodeURIComponent(match[1])
      const newPlanId = this.planIdMap[oldPlanId]
      if (newPlanId) return next.replace(match[1], encodeURIComponent(newPlanId))
      return oldPlanId.startsWith('guest-plan') ? '/plans' : next
    },
    log(message, type = 'ok') {
      this.logs.push({ message, type })
    },
    async run() {
      let guest = readGuest()
      this.snapshot = guest
      this.running = true
      this.error = ''
      this.logs = []
      this.current = 0
      this.total = guest.wishlist.length + guest.plans.length
      this.planIdMap = {}
      try {
        for (const scene of [...guest.wishlist]) {
          try {
            await wishlistApi.add(scene.sceneId)
            this.log(`‘${scene.name}’을(를) 위시리스트에 등록했습니다.`)
          } catch (error) {
            if (error.status !== 409) throw error
            this.log(`‘${scene.name}’은(는) 이미 등록되어 있어 건너뛰었습니다.`, 'skip')
          }
          guest.wishlist = guest.wishlist.filter((item) => String(item.sceneId) !== String(scene.sceneId))
          writeGuest(guest)
          this.snapshot = { ...guest }
          this.current += 1
        }

        const plansStore = usePlansStore()
        for (const localPlan of [...guest.plans]) {
          const created = await planApi.create(localPlan)
          if (localPlan.details.length) await planApi.syncDetails(created.planId, localPlan.details)
          this.planIdMap[String(localPlan.planId)] = String(created.planId)
          plansStore.assignColor(created.planId, localPlan.color)
          guest.plans = guest.plans.filter((plan) => String(plan.planId) !== String(localPlan.planId))
          writeGuest(guest)
          this.snapshot = { ...guest }
          this.current += 1
          this.log(`‘${localPlan.title}’ 여행 계획을 등록했습니다.`)
        }

        clearGuest()
        this.snapshot = { wishlist: [], plans: [] }
        useEditorStore().close()
        await Promise.all([useWishlistStore().load(), usePlansStore().load()])
        this.log('모든 로컬 작업을 안전하게 동기화했습니다.')
      } catch (error) {
        this.error = error.message
        this.log(`동기화가 중단되었습니다: ${error.message}`, 'error')
        throw error
      } finally {
        this.running = false
      }
    },
    discard() {
      clearGuest()
      this.snapshot = { wishlist: [], plans: [] }
      this.planIdMap = {}
      useEditorStore().close()
      this.logs = [{ message: '로컬 작업을 삭제했습니다.', type: 'skip' }]
    },
  },
})
