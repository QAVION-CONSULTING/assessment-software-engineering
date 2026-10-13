export interface AmountRules {
  /** true: value must be > 0; false: value must be >= 0 */
  positive: boolean
  max?: number
}

/**
 * Client-side input hygiene only (required / numeric / precision / simple bounds) for fast feedback.
 * The backend remains the authority for every rule, including the 24 h per day total.
 * Returns an error message, or null when the input is acceptable.
 */
export function validateAmount(raw: string, rules: AmountRules): string | null {
  const text = raw.trim()
  if (text === '') return 'This field is required.'
  if (!/^\d+(\.\d+)?$/.test(text)) return 'Enter a valid number.'
  if (!/^\d+(\.\d{1,2})?$/.test(text)) return 'Use at most 2 decimal places.'
  const value = Number(text)
  if (rules.positive && value <= 0) return 'Must be greater than 0.'
  if (rules.max !== undefined && value > rules.max) return `Must be at most ${rules.max}.`
  return null
}
