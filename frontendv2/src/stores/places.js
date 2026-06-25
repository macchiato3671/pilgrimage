import { defineStore } from 'pinia'
import { placeApi } from '../api/services'
import { DEFAULT_PAGE_SIZE, SEOUL_CENTER } from '../config/app'

export const usePlacesStore = defineStore('places', {
  state: () => ({
    items: [],
    keyword: '',
    category: null,
    sceneId: null,
    page: 0,
    hasNext: false,
    loading: false,
  }),
  actions: {
    async search({ keyword = '', category = null, center = SEOUL_CENTER, sceneId = null, page = 0 } = {}) {
      this.loading = true
      this.keyword = keyword
      this.category = category
      this.sceneId = sceneId
      try {
        if (sceneId) {
          this.items = await placeApi.nearby(sceneId, {
            keyword: keyword || undefined,
            contentTypeId: category || undefined,
          })
          this.hasNext = false
          return
        }
        const response = await placeApi.search({
          keyword: keyword || undefined,
          contentTypeId: category || undefined,
          latitude: center?.latitude,
          longitude: center?.longitude,
          page,
          size: DEFAULT_PAGE_SIZE,
        })
        this.items = page ? [...this.items, ...response.items] : response.items
        this.page = response.page
        this.hasNext = response.hasNext
      } finally {
        this.loading = false
      }
    },
    async detail(placeId) {
      const detail = await placeApi.detail(placeId)
      const index = this.items.findIndex((item) => String(item.placeId) === String(placeId))
      if (index >= 0) this.items[index] = { ...this.items[index], ...detail }
      return detail
    },
  },
})
