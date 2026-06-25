import { computed } from 'vue'
import { useEditorStore } from '../stores/editor'

const hasCoordinate = (item) => item?.latitude != null && item?.longitude != null

export const usePlanMapOverlay = () => {
  const editor = useEditorStore()
  const planOverlayItems = computed(() =>
    [...(editor.activePlan?.details || [])]
      .sort((a, b) => Number(a.dayNo) - Number(b.dayNo) || String(a.beginTime).localeCompare(String(b.beginTime)))
      .map((detail) => detail.item && { ...detail.item, planOverlay: true, planColor: editor.activePlan?.color })
      .filter(hasCoordinate),
  )
  const planOverlayColor = computed(() => editor.activePlan?.color || '')

  return { planOverlayItems, planOverlayColor }
}
