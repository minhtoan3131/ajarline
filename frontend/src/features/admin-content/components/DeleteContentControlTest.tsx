import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { HttpClient } from '../../../shared/api/httpClient'
import { HttpClientProvider } from '../../../shared/api/HttpClientContext'
import { problemResponse, StubApiServer } from '../../../shared/api/testsupport/StubApiServer'
import { AdminSessionProvider } from '../../admin-auth/AdminSessionContext'
import { givenSignedInAdmin } from '../../admin-auth/fixture/adminSessionFixture'
import { holdRequestsTo, ITEM_IDS } from '../fixture/contentIndexFixture'
import type { DeleteTarget } from '../model/contentDelete'
import { DeleteContentControl, type DeleteContentControlProps } from './DeleteContentControl'

const DELETE_PATH = `/api/admin/v1/series/${ITEM_IDS.series}`
const TARGET: DeleteTarget = { type: 'series', id: ITEM_IDS.series, title: 'Series Java', etag: '"4"' }

function renderControl(overrides: Partial<DeleteContentControlProps> = {}): DeleteContentControlProps {
  const props: DeleteContentControlProps = { target: TARGET, onDeleted: vi.fn(), onFailed: vi.fn(), ...overrides }
  render(
    <HttpClientProvider client={new HttpClient()}>
      <AdminSessionProvider>
        <DeleteContentControl {...props} />
      </AdminSessionProvider>
    </HttpClientProvider>,
  )
  return props
}

async function confirmDelete(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: 'Xóa Series Java' }))
  await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))
}

describe('DeleteContentControl', () => {
  it('delete_ShouldAskWithTitleAndSendNothing_WhenDeleteIsClickedThenDeclined', async () => {
    const api = givenSignedInAdmin(new StubApiServer()).install()
    const props = renderControl()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Xóa Series Java' }))

    expect(screen.getByRole('group', { name: 'Xác nhận xóa Series Java' })).toHaveTextContent('Xóa “Series Java”?')
    expect(api.requestsTo('DELETE', DELETE_PATH)).toHaveLength(0)

    await user.click(screen.getByRole('button', { name: 'Giữ lại' }))

    expect(screen.getByRole('button', { name: 'Xóa Series Java' })).toBeInTheDocument()
    expect(api.requestsTo('DELETE', DELETE_PATH)).toHaveLength(0)
    expect(props.onDeleted).not.toHaveBeenCalled()
  })

  it('delete_ShouldSendOneRequestWithEtagAndCallOnDeleted_WhenDeleteIsConfirmed', async () => {
    const api = givenSignedInAdmin(new StubApiServer()).on('DELETE', DELETE_PATH, { status: 204 }).install()
    const props = renderControl()
    const user = userEvent.setup()

    await confirmDelete(user)

    await waitFor(() => expect(props.onDeleted).toHaveBeenCalledTimes(1))
    const removals = api.requestsTo('DELETE', DELETE_PATH)
    expect(removals).toHaveLength(1)
    expect(removals[0].headers.get('If-Match')).toBe('"4"')
    expect(removals[0].body).toBeUndefined()
  })

  it('delete_ShouldDisableButtonAndSendOnce_WhenRequestIsRunning', async () => {
    const api = givenSignedInAdmin(new StubApiServer()).on('DELETE', DELETE_PATH, { status: 204 }).install()
    const release = holdRequestsTo('DELETE')
    const props = renderControl()
    const user = userEvent.setup()

    await confirmDelete(user)

    expect(await screen.findByRole('button', { name: 'Đang xóa…' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Xác nhận xóa' })).toBeNull()
    release()
    await waitFor(() => expect(props.onDeleted).toHaveBeenCalledTimes(1))
    expect(api.requestsTo('DELETE', DELETE_PATH)).toHaveLength(1)
  })

  it('delete_ShouldNotOfferStart_WhenCallerDisablesControl', () => {
    givenSignedInAdmin(new StubApiServer()).install()

    renderControl({ disabled: true })

    expect(screen.getByRole('button', { name: 'Xóa Series Java' })).toBeDisabled()
  })

  it('delete_ShouldShowReasonAndNoRetry_WhenItemWasEverPublished', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('DELETE', DELETE_PATH, problemResponse(409, 'CONTENT_ALREADY_EVER_PUBLISHED'))
      .install()
    renderControl()
    const user = userEvent.setup()

    await confirmDelete(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('gỡ xuất bản')
    expect(screen.queryByRole('button', { name: 'Xóa Series Java' })).toBeNull()
  })

  it('delete_ShouldShowReasonAndKeepButton_WhenItemIsInUse', async () => {
    givenSignedInAdmin(new StubApiServer()).on('DELETE', DELETE_PATH, problemResponse(409, 'CONTENT_IN_USE')).install()
    const props = renderControl()
    const user = userEvent.setup()

    await confirmDelete(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('còn Module')
    expect(screen.getByRole('button', { name: 'Xóa Series Java' })).toBeEnabled()
    expect(props.onFailed).toHaveBeenCalledWith(expect.objectContaining({ kind: 'in-use' }))
    expect(props.onDeleted).not.toHaveBeenCalled()
  })

  it('delete_ShouldLeaveMessageToCaller_WhenInlineFailureIsOff', async () => {
    givenSignedInAdmin(new StubApiServer()).on('DELETE', DELETE_PATH, problemResponse(409, 'CONTENT_IN_USE')).install()
    const props = renderControl({ inlineFailure: false })
    const user = userEvent.setup()

    await confirmDelete(user)

    await waitFor(() => expect(props.onFailed).toHaveBeenCalledTimes(1))
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
