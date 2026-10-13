import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { field, makeEntry, makeProject } from '@/test-utils'
import TimeEntryForm from './TimeEntryForm.vue'

const projects = [makeProject({ id: 1, name: 'Website' }), makeProject({ id: 2, name: 'Mobile' })]
const baseProps = { projects, submitting: false, fieldErrors: {}, errorMessage: '' }

describe('TimeEntryForm', () => {
  it('emits a request with decimal hours and optional description omitted', async () => {
    const wrapper = mount(TimeEntryForm, { props: baseProps })
    await field(wrapper, 'Date').setValue('2025-03-10')
    await field(wrapper, 'Project').setValue('2')
    await field(wrapper, 'Duration').setValue('7.5')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')![0]).toEqual([
      { projectId: 2, entryDate: '2025-03-10', durationHours: 7.5, description: undefined },
    ])
  })

  it('includes a trimmed description when given', async () => {
    const wrapper = mount(TimeEntryForm, { props: baseProps })
    await field(wrapper, 'Project').setValue('1')
    await field(wrapper, 'Duration').setValue('1')
    await field(wrapper, 'Description').setValue('  Standup  ')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')![0]![0]).toMatchObject({ description: 'Standup' })
  })

  it('requires date, project and a positive duration of at most 24 hours', async () => {
    const wrapper = mount(TimeEntryForm, { props: baseProps })
    await field(wrapper, 'Date').setValue('')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('Date is required.')
    expect(wrapper.text()).toContain('Select a project.')
    expect(wrapper.text()).toContain('This field is required.')

    await field(wrapper, 'Duration').setValue('24.5')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).toContain('Must be at most 24.')

    await field(wrapper, 'Duration').setValue('0')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).toContain('Must be greater than 0.')
  })

  it('prepopulates every field when editing', () => {
    const wrapper = mount(TimeEntryForm, {
      props: { ...baseProps, entry: makeEntry({ projectId: 2, entryDate: '2025-04-01', durationHours: 3.25, description: 'Review' }) },
    })

    expect((field(wrapper, 'Date').element as HTMLInputElement).value).toBe('2025-04-01')
    expect((field(wrapper, 'Project').element as HTMLSelectElement).value).toBe('2')
    expect((field(wrapper, 'Duration').element as HTMLInputElement).value).toBe('3.25')
    expect((field(wrapper, 'Description').element as HTMLTextAreaElement).value).toBe('Review')
  })

  it('keeps the user input and shows the backend message after a rejected submit', async () => {
    const wrapper = mount(TimeEntryForm, { props: baseProps })
    await field(wrapper, 'Project').setValue('1')
    await field(wrapper, 'Duration').setValue('5')
    await wrapper.setProps({ errorMessage: 'Cannot record 5 h: 23 h are already booked.' })

    expect(wrapper.get('[role="alert"]').text()).toContain('23 h are already booked')
    expect((field(wrapper, 'Duration').element as HTMLInputElement).value).toBe('5')
    expect((field(wrapper, 'Project').element as HTMLSelectElement).value).toBe('1')
  })

  it('shows backend field errors', async () => {
    const wrapper = mount(TimeEntryForm, { props: { ...baseProps, fieldErrors: { durationHours: 'must be less than or equal to 24' } } })
    expect(wrapper.text()).toContain('must be less than or equal to 24')
    expect(field(wrapper, 'Duration').attributes('aria-invalid')).toBe('true')
  })
})
