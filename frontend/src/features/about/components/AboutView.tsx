import { AboutNextLinks } from './AboutNextLinks'
import { ResponsibilityProfile } from './ResponsibilityProfile'

/**
 * The About page. It is static text: it never loads data, so it has no
 * loading, empty or error state.
 */
export function AboutView() {
  return (
    <article className="reader-about" aria-labelledby="about-title">
      <title>Về Ajarline · Ajarline</title>
      <header className="reader-browse-header">
        <h1 id="about-title">Về Ajarline</h1>
        <p className="reader-browse-intro">Một nơi để đọc và tự học công nghệ bằng tiếng Việt.</p>
      </header>

      <section className="reader-about-section" aria-labelledby="about-what-title">
        <h2 id="about-what-title">Ajarline là gì</h2>
        <p>
          Ajarline là nơi đọc và tự học công nghệ bằng tiếng Việt. Một người viết và biên tập toàn bộ nội dung.
        </p>
        <p>
          Hiểu kỹ một điều được đặt cao hơn việc theo kịp mọi thứ đang xảy ra. Vì vậy học liệu được sắp có thứ tự, bài
          viết tự đứng vững, và tin được chọn lọc.
        </p>
      </section>

      <section className="reader-about-section" aria-labelledby="about-method-title">
        <h2 id="about-method-title">Cách làm nội dung</h2>
        <p>Không phải nội dung nào cũng cần được tổ chức theo cùng một cách.</p>
        <ul className="reader-about-list">
          <li>
            <strong>Series và Module</strong> khi kiến thức có thứ tự hoặc phụ thuộc nhau, để bạn biết mình đang ở đâu
            và học gì tiếp theo.
          </li>
          <li>
            <strong>Bài viết độc lập</strong> khi một vấn đề tự đứng vững, không cần ép vào lộ trình.
          </li>
          <li>
            <strong>Tin công nghệ</strong> được chọn lọc, luôn kèm nguồn gốc và chỉ đường về nguồn để bạn tự kiểm chứng.
          </li>
          <li>
            <strong>Góc nhìn cá nhân</strong> được nói đúng là góc nhìn, không trộn với dữ kiện.
          </li>
        </ul>
      </section>

      <ResponsibilityProfile />
      <AboutNextLinks />
    </article>
  )
}
