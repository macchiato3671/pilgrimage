import { defineStore } from 'pinia'
import { placeApi } from '../api/services'
import { DEFAULT_PAGE_SIZE, SEOUL_CENTER } from '../config/app'

export const usePlacesStore = defineStore('places', {
  state: () => ({
    items: [],
    mapItems: [],
    keyword: '',
    category: null,
    sceneId: null,
    page: 0,
    hasNext: false,
    loading: false,
  }),
  actions: {
    mergeItems(nextItems) {
      const byId = new Map(this.items.map((item) => [String(item.placeId), item]))
      nextItems.forEach((item) => byId.set(String(item.placeId), item))
      this.items = [...byId.values()]
    },
    async search({
      keyword = '',
      category = null,
      center = SEOUL_CENTER,
      sceneId = null,
      page = 0,
      useCenter = true,
      useCategory = true,
    } = {}) {
      this.loading = true
      this.keyword = keyword
      this.category = useCategory ? category : null
      this.sceneId = sceneId
      try {
        if (sceneId && category && !keyword.trim() && useCenter && useCategory) {
          const response = await placeApi.nearby(sceneId, {
            contentTypeId: category || undefined,
            page,
            size: DEFAULT_PAGE_SIZE,
          })
          if (page) this.mergeItems(response.items)
          else this.items = response.items
          this.mapItems = response.items
          this.page = response.page
          this.hasNext = response.hasNext
          return
        }
        const response = await placeApi.search({
          keyword: keyword || undefined,
          contentTypeId: useCategory ? category || undefined : undefined,
          latitude: useCenter ? center?.latitude : undefined,
          longitude: useCenter ? center?.longitude : undefined,
          page,
          size: DEFAULT_PAGE_SIZE,
        })
        if (page) this.mergeItems(response.items)
        else this.items = response.items
        this.mapItems = response.items
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
      const mapIndex = this.mapItems.findIndex((item) => String(item.placeId) === String(placeId))
      if (mapIndex >= 0) this.mapItems[mapIndex] = { ...this.mapItems[mapIndex], ...detail }
      return detail
    },
  },
})
