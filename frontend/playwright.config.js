import { defineConfig } from '@playwright/test'

// Teste in browser (Playwright): folosesc site-ul ca un utilizator real.
// Au nevoie de backend pornit pe portul 8080; interfata (Vite) o porneste Playwright singur.
// Rulare: npm run test:e2e   (local se foloseste Google Chrome instalat; in CI, Chromium de la Playwright)
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:5173',
    channel: process.env.CI ? undefined : 'chrome',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
})
