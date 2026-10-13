<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { createProject, listProjects, updateProject } from '@/api/projects'
import { describeError } from '@/api/problem'
import ProjectCard from '@/components/ProjectCard.vue'
import ProjectForm from '@/components/ProjectForm.vue'
import type { Project, ProjectRequest } from '@/types/project'

const projects = ref<Project[]>([])
const loading = ref(true)
const loadError = ref('')

const formOpen = ref(false)
const editing = ref<Project | undefined>()
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref<Record<string, string>>({})
const success = ref('')
const formHeading = ref<HTMLElement | null>(null)

async function load() {
  loadError.value = ''
  try {
    projects.value = await listProjects()
  } catch (error) {
    loadError.value = describeError(error).message
  } finally {
    loading.value = false
  }
}

async function openForm(project?: Project) {
  editing.value = project
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

async function save(request: ProjectRequest) {
  if (submitting.value) return
  submitting.value = true
  formError.value = ''
  fieldErrors.value = {}
  try {
    const wasEditing = editing.value !== undefined
    if (editing.value) await updateProject(editing.value.id, request)
    else await createProject(request)
    success.value = wasEditing ? `Project "${request.name}" updated.` : `Project "${request.name}" created.`
    closeForm()
    await load()
  } catch (error) {
    // The form stays open and keeps everything the user typed.
    const info = describeError(error)
    formError.value = info.message
    fieldErrors.value = info.fieldErrors
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-head">
    <h1>Projects</h1>
    <button v-if="!formOpen" type="button" class="btn btn--primary" @click="openForm()">New project</button>
  </div>

  <p v-if="success" class="alert alert--success" role="status">{{ success }}</p>

  <section v-if="formOpen" class="panel" aria-labelledby="project-form-heading">
    <h2 id="project-form-heading" ref="formHeading" tabindex="-1">{{ editing ? 'Edit project' : 'New project' }}</h2>
    <ProjectForm
      :key="editing?.id ?? 'new'"
      :project="editing"
      :submitting="submitting"
      :field-errors="fieldErrors"
      :error-message="formError"
      @submit="save"
      @cancel="closeForm"
    />
  </section>

  <p v-if="loading" role="status">Loading projects…</p>
  <div v-else-if="loadError" class="alert alert--error" role="alert">
    <p>{{ loadError }}</p>
    <button type="button" class="btn" @click="load">Retry</button>
  </div>
  <p v-else-if="projects.length === 0" class="empty">No projects yet. Create your first project to start tracking time.</p>
  <ul v-else class="card-list">
    <li v-for="project in projects" :key="project.id">
      <ProjectCard :project="project" @edit="openForm" />
    </li>
  </ul>
</template>
