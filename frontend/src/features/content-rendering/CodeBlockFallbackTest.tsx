import { render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { MarkdownRenderer } from './MarkdownRenderer'

// Replace the highlighter with one that always fails while rendering.
vi.mock('react-syntax-highlighter/dist/esm/prism-light', () => {
  function BrokenHighlighter(): never {
    throw new Error('Highlighter failed')
  }
  BrokenHighlighter.registerLanguage = () => undefined
  return { default: BrokenHighlighter }
})

describe('CodeBlock fallback', () => {
  it('render_ShouldShowPlainCode_WhenHighlighterThrows', () => {
    // Given
    // React logs errors caught by error boundaries. The log is expected here.
    vi.spyOn(console, 'error').mockImplementation(() => undefined)

    // When
    const { container } = render(<MarkdownRenderer markdown={'```java\nclass A {}\n```'} />)

    // Then
    expect(container.querySelector('pre code')).toHaveTextContent('class A {}')
    expect(container.querySelector('.token')).toBeNull()
  })
})
