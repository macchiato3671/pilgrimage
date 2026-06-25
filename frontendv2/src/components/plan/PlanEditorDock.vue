<script setup>
import { computed, ref } from 'vue'
import { dateRange, daysBetween } from '../../models/date'
import { useEditorStore } from '../../stores/editor'
import { usePlansStore } from '../../stores/plans'
import { useUiStore } from '../../stores/ui'
import AppIcon from '../common/AppIcon.vue'
import PlanDayColumn from './PlanDayColumn.vue'
import PlanFormModal from './PlanFormModal.vue'

const editor = useEditorStore()
const plans = usePlansStore()
const ui = useUiStore()
const formOpen = ref(false)
const days = computed(() => (editor.activePlan ? daysBetween(editor.activePlan.beginDate, editor.activePlan.endDate) : []))
const byDay = (dayNo) => editor.activePlan?.details.filter((detail) => Number(detail.dayNo) === Number(dayNo)) || []

const close = () => {
  if (editor.dirty && !window.confirm('저장하지 않은 일정 편집 내용이 있습니다. 닫을까요?')) return
  editor.close()
}
const save = async () => {
  try {
    await editor.save()
    ui.toast('여행 세부 일정을 저장했습니다.', 'success')
  } catch (error) {
    ui.toast(error.message, 'error')
  }
}
const updateInfo = async (form) => {
  const wasDirty = editor.dirty
  try {
    const updated = await plans.updateInfo(editor.activePlan.planId, form)
    editor.activePlan = {
      ...editor.activePlan,
      ...updated,
      details: editor.activePlan.details.filter((detail) => detail.dayNo <= daysBetween(updated.beginDate, updated.endDate).length),
    }
    editor.dirty = wasDirty
    editor.persist()
    formOpen.value = false
    ui.toast('여행 정보를 수정했습니다.', 'success')
  } catch (error) {
    ui.toast(error.message, 'error')
  }
}
</script>

<template>
  <Transition name="dock">
    <button v-if="editor.isOpen && editor.minimized" class="editor-minimized" @click="editor.toggleMinimize()">
      <span :style="{ background: editor.activePlan.color }" /><strong>{{ editor.activePlan.title }}</strong><small>일정 편집 계속하기</small><AppIcon name="chevron" :size="18" />
    </button>
    <aside v-else-if="editor.isOpen" class="plan-editor" :class="{ dragging: editor.dragging }">
      <header class="plan-editor-header">
        <div><span class="eyebrow">현재 편집 중</span><h2>{{ editor.activePlan.title }}</h2><p>{{ dateRange(editor.activePlan.beginDate, editor.activePlan.endDate) }}</p></div>
        <div class="header-actions">
          <button class="icon-button" title="여행 정보 수정" @click="formOpen = true"><AppIcon name="edit" /></button>
          <button class="icon-button" title="최소화" @click="editor.toggleMinimize()"><AppIcon name="minus" /></button>
          <button class="icon-button" title="닫기" @click="close"><AppIcon name="close" /></button>
        </div>
      </header>
      <div class="editor-tip"><AppIcon name="drag" :size="17" /><span>촬영지·장소 카드를 원하는 날짜에 끌어놓고 시간을 조정하세요.</span></div>
      <div class="plan-days-scroll">
        <PlanDayColumn
          v-for="day in days"
          :key="day.dayNo"
          :day="day"
          :details="byDay(day.dayNo)"
          @external-drop="editor.addItem"
          @move="editor.moveDetail"
          @remove="editor.removeDetail"
          @time="editor.updateTime"
        />
      </div>
      <footer class="plan-editor-footer">
        <span v-if="editor.dirty" class="dirty-state">저장하지 않은 변경사항</span><span v-else class="saved-state"><AppIcon name="check" :size="15" /> 저장됨</span>
        <button class="button primary" :disabled="editor.saving || !editor.dirty" @click="save">{{ editor.saving ? '저장 중…' : '일정 저장' }}</button>
      </footer>
      <PlanFormModal :open="formOpen" :plan="editor.activePlan" @close="formOpen = false" @submit="updateInfo" />
    </aside>
  </Transition>
</template>
