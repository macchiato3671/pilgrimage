import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({ toasts: [], sidebarCollapsed: false, routePanelOpen: true }),
  actions: {
    toast(message, type = 'info', duration = 3500) {
      const id = `${Date.now()}-${Math.random()}`
      this.toasts.push({ id, message, type })
      window.setTimeout(() => this.dismiss(id), duration)
    },
    dismiss(id) {
      this.toasts = this.toasts.filter((toast) => toast.id !== id)
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    openRoutePanel() {
      this.routePanelOpen = true
    },
    closeRoutePanel() {
      this.routePanelOpen = false
    },
  },
})
