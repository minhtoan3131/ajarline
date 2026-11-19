import type { Versioned } from '../admin-content/model/article'
import { createSeries, getSeries, updateSeries } from './api/learningAdminApi'
import type { LearningNotice } from './model/learningEditorState'
import { toSeriesEditable, type SeriesAdminDetail } from './model/series'
import { useLearningEditor, type LearningEditorController, type LearningSource } from './useLearningEditor'

const SERIES_SOURCE: LearningSource = {
  create: (client, fields) => createSeries(client, toSeriesEditable(fields)),
  update: (client, current, fields) => updateSeries(client, current.value.id, toSeriesEditable(fields), current.etag),
  load: (client, id) => getSeries(client, id),
}

export function useSeriesEditor(
  initial: Versioned<SeriesAdminDetail> | null,
  initialNotice: LearningNotice | null,
  onCreated?: (seriesId: string) => void,
): LearningEditorController {
  return useLearningEditor(initial, initialNotice, SERIES_SOURCE, { onCreated })
}
