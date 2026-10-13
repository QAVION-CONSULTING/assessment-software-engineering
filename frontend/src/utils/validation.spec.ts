import { describe, expect, it } from 'vitest'
import { validateAmount } from './validation'

describe('validateAmount', () => {
  it.each([
    ['', 'required'],
    ['abc', 'valid number'],
    ['-1', 'valid number'],
    ['1.234', '2 decimal'],
    ['0', 'greater than 0'],
  ])('rejects %j', (input, message) => {
    expect(validateAmount(input, { positive: true })).toContain(message)
  })

  it('accepts zero when not required to be positive', () => {
    expect(validateAmount('0', { positive: false })).toBeNull()
  })

  it('enforces an upper bound and accepts decimals', () => {
    expect(validateAmount('24.01', { positive: true, max: 24 })).toContain('at most 24')
    expect(validateAmount('7.5', { positive: true, max: 24 })).toBeNull()
  })
})
