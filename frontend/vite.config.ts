import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

// In dev, /api is proxied to the backend so the browser never deals with CORS.
// In Docker the same proxy target is the backend service hostname.
const apiTarget = process.env.API_PROXY_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    host: true,
    port: 5173,
    proxy: { '/api': apiTarget },
  },
  preview: {
    host: true,
    port: 5173,
    proxy: { '/api': apiTarget },
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.spec.ts'],
  },
})
