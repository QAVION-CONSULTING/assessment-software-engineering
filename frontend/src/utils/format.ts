const numbers = new Intl.NumberFormat('en-IE', { maximumFractionDigits: 2 })
const euros = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' })

export const formatHours = (value: number) => `${numbers.format(value)} h`
export const formatEur = (value: number) => euros.format(value)
export const formatPercent = (value: number) => `${value.toFixed(2)}%`

/** Today as yyyy-MM-dd in the user's local time zone. */
export function todayIso(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
