<script setup lang="ts">
import { computed, ref, useId } from 'vue'
import type { Project } from '@/types/project'
import type { TimeEntry, TimeEntryRequest } from '@/types/timeEntry'
import { todayIso } from '@/utils/format'
import { validateAmount } from '@/utils/validation'
import FormField from './FormField.vue'

const props = defineProps<{
  /** When set, the form edits this entry; otherwise it creates a new one. */
  entry?: TimeEntry
  projects: Project[]
  submitting: boolean
  /** Messages returned by the backend (HTTP 400), keyed by field name. */
  fieldErrors: Record<string, string>
  /** Page-level problem such as a 422 business-rule message. */
  errorMessage: string
}>()
const emit = defineEmits<{ submit: [request: TimeEntryRequest]; cancel: [] }>()

const entryDate = ref(props.entry?.entryDate ?? todayIso())
const projectId = ref(props.entry ? String(props.entry.projectId) : '')
const durationHours = ref(props.entry ? String(props.entry.durationHours) : '')
const description = ref(props.entry?.description ?? '')

const clientErrors = ref<Record<string, string>>({})
const errors = computed(() => ({ ...props.fieldErrors, ...clientErrors.value }))
const projectSelectId = useId()

function validate(): boolean {
  const found: Record<string, string> = {}
  if (!entryDate.value) found.entryDate = 'Date is required.'
  if (!projectId.value) found.projectId = 'Select a project.'
  const duration = validateAmount(durationHours.value, { positive: true, max: 24 })
  if (duration) found.durationHours = duration
  clientErrors.value = found
  return Object.keys(found).length === 0
}

function onSubmit() {
  if (props.submitting || !validate()) return
  const text = description.value.trim()
  emit('submit', {
    projectId: Number(projectId.value),
    entryDate: entryDate.value,
    durationHours: Number(durationHours.value),
    description: text === '' ? undefined : text,
  })
}
</script>

<template>
  <form novalidate @submit.prevent="onSubmit">
    <p v-if="errorMessage" class="alert alert--error" role="alert">{{ errorMessage }}</p>
    <FormField v-model="entryDate" label="Date" type="date" required :error="errors.entryDate" />
    <div class="field" :class="{ 'field--invalid': errors.projectId }">
      <label :for="projectSelectId">Project <span aria-hidden="true">*</span></label>
      <select
        :id="projectSelectId"
        v-model="projectId"
        aria-required="true"
        :aria-invalid="errors.projectId ? 'true' : undefined"
        :aria-describedby="errors.projectId ? `${projectSelectId}-error` : undefined"
      >
        <option value="">Select a project</option>
        <option v-for="project in projects" :key="project.id" :value="String(project.id)">
          {{ project.name }}
        </option>
      </select>
      <p v-if="errors.projectId" :id="`${projectSelectId}-error`" class="field-error">{{ errors.projectId }}</p>
    </div>
    <FormField
      v-model="durationHours"
      label="Duration (hours)"
      type="number"
      step="0.25"
      inputmode="decimal"
      required
      :error="errors.durationHours"
    />
    <FormField
      v-model="description"
      label="Description (optional)"
      multiline
      :maxlength="1000"
      :error="errors.description"
    />
    <div class="actions">
      <button type="submit" class="btn btn--primary" :disabled="submitting">
        {{ submitting ? 'Saving…' : entry ? 'Save changes' : 'Add entry' }}
      </button>
      <button type="button" class="btn" :disabled="submitting" @click="emit('cancel')">Cancel</button>
    </div>
  </form>
</template>
