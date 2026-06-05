import { createRouter, createWebHistory } from 'vue-router'
import SignupView from '@/views/member/SignupView.vue'
import LoginView from '@/views/member/LoginView.vue'
//import HomeView from '@/views/HomeView.vue'

const routes = [
  {
    path: '/signup',
    name: 'signup',
    component: SignupView
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router