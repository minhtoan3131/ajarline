import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Vitest runs without global test APIs, so Testing Library cannot register
// its automatic cleanup. Unmount every rendered tree after each test.
afterEach(() => {
  cleanup()
})
