import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null);
  const member = ref(null);

  const isLoggedIn = computed(() => !!accessToken.value);

  const login = (loginData) => {
    accessToken.value = loginData.accessToken;
    member.value = loginData.member || null;
  };

  const logout = () => {
    accessToken.value = null;
    member.value = null;
  };

  return {
    accessToken,
    member,
    isLoggedIn,
    login,
    logout,
  };
});