import { defineStore } from 'pinia'
import { wishlistApi } from '../api/services'
import { readGuest, writeGuest } from '../models/guest'
import { useAuthStore } from './auth'

export const useWishlistStore = defineStore('wishlist', {
  state: () => ({ items: [], loading: false }),
  getters: {
    ids: (state) => new Set(state.items.map((item) => String(item.sceneId))),
    grouped: (state) =>
      Object.values(
        state.items.reduce((groups, scene) => {
          const key = scene.dramaId || scene.dramaTitle || 'unknown'
          groups[key] ||= { dramaId: scene.dramaId, title: scene.dramaTitle || '작품 정보 없음', scenes: [] }
          groups[key].scenes.push(scene)
          return groups
        }, {}),
      ),
  },
  actions: {
    has(sceneId) {
      return this.ids.has(String(sceneId))
    },
    persistGuest() {
      const guest = readGuest()
      writeGuest({ ...guest, wishlist: this.items })
    },
    async load() {
      const auth = useAuthStore()
      this.loading = true
      try {
        if (!auth.isAuthenticated) {
          this.items = readGuest().wishlist
          return
        }
        this.items = await wishlistApi.list()
        if (this.items.length && this.items.every((scene) => !scene.dramaTitle)) {
          const dramas = await wishlistApi.dramas()
          const groups = await Promise.all(
            dramas.map(async (drama) =>
              (await wishlistApi.scenesForDrama(drama.dramaId)).map((scene) => ({
                ...scene,
                dramaId: drama.dramaId,
                dramaTitle: drama.title,
              })),
            ),
          )
          this.items = groups.flat()
        }
      } finally {
        this.loading = false
      }
    },
    async add(scene) {
      if (this.has(scene.sceneId)) return
      const auth = useAuthStore()
      this.items.push(scene)
      if (!auth.isAuthenticated) return this.persistGuest()
      try {
        await wishlistApi.add(scene.sceneId)
      } catch (error) {
        this.items = this.items.filter((item) => String(item.sceneId) !== String(scene.sceneId))
        throw error
      }
    },
    async remove(sceneId) {
      const auth = useAuthStore()
      const previous = this.items
      this.items = previous.filter((item) => String(item.sceneId) !== String(sceneId))
      if (!auth.isAuthenticated) return this.persistGuest()
      try {
        await wishlistApi.remove(sceneId)
      } catch (error) {
        this.items = previous
        throw error
      }
    },
    toggle(scene) {
      return this.has(scene.sceneId) ? this.remove(scene.sceneId) : this.add(scene)
    },
  },
})
