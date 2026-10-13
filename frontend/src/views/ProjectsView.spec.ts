import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/http'
import { createProject, listProjects, updateProject } from '@/api/projects'
import { buttonByText, field, makeProject } from '@/test-utils'
import ProjectsView from './ProjectsView.vue'

vi.mock('@/api/projects', () => ({ listProjects: vi.fn(), createProject: vi.fn(), updateProject: vi.fn() }))

beforeEach(() => vi.resetAllMocks())

async function mountView() {
  const wrapper = mount(ProjectsView)
  await flushPromises()
  return wrapper
}

describe('ProjectsView', () => {
  it('shows a loading state, then the projects with budget status', async () => {
    let resolve!: (v: ReturnType<typeof makeProject>[]) => void
    vi.mocked(listProjects).mockReturnValue(new Promise((r) => (resolve = r)))
    const wrapper = mount(ProjectsView)
    expect(wrapper.text()).toContain('Loading projects')

    resolve([makeProject({ overall: 'EXCEEDED', timePercent: 120 })])
    await flushPromises()
    expect(wrapper.text()).not.toContain('Loading projects')
    expect(wrapper.get('[data-testid="overall-status"]').text()).toContain('Exceeded')
  })

  it('shows an empty state', async () => {
    vi.mocked(listProjects).mockResolvedValue([])
    expect((await mountView()).text()).toContain('No projects yet')
  })

  it('shows a load error with retry', async () => {
    vi.mocked(listProjects).mockRejectedValueOnce(new TypeError('Failed to fetch')).mockResolvedValueOnce([])
    const wrapper = await mountView()
    expect(wrapper.get('[role="alert"]').text()).toContain('Cannot reach the server')

    await buttonByText(wrapper, 'Retry').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('No projects yet')
  })

  it('creates a project, shows success feedback and refreshes the list', async () => {
    vi.mocked(listProjects).mockResolvedValueOnce([]).mockResolvedValueOnce([makeProject({ name: 'New one' })])
    vi.mocked(createProject).mockResolvedValue(makeProject())
    const wrapper = await mountView()

    await buttonByText(wrapper, 'New project').trigger('click')
    await field(wrapper, 'Project name').setValue('New one')
    await field(wrapper, 'Time budget').setValue('10')
    await field(wrapper, 'Cost budget').setValue('1000')
    await field(wrapper, 'Hourly rate').setValue('50')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createProject).toHaveBeenCalledWith({ name: 'New one', timeBudgetHours: 10, costBudgetEur: 1000, hourlyRateEur: 50 })
    expect(wrapper.get('[role="status"]').text()).toContain('Project "New one" created.')
    expect(wrapper.find('form').exists()).toBe(false)
    expect(wrapper.text()).toContain('New one')
  })

  it('submits only once when the form is submitted twice quickly', async () => {
    vi.mocked(listProjects).mockResolvedValue([])
    vi.mocked(createProject).mockReturnValue(new Promise(() => {}))
    const wrapper = await mountView()
    await buttonByText(wrapper, 'New project').trigger('click')
    await field(wrapper, 'Project name').setValue('A')
    await field(wrapper, 'Time budget').setValue('1')
    await field(wrapper, 'Cost budget').setValue('1')
    await field(wrapper, 'Hourly rate').setValue('1')

    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(createProject).toHaveBeenCalledTimes(1)
  })

  it('edits an existing project with prepopulated values', async () => {
    vi.mocked(listProjects).mockResolvedValue([makeProject({ id: 7, name: 'Old name' })])
    vi.mocked(updateProject).mockResolvedValue(makeProject())
    const wrapper = await mountView()

    await wrapper.get('button[aria-label="Edit Old name"]').trigger('click')
    const name = field(wrapper, 'Project name')
    expect((name.element as HTMLInputElement).value).toBe('Old name')
    await name.setValue('Renamed')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateProject).toHaveBeenCalledWith(7, { name: 'Renamed', timeBudgetHours: 100, costBudgetEur: 10000, hourlyRateEur: 50 })
    expect(wrapper.get('[role="status"]').text()).toContain('updated')
  })

  it('presents API validation errors on the fields and keeps the form open with its input', async () => {
    vi.mocked(listProjects).mockResolvedValue([])
    vi.mocked(createProject).mockRejectedValue(new ApiError(400, { status: 400, errors: { costBudgetEur: 'numeric value out of bounds' } }))
    const wrapper = await mountView()
    await buttonByText(wrapper, 'New project').trigger('click')
    await field(wrapper, 'Project name').setValue('Big')
    await field(wrapper, 'Time budget').setValue('10')
    await field(wrapper, 'Cost budget').setValue('99999999999')
    await field(wrapper, 'Hourly rate').setValue('1')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('numeric value out of bounds')
    expect(field(wrapper, 'Cost budget').attributes('aria-invalid')).toBe('true')
    expect((field(wrapper, 'Project name').element as HTMLInputElement).value).toBe('Big')
    expect(buttonByText(wrapper, 'Create project').attributes('disabled')).toBeUndefined()
  })
})
