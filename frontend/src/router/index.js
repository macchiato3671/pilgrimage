import { createRouter, createWebHistory } from 'vue-router'
import SignupView from '@/views/member/SignupView.vue'
import LoginView from '@/views/member/LoginView.vue'
import DramaView from '@/views/drama/DramaView.vue'
import WishlistView from '@/views/wishlist/SampleWishlist.vue'
import MyPageView from '@/views/member/MyPage.vue'
import TestMapView from '@/views/test/TestMapView.vue'
import PlanView from '@/views/plan/PlanView.vue'
import PlanDetailView from '@/views/plan/PlanDetailView.vue'
import TempPlanView from '@/views/plan/TempPlanView.vue'
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
