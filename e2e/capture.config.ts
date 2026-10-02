import { defineConfig } from '@playwright/test';

import { BASE_URL } from './base-url';

// Generates the README screenshots from the running stack (see `npm run capture`).
export default defineConfig({
  testDir: './capture',
  testMatch: '*.capture.ts',
  timeout: 180_000,
  workers: 1,
  reporter: 'list',
  use: { baseURL: BASE_URL, locale: 'es-EC', colorScheme: 'dark' },
});
