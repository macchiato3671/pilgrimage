<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { translateScreenTexts } from '../api/translation'
import { DEFAULT_TRANSLATION_LANGUAGE, TRANSLATION_LANGUAGES } from '../config/languages'
import { STORAGE_KEYS } from '../config/app'
import { readStorage, writeStorage } from '../models/storage'
import { useUiStore } from '../stores/ui'
import AppIcon from './common/AppIcon.vue'

const MAX_BATCH_SIZE = 18
const MAX_CONCURRENT_BATCHES = 8
const TRANSLATION_DELAY_MS = 420
const SKIPPED_TAGS = new Set(['SCRIPT', 'STYLE', 'NOSCRIPT', 'TEXTAREA', 'INPUT', 'SELECT', 'OPTION', 'CODE', 'PRE', 'SVG', 'CANVAS'])
const RETRYABLE_TRANSLATION_STATUSES = new Set([400, 413, 429, 502, 503, 504])
const NON_RETRYABLE_TRANSLATION_CODES = new Set(['GMS_KEY_MISSING'])
const savedPreference = readStorage(STORAGE_KEYS.translation, {})

const ui = useUiStore()
const enabled = ref(Boolean(savedPreference.enabled))
const targetLanguage = ref(
  TRANSLATION_LANGUAGES.some((language) => language.value === savedPreference.targetLanguage)
    ? savedPreference.targetLanguage
    : DEFAULT_TRANSLATION_LANGUAGE,
)
const translating = ref(false)
const error = ref('')

const originals = new Map()
const translatedValues = new Map()
const translatedTargets = new Map()
const translationCache = new Map()

let observer
let timerId = 0
let runId = 0
let queued = false

const statusText = computed(() => {
  if (translating.value) return 'Translating'
  return enabled.value ? 'Translation on' : 'Translation off'
})
const shortError = computed(() => (error.value.length > 32 ? `${error.value.slice(0, 32)}...` : error.value))

const savePreference = () => {
  writeStorage(STORAGE_KEYS.translation, {
    enabled: enabled.value,
    targetLanguage: targetLanguage.value,
  })
}

const cacheKey = (text) => `${targetLanguage.value}\n${text}`
const normalizeText = (text) => text.replace(/\s+/g, ' ').trim()
const hasLetters = (text) => /\p{L}/u.test(text)

const shouldSkipElement = (element) => {
  if (!element || SKIPPED_TAGS.has(element.tagName)) return true
  if (element.closest('[data-translation-control], [data-no-translate], [contenteditable="true"]')) return true

  const style = window.getComputedStyle(element)
  return style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0' || element.getClientRects().length === 0
}

const rememberOriginal = (node) => {
  const current = node.nodeValue || ''
  const translated = translatedValues.get(node)

  if (!originals.has(node)) {
    originals.set(node, current)
  } else if (translated) {
    if (current !== translated) {
      originals.set(node, current)
      translatedValues.delete(node)
      translatedTargets.delete(node)
    }
  } else if (current !== originals.get(node)) {
    originals.set(node, current)
  }

  return originals.get(node) || ''
}

const cleanupDisconnectedNodes = () => {
  for (const node of originals.keys()) {
    if (!node.isConnected) {
      originals.delete(node)
      translatedValues.delete(node)
      translatedTargets.delete(node)
    }
  }
}

const collectTextNodes = () => {
  const root = document.querySelector('#app')
  if (!root) return []

  cleanupDisconnectedNodes()

  const nodes = []
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      const parent = node.parentElement
      if (shouldSkipElement(parent)) return NodeFilter.FILTER_REJECT

      const original = rememberOriginal(node)
      const normalized = normalizeText(original)
      if (!normalized || normalized.length > 900 || !hasLetters(normalized)) return NodeFilter.FILTER_REJECT

      return NodeFilter.FILTER_ACCEPT
    },
  })

  let node = walker.nextNode()
  while (node) {
    const original = rememberOriginal(node)
    nodes.push({ node, original, text: normalizeText(original) })
    node = walker.nextNode()
  }

  return nodes
}

const applyTranslation = ({ node, original }, translatedText) => {
  if (!node.isConnected || !translatedText) return

  const leading = original.match(/^\s*/)?.[0] || ''
  const trailing = original.match(/\s*$/)?.[0] || ''
  const nextValue = `${leading}${translatedText}${trailing}`

  translatedValues.set(node, nextValue)
  translatedTargets.set(node, targetLanguage.value)
  node.nodeValue = nextValue
}

const translateBatchTexts = async (texts) => {
  try {
    return await translateScreenTexts({
      targetLanguage: targetLanguage.value,
      texts,
    })
  } catch (translationError) {
    const retryable = RETRYABLE_TRANSLATION_STATUSES.has(translationError?.status)
      && !NON_RETRYABLE_TRANSLATION_CODES.has(translationError?.code)

    if (!retryable) throw translationError
    if (texts.length === 1) return texts

    const pivot = Math.ceil(texts.length / 2)
    const first = await translateBatchTexts(texts.slice(0, pivot))
    const second = await translateBatchTexts(texts.slice(pivot))
    return [...first, ...second]
  }
}

const restoreOriginals = () => {
  runId += 1
  window.clearTimeout(timerId)
  timerId = 0

  for (const [node, original] of originals.entries()) {
    const translated = translatedValues.get(node)
    if (node.isConnected && translated && node.nodeValue === translated) {
      node.nodeValue = original
    }
  }

  translatedValues.clear()
  translatedTargets.clear()
  originals.clear()
  translating.value = false
}

const requestAndApplyTranslations = async (candidates, currentRunId) => {
  const pendingByText = new Map()

  candidates.forEach((candidate) => {
    if (translatedTargets.get(candidate.node) === targetLanguage.value) return

    const cached = translationCache.get(cacheKey(candidate.text))
    if (cached) {
      applyTranslation(candidate, cached)
      return
    }

    const group = pendingByText.get(candidate.text) || []
    group.push(candidate)
    pendingByText.set(candidate.text, group)
  })

  const pendingTexts = [...pendingByText.keys()]
  const batches = []
  for (let index = 0; index < pendingTexts.length; index += MAX_BATCH_SIZE) {
    batches.push(pendingTexts.slice(index, index + MAX_BATCH_SIZE))
  }

  let nextBatchIndex = 0
  const translateNextBatch = async () => {
    if (!enabled.value || currentRunId !== runId) return

    const batchTexts = batches[nextBatchIndex]
    nextBatchIndex += 1
    if (!batchTexts) return

    const translations = await translateBatchTexts(batchTexts)

    if (!enabled.value || currentRunId !== runId) return
    if (translations.length !== batchTexts.length) throw new Error('The translation response was malformed.')

    translations.forEach((translation, batchIndex) => {
      const text = batchTexts[batchIndex]
      translationCache.set(cacheKey(text), translation)
      pendingByText.get(text)?.forEach((candidate) => applyTranslation(candidate, translation))
    })

    await translateNextBatch()
  }

  const workerCount = Math.min(MAX_CONCURRENT_BATCHES, batches.length)
  await Promise.all(Array.from({ length: workerCount }, translateNextBatch))
}

const translateCurrentScreen = async () => {
  if (!enabled.value) return
  if (translating.value) {
    queued = true
    return
  }

  const currentRunId = ++runId
  translating.value = true
  error.value = ''

  try {
    await nextTick()
    const candidates = collectTextNodes()
    await requestAndApplyTranslations(candidates, currentRunId)
  } catch (translationError) {
    const message = translationError?.message || 'Could not translate this screen.'
    error.value = message
    ui.toast(`Translation failed: ${message}`, 'error', 5000)
    enabled.value = false
  } finally {
    if (currentRunId === runId) translating.value = false
    if (queued && enabled.value) {
      queued = false
      scheduleTranslation()
    }
  }
}

function scheduleTranslation(delay = TRANSLATION_DELAY_MS) {
  if (!enabled.value) return
  window.clearTimeout(timerId)
  timerId = window.setTimeout(() => translateCurrentScreen(), delay)
}

const toggle = () => {
  enabled.value = !enabled.value
}

watch(enabled, (value) => {
  savePreference()
  if (value) scheduleTranslation(0)
  else restoreOriginals()
})

watch(targetLanguage, () => {
  savePreference()
  if (enabled.value) {
    restoreOriginals()
    scheduleTranslation(0)
  }
})

onMounted(() => {
  const root = document.querySelector('#app')
  if (!root) return

  observer = new MutationObserver((mutations) => {
    if (!enabled.value) return
    if (mutations.every((mutation) => mutation.target?.parentElement?.closest?.('[data-translation-control]'))) return
    scheduleTranslation()
  })
  observer.observe(root, { childList: true, characterData: true, subtree: true })

  if (enabled.value) scheduleTranslation(0)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  restoreOriginals()
})
</script>

<template>
  <div class="screen-translator" data-translation-control>
    <button
      type="button"
      class="translator-toggle"
      :class="{ active: enabled }"
      :aria-pressed="enabled"
      title="Turn screen translation on or off"
      @click="toggle"
    >
      <span class="translator-switch" aria-hidden="true"><i /></span>
      <AppIcon name="language" :size="17" />
      <strong>{{ statusText }}</strong>
    </button>
    <select v-model="targetLanguage" title="Choose translation language">
      <option v-for="language in TRANSLATION_LANGUAGES" :key="language.value" :value="language.value">
        {{ language.name }} ({{ language.code }})
      </option>
    </select>
    <span v-if="error && !enabled" class="translator-error" :title="error">{{ shortError }}</span>
  </div>
</template>
