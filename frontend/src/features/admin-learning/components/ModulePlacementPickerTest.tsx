import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'

import { HttpClient } from '../../../shared/api/httpClient'
import { HttpClientProvider } from '../../../shared/api/HttpClientContext'
import { jsonResponse, StubApiServer } from '../../../shared/api/testsupport/StubApiServer'
import type { Placement } from '../../admin-content/model/article'
import {
  ARTICLE_ONE_ID,
  ARTICLE_TWO_ID,
  articleChild,
  draftModule,
  draftSeries,
  givenSeriesList,
  MODULE_ID,
  MODULE_PATH,
  moduleChild,
  moduleHeaders,
  OTHER_MODULE_ID,
  SERIES_API_PATH,
  SERIES_ID,
  SERIES_PATH,
  seriesHeaders,
  seriesItem,
} from '../fixture/learningAdminFixture'
import { ModulePlacementPicker } from './ModulePlacementPicker'

const OTHER_SERIES_ID = '9d3e5f70-1a2b-4c6d-8e90-a1b2c3d4e5f6'
const OTHER_SERIES_PATH = `${SERIES_API_PATH}/${OTHER_SERIES_ID}`

function Harness({ articleId, start }: { articleId: string | null; start: Placement | null }) {
  const [value, setValue] = useState(start)
  return (
    <>
      <ModulePlacementPicker articleId={articleId} value={value} onChange={setValue} error={null} disabled={false} />
      <span data-testid="placement">{JSON.stringify(value)}</span>
    </>
  )
}

function renderPicker(articleId: string | null, start: Placement | null = null) {
  return render(
    <HttpClientProvider client={new HttpClient()}>
      <Harness articleId={articleId} start={start} />
    </HttpClientProvider>,
  )
}

/** Holds back the answer to one path until the returned function is called. Install the stub first. */
function holdRequestsTo(path: string): () => Promise<void> {
  const respond = globalThis.fetch
  let open: () => void = () => undefined
  const gate = new Promise<void>((resolve) => {
    open = resolve
  })
  let answered: Promise<void> = Promise.resolve()
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
    if (String(input) !== path) {
      return respond(input, init)
    }
    const response = gate.then(() => respond(input, init))
    answered = response.then(() => undefined)
    return response
  })
  return async () => {
    open()
    await answered
  }
}

function seriesWithOneModule() {
  const series = draftSeries({ modules: [moduleChild()] })
  return { series, headers: seriesHeaders(series) }
}

describe('ModulePlacementPicker', () => {
  it('render_ShouldOfferOnlyStandalone_WhenNoSeriesExists', async () => {
    givenSeriesList(new StubApiServer(), []).install()

    renderPicker(null)

    const select = await screen.findByLabelText('Series')
    await waitFor(() => expect(select).toBeEnabled())
    expect(within(select).getAllByRole('option').map((option) => option.textContent)).toEqual([
      'Bài độc lập (không thuộc Series)',
    ])
    expect(screen.queryByLabelText('Module')).toBeNull()
  })

  it('choose_ShouldShowModulesOfLatestSeries_WhenSeriesChangesBeforeFirstAnswer', async () => {
    const seriesA = draftSeries({ title: 'Series A', modules: [moduleChild({ title: 'Module của A' })] })
    const seriesB = draftSeries({
      id: OTHER_SERIES_ID,
      title: 'Series B',
      modules: [moduleChild({ id: OTHER_MODULE_ID, title: 'Module của B' })],
    })
    givenSeriesList(new StubApiServer(), [seriesItem(), seriesItem({ id: OTHER_SERIES_ID, title: 'Series B' })])
      .on('GET', SERIES_PATH, jsonResponse(200, seriesA, seriesHeaders(seriesA)))
      .on('GET', OTHER_SERIES_PATH, jsonResponse(200, seriesB, seriesHeaders(seriesB)))
      .install()
    const releaseA = holdRequestsTo(SERIES_PATH)
    const user = userEvent.setup()
    renderPicker(null)
    const select = await screen.findByLabelText('Series')
    await waitFor(() => expect(select).toBeEnabled())

    await user.selectOptions(select, SERIES_ID)
    await user.selectOptions(select, OTHER_SERIES_ID)
    expect(await screen.findByRole('option', { name: '1. Module của B' })).toBeInTheDocument()
    await act(async () => {
      await releaseA()
    })

    expect(screen.getByRole('option', { name: '1. Module của B' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: '1. Module của A' })).toBeNull()
  })

  it('choose_ShouldDefaultToEndOfModuleWithoutThisArticle_WhenModuleIsChosen', async () => {
    const { series, headers } = seriesWithOneModule()
    const module = draftModule({ articles: [articleChild(), articleChild({ id: ARTICLE_TWO_ID, position: 2 })] })
    givenSeriesList(new StubApiServer(), [seriesItem()])
      .on('GET', SERIES_PATH, jsonResponse(200, series, headers))
      .on('GET', MODULE_PATH, jsonResponse(200, module, moduleHeaders(module)))
      .install()
    const user = userEvent.setup()
    renderPicker(ARTICLE_ONE_ID)
    const select = await screen.findByLabelText('Series')
    await waitFor(() => expect(select).toBeEnabled())

    await user.selectOptions(select, SERIES_ID)
    await user.selectOptions(await screen.findByLabelText('Module'), MODULE_ID)

    await waitFor(() =>
      expect(screen.getByTestId('placement')).toHaveTextContent(JSON.stringify({ moduleId: MODULE_ID, position: 2 })),
    )
    expect(screen.getByLabelText('Vị trí trong Module')).toHaveValue('2')
  })

  it('render_ShouldSelectSeriesAndModule_WhenArticleIsAlreadyInModule', async () => {
    const { series, headers } = seriesWithOneModule()
    const module = draftModule()
    givenSeriesList(new StubApiServer(), [seriesItem()])
      .on('GET', SERIES_PATH, jsonResponse(200, series, headers))
      .on('GET', MODULE_PATH, jsonResponse(200, module, moduleHeaders(module)))
      .install()

    renderPicker(ARTICLE_ONE_ID, { moduleId: MODULE_ID, position: 3 })

    expect(await screen.findByLabelText('Module')).toHaveValue(MODULE_ID)
    expect(screen.getByLabelText('Series')).toHaveValue(SERIES_ID)
    expect(screen.getByLabelText('Vị trí trong Module')).toHaveValue('3')
  })

  it('choose_ShouldClearPlacement_WhenStandaloneIsChosen', async () => {
    const { series, headers } = seriesWithOneModule()
    const module = draftModule()
    givenSeriesList(new StubApiServer(), [seriesItem()])
      .on('GET', SERIES_PATH, jsonResponse(200, series, headers))
      .on('GET', MODULE_PATH, jsonResponse(200, module, moduleHeaders(module)))
      .install()
    const user = userEvent.setup()
    renderPicker(ARTICLE_ONE_ID, { moduleId: MODULE_ID, position: 1 })
    await screen.findByLabelText('Module')

    await user.selectOptions(screen.getByLabelText('Series'), '')

    expect(screen.getByTestId('placement')).toHaveTextContent('null')
    expect(screen.queryByLabelText('Module')).toBeNull()
    expect(screen.queryByLabelText('Vị trí trong Module')).toBeNull()
  })

  it('choose_ShouldClearPlacement_WhenAnotherSeriesIsChosen', async () => {
    const { series, headers } = seriesWithOneModule()
    const module = draftModule()
    const other = draftSeries({ id: OTHER_SERIES_ID, title: 'Series B', modules: [] })
    givenSeriesList(new StubApiServer(), [seriesItem(), seriesItem({ id: OTHER_SERIES_ID, title: 'Series B' })])
      .on('GET', SERIES_PATH, jsonResponse(200, series, headers))
      .on('GET', OTHER_SERIES_PATH, jsonResponse(200, other, seriesHeaders(other)))
      .on('GET', MODULE_PATH, jsonResponse(200, module, moduleHeaders(module)))
      .install()
    const user = userEvent.setup()
    renderPicker(ARTICLE_ONE_ID, { moduleId: MODULE_ID, position: 1 })
    await screen.findByLabelText('Module')

    await user.selectOptions(screen.getByLabelText('Series'), OTHER_SERIES_ID)

    expect(screen.getByTestId('placement')).toHaveTextContent('null')
    expect(await screen.findByText('Series này chưa có Module. Hãy tạo Module ở màn Series trước.')).toBeInTheDocument()
  })
})
