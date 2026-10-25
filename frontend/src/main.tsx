import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import { App } from './app/App'
import './app/app.css'

const rootElement = document.getElementById('root')

// Failing early gives a clear error if index.html and the bootstrap drift.
if (rootElement === null) {
  throw new Error('Ajarline root element was not found')
}

createRoot(rootElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
