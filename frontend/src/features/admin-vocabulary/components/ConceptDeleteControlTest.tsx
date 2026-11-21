import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ConceptDeleteControl, type ConceptDeleteControlProps } from './ConceptDeleteControl'

function renderControl(overrides: Partial<ConceptDeleteControlProps> = {}): ConceptDeleteControlProps {
  const props: ConceptDeleteControlProps = {
    conceptName: 'Thread',
    deleting: false,
    disabled: false,
    onDelete: vi.fn(),
    ...overrides,
  }
  render(<ConceptDeleteControl {...props} />)
  return props
}

describe('ConceptDeleteControl', () => {
  it('delete_ShouldAskWithNameAndCallNothing_WhenDeleteIsClickedThenDeclined', async () => {
    const props = renderControl()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Xóa Concept' }))

    const question = screen.getByRole('group', { name: 'Xác nhận xóa Concept' })
    expect(question).toHaveTextContent('Xóa Concept “Thread”?')
    expect(question).toHaveTextContent('expression')
    expect(props.onDelete).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Giữ lại' }))

    expect(screen.getByRole('button', { name: 'Xóa Concept' })).toBeInTheDocument()
    expect(props.onDelete).not.toHaveBeenCalled()
  })

  it('delete_ShouldCallOnDeleteOnceAndCollapseQuestion_WhenDeleteIsConfirmed', async () => {
    const props = renderControl()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Xóa Concept' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(props.onDelete).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('group', { name: 'Xác nhận xóa Concept' })).toBeNull()
    expect(screen.getByRole('button', { name: 'Xóa Concept' })).toBeInTheDocument()
  })

  it('delete_ShouldShowDisabledProgressButtonOnly_WhenDeleteIsRunning', () => {
    renderControl({ deleting: true })

    expect(screen.getByRole('button', { name: 'Đang xóa…' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Xóa Concept' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Xác nhận xóa' })).toBeNull()
  })

  it('delete_ShouldNotOfferStart_WhenParentDisablesControl', () => {
    renderControl({ disabled: true })

    expect(screen.getByRole('button', { name: 'Xóa Concept' })).toBeDisabled()
  })
})
