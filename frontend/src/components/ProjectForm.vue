<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Project, ProjectRequest } from '@/types/project'
import { validateAmount } from '@/utils/validation'
import FormField from './FormField.vue'

const props = defineProps<{
  /** When set, the form edits this project; otherwise it creates a new one. */
  project?: Project
  submitting: boolean
  /** Messages returned by the backend (HTTP 400), keyed by field name. */
  fieldErrors: Record<string, string>
  errorMessage: string
}>()
const emit = defineEmits<{ submit: [request: ProjectRequest]; cancel: [] }>()

const name = ref(props.project?.name ?? '')
const timeBudgetHours = ref(props.project ? String(props.project.timeBudgetHours) : '')
const costBudgetEur = ref(props.project ? String(props.project.costBudgetEur) : '')
const hourlyRateEur = ref(props.project ? String(props.project.hourlyRateEur) : '')

const clientErrors = ref<Record<string, string>>({})
const errors = computed(() => ({ ...props.fieldErrors, ...clientErrors.value }))

function validate(): boolean {
  const found: Record<string, string> = {}
  const trimmed = name.value.trim()
  if (!trimmed) found.name = 'Name is required.'
  else if (trimmed.length > 255) found.name = 'Name must be at most 255 characters.'
  const time = validateAmount(timeBudgetHours.value, { positive: true })
  if (time) found.timeBudgetHours = time
  const cost = validateAmount(costBudgetEur.value, { positive: true })
  if (cost) found.costBudgetEur = cost
  const rate = validateAmount(hourlyRateEur.value, { positive: false })
  if (rate) found.hourlyRateEur = rate
  clientErrors.value = found
  return Object.keys(found).length === 0
}

function onSubmit() {
  if (props.submitting || !validate()) return
  emit('submit', {
    name: name.value.trim(),
    timeBudgetHours: Number(timeBudgetHours.value),
    costBudgetEur: Number(costBudgetEur.value),
    hourlyRateEur: Number(hourlyRateEur.value),
  })
}
</script>

<template>
  <form novalidate @submit.prevent="onSubmit">
    <p v-if="errorMessage" class="alert alert--error" role="alert">{{ errorMessage }}</p>
    <FormField v-model="name" label="Project name" required :maxlength="255" :error="errors.name" />
    <FormField
      v-model="timeBudgetHours"
      label="Time budget (hours)"
      type="number"
      step="0.01"
      inputmode="decimal"
      required
      :error="errors.timeBudgetHours"
    />
    <FormField
      v-model="costBudgetEur"
      label="Cost budget (EUR)"
      type="number"
      step="0.01"
      inputmode="decimal"
      required
      :error="errors.costBudgetEur"
    />
    <FormField
      v-model="hourlyRateEur"
      label="Hourly rate (EUR)"
      type="number"
      step="0.01"
      inputmode="decimal"
      required
      :error="errors.hourlyRateEur"
    />
    <div class="actions">
      <button type="submit" class="btn btn--primary" :disabled="submitting">
        {{ submitting ? 'Saving…' : project ? 'Save changes' : 'Create project' }}
      </button>
      <button type="button" class="btn" :disabled="submitting" @click="emit('cancel')">Cancel</button>
    </div>
  </form>
</template>
