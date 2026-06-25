<script setup>
const props = defineProps({ name: { type: String, required: true }, size: { type: Number, default: 20 } })

const icons = {
  search: ['M21 21l-4.35-4.35', 'M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15Z'],
  film: ['M4 3h16v18H4z', 'M8 3v18M16 3v18M4 8h4M16 8h4M4 16h4M16 16h4'],
  pin: ['M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z', 'M12 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z'],
  heart: ['M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z'],
  calendar: ['M3 5h18v16H3z', 'M16 3v4M8 3v4M3 10h18'],
  user: ['M20 21a8 8 0 0 0-16 0', 'M12 13a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z'],
  login: ['M15 3h6v18h-6', 'M10 17l5-5-5-5M15 12H3'],
  logout: ['M9 21H3V3h6', 'M14 17l5-5-5-5M19 12H8'],
  language: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M3.6 9h16.8M3.6 15h16.8', 'M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18'],
  menu: ['M4 6h16M4 12h16M4 18h16'],
  close: ['M5 5l14 14M19 5 5 19'],
  back: ['M19 12H5M12 19l-7-7 7-7'],
  chevron: ['M9 18l6-6-6-6'],
  down: ['m6 9 6 6 6-6'],
  plus: ['M12 5v14M5 12h14'],
  minus: ['M5 12h14'],
  edit: ['M12 20h9', 'M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z'],
  trash: ['M3 6h18M8 6V4h8v2M19 6l-1 15H6L5 6M10 11v6M14 11v6'],
  clock: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z', 'M12 6v6l4 2'],
  drag: ['M9 5h.01M15 5h.01M9 12h.01M15 12h.01M9 19h.01M15 19h.01'],
  sync: ['M20 7h-5V2', 'M20 7a8 8 0 0 0-13.7-3M4 17h5v5', 'M4 17a8 8 0 0 0 13.7 3'],
  check: ['m5 12 4 4L19 6'],
  alert: ['M12 9v4M12 17h.01', 'M10.3 2.9 1.8 18h20.4l-9-18a1.8 1.8 0 0 0-3.2 0Z'],
  image: ['M3 5h18v14H3z', 'm3 15 5-5 4 4 3-3 6 6', 'M16 9h.01'],
  external: ['M14 3h7v7M10 14 21 3', 'M21 14v7H3V3h7'],
}
</script>

<template>
  <svg
    :width="props.size"
    :height="props.size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path v-for="path in icons[props.name] || icons.alert" :key="path" :d="path" />
  </svg>
</template>
