import { defineStore } from 'pinia'
import { dramaApi } from '../api/services'
import { DEFAULT_PAGE_SIZE } from '../config/app'

export const useExploreStore = defineStore('explore', {
  state: () => ({
    mode: 'year',
    years: [],
    genres: [],
    selectedYear: null,
    selectedGenre: null,
    keyword: '',
    dramas: [],
    page: 0,
    totalPages: 1,
    hasNext: false,
    drama: null,
    scenes: [],
    loading: false,
    scenesLoading: false,
  }),
  actions: {
    async bootstrap() {
      this.loading = true
      try {
        const [years, genres] = await Promise.all([dramaApi.years(), dramaApi.genres()])
        this.years = [...years].sort((a, b) => b - a)
        this.genres = genres
        if (!this.selectedYear && this.years.length) this.selectedYear = this.years[0]
        if (this.selectedYear) await this.loadByYear(this.selectedYear)
      } finally {
        this.loading = false
      }
    },
    async loadPage(page = 0, append = false) {
      const params = { page, size: DEFAULT_PAGE_SIZE }
      let response
      if (this.keyword.trim()) response = await dramaApi.search(this.keyword.trim(), params)
      else if (this.mode === 'genre') response = await dramaApi.byGenre(this.selectedGenre, params)
      else response = await dramaApi.byYear(this.selectedYear, params)
      this.dramas = append ? [...this.dramas, ...response.items] : response.items
      this.page = response.page
      this.totalPages = response.totalPages
      this.hasNext = response.hasNext
    },
    async loadByYear(year) {
      this.mode = 'year'
      this.selectedYear = Number(year)
      this.keyword = ''
      this.loading = true
      try {
        await this.loadPage()
      } finally {
        this.loading = false
      }
    },
    async loadByGenre(genreId) {
      this.mode = 'genre'
      this.selectedGenre = genreId
      this.keyword = ''
      this.loading = true
      try {
        await this.loadPage()
      } finally {
        this.loading = false
      }
    },
    async search(keyword) {
      this.keyword = keyword
      if (!keyword.trim()) {
        return this.mode === 'year' ? this.loadByYear(this.selectedYear) : this.loadByGenre(this.selectedGenre)
      }
      this.loading = true
      try {
        await this.loadPage()
      } finally {
        this.loading = false
      }
    },
    async loadMore() {
      if (!this.hasNext || this.loading) return
      this.loading = true
      try {
        await this.loadPage(this.page + 1, true)
      } finally {
        this.loading = false
      }
    },
    async loadScenes(dramaId) {
      this.scenesLoading = true
      try {
        const result = await dramaApi.scenes(dramaId)
        this.drama = result.drama
        this.scenes = result.scenes
      } finally {
        this.scenesLoading = false
      }
    },
    async sceneDetail(sceneId) {
      const detail = await dramaApi.scene(sceneId)
      const index = this.scenes.findIndex((scene) => String(scene.sceneId) === String(sceneId))
      if (index >= 0) this.scenes[index] = { ...this.scenes[index], ...detail }
      return detail
    },
  },
})
