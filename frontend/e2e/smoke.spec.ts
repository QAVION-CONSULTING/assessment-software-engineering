import { expect, test } from '@playwright/test'

test('app shell renders with navigation', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Projects', level: 1 })).toBeVisible()
  await page.getByRole('navigation', { name: 'Main' }).getByRole('link', { name: 'Time entries' }).click()
  await expect(page.getByRole('heading', { name: 'Time entries', level: 1 })).toBeVisible()
})
