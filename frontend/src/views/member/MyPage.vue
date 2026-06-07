<template>
  <div>
    <h1>MyPage</h1>

    <p v-if="isLoading">회원 정보를 불러오는 중...</p>

    <form @submit.prevent="handleUpdateMember">
      <div>
        <label>이메일</label>
        <input
          type="email"
          v-model="form.email"
        />
      </div>

      <div>
        <label>닉네임</label>
        <input
          type="text"
          v-model="form.nickname"
        />
      </div>

      <div>
        <label>현재 비밀번호</label>
        <input
          type="password"
          v-model="form.currentPassword"
          placeholder="회원 정보 변경 시 입력"
        />
      </div>

      <div>
        <label>새 비밀번호</label>
        <input
          type="password"
          v-model="form.newPassword"
          placeholder="변경할 새 비밀번호"
        />
      </div>

      <div>
        <label>새 비밀번호 확인</label>
        <input
          type="password"
          v-model="form.newPasswordConfirm"
          placeholder="새 비밀번호 확인"
        />
      </div>

      <p v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </p>

      <button type="submit" :disabled="isLoading">
        {{ isLoading ? '수정 중...' : '정보 수정' }}
      </button>
    </form>

    <hr />

    <button
      type="button"
      class="delete-button"
      :disabled="isLoading"
      @click="openDeleteModal"
    >
      계정 탈퇴
    </button>

    <div v-if="isDeleteModalOpen" class="modal-backdrop">
      <div class="modal">
        <h2>계정 탈퇴</h2>

        <p>계정을 탈퇴하려면 현재 비밀번호를 입력해주세요.</p>

        <div>
          <label>현재 비밀번호</label>
          <input
            type="password"
            v-model="withdrawalForm.password"
            placeholder="현재 비밀번호"
          />
        </div>

        <div>
          <label>탈퇴 사유</label>
          <textarea
            v-model="withdrawalForm.reason"
            placeholder="탈퇴 사유를 입력하세요"
          ></textarea>
        </div>

        <p v-if="deleteErrorMessage" class="error-message">
          {{ deleteErrorMessage }}
        </p>

        <button type="button" @click="closeDeleteModal">
          취소
        </button>

        <button
          type="button"
          class="delete-button"
          :disabled="isLoading"
          @click="handleDeleteAccount"
        >
          탈퇴하기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { reactive, ref, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { getMyPage, updateMyPage, deleteAccount } from '@/api/memberApi';
  import { useAuthStore } from '@/stores/authStore';

  const router = useRouter();
  const authStore = useAuthStore();

  const form = reactive({
    email: '',
    nickname: '',
    currentPassword: '',
    newPassword: '',
    newPasswordConfirm: '',
  });

  const withdrawalForm = reactive({
    password: '',
    reason: '',
  })

  const openDeleteModal = () => {
    deleteErrorMessage.value = '';
    withdrawalForm.password = '';
    withdrawalForm.reason = '';
    isDeleteModalOpen.value = true;
  }

  const closeDeleteModal = () => {
    isDeleteModalOpen.value = false;
    deleteErrorMessage.value = '';
    withdrawalForm.password = '';
    withdrawalForm.reason = '';
  }

  const originalMember = ref(null);
  const errorMessage = ref('');
  const isLoading = ref(false);
  const isDeleteModalOpen = ref(false);
  const deleteErrorMessage = ref('');

  const fetchMyPage = async() => {
    try {
      isLoading.value = true;
      errorMessage.value = '';

      const data = await getMyPage();
      originalMember.value = data;

      console.log('마이페이지 조회 성공: ', data);

      form.email = data.email;
      form.nickname = data.nickname;
    } catch(error) {
      console.error('마이페이지 조회 실패:', error);

      if (!error.status) {
        alert('서버와 연결할 수 없습니다.');
        router.replace('/drama');
        return;
      }

      const errorCode = error.errorCode;

      if (errorCode === 'UNAUTHORIZED') {
        alert('로그인이 필요합니다.');
        authStore.logout();
        router.replace('/login');
      } else if (errorCode === 'MEMBER_NOT_FOUND') {
        alert('회원 정보를 찾을 수 없습니다. 다시 로그인해주세요.');
        authStore.logout();
        router.replace('/login');
      } else if (errorCode === 'MEMBER_ALREADY_WITHDRAWN') {
        alert('이미 탈퇴 처리된 회원입니다.');
        authStore.logout();
        router.replace('/login');
      } else if (errorCode === 'MEMBER_ACCESS_DENIED') {
        alert('회원 정보에 접근할 권한이 없습니다.');
        router.replace('/drama');
      } else {
        alert('회원 정보를 불러오지 못했습니다.');
        router.replace('/drama');
      }
    } finally {
      isLoading.value = false;
    }
  }

  const handleUpdateMember = async () => {
    errorMessage.value = '';

    const requestBody = {};
    const email = form.email.trim();
    const nickname = form.nickname.trim();
    const currentPassword = form.currentPassword.trim();
    const newPassword = form.newPassword.trim();
    const newPasswordConfirm = form.newPasswordConfirm.trim();

    if (!email) {
      errorMessage.value = '이메일을 입력해주세요.';
      return;
    }

    if (!nickname) {
      errorMessage.value = '닉네임을 입력해주세요.';
      return;
    }

    if (email !== originalMember.value.email) {
      requestBody.email = email;
    }

    if (nickname !== originalMember.value.nickname) {
      requestBody.nickname = nickname;
    }

    if (newPassword || newPasswordConfirm) {
      if (!newPassword) {
        errorMessage.value = '새 비밀번호를 입력해주세요.';
        return;
      }

      if (!newPasswordConfirm) {
        errorMessage.value = '새 비밀번호 확인을 입력해주세요.';
        return;
      }

      if (newPassword !== newPasswordConfirm) {
        errorMessage.value = '새 비밀번호가 일치하지 않습니다.';
        return;
      }

      requestBody.newPassword = newPassword;
    }

    if (Object.keys(requestBody).length === 0) {
      errorMessage.value = '변경된 정보가 없습니다.';
      return;
    }

    if (!currentPassword) {
      errorMessage.value = '회원 정보를 변경하려면 현재 비밀번호를 입력해주세요.';
      return;
    }

    requestBody.currentPassword = currentPassword;

    try {
      isLoading.value = true;

      const data = await updateMyPage(requestBody);

      console.log('회원 정보 수정 성공:', data);

      alert('회원 정보가 수정되었습니다.');

      form.currentPassword = '';
      form.newPassword = '';
      form.newPasswordConfirm = '';

      await fetchMyPage();
    } catch (error) {
      console.error('회원 정보 수정 실패:', error);

      if (!error.status) {
        errorMessage.value = '서버와 연결할 수 없습니다.';
        return;
      }

      const errorCode = error.errorCode;

      if (errorCode === 'EMPTY_UPDATE_FIELDS') {
        errorMessage.value = '수정할 정보를 입력해주세요.';
        return;
      }

      if (errorCode === 'INVALID_EMAIL') {
        errorMessage.value = '이메일 형식이 올바르지 않습니다.';
        return;
      }

      if (errorCode === 'INVALID_PASSWORD') {
        errorMessage.value = '비밀번호 형식이 올바르지 않거나 현재 비밀번호가 일치하지 않습니다.';
        return;
      }

      if (errorCode === 'INVALID_NICKNAME') {
        errorMessage.value = '닉네임 형식이 올바르지 않습니다.';
        return;
      }

      if (errorCode === 'EMAIL_ALREADY_EXISTS') {
        errorMessage.value = '이미 사용 중인 이메일입니다.';
        return;
      }

      if (errorCode === 'INVALID_OR_EXPIRED_TOKEN') {
        alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
        authStore.logout();
        router.replace('/login');
        return;
      }

      if (errorCode === 'MEMBER_NOT_FOUND') {
        alert('회원 정보를 찾을 수 없습니다. 다시 로그인해주세요.');
        authStore.logout();
        router.replace('/login');
        return;
      }

      if (errorCode === 'MEMBER_ALREADY_WITHDRAWN') {
        alert('이미 탈퇴 처리된 계정입니다.');
        authStore.logout();
        router.replace('/login');
        return;
      }

      if (errorCode === 'MEMBER_ACCESS_DENIED') {
        alert('회원 정보에 접근할 권한이 없습니다.');
        router.replace('/drama');
        return;
      }

      errorMessage.value = '회원 정보 수정 중 오류가 발생했습니다.';
    } finally {
      isLoading.value = false;
    }
  }

  const handleDeleteAccount = async () => {
    deleteErrorMessage.value = '';

    const password = withdrawalForm.password;
    const reason = withdrawalForm.reason.trim();

    if (!password) {
      deleteErrorMessage.value = '현재 비밀번호를 입력해주세요.';
      return;
    }
    const confirmed = confirm('정말 계정을 탈퇴하시겠습니까?');

    if (!confirmed) {
      return;
    }

    const requestBody = {
      password,
    }

    if (reason) {
      requestBody.reason = reason;
    }

    try {
      isLoading.value = true;
      errorMessage.value = '';

      await deleteAccount(requestBody);

      alert('회원 탈퇴가 완료되었습니다.');

      authStore.logout();
      router.replace('/login');
    } catch (error) {
      console.error('회원 탈퇴 실패:', error);

      if (!error.status) {
        deleteErrorMessage.value = '서버와 연결할 수 없습니다.';
        return;
      }

      const errorCode = error.errorCode;

      if (errorCode === 'INVALID_OR_EXPIRED_TOKEN') {
        alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
        authStore.logout();
        router.replace('/login');
        return;
      }

      if (errorCode === 'MEMBER_ACCESS_DENIED') {
        alert('회원 탈퇴 권한이 없습니다.');
        router.replace('/drama');
        return;
      }

      if (errorCode === 'INVALID_WITHDRAWAL_REASON') {
        deleteErrorMessage.value = '탈퇴 사유 형식이 올바르지 않습니다.';
        return;
      }

      if (errorCode === 'MEMBER_NOT_FOUND') {
        alert('회원 정보를 찾을 수 없습니다. 다시 로그인해주세요.');
        authStore.logout();
        router.replace('/login');
        return;
      }

      if (errorCode === 'INVALID_PASSWORD') {
        deleteErrorMessage.value = '현재 비밀번호가 올바르지 않습니다.';
        return;
      }

      if (errorCode === 'MEMBER_ALREADY_WITHDRAWN') {
        alert('이미 탈퇴 처리된 회원입니다.');
        authStore.logout();
        router.replace('/login');
        return;
      }

      deleteErrorMessage.value = '회원 탈퇴 중 오류가 발생했습니다.';
    } finally {
      isLoading.value = false;
    }
  }

  onMounted(() => {
    fetchMyPage();
  })
</script>

<style scoped>
  .error-message {
    color: red;
  }
</style>