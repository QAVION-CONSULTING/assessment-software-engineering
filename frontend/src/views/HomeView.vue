<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealth } from '@/api/health'

const status = ref<'checking' | 'up' | 'down'>('checking')

onMounted(async () => {
  try {
    status.value = (await getHealth()).status === 'UP' ? 'up' : 'down'
  } catch {
    status.value = 'down'
  }
})
</script>

<template>
  <h1>Time Tracking</h1>
  <p data-testid="backend-status">Backend: {{ status }}</p>
</template>
