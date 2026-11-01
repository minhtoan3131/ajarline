# Convention — Explicit Configuration & Framework Defaults

**Mã tài liệu:** ENG-CONV-EXPLICIT-001  
**Phiên bản:** 1.2  
**Phạm vi:** Mọi dự án, ngôn ngữ và framework; ví dụ minh họa chủ yếu bằng Java/Spring.  
**Mục đích:** Giúp người đọc nhìn vào code hiểu được **ý định thiết kế** và những **hành vi quan trọng** mà không phải đoán các giá trị mặc định ẩn.

## 1. Nguyên tắc cốt lõi

**Quy tắc mặc định của convention: NÊN khai báo tường minh ngay cả khi framework đã hỗ trợ hành vi tương đương theo mặc định, nếu khai báo đó giúp người đọc hiểu ngay ý định và hành vi mong đợi.**

Ví dụ, khi một API được thiết kế chỉ nhận và trả JSON, việc ghi rõ `consumes = application/json` và `produces = application/json` là có giá trị, dù cấu hình hiện tại của Spring có thể hoạt động đúng khi bỏ chúng.

Đây là ưu tiên về **khả năng đọc hiểu và duy trì ý định thiết kế**, không phải yêu cầu cấu hình tất cả mặc định.

> **NÊN khai báo tường minh các hành vi có ý nghĩa đối với người đọc, ngay cả khi framework đã cung cấp đúng hành vi đó theo mặc định.** Mục tiêu là để code tự nói rõ ý định và hành vi mong đợi, không buộc người đọc phải ghi nhớ những mặc định ẩn của framework.
>
> **KHÔNG cần khai báo mọi mặc định.** Chỉ bỏ qua khai báo khi nó thực sự không tăng độ rõ ràng, hoặc gây trùng lặp, rủi ro và khó bảo trì.

Mục tiêu không phải là viết ít code nhất, cũng không phải cấu hình mọi thứ. Mục tiêu là **dễ hiểu đúng, khó cấu hình sai và dễ bảo trì**.

Phân biệt:

- **Hành vi mặc định (default behavior):** Framework tự áp dụng khi code không chỉ rõ.
- **Khai báo tường minh (explicit declaration):** Code chủ động xác định giá trị hoặc quy tắc.
- **Chính sách của dự án (project policy):** Hành vi hệ thống **phải giữ đúng**, dù framework có mặc định gì.

**Quy tắc quan trọng:** Không coi một mặc định hiện tại là chính sách dự án nếu chưa có quyết định rõ ràng. Ngược lại, **không được xem một khai báo là thừa chỉ vì giá trị của nó bằng mặc định hiện tại của framework**.

## 2. Mức độ áp dụng

Dùng ba từ khóa thống nhất:

- **MUST (Bắt buộc):** Phải tuân thủ; chỉ được ngoại lệ khi có quyết định được ghi nhận.
- **SHOULD (Nên):** Thông thường phải làm; có thể không làm nếu lý do hợp lý được ghi rõ.
- **MAY (Có thể):** Tùy ngữ cảnh và lợi ích thực tế.

### 2.1. MUST — Khai báo rõ các quyết định quan trọng

Phải khai báo hoặc quy định tại **một nơi có thẩm quyền, dễ tìm** đối với các hành vi:

1. **API contract:** media type được hỗ trợ, status/error contract, quy tắc versioning, các header có ý nghĩa nghiệp vụ.
2. **Security:** quy tắc xác thực/phân quyền, CSRF, session, cookie, CORS, xử lý secret và các giới hạn chống lạm dụng.
3. **Dữ liệu:** schema, tính toàn vẹn, quy tắc transaction, cách ghi và xóa dữ liệu, migration.
4. **Đồng thời và nhất quán:** lock, retry, idempotency, concurrency precondition, thứ tự xử lý khi quan trọng.
5. **Vận hành:** timeout, retry policy, kích thước request, giới hạn tài nguyên, hành vi khi dependency lỗi nếu chúng ảnh hưởng đến tính đúng hoặc độ an toàn.
6. **Khác biệt so với mặc định:** mọi thay đổi default có chủ đích phải được thể hiện rõ bằng cấu hình/code hoặc tài liệu cấu hình được kiểm soát phiên bản.

**Lưu ý:** “Khai báo rõ” không có nghĩa phải lặp lại trong từng class. Có thể quy định tại API schema, file cấu hình tập trung, module policy hoặc tài liệu kiến trúc; phải có đường dẫn rõ từ code tới nơi định nghĩa.

### 2.2. SHOULD — Khai báo tường minh ngay cả khi framework đã có mặc định

**Đây là quy tắc ưu tiên của convention.** Nếu một giá trị hoặc hành vi giúp người đọc hiểu **hệ thống chủ định hoạt động như thế nào**, **NÊN khai báo rõ nó dù bỏ khai báo thì chương trình hiện tại vẫn chạy giống hệt**.

Ưu tiên khai báo khi thỏa mãn một hoặc nhiều điều kiện:

- Khi đọc code, người khác hoặc chính người viết trong tương lai sẽ phải nhớ hoặc tra tài liệu mới biết hành vi đó.
- Việc khai báo làm rõ **ý định thiết kế** (ví dụ API chủ định chỉ nhận/trả JSON), thay vì để người đọc suy đoán từ cấu hình ngầm.
- Hành vi là một giả định quan trọng để hiểu đúng luồng xử lý, đầu vào, đầu ra hoặc lifecycle.
- Mặc định có thể phụ thuộc phiên bản framework, dependency, profile hay cấu hình toàn cục.
- Cú pháp khai báo ngắn, rõ và không gây trùng lặp đáng kể.

**Ví dụ:** Dù Spring Boot hiện có thể tự serialize response thành JSON, vẫn **NÊN** viết `produces = MediaType.APPLICATION_JSON_VALUE` nếu đây là định dạng API mà dự án cố ý cam kết. Tương tự, **NÊN** viết `consumes = MediaType.APPLICATION_JSON_VALUE` để thể hiện đầu vào được phép nhận.

**Nguyên tắc review:** Không yêu cầu xóa một khai báo **chỉ vì “framework đã mặc định như vậy”**. Muốn bỏ nó, cần chỉ ra rằng khai báo không giúp hiểu đúng hơn hoặc gây bất lợi cụ thể.

### 2.3. Tiêu chí áp dụng khai báo tường minh trùng mặc định

Đối với một hành vi mà framework đã hỗ trợ theo mặc định, **SHOULD** khai báo nếu:

- Đó là một quyết định có chủ đích của dự án, không chỉ là hành vi ngẫu nhiên của cấu hình hiện tại.
- Một người đọc lại code sau nhiều tháng sẽ hiểu rõ hơn nhờ khai báo đó.
- Khai báo có tác dụng thực tế và nằm tại đúng nơi framework xử lý.
- Chi phí duy trì và nguy cơ xung đột của khai báo là chấp nhận được.

**Không nên loại bỏ khai báo chỉ vì nó bằng default.** Nếu muốn bỏ, người review cần nêu được bất lợi cụ thể: thừa lặp, không có tác dụng, sai phạm vi, dễ xung đột hoặc gây rủi ro khi nâng cấp.

### 2.4. SHOULD — Khai báo khi mặc định dễ gây hiểu sai

Càng nên khai báo tường minh nếu:

- Người đọc khó suy ra hành vi nếu không biết sâu framework.
- Hành vi khác nhau theo môi trường, cấu hình, phiên bản hoặc dependency.
- Một giá trị mặc định có tác động lớn khi thay đổi.
- Nhiều thành viên phải thống nhất cùng cách hiểu hoặc cùng quy tắc.
- Có khả năng việc thêm thư viện/cấu hình mới làm thay đổi hành vi.

Nếu khai báo tường minh không khả thi, bổ sung test hoặc mô tả ngắn tại nơi thích hợp.

### 2.5. MAY — Giữ mặc định khi thật sự rõ ràng

Có thể không khai báo nếu **đồng thời** thỏa mãn:

- Hành vi đơn giản, quen thuộc với người dùng framework trong dự án.
- Không phải quy tắc nghiệp vụ, bảo mật hay API contract quan trọng.
- Việc viết thêm chỉ tạo code lặp hoặc nhiễu.
- Dễ xác minh bằng test, IDE hoặc tài liệu framework.

Ví dụ: Không nhất thiết thêm `@Autowired` cho constructor duy nhất trong Spring khi dự án đã thống nhất dùng constructor injection.

## 3. Quy trình quyết định trước khi thêm cấu hình

Với mỗi giá trị mặc định hoặc annotation dự kiến khai báo, hỏi theo thứ tự:

1. **Đây có phải là yêu cầu bắt buộc của sản phẩm, API, bảo mật hoặc vận hành?** → Có: **MUST**.
2. **Nếu default thay đổi, hệ thống có thể sai, không an toàn hoặc phá vỡ tương thích?** → Có: **MUST**, kèm test nếu có thể.
3. **Ngay cả khi framework đã có mặc định tương đương, khai báo có giúp người đọc hiểu ngay hành vi hoặc ý định thiết kế mà không cần nhớ default?** → Có: **SHOULD khai báo**.
4. **Khai báo thêm có gây trùng lặp, xung đột hoặc che khuất các quyết định quan trọng?** → Có: ưu tiên cấu hình tập trung hoặc giữ mặc định.
5. **Nó có thực sự làm rõ hành vi hay chỉ làm code dài hơn?** → Nếu chỉ dài hơn: **MAY** bỏ.

**Khi còn phân vân:** Ưu tiên chỉ rõ **ý định thiết kế tại một nơi**, và dùng test chứng minh hành vi thực tế thay vì rải nhiều khai báo giống nhau.

## 4. Quy tắc triển khai

### Rule 1 — Phân biệt “điều framework đang làm” với “điều hệ thống yêu cầu”

Một hành vi ngầm định chỉ là chi tiết triển khai cho đến khi được xác nhận là yêu cầu. Nếu là yêu cầu, phải được ghi nhận và kiểm chứng.

### Rule 2 — Không xóa khai báo có chủ đích chỉ vì trùng mặc định

Khai báo trùng với default của framework **không tự động là code thừa**. Nếu khai báo giúp đọc hiểu contract hoặc ý định mà không phải tra cứu, hãy giữ lại. Chỉ bỏ khi nó không còn đem lại giá trị hoặc làm phát sinh chi phí/rủi ro cụ thể.

### Rule 3 — Một chính sách có một nguồn sự thật

Không sao chép cùng giá trị ở nhiều class hoặc nhiều file nếu có thể đặt tập trung. Ví dụ một timeout nên được cấu hình tại một nơi và inject/sử dụng lại.

### Rule 4 — Cấu hình phải thật sự có tác dụng

Không khai báo chỉ để “trông có vẻ an toàn”. Cần biết framework có đọc cấu hình đó không, phạm vi tác động đến đâu, có bị nơi khác ghi đè không.

### Rule 5 — Không ghi đè mặc định bảo mật nếu chưa hiểu hậu quả

Không tắt CSRF, auth checks, encoding, validation hay security headers chỉ để giảm code hoặc tránh lỗi test. Nếu phải thay đổi, ghi rõ lý do và rủi ro.

### Rule 6 — Test các hợp đồng quan trọng

Các quy tắc thuộc nhóm **MUST** cần được kiểm chứng ở lớp phù hợp (unit/integration/contract test). Annotation hoặc cấu hình chỉ là ý định; **test xác nhận hành vi**.

### Rule 7 — Comment giải thích “tại sao”, không lặp lại “làm gì”

Chỉ thêm comment khi có quyết định khó hiểu hoặc khác biệt với mặc định. Tránh comment đọc lại đúng cú pháp của annotation.

### Rule 8 — Xem lại cấu hình khi nâng cấp

Khi nâng framework hoặc thay dependency, kiểm tra các giá trị default đã dựa vào, các cấu hình đã ghi đè và những thay đổi về hành vi. Không giả định giá trị tường minh luôn an toàn hơn bản mặc định mới.

### Rule 9 — Nhất quán theo cấp dự án

Một nhóm chức năng nên dùng cùng quy ước. Nếu có ngoại lệ, ghi lại lý do ở gần nơi áp dụng hoặc trong tài liệu quyết định thiết kế.

## 5. Ví dụ minh họa

### 5.1. Nên khai báo: HTTP API có hợp đồng JSON rõ ràng

Ngay cả khi bỏ `consumes`/`produces` thì Spring hiện tại vẫn nhận/trả JSON đúng, việc khai báo **vẫn có giá trị**: người đọc thấy ngay các media type được thiết kế cho endpoint và không phải suy luận từ các converter được cài đặt.

```java
@PostMapping(
    value = "/articles",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public ArticleResponse create(@RequestBody ArticleRequest request) {
    // ...
}
```

Lý do: Thể hiện endpoint chỉ nhận và cung cấp JSON, đồng thời ảnh hưởng đến lựa chọn mapping/content negotiation. **Không có nghĩa** chỉ riêng annotation này bảo đảm mọi response lỗi đều có JSON; phần xử lý lỗi vẫn cần thiết kế và kiểm thử riêng.

Nếu dự án có API contract tập trung và mọi endpoint cùng một media type, có thể chọn cấu hình/chính sách thống nhất thay vì sao chép không cần thiết; lựa chọn phải nhất quán.

### 5.2. Nên khai báo: một timeout là yêu cầu vận hành

```yaml
server:
  servlet:
    session:
      timeout: 2h
```

Lý do: Thời hạn session là chính sách có thể ảnh hưởng trực tiếp đến trải nghiệm và bảo mật, không nên chỉ phụ thuộc vào default chưa được xác nhận.

### 5.3. Có thể giữ mặc định: constructor injection

```java
@Service
public class ArticleService {
    private final ArticleRepository repository;

    public ArticleService(ArticleRepository repository) {
        this.repository = repository;
    }
}
```

Với một constructor, trong cách dùng Spring thông thường không cần thêm `@Autowired`. Thêm annotation ở đây thường không làm rõ thêm chính sách hệ thống.

### 5.4. Không nên khai báo: cấu hình thừa hoặc có hại

```java
// KHÔNG làm vậy chỉ để đơn giản hóa test.
http.csrf(csrf -> csrf.disable());
```

Nếu ứng dụng sử dụng xác thực dựa trên cookie và có nguy cơ CSRF, việc tắt bảo vệ mà không có đánh giá kiến trúc có thể làm suy giảm an toàn.

### 5.5. Khi không cần lặp lại cấu hình: dùng chính sách tập trung

Nếu toàn bộ hệ thống dùng cùng giới hạn request, không hard-code một con số ở nhiều controller. Đặt giới hạn tại lớp phù hợp rồi kiểm chứng tại boundary; tài liệu chỉ dẫn nơi quản lý chính sách.

## 6. Checklist khi code review

- [ ] Các quyết định quan trọng đã được thể hiện rõ ở một nơi dễ tìm.
- [ ] Đã phân biệt rõ mặc định framework và quy tắc bắt buộc của dự án.
- [ ] Các hành vi quan trọng đã được khai báo tường minh **ngay cả khi đang bằng mặc định framework**, nếu điều đó giúp người đọc hiểu mà không cần tra cứu.
- [ ] Không có đề xuất xóa khai báo với lý do duy nhất là “framework mặc định đã làm rồi”.
- [ ] Những giá trị dựa vào default đã được đánh giá rủi ro khi đổi phiên bản/cấu hình.
- [ ] Không có cấu hình lặp hoặc mâu thuẫn giữa nhiều lớp.
- [ ] Không có cấu hình “cho có” nhưng không ảnh hưởng đến thực tế.
- [ ] Các thay đổi bảo mật được giải thích và có kiểm chứng phù hợp.
- [ ] Những hành vi thuộc API contract hoặc dữ liệu quan trọng có test.
- [ ] Comment giải thích lý do, không chỉ nhắc lại code.
- [ ] Ngoại lệ đối với convention có lý do rõ ràng.

## 7. Cách ghi nhận ngoại lệ

Khi không theo convention, ghi ngắn gọn:

```text
Decision: Keep the framework default for <behavior>.
Reason: <why explicit configuration adds no value / creates risk>.
Verification: <test, documentation or review reference>.
Review trigger: <framework upgrade, new API format, security change, etc.>.
```

Không cần tạo tài liệu ngoại lệ cho những quyết định nhỏ, không có tác động; chỉ ghi nhận khi ảnh hưởng đến **MUST** hoặc một **SHOULD** quan trọng.

## 8. Kết luận

**Ưu tiên tường minh ngay cả khi đã có mặc định, nếu sự tường minh giúp hiểu đúng hệ thống:**

- **Rõ yêu cầu:** Điều gì bắt buộc phải đúng?
- **Rõ trách nhiệm:** Framework làm gì, ứng dụng quyết định gì?
- **Rõ bằng chứng:** Hành vi đó đã được kiểm chứng như thế nào?

> **Prefer explicit intent, even when it matches a framework default. Omit only when the declaration adds no clarity or creates unnecessary cost.**
