import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { MarkdownRenderer } from './MarkdownRenderer'

const JAVA_CODE = 'public class Person {\n    private String name;\n}'
const JAVA_BLOCK = '```java\n' + JAVA_CODE + '\n```'

describe('CodeBlock', () => {
  it('render_ShouldHighlightWithClassesAndLabel_WhenLanguageIsSupported', () => {
    // When
    const { container } = render(<MarkdownRenderer markdown={JAVA_BLOCK} />)

    // Then
    expect(screen.getByText('Java')).toBeInTheDocument()
    expect(container.querySelector('pre .token.keyword')).not.toBeNull()
    // CSP forbids inline styles, so highlighting must only use classes.
    expect(container.querySelector('[style]')).toBeNull()
  })

  it('render_ShouldShowPlainCodeWithoutLabel_WhenLanguageIsUnknown', () => {
    // When
    const { container } = render(<MarkdownRenderer markdown={'```cobol\nDISPLAY "XIN CHAO".\n```'} />)

    // Then
    expect(container.querySelector('.token')).toBeNull()
    expect(container.querySelector('.md-code-language')).toBeNull()
    expect(container.querySelector('pre code')).toHaveTextContent('DISPLAY "XIN CHAO".')
  })

  it('render_ShouldKeepWhitespaceAndSpecialCharacters_WhenCodeIsRendered', () => {
    // Given
    const code = 'if (a < b && c > d) {\n\n    say("<script>");\n}'

    // When
    const { container } = render(<MarkdownRenderer markdown={'```text\n' + code + '\n```'} />)

    // Then
    expect(container.querySelector('pre code')?.textContent).toBe(code)
    expect(container.querySelector('script')).toBeNull()
  })

  it('render_ShouldLetKeyboardScrollCode_WhenLineIsLong', () => {
    // When
    const { container } = render(<MarkdownRenderer markdown={'```bash\n' + 'echo x '.repeat(80) + '\n```'} />)

    // Then
    expect(container.querySelector('pre')).toHaveAttribute('tabindex', '0')
  })

  it('copy_ShouldWriteRawCodeAndAnnounceIt_WhenCopyIsActivatedByKeyboard', async () => {
    // Given
    const user = userEvent.setup()
    render(<MarkdownRenderer markdown={JAVA_BLOCK} />)

    // When
    await user.tab()
    await user.keyboard('{Enter}')

    // Then
    expect(await navigator.clipboard.readText()).toBe(JAVA_CODE)
    expect(screen.getByRole('status')).toHaveTextContent('Đã sao chép mã.')
    expect(screen.getByRole('button', { name: 'Đã sao chép' })).toHaveFocus()
  })

  it('copy_ShouldAnnounceFailure_WhenClipboardRejects', async () => {
    // Given
    const user = userEvent.setup()
    vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValue(new Error('Permission denied'))
    render(<MarkdownRenderer markdown={JAVA_BLOCK} />)

    // When
    await user.click(screen.getByRole('button', { name: 'Sao chép' }))

    // Then
    expect(await screen.findByText('Không sao chép được mã. Hãy chọn và sao chép thủ công.')).toBeInTheDocument()
  })
})
