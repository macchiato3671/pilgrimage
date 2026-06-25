import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({ toasts: [] }),
  actions: {
    toast(message, type = 'info', duration = 3500) {
      const id = `${Date.now()}-${Math.random()}`
      this.toasts.push({ id, message, type })
      window.setTimeout(() => this.dismiss(id), duration)
    },
    dismiss(id) {
      this.toasts = this.toasts.filter((toast) => toast.id !== id)
    },
  },
})
