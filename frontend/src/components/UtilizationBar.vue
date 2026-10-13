<script setup lang="ts">
import { computed } from 'vue'
import type { BudgetStatus } from '@/types/project'
import { formatPercent } from '@/utils/format'
import BudgetStatusBadge from './BudgetStatusBadge.vue'

const props = defineProps<{ label: string; percent: number; status: BudgetStatus }>()

// Only the visual fill is capped; the displayed number is always the real utilization.
const fillWidth = computed(() => Math.min(Math.max(props.percent, 0), 100))
</script>

<template>
  <div class="utilization">
    <div class="utilization-head">
      <span class="utilization-label">{{ label }}</span>
      <span class="utilization-percent" data-testid="utilization-percent">{{ formatPercent(percent) }}</span>
      <BudgetStatusBadge :status="status" />
    </div>
    <div
      class="bar"
      role="progressbar"
      :aria-label="`${label} utilization`"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="fillWidth"
      :aria-valuetext="`${formatPercent(percent)} used`"
    >
      <div class="bar-fill" :class="`bar-fill--${status.toLowerCase()}`" :style="{ width: `${fillWidth}%` }" />
    </div>
  </div>
</template>
