<script setup lang="ts">
import { computed } from 'vue'
import type { BudgetStatus } from '@/types/project'

const props = defineProps<{ status: BudgetStatus }>()

// Text + symbol + colour: status is never conveyed by colour alone.
const display = computed(() => {
  switch (props.status) {
    case 'OK':
      return { icon: '✓', label: 'OK' }
    case 'WARNING':
      return { icon: '!', label: 'Warning' }
    default:
      return { icon: '✕', label: 'Exceeded' }
  }
})
</script>

<template>
  <span class="badge" :class="`badge--${status.toLowerCase()}`">
    <span aria-hidden="true">{{ display.icon }}</span>
    {{ display.label }}
  </span>
</template>
