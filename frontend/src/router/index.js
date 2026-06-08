import { createRouter, createWebHistory } from 'vue-router'
import SignupView from '@/views/member/SignupView.vue'
import LoginView from '@/views/member/LoginView.vue'
import DramaView from '@/views/drama/DramaView.vue'
import WishlistView from '@/views/wishlist/SampleWishlist.vue'
import PlanListView from '@/views/plan/SamplePlan.vue'
import MyPageView from '@/views/member/MyPage.vue'
import TestMapView from '@/views/test/TestMapView.vue'
import PlanView from '@/views/plan/PlanView.vue'
import PlanDetailView from '@/views/plan/PlanDetailView.vue'
import TempPlanView from '@/views/plan/TempPlanView.vue'
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
    path: '/plans',
    name: 'plans',
    component: PlanListView
  },
  {
    path: '/mypage',
    name: 'mypage',
    component: MyPageView
  },
  {
    path: '/map-test',
    name: 'map-test',
    component: TestMapView,
  },
  {
    path: '/plans/create',
    name: 'planCreate',
    component: PlanView,
  },
  {
    path: '/plans/:planId',
    name: 'planDetail',
    component: PlanDetailView,
  },
  {
    path: '/plans/local/:localPlanId',
    name: 'localPlanDetail',
    component: PlanDetailView,
  },
  {
    path: '/search',
    name: 'PlanButtonTemp',
    component: TempPlanView,
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
