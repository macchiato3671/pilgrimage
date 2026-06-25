import { makeId } from './storage'

export const planPayload = ({ title, beginDate, endDate, memo }) => ({
  title: title.trim(),
  beginDate,
  endDate,
  memo: memo?.trim() || '',
})

export const detailPayload = (detail) => ({
  ...(detail.detailId ? { detailId: detail.detailId } : {}),
  dayNo: Number(detail.dayNo),
  beginTime: `${String(detail.beginTime || '09:00').slice(0, 5)}:00`,
  ...(detail.sceneId ? { sceneId: detail.sceneId } : { placeId: detail.placeId }),
})

export const itemToDetail = (item, dayNo, beginTime = '09:00') => ({
  detailId: null,
  clientId: makeId('detail'),
  dayNo,
  beginTime,
  sceneId: item.kind === 'scene' ? item.sceneId : null,
  placeId: item.kind === 'place' ? item.placeId : null,
  item,
})

export const validatePlan = (plan) => {
  if (!plan.title?.trim()) return '여행 제목을 입력해 주세요.'
  if (!plan.beginDate || !plan.endDate) return '여행 시작일과 종료일을 선택해 주세요.'
  if (plan.beginDate > plan.endDate) return '종료일은 시작일보다 빠를 수 없습니다.'
  return ''
}
