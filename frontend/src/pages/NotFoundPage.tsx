/**
 * Public not-found state. Missing and non-public content share this one
 * message, so a reader can never tell a draft from a missing page.
 */
export function NotFoundPage() {
  return (
    <section className="reader-state">
      <title>Không tìm thấy · Ajarline</title>
      <h1>Không tìm thấy nội dung</h1>
      <p>Nội dung này không khả dụng hoặc không tồn tại.</p>
    </section>
  )
}
