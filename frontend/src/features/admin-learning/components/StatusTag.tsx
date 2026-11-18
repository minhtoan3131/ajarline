import { STATUS_LABELS } from '../../admin-content/components/PublicationControls'
import type { PublicationStatus } from '../../admin-content/model/article'

/** Short status label for one row of a list. */
export function StatusTag({ status }: { status: PublicationStatus }) {
  return <span className={`admin-badge admin-badge-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>
}
