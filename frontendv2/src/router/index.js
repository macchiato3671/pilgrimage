import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import { hasGuestWork } from '../models/guest'
import { useAuthStore } from '../stores/auth'
import DramaScenesView from '../views/DramaScenesView.vue'
import DramasView from '../views/DramasView.vue'
import LoginView from '../views/LoginView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import PlacesView from '../views/PlacesView.vue'
import PlanDetailView from '../views/PlanDetailView.vue'
import PlansView from '../views/PlansView.vue'
import SignupView from '../views/SignupView.vue'
import SyncView from '../views/SyncView.vue'
import WishlistView from '../views/WishlistView.vue'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: AppShell,
      children: [
        { path: '', redirect: '/dramas' },
        { path: 'dramas', name: 'dramas', component: DramasView },
        { path: 'dramas/:dramaId/scenes', name: 'drama-scenes', component: DramaScenesView },
        { path: 'places', name: 'places', component: PlacesView },
        { path: 'wishlist', name: 'wishlist', component: WishlistView },
        { path: 'plans', name: 'plans', component: PlansView },
        { path: 'plans/:planId', name: 'plan-detail', component: PlanDetailView },
      ],
    },
    { path: '/login', name: 'login', component: LoginView, meta: { guestOnly: true, allowPendingGuest: true } },
    { path: '/signup', name: 'signup', component: SignupView, meta: { guestOnly: true, allowPendingGuest: true } },
    { path: '/sync', name: 'sync', component: SyncView, meta: { requiresAuth: true, allowPendingGuest: true } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView, meta: { allowPendingGuest: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) return { name: 'login', query: { next: to.fullPath } }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return hasGuestWork() ? { name: 'sync', query: { next: String(to.query.next || '/dramas') } } : '/dramas'
  }
  if (auth.isAuthenticated && hasGuestWork() && !to.meta.allowPendingGuest) {
    return { name: 'sync', query: { next: to.fullPath } }
  }
  return true
})

export default router
