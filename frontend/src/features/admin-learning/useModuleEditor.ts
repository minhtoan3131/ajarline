import type { Versioned } from '../admin-content/model/article'
import { createModule, getModule, updateModule } from './api/learningAdminApi'
import type { LearningNotice } from './model/learningEditorState'
import { toModuleEditable, type ModuleAdminDetail } from './model/module'
import {
  useLearningEditor,
  type LearningEditorController,
  type LearningEditorOptions,
  type LearningSource,
} from './useLearningEditor'

const MODULE_SOURCE: LearningSource = {
  create: (client, fields) => createModule(client, toModuleEditable(fields)),
  update: (client, current, fields) => updateModule(client, current.value.id, toModuleEditable(fields), current.etag),
  load: (client, id) => getModule(client, id),
}

export function useModuleEditor(
  initial: Versioned<ModuleAdminDetail> | null,
  initialNotice: LearningNotice | null,
  options: LearningEditorOptions = {},
): LearningEditorController {
  return useLearningEditor(initial, initialNotice, MODULE_SOURCE, options)
}
