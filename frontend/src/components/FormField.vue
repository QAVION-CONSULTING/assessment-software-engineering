<script setup lang="ts">
import { computed, useId } from 'vue'

const props = defineProps<{
  label: string
  error?: string
  type?: string
  multiline?: boolean
  step?: string
  inputmode?: 'decimal' | 'text'
  maxlength?: number
  required?: boolean
}>()
const model = defineModel<string>({ required: true })

const id = useId()
const errorId = computed(() => `${id}-error`)

// Not v-model on the element: Vue would cast type="number" inputs to numbers, but this model is always a string.
function onInput(event: Event) {
  model.value = (event.target as HTMLInputElement | HTMLTextAreaElement).value
}
</script>

<template>
  <div class="field" :class="{ 'field--invalid': error }">
    <label :for="id">{{ label }}<span v-if="required" aria-hidden="true"> *</span></label>
    <textarea
      v-if="multiline"
      :id="id"
      :value="model"
      rows="3"
      :maxlength="maxlength"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="error ? errorId : undefined"
      @input="onInput"
    />
    <input
      v-else
      :id="id"
      :value="model"
      :type="props.type ?? 'text'"
      :step="step"
      :inputmode="inputmode"
      :maxlength="maxlength"
      :required="required"
      :aria-required="required ? 'true' : undefined"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="error ? errorId : undefined"
      @input="onInput"
    />
    <p v-if="error" :id="errorId" class="field-error">{{ error }}</p>
  </div>
</template>
