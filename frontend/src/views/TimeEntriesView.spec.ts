import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/http'
import { listProjects } from '@/api/projects'
import { createTimeEntry, deleteTimeEntry, listTimeEntries, updateTimeEntry } from '@/api/timeEntries'
import { buttonByText, field, makeEntry, makeProject } from '@/test-utils'
import TimeEntriesView from './TimeEntriesView.vue'

vi.mock('@/api/projects', () => ({ listProjects: vi.fn() }))
vi.mock('@/api/timeEntries', () => ({
  listTimeEntries: vi.fn(),
  createTimeEntry: vi.fn(),
  updateTimeEntry: vi.fn(),
  deleteTimeEntry: vi.fn(),
}))

const projects = [makeProject({ id: 1, name: 'Website' }), makeProject({ id: 2, name: 'Mobile' })]

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(listProjects).mockResolvedValue(projects)
  vi.mocked(listTimeEntries).mockResolvedValue([])
})

async function mountView() {
  const wrapper = mount(TimeEntriesView, { global: { stubs: { RouterLink: true } } })
  await flushPromises()
  return wrapper
}

describe('TimeEntriesView', () => {
  it('shows loading, then entries with project name, duration and description', async () => {
    let resolve!: (v: ReturnType<typeof makeEntry>[]) => void
    vi.mocked(listTimeEntries).mockReturnValue(new Promise((r) => (resolve = r)))
    const wrapper = mount(TimeEntriesView, { global: { stubs: { RouterLink: true } } })
    expect(wrapper.text()).toContain('Loading time entries')

    resolve([makeEntry({ projectId: 2, durationHours: 7.5, description: 'Coding' })])
    await flushPromises()
    const row = wrapper.get('[data-testid="time-entry"]')
    expect(row.text()).toContain('2025-03-10')
    expect(row.text()).toContain('Mobile')
    expect(row.text()).toContain('7.5 h')
    expect(row.text()).toContain('Coding')
  })

  it('shows an empty state', async () => {
    expect((await mountView()).text()).toContain('No time entries yet')
  })

  it('creates an entry and refreshes the list', async () => {
    vi.mocked(listTimeEntries).mockResolvedValueOnce([]).mockResolvedValueOnce([makeEntry()])
    vi.mocked(createTimeEntry).mockResolvedValue(makeEntry())
    const wrapper = await mountView()

    await buttonByText(wrapper, 'Add time entry').trigger('click')
    await field(wrapper, 'Date').setValue('2025-03-10')
    await field(wrapper, 'Project').setValue('1')
    await field(wrapper, 'Duration').setValue('7.5')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createTimeEntry).toHaveBeenCalledWith({ projectId: 1, entryDate: '2025-03-10', durationHours: 7.5, description: undefined })
    expect(wrapper.get('[role="status"]').text()).toContain('Time entry added.')
    expect(wrapper.findAll('[data-testid="time-entry"]')).toHaveLength(1)
  })

  it('edits an entry: prepopulates, and allows changing date and project', async () => {
    vi.mocked(listTimeEntries).mockResolvedValue([makeEntry({ id: 10, projectId: 1, entryDate: '2025-03-10' })])
    vi.mocked(updateTimeEntry).mockResolvedValue(makeEntry())
    const wrapper = await mountView()

    await wrapper.get('button[aria-label^="Edit entry"]').trigger('click')
    expect((field(wrapper, 'Project').element as HTMLSelectElement).value).toBe('1')
    expect((field(wrapper, 'Duration').element as HTMLInputElement).value).toBe('7.5')
    await field(wrapper, 'Date').setValue('2025-03-12')
    await field(wrapper, 'Project').setValue('2')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateTimeEntry).toHaveBeenCalledWith(10, { projectId: 2, entryDate: '2025-03-12', durationHours: 7.5, description: 'Coding' })
    expect(wrapper.get('[role="status"]').text()).toContain('updated')
  })

  it('on a 422 daily-limit error shows the message, keeps the input and allows resubmission', async () => {
    const detail = 'Cannot record 5 h on 2025-03-10: 23 h are already booked and the daily maximum is 24 h.'
    vi.mocked(createTimeEntry)
      .mockRejectedValueOnce(new ApiError(422, { status: 422, detail }))
      .mockResolvedValueOnce(makeEntry())
    const wrapper = await mountView()
    await buttonByText(wrapper, 'Add time entry').trigger('click')
    await field(wrapper, 'Date').setValue('2025-03-10')
    await field(wrapper, 'Project').setValue('1')
    await field(wrapper, 'Duration').setValue('5')
    await field(wrapper, 'Description').setValue('Long day')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('daily maximum is 24 h')
    expect((field(wrapper, 'Duration').element as HTMLInputElement).value).toBe('5')
    expect((field(wrapper, 'Description').element as HTMLTextAreaElement).value).toBe('Long day')

    await field(wrapper, 'Duration').setValue('1')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(createTimeEntry).toHaveBeenCalledTimes(2)
    expect(createTimeEntry).toHaveBeenLastCalledWith(expect.objectContaining({ durationHours: 1 }))
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('requires confirmation before deleting and refreshes afterwards', async () => {
    vi.mocked(listTimeEntries).mockResolvedValueOnce([makeEntry({ id: 10 })]).mockResolvedValueOnce([])
    vi.mocked(deleteTimeEntry).mockResolvedValue(undefined)
    const wrapper = await mountView()

    await wrapper.get('button[aria-label^="Delete entry"]').trigger('click')
    expect(deleteTimeEntry).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Delete this entry?')

    await buttonByText(wrapper, 'Confirm delete').trigger('click')
    await flushPromises()
    expect(deleteTimeEntry).toHaveBeenCalledWith(10)
    expect(wrapper.text()).toContain('Time entry deleted.')
    expect(wrapper.text()).toContain('No time entries yet')
  })

  it('cancelling the confirmation keeps the entry', async () => {
    vi.mocked(listTimeEntries).mockResolvedValue([makeEntry()])
    const wrapper = await mountView()

    await wrapper.get('button[aria-label^="Delete entry"]').trigger('click')
    await buttonByText(wrapper, 'Cancel').trigger('click')

    expect(deleteTimeEntry).not.toHaveBeenCalled()
    expect(wrapper.findAll('[data-testid="time-entry"]')).toHaveLength(1)
    expect(wrapper.text()).not.toContain('Delete this entry?')
  })

  it('reports a failed delete instead of failing silently', async () => {
    vi.mocked(listTimeEntries).mockResolvedValue([makeEntry()])
    vi.mocked(deleteTimeEntry).mockRejectedValue(new ApiError(404, { status: 404, detail: 'Time entry with id 10 not found.' }))
    const wrapper = await mountView()

    await wrapper.get('button[aria-label^="Delete entry"]').trigger('click')
    await buttonByText(wrapper, 'Confirm delete').trigger('click')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('not found')
  })
})
