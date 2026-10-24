<template>
  <div v-if="open" class="modal-root">
    <button class="backdrop" type="button" aria-label="关闭" @click="$emit('close')"></button>
    <div class="dialog" role="dialog" aria-modal="true" :aria-label="title">
      <header>
        <h2>{{ title }}</h2>
        <button type="button" class="quiet" @click="$emit('close')">关闭</button>
      </header>
      <slot />
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted } from 'vue'

const props = defineProps({
  open: Boolean,
  title: { type: String, default: '' }
})
const emit = defineEmits(['close'])

function onKey(event) {
  if (props.open && event.key === 'Escape') {
    emit('close')
  }
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>
