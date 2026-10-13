import { expect, test, type Page } from '@playwright/test'

/**
 * Main workflow against the real stack (frontend -> /api proxy -> Spring Boot -> PostgreSQL); nothing is mocked.
 * Isolation: every run uses a uniquely named project and a random historic date, so runs (and the desktop/mobile
 * projects executing in parallel) cannot collide on the 24 h/day rule. The test deletes its own time entry.
 */
function uniqueDate(): string {
  const year = 1950 + Math.floor(Math.random() * 40)
  const month = 1 + Math.floor(Math.random() * 12)
  const day = 1 + Math.floor(Math.random() * 28)
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

async function expectNoHorizontalOverflow(page: Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(overflow).toBeLessThanOrEqual(0)
}

test('create project, track time, see budget warning, edit and delete entry', async ({ page }) => {
  const projectName = `E2E ${Date.now()}-${Math.floor(Math.random() * 1e6)}`
  const date = uniqueDate()
  const projectCard = () => page.getByRole('listitem').filter({ hasText: projectName })
  const nav = page.getByRole('navigation', { name: 'Main' })

  // 1-3. Create a project: 10 h time budget, 1000 EUR cost budget, 10 EUR/h
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Projects', level: 1 })).toBeVisible()
  await page.getByRole('button', { name: 'New project' }).click()
  await page.getByLabel('Project name').fill(projectName)
  await page.getByLabel('Time budget (hours)').fill('10')
  await page.getByLabel('Cost budget (EUR)').fill('1000')
  await page.getByLabel('Hourly rate (EUR)').fill('10')
  await page.getByRole('button', { name: 'Create project' }).click()
  await expect(page.getByRole('status').filter({ hasText: `Project "${projectName}" created.` })).toBeVisible()
  await expect(projectCard().getByTestId('overall-status')).toHaveText(/OK/)
  await expect(projectCard().getByTestId('recorded-hours')).toHaveText('0 h')
  await expectNoHorizontalOverflow(page)

  // 4-5. Record 8 h on a date
  await nav.getByRole('link', { name: 'Time entries' }).click()
  await page.getByRole('button', { name: 'Add time entry' }).click()
  await page.getByLabel('Date').fill(date)
  await page.getByRole('combobox', { name: 'Project' }).selectOption({ label: projectName })
  await page.getByLabel('Duration (hours)').fill('8')
  await page.getByLabel('Description (optional)').fill('Initial implementation')
  await page.getByRole('button', { name: 'Add entry' }).click()
  const entry = () => page.getByTestId('time-entry').filter({ hasText: projectName })
  await expect(entry()).toContainText(date)
  await expect(entry()).toContainText('8 h')
  await expect(entry()).toContainText('Initial implementation')
  await expectNoHorizontalOverflow(page)

  // 6-7. 8 of 10 h = 80 % -> warning
  await nav.getByRole('link', { name: 'Projects' }).click()
  await expect(projectCard().getByTestId('recorded-hours')).toHaveText('8 h')
  await expect(projectCard().getByTestId('recorded-cost')).toContainText('80.00')
  await expect(projectCard().getByTestId('overall-status')).toHaveText(/Warning/)
  await expect(projectCard().getByTestId('utilization-percent').first()).toHaveText('80.00%')

  // 8-9. Edit the entry to 11 h -> exceeded, bar fill capped at 100 % while the real value is shown
  await nav.getByRole('link', { name: 'Time entries' }).click()
  await entry().getByRole('button', { name: /^Edit entry/ }).click()
  await expect(page.getByLabel('Duration (hours)')).toHaveValue('8')
  await page.getByLabel('Duration (hours)').fill('11')
  await page.getByRole('button', { name: 'Save changes' }).click()
  await expect(entry()).toContainText('11 h')
  await nav.getByRole('link', { name: 'Projects' }).click()
  await expect(projectCard().getByTestId('overall-status')).toHaveText(/Exceeded/)
  await expect(projectCard().getByTestId('utilization-percent').first()).toHaveText('110.00%')
  await expect(projectCard().getByRole('progressbar', { name: 'Time utilization' })).toHaveAttribute('aria-valuenow', '100')

  // Delete (with confirmation) -> back to OK
  await nav.getByRole('link', { name: 'Time entries' }).click()
  await entry().getByRole('button', { name: /^Delete entry/ }).click()
  await entry().getByRole('button', { name: 'Confirm delete' }).click()
  await expect(entry()).toHaveCount(0)
  await nav.getByRole('link', { name: 'Projects' }).click()
  await expect(projectCard().getByTestId('recorded-hours')).toHaveText('0 h')
  await expect(projectCard().getByTestId('overall-status')).toHaveText(/OK/)
})

test('daily 24 h limit is explained by the backend and the form input is preserved', async ({ page }) => {
  const projectName = `E2E limit ${Date.now()}-${Math.floor(Math.random() * 1e6)}`
  const date = uniqueDate()

  await page.goto('/projects')
  await page.getByRole('button', { name: 'New project' }).click()
  await page.getByLabel('Project name').fill(projectName)
  await page.getByLabel('Time budget (hours)').fill('100')
  await page.getByLabel('Cost budget (EUR)').fill('1000')
  await page.getByLabel('Hourly rate (EUR)').fill('1')
  await page.getByRole('button', { name: 'Create project' }).click()
  await expect(page.getByRole('status').filter({ hasText: 'created' })).toBeVisible()

  await page.goto('/time-entries')
  const add = async (hours: string) => {
    await page.getByLabel('Date').fill(date)
    await page.getByRole('combobox', { name: 'Project' }).selectOption({ label: projectName })
    await page.getByLabel('Duration (hours)').fill(hours)
    await page.getByRole('button', { name: 'Add entry' }).click()
  }
  await page.getByRole('button', { name: 'Add time entry' }).click()
  await add('20')
  await expect(page.getByTestId('time-entry').filter({ hasText: projectName })).toContainText('20 h')

  await page.getByRole('button', { name: 'Add time entry' }).click()
  await add('5')
  await expect(page.getByRole('alert')).toContainText('daily maximum is 24 h')
  await expect(page.getByLabel('Duration (hours)')).toHaveValue('5')

  await page.getByLabel('Duration (hours)').fill('4')
  await page.getByRole('button', { name: 'Add entry' }).click()
  await expect(page.getByTestId('time-entry').filter({ hasText: projectName })).toHaveCount(2)
})
