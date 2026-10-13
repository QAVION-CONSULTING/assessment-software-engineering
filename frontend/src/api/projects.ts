import type { Project, ProjectRequest } from '@/types/project'
import { request } from './http'

export const listProjects = () => request<Project[]>('/projects')

export const createProject = (body: ProjectRequest) =>
  request<Project>('/projects', { method: 'POST', body: JSON.stringify(body) })

export const updateProject = (id: number, body: ProjectRequest) =>
  request<Project>(`/projects/${id}`, { method: 'PUT', body: JSON.stringify(body) })
