import { authApiClient } from '@/api/apiClient'
import { localApiClient } from '@/api/localClient'

const getPlanStorage = ({ isLoggedIn }) => {
  if (isLoggedIn) {
    return {
      add: (requestBody) => authApiClient.post('/plans', requestBody),
      fetch: () => authApiClient.get('/plans'),
      fetchDetail: async (planId) => {
        const plan = await authApiClient.get(`/plans/${planId}`)
        return { plan }
      },
      update: (planId, requestBody) => authApiClient.put(`/plans/${planId}`, requestBody),
      remove: (planId) => authApiClient.delete(`/plans/${planId}`),
    }
  }

  return {
    add: (requestBody) => localApiClient.post('/plans', requestBody),
    fetch: () => localApiClient.get('/plans'),
    fetchDetail: async (planId) => {
      const plan = await localApiClient.get(`/plans/${planId}`)
      return { plan }
    },
    update: (planId, requestBody) => localApiClient.put(`/plans/${planId}`, requestBody),
    remove: (planId) => localApiClient.delete(`/plans/${planId}`),
  }
}

export const planService = {
  getId(plan) {
    return plan?.planId ?? plan?.localPlanId
  },

  add({ requestBody, isLoggedIn }) {
    return getPlanStorage({ isLoggedIn }).add(requestBody)
  },

  fetch({ isLoggedIn }) {
    return getPlanStorage({ isLoggedIn }).fetch()
  },

  fetchDetail({ planId, isLoggedIn }) {
    return getPlanStorage({ isLoggedIn }).fetchDetail(planId)
  },

  update({planId, requestBody, isLoggedIn}) {
    return getPlanStorage({ isLoggedIn }).update(planId, requestBody)
  },

  remove({ plan, isLoggedIn }) {
    return getPlanStorage({ isLoggedIn }).remove(this.getId(plan))
  },
}
