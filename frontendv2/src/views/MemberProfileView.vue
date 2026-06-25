<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/common/AppIcon.vue'
import LoadingState from '../components/common/LoadingState.vue'
import { useAuthStore } from '../stores/auth'
import { useEditorStore } from '../stores/editor'
import { useUiStore } from '../stores/ui'

const auth = useAuthStore()
const editor = useEditorStore()
const ui = useUiStore()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const withdrawOpen = ref(false)

const form = reactive({
  email: '',
  nickname: '',
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const withdraw = reactive({ password: '', reason: '' })

const fillMember = () => {
  form.email = auth.member?.email || ''
  form.nickname = auth.member?.nickname || ''
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  error.value = ''
}

const save = async () => {
  error.value = ''
  if (!form.email.trim() || !form.nickname.trim()) return (error.value = '이메일과 닉네임을 입력해 주세요.')
  if (!form.currentPassword) return (error.value = '회원정보를 수정하려면 현재 비밀번호가 필요합니다.')
  if (form.newPassword && form.newPassword !== form.confirmPassword) return (error.value = '새 비밀번호가 일치하지 않습니다.')
  try {
    await auth.updateMe({
      email: form.email.trim(),
      nickname: form.nickname.trim(),
      currentPassword: form.currentPassword,
      newPassword: form.newPassword || form.currentPassword,
    })
    fillMember()
    ui.toast('회원정보를 수정했습니다.', 'success')
  } catch (reason) {
    error.value = reason.message
    ui.toast(reason.message, 'error')
  }
}

const removeAccount = async () => {
  if (!withdraw.password) return ui.toast('탈퇴하려면 비밀번호를 입력해 주세요.', 'error')
  if (!window.confirm('정말로 회원 탈퇴를 진행할까요? 저장된 계정 데이터에 접근할 수 없게 됩니다.')) return
  try {
    await auth.removeMe({ password: withdraw.password, reason: withdraw.reason })
    editor.close()
    ui.toast('회원 탈퇴가 완료되었습니다.', 'success')
    router.push('/dramas')
  } catch (reason) {
    ui.toast(reason.message, 'error')
  }
}

watch(() => auth.member, fillMember, { deep: true })
onMounted(async () => {
  loading.value = true
  try {
    await auth.loadMe()
    fillMember()
  } catch (reason) {
    ui.toast(reason.message, 'error')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <LoadingState v-if="loading" />
  <div v-else class="profile-page">
    <section class="content-panel profile-panel">
      <header class="page-heading compact-heading">
        <span class="eyebrow">MEMBER</span>
        <h1>회원정보</h1>
        <p>계정 정보와 로그인 정보를 현재 디자인 안에서 관리합니다.</p>
      </header>

      <form class="profile-form" @submit.prevent="save">
        <div class="profile-card">
          <div class="profile-avatar-large">{{ auth.member?.nickname?.slice(0, 1) || '여' }}</div>
          <div>
            <span class="eyebrow">SIGNED IN</span>
            <h2>{{ auth.member?.nickname || '여행자' }}</h2>
            <p>{{ auth.member?.email }}</p>
          </div>
        </div>

        <label class="field"><span>이메일</span><input v-model.trim="form.email" type="email" autocomplete="email" required /></label>
        <label class="field"><span>닉네임</span><input v-model.trim="form.nickname" maxlength="255" autocomplete="nickname" required /></label>
        <label class="field"><span>현재 비밀번호</span><input v-model="form.currentPassword" type="password" autocomplete="current-password" /></label>
        <label class="field"><span>새 비밀번호</span><input v-model="form.newPassword" type="password" autocomplete="new-password" placeholder="변경하지 않으려면 비워 두세요" /></label>
        <label class="field"><span>새 비밀번호 확인</span><input v-model="form.confirmPassword" type="password" autocomplete="new-password" /></label>

        <p v-if="error" class="form-error"><AppIcon name="alert" :size="16" />{{ error }}</p>
        <div class="form-actions">
          <button type="button" class="button secondary" @click="fillMember">되돌리기</button>
          <button class="button primary" :disabled="auth.busy">{{ auth.busy ? '저장 중' : '저장' }}</button>
        </div>
      </form>
    </section>

    <section class="profile-side">
      <article class="profile-summary-card">
        <span class="eyebrow">ACCOUNT</span>
        <dl>
          <div><dt>회원 ID</dt><dd>{{ auth.member?.memberId || '-' }}</dd></div>
          <div><dt>권한</dt><dd>{{ auth.member?.role || '-' }}</dd></div>
          <div><dt>상태</dt><dd>{{ auth.member?.status || '-' }}</dd></div>
        </dl>
      </article>

      <article class="profile-summary-card danger-zone">
        <span class="eyebrow">DANGER ZONE</span>
        <h2>회원 탈퇴</h2>
        <p>탈퇴를 진행하면 현재 계정으로 저장된 데이터에 더 이상 접근할 수 없습니다.</p>
        <button class="button danger" @click="withdrawOpen = !withdrawOpen">
          <AppIcon name="trash" :size="16" /> 탈퇴 정보 입력
        </button>
        <div v-if="withdrawOpen" class="withdraw-form">
          <label class="field"><span>비밀번호</span><input v-model="withdraw.password" type="password" autocomplete="current-password" /></label>
          <label class="field"><span>사유</span><textarea v-model="withdraw.reason" rows="3" /></label>
          <button class="button danger" :disabled="auth.busy" @click="removeAccount">회원 탈퇴</button>
        </div>
      </article>
    </section>
  </div>
</template>
