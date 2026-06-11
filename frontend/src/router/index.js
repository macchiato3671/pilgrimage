import { createRouter, createWebHistory } from 'vue-router'
import SignupView from '@/views/member/SignupView.vue'
import LoginView from '@/views/member/LoginView.vue'
import DramaView from '@/views/drama/DramaView.vue'
import WishlistView from '@/views/wishlist/WishlistView.vue'
import MyPageView from '@/views/member/MyPage.vue'
import TestSceneCard from '@/views/test/TestSceneCard.vue'
import SceneList from '@/views/scene/SceneList.vue'
import PlanView from '@/views/plan/PlanView.vue'
import PlanDetailView from '@/views/plan/PlanDetailView.vue'
import PlanList from '@/views/plan/PlanList.vue'

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
  {
    path: '/drama',
    name: 'drama',
    component: DramaView
  },
  {
    path: '/wishlist',
    name: 'wishlist',
    component: WishlistView
  },
  {
    path: '/mypage',
    name: 'mypage',
    component: MyPageView
  },
  {
    path: '/scene-card-test',
    name: 'scene-card-test',
    component: TestSceneCard,
  },
  {
    path: '/dramas/:dramaId/scenes',
    name: 'scenes',
    component: SceneList,
  },
  {
    path: '/plans/create',
    name: 'planCreate',
    component: PlanView,
  },
  {
    path: '/plans/detail',
    name: 'planDetailCreate',
    component: PlanDetailView,
  },
  {
    path: '/plans',
    name: 'planList',
    component: PlanList,
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
