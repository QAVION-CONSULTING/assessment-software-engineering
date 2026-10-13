<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { listProjects } from '@/api/projects'
import { describeError } from '@/api/problem'
import { createTimeEntry, deleteTimeEntry, listTimeEntries, updateTimeEntry } from '@/api/timeEntries'
import TimeEntryForm from '@/components/TimeEntryForm.vue'
import type { Project } from '@/types/project'
import type { TimeEntry, TimeEntryRequest } from '@/types/timeEntry'
import { formatHours } from '@/utils/format'

const entries = ref<TimeEntry[]>([])
const projects = ref<Project[]>([])
const loading = ref(true)
const loadError = ref('')

const formOpen = ref(false)
const editing = ref<TimeEntry | undefined>()
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref<Record<string, string>>({})
const success = ref('')
const pageError = ref('')
const formHeading = ref<HTMLElement | null>(null)

const confirmingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)

const projectNames = computed(() => new Map(projects.value.map((p) => [p.id, p.name])))
const projectName = (id: number) => projectNames.value.get(id) ?? `Project #${id}`

async function load() {
  loadError.value = ''
  try {
    ;[entries.value, projects.value] = await Promise.all([listTimeEntries(), listProjects()])
  } catch (error) {
    loadError.value = describeError(error).message
  } finally {
    loading.value = false
  }
}

async function openForm(entry?: TimeEntry) {
  editing.value = entry
  formOpen.value = true
  formError.value = ''
  fieldErrors.value = {}
  success.value = ''
  await nextTick()
  formHeading.value?.focus()
}

function closeForm() {
  formOpen.value = false
  editing.value = undefined
}

async function save(request: TimeEntryRequest) {
  if (submitting.value) return
  submitting.value = true
  formError.value = ''
  fieldErrors.value = {}
  try {
    const wasEditing = editing.value !== undefined
    if (editing.value) await updateTimeEntry(editing.value.id, request)
    else await createTimeEntry(request)
    success.value = wasEditing ? 'Time entry updated.' : 'Time entry added.'
    closeForm()
    await load()
  } catch (error) {
    // 400 -> field messages, 422 (e.g. more than 24 h on a day) -> message; the form keeps the user's input.
    const info = describeError(error)
    formError.value = info.message
    fieldErrors.value = info.fieldErrors
  } finally {
    submitting.value = false
  }
}

async function remove(entry: TimeEntry) {
  if (deletingId.value !== null) return
  deletingId.value = entry.id
  pageError.value = ''
  try {
    await deleteTimeEntry(entry.id)
    confirmingId.value = null
    success.value = 'Time entry deleted.'
    await load()
  } catch (error) {
    pageError.value = describeError(error).message
  } finally {
    deletingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page-head">
    <h1>Time entries</h1>
    <button v-if="!formOpen" type="button" class="btn btn--primary" @click="openForm()">Add time entry</button>
  </div>

  <p v-if="success" class="alert alert--success" role="status">{{ success }}</p>
  <p v-if="pageError" class="alert alert--error" role="alert">{{ pageError }}</p>

  <section v-if="formOpen" class="panel" aria-labelledby="entry-form-heading">
    <h2 id="entry-form-heading" ref="formHeading" tabindex="-1">{{ editing ? 'Edit time entry' : 'New time entry' }}</h2>
    <p v-if="!loading && projects.length === 0" class="hint">
      There are no projects yet. <RouterLink to="/projects">Create a project</RouterLink> first.
    </p>
    <TimeEntryForm
      :key="editing?.id ?? 'new'"
      :entry="editing"
      :projects="projects"
      :submitting="submitting"
      :field-errors="fieldErrors"
      :error-message="formError"
      @submit="save"
      @cancel="closeForm"
    />
  </section>

  <p v-if="loading" role="status">Loading time entries…</p>
  <div v-else-if="loadError" class="alert alert--error" role="alert">
    <p>{{ loadError }}</p>
    <button type="button" class="btn" @click="load">Retry</button>
  </div>
  <p v-else-if="entries.length === 0" class="empty">No time entries yet. Add your first one to start tracking.</p>
  <ul v-else class="card-list card-list--compact">
    <li v-for="entry in entries" :key="entry.id" class="card" data-testid="time-entry">
      <div class="card-head">
        <h3><time :datetime="entry.entryDate">{{ entry.entryDate }}</time> · {{ projectName(entry.projectId) }}</h3>
        <strong>{{ formatHours(entry.durationHours) }}</strong>
      </div>
      <p v-if="entry.description" class="description">{{ entry.description }}</p>

      <div v-if="confirmingId === entry.id" class="confirm" role="group" :aria-label="`Confirm deletion of entry on ${entry.entryDate}`">
        <span>Delete this entry?</span>
        <button type="button" class="btn btn--danger" :disabled="deletingId !== null" @click="remove(entry)">
          {{ deletingId === entry.id ? 'Deleting…' : 'Confirm delete' }}
        </button>
        <button type="button" class="btn" :disabled="deletingId !== null" @click="confirmingId = null">Cancel</button>
      </div>
      <div v-else class="actions">
        <button type="button" class="btn" :aria-label="`Edit entry on ${entry.entryDate} for ${projectName(entry.projectId)}`" @click="openForm(entry)">Edit</button>
        <button type="button" class="btn" :aria-label="`Delete entry on ${entry.entryDate} for ${projectName(entry.projectId)}`" @click="confirmingId = entry.id">Delete</button>
      </div>
    </li>
  </ul>
</template>
