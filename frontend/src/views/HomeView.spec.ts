import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import HomeView from './HomeView.vue'

vi.mock('@/api/health', () => ({ getHealth: vi.fn().mockResolvedValue({ status: 'UP' }) }))

describe('HomeView', () => {
  it('shows backend status', async () => {
    const wrapper = mount(HomeView)
    await flushPromises()
    expect(wrapper.get('[data-testid="backend-status"]').text()).toContain('up')
  })
})
