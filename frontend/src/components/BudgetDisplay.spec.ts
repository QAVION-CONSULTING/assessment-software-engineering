import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { makeProject } from '@/test-utils'
import BudgetStatusBadge from './BudgetStatusBadge.vue'
import ProjectCard from './ProjectCard.vue'
import UtilizationBar from './UtilizationBar.vue'

describe('BudgetStatusBadge', () => {
  it.each([
    ['OK', 'OK'],
    ['WARNING', 'Warning'],
    ['EXCEEDED', 'Exceeded'],
  ] as const)('renders %s as readable text (not colour alone)', (status, text) => {
    const wrapper = mount(BudgetStatusBadge, { props: { status } })
    expect(wrapper.text()).toContain(text)
    expect(wrapper.classes()).toContain(`badge--${status.toLowerCase()}`)
  })
})

describe('UtilizationBar', () => {
  it('shows the real percentage and a proportional fill below 100%', () => {
    const wrapper = mount(UtilizationBar, { props: { label: 'Time', percent: 85, status: 'WARNING' } })
    expect(wrapper.get('[data-testid="utilization-percent"]').text()).toBe('85.00%')
    expect(wrapper.get('.bar-fill').attributes('style')).toContain('width: 85%')
  })

  it('caps the visual fill at 100% but still displays the actual value', () => {
    const wrapper = mount(UtilizationBar, { props: { label: 'Cost', percent: 137.5, status: 'EXCEEDED' } })
    expect(wrapper.get('[data-testid="utilization-percent"]').text()).toBe('137.50%')
    expect(wrapper.get('.bar-fill').attributes('style')).toContain('width: 100%')
    expect(wrapper.get('[role="progressbar"]').attributes('aria-valuenow')).toBe('100')
    expect(wrapper.get('[role="progressbar"]').attributes('aria-valuetext')).toBe('137.50% used')
  })
})

describe('ProjectCard', () => {
  it('renders the backend-provided budget figures and status without recalculating', () => {
    const project = makeProject({ overall: 'WARNING', timePercent: 80, costPercent: 12.5 })
    const wrapper = mount(ProjectCard, { props: { project } })

    expect(wrapper.get('[data-testid="overall-status"]').text()).toContain('Warning')
    expect(wrapper.get('[data-testid="recorded-hours"]').text()).toBe('10 h')
    expect(wrapper.text()).toContain('80.00%')
    expect(wrapper.text()).toContain('12.50%')
  })

  it('emits edit with the project', async () => {
    const project = makeProject()
    const wrapper = mount(ProjectCard, { props: { project } })
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('edit')![0]).toEqual([project])
  })
})
