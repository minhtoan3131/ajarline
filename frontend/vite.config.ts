import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Development-only proxy: browser code still calls same-origin /api.
      // Vite forwards the request to the local Spring Boot process.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/app/test-setup.ts',
    include: ['src/**/*Test.{ts,tsx}'],
    exclude: ['src/**/*IT.{ts,tsx}'],
  },
})
