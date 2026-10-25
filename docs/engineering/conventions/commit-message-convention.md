# Commit Message Convention

## 1. Mục đích

Quy ước này giúp lịch sử Git:

- Dễ đọc và tìm kiếm.
- Thể hiện rõ mục đích của từng thay đổi.
- Hỗ trợ tạo changelog và tự động hóa release.
- Giúp review, rollback và điều tra lỗi dễ dàng hơn.

Mỗi commit chỉ nên chứa một thay đổi logic hoàn chỉnh.

## 2. Cấu trúc

```text
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Ví dụ:

```text
feat(order): add order cancellation
fix(payment): prevent duplicate transactions
test(customer): cover invalid email validation
docs(api): document pagination parameters
```

`scope` có thể bỏ qua khi thay đổi ảnh hưởng toàn repository hoặc không có phạm vi rõ ràng:

```text
build: upgrade Java version
ci: add pull request verification
```

## 3. Commit type

Chỉ sử dụng các type sau:

| Type | Sử dụng khi |
|---|---|
| `feat` | Thêm hoặc mở rộng hành vi mà người dùng hay hệ thống có thể sử dụng. |
| `fix` | Sửa hành vi sai hoặc lỗi đã tồn tại. |
| `refactor` | Thay đổi cấu trúc code nhưng không thêm tính năng và không sửa lỗi. |
| `test` | Chỉ thêm hoặc sửa test. |
| `docs` | Chỉ thay đổi tài liệu. |
| `build` | Thay đổi dependency, build script hoặc cấu hình công cụ build. |
| `ci` | Thay đổi pipeline hoặc cấu hình CI/CD. |
| `perf` | Cải thiện hiệu năng mà không thay đổi contract. |
| `style` | Chỉ thay đổi định dạng code, khoảng trắng hoặc import; không thay đổi hành vi. |
| `chore` | Công việc bảo trì không phù hợp với các type trên. |
| `revert` | Hoàn tác một commit trước đó. |

Không dùng `chore` thay cho một type cụ thể đã có.

## 4. Scope

`scope` mô tả khu vực chính bị thay đổi, ví dụ:

- Module hoặc service.
- Feature hoặc business capability.
- Thư viện hoặc hạ tầng cụ thể.
- `api`, `database`, `security`, `docs` hoặc `build`.

Ví dụ:

```text
feat(catalog): support product filtering
fix(database): correct migration checksum
test(authentication): cover expired access tokens
```

Quy tắc:

- Viết chữ thường.
- Dùng một danh từ ngắn.
- Không dùng mã công việc như `task-01`, `w3` hoặc tên cá nhân.
- Không liệt kê nhiều scope trong cùng một commit.
- Nếu commit chạm nhiều khu vực không liên quan, nên tách thành nhiều commit.

## 5. Description

Description phải:

- Viết bằng tiếng Anh đơn giản.
- Bắt đầu bằng động từ ở dạng nguyên thể.
- Mô tả kết quả của thay đổi, không liệt kê tên file.
- Viết chữ thường ở đầu câu, trừ tên riêng hoặc định danh kỹ thuật.
- Không kết thúc bằng dấu chấm.
- Không dài quá 72 ký tự cho toàn bộ dòng tiêu đề.

Ưu tiên:

```text
feat(order): add order cancellation
fix(payment): reject expired payment requests
refactor(customer): extract email validation policy
```

Tránh:

```text
feat(order): added some new code.
fix: bug fix
update files
changes
WIP
```

## 6. Body

Body là tùy chọn. Chỉ thêm khi tiêu đề chưa đủ giải thích:

- Vì sao thay đổi cần thiết.
- Hành vi trước và sau thay đổi.
- Ràng buộc hoặc quyết định quan trọng.
- Ảnh hưởng mà reviewer cần biết.

Body không nên đọc lại danh sách file đã thay đổi.

Ví dụ:

```text
fix(payment): prevent duplicate transaction creation

Reuse the existing transaction when the same idempotency key and
request payload are received.
```

## 7. Breaking change

Breaking change (thay đổi không tương thích ngược) phải có dấu `!` và footer giải thích rõ ảnh hưởng:

```text
feat(api)!: replace offset pagination with cursor pagination

BREAKING CHANGE: list endpoints no longer accept page and size parameters.
Clients must use the cursor parameter returned by the previous response.
```

Không dùng breaking change cho refactor nội bộ không làm thay đổi contract.

## 8. Issue reference

Khi dự án có hệ thống quản lý issue, đặt tham chiếu ở footer:

```text
fix(profile): reject unsupported image formats

Closes: #142
```

Có thể dùng:

```text
Refs: #142
Closes: #142
```

`Closes` chỉ được dùng khi commit thực sự hoàn thành issue.

## 9. Quy tắc tách commit

Nên tách commit khi thay đổi có nhiều mục đích độc lập, ví dụ:

```text
feat(order): add cancellation policy
test(order): cover cancellation policy
docs(order): document cancellation rules
```

Có thể để code và test bảo vệ trực tiếp hành vi đó trong cùng một commit:

```text
feat(order): add order cancellation
```

Không bắt buộc tách test khỏi code nếu việc tách làm commit trung gian không build được hoặc hành vi chưa hoàn chỉnh.

Mỗi commit phải:

- Build được.
- Không làm hỏng test hiện có.
- Không chứa file tạm, thông tin bí mật hoặc thay đổi không liên quan.
- Có thể review và hoàn tác như một đơn vị độc lập.

## 10. Ví dụ chọn type

```text
feat(search): add filtering by creation date
fix(search): preserve filters when changing pages
refactor(search): extract query specification builder
test(search): cover empty search results
docs(search): document supported filter parameters
build: upgrade the compiler plugin
ci: run integration tests on pull requests
perf(search): reduce repeated database queries
style(search): organize imports
chore: remove obsolete development script
```

## 11. Checklist

Trước khi commit, kiểm tra:

- Commit có đúng một mục đích chính không?
- `type` có phản ánh đúng bản chất thay đổi không?
- `scope` có ngắn gọn và ổn định không?
- Description có nói rõ thay đổi mang lại điều gì không?
- Tiêu đề có dùng động từ nguyên thể và không kết thúc bằng dấu chấm không?
- Breaking change đã được đánh dấu và giải thích chưa?
- Commit có chứa thay đổi hoặc file không liên quan không?
- Code có build được và các test liên quan đã chạy thành công không?
