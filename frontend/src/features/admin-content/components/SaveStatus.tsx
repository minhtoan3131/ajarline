/** One line that tells the Admin whether the form is saved. */
export function SaveStatus({ pending, dirty, saved }: { pending: string | null; dirty: boolean; saved: boolean }) {
  let text = 'Đã lưu mọi thay đổi'
  if (pending === 'saving') {
    text = 'Đang lưu…'
  } else if (dirty) {
    text = 'Có thay đổi chưa lưu'
  } else if (!saved) {
    text = 'Chưa có nội dung'
  }
  return (
    <p className="article-save-status" aria-live="polite">
      {text}
    </p>
  )
}
