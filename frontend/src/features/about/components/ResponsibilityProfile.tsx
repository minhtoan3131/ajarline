import { EDITORIAL_PROFILE } from '../../../shared/content/editorialProfile'

/**
 * Who is responsible for the content, and what that person promises.
 * The profile is read from the one shared source, never typed again here.
 */
export function ResponsibilityProfile() {
  return (
    <section className="reader-about-section" aria-labelledby="about-responsibility-title">
      <h2 id="about-responsibility-title">Trách nhiệm</h2>
      <p>Niềm tin không đến từ việc tự nói mình đáng tin. Ajarline cố giữ ba điều sau.</p>
      <ul className="reader-about-list">
        <li>
          <strong>Nguồn nhìn thấy được.</strong> Thông tin kiểm chứng được thì chỉ rõ nguồn, tách khỏi phần tóm tắt và
          lý giải của người viết.
        </li>
        <li>
          <strong>Nói về giới hạn.</strong> Phiên bản, điều kiện áp dụng và mức độ chắc chắn được nói rõ.
        </li>
        <li>
          <strong>Sai thì sửa.</strong> Khi phát hiện sai, nội dung được sửa thay vì để nguyên.
        </li>
      </ul>
      <div className="reader-about-profile">
        <p className="reader-about-profile-label">{EDITORIAL_PROFILE.responsibilityLabel}</p>
        <p className="reader-about-profile-name">{EDITORIAL_PROFILE.displayName}</p>
        <p className="reader-about-profile-bio">{EDITORIAL_PROFILE.shortBio}</p>
      </div>
    </section>
  )
}
