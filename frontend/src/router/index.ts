import { createRouter, createWebHistory } from 'vue-router'
import ProjectsView from '@/views/ProjectsView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
    { path: '/projects', name: 'projects', component: ProjectsView, meta: { title: 'Projects' } },
  ],
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? 'Time Tracking'} · Time Tracking`
})
