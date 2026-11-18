import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { PublicationControls, type PublicationControlsProps } from './PublicationControls'

function renderControls(overrides: Partial<PublicationControlsProps> = {}): PublicationControlsProps {
  const props: PublicationControlsProps = {
    status: 'PUBLISHED',
    dirty: false,
    pending: null,
    hintId: 'publish-hint',
    onPublish: vi.fn(),
    onUnpublish: vi.fn(),
    ...overrides,
  }
  render(<PublicationControls {...props} />)
  return props
}

describe('PublicationControls', () => {
  it('unpublish_ShouldRunAtOnce_WhenNoConfirmationIsGiven', async () => {
    const props = renderControls()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Gỡ xuất bản' }))

    expect(props.onUnpublish).toHaveBeenCalledTimes(1)
  })

  it('unpublish_ShouldNotRun_WhenConfirmationIsDeclined', async () => {
    const confirmUnpublish = vi.fn(() => false)
    const props = renderControls({ confirmUnpublish })
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Gỡ xuất bản' }))

    await waitFor(() => expect(confirmUnpublish).toHaveBeenCalledTimes(1))
    expect(props.onUnpublish).not.toHaveBeenCalled()
  })

  it('unpublish_ShouldRun_WhenConfirmationIsAccepted', async () => {
    const props = renderControls({ confirmUnpublish: () => Promise.resolve(true) })
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Gỡ xuất bản' }))

    await waitFor(() => expect(props.onUnpublish).toHaveBeenCalledTimes(1))
  })

  it('render_ShouldShowNoUnpublish_WhenStatusIsDraft', () => {
    renderControls({ status: 'DRAFT' })

    expect(screen.getByRole('button', { name: 'Xuất bản' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gỡ xuất bản' })).toBeNull()
  })

  it('render_ShouldLockPublish_WhenFormHasUnsavedChanges', () => {
    renderControls({ status: 'DRAFT', dirty: true })

    expect(screen.getByRole('button', { name: 'Xuất bản' })).toBeDisabled()
  })

  it('render_ShouldLockButtonAndChangeLabel_WhenUnpublishIsRunning', () => {
    renderControls({ pending: 'unpublishing' })

    expect(screen.getByRole('button', { name: 'Đang gỡ xuất bản…' })).toBeDisabled()
  })
})
