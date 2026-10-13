import { createRouter, createWebHistory } from 'vue-router'
import ProjectsView from '@/views/ProjectsView.vue'
import TimeEntriesView from '@/views/TimeEntriesView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
    { path: '/projects', name: 'projects', component: ProjectsView, meta: { title: 'Projects' } },
    { path: '/time-entries', name: 'time-entries', component: TimeEntriesView, meta: { title: 'Time entries' } },
  ],
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? 'Time Tracking'} · Time Tracking`
})
