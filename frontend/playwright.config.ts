import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  reporter: 'list',
  use: { baseURL },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile', use: { ...devices['Pixel 5'] } },
  ],
  // Without E2E_BASE_URL, start the dev server (needs the backend for real flows).
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : { command: 'npm run dev', url: baseURL, reuseExistingServer: true },
})
