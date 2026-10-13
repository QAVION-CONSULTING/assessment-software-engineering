import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { buttonByText, field, makeProject } from '@/test-utils'
import ProjectForm from './ProjectForm.vue'

const baseProps = { submitting: false, fieldErrors: {}, errorMessage: '' }

async function fillValid(wrapper: ReturnType<typeof mount>) {
  await field(wrapper, 'Project name').setValue('  Website  ')
  await field(wrapper, 'Time budget').setValue('100')
  await field(wrapper, 'Cost budget').setValue('5000.50')
  await field(wrapper, 'Hourly rate').setValue('0')
}

describe('ProjectForm', () => {
  it('emits a typed request with trimmed name and numeric values', async () => {
    const wrapper = mount(ProjectForm, { props: baseProps })
    await fillValid(wrapper)
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')![0]).toEqual([
      { name: 'Website', timeBudgetHours: 100, costBudgetEur: 5000.5, hourlyRateEur: 0 },
    ])
  })

  it('blocks submission and shows accessible messages for invalid input', async () => {
    const wrapper = mount(ProjectForm, { props: baseProps })
    await field(wrapper, 'Time budget').setValue('0')
    await field(wrapper, 'Cost budget').setValue('1.234')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    const name = field(wrapper, 'Project name')
    expect(name.attributes('aria-invalid')).toBe('true')
    expect(wrapper.get(`[id="${name.attributes('aria-describedby')}"]`).text()).toBe('Name is required.')
    expect(wrapper.text()).toContain('Must be greater than 0.')
    expect(wrapper.text()).toContain('Use at most 2 decimal places.')
    expect(wrapper.text()).toContain('This field is required.') // hourly rate
  })

  it('prepopulates all fields when editing', () => {
    const wrapper = mount(ProjectForm, { props: { ...baseProps, project: makeProject({ name: 'Mobile', hourlyRateEur: 75.5 }) } })

    expect((field(wrapper, 'Project name').element as HTMLInputElement).value).toBe('Mobile')
    expect((field(wrapper, 'Time budget').element as HTMLInputElement).value).toBe('100')
    expect((field(wrapper, 'Cost budget').element as HTMLInputElement).value).toBe('10000')
    expect((field(wrapper, 'Hourly rate').element as HTMLInputElement).value).toBe('75.5')
    expect(buttonByText(wrapper, 'Save changes').exists()).toBe(true)
  })

  it('shows backend field errors next to the matching fields and keeps input', async () => {
    const wrapper = mount(ProjectForm, { props: baseProps })
    await fillValid(wrapper)
    await wrapper.setProps({
      fieldErrors: { name: 'size must be between 0 and 255' },
      errorMessage: 'Please correct the highlighted fields.',
    })

    expect(wrapper.get('[role="alert"]').text()).toBe('Please correct the highlighted fields.')
    expect(wrapper.text()).toContain('size must be between 0 and 255')
    expect(field(wrapper, 'Project name').attributes('aria-invalid')).toBe('true')
    expect((field(wrapper, 'Time budget').element as HTMLInputElement).value).toBe('100')
  })

  it('prevents duplicate submission while a request is in flight', async () => {
    const wrapper = mount(ProjectForm, { props: { ...baseProps, submitting: true } })
    await fillValid(wrapper)

    expect(buttonByText(wrapper, 'Saving…').attributes('disabled')).toBeDefined()
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })
})
