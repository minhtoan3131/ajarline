# Convention — API Documentation: Prefer Source-Derived Metadata

**Mã tài liệu:** ENG-CONV-API-DOC-001  
**Phiên bản:** 1.0  
**Phạm vi:** HTTP API sử dụng code-first hoặc source-generated documentation.  
**Ví dụ chính:** Java, Spring MVC, Bean Validation, Springdoc và OpenAPI.

## 1. Mục đích

Convention này giúp tài liệu API chính xác nhưng không làm source bị ngập trong annotation hoặc
metadata chỉ phục vụ documentation.

Nguyên tắc cốt lõi:

```text
Để runtime source tự mô tả API được càng nhiều càng tốt.

Chỉ thêm documentation metadata ở nơi runtime source chưa thể hiện đủ public contract
hoặc documentation tool không thể suy ra chính xác.
```

Mục tiêu là đạt đồng thời:

```text
runtime behavior chính xác
+
generated API documentation chính xác
+
source dễ đọc
+
ít duplication
+
ít nguy cơ contract drift
```

## 2. Phạm vi áp dụng

Convention này áp dụng mặc định cho dự án theo một trong các hướng:

- code-first;
- source-generated OpenAPI;
- source code và passing tests là operational source of truth;
- OpenAPI/Swagger được sinh từ source để làm product API documentation.

Nguyên tắc cốt lõi có thể áp dụng cho nhiều ngôn ngữ và framework. Java/Spring trong tài liệu chỉ
là ví dụ minh họa.

Convention này không được áp dụng máy móc cho dự án contract-first, nơi file OpenAPI hoặc một API
schema khác là nguồn sự thật chính và source được sinh hoặc kiểm tra từ contract đó. Dự án
contract-first phải có convention quản trị contract riêng.

## 3. Từ khóa quy ước

Tài liệu sử dụng ba mức độ:

- **MUST (Bắt buộc):** Phải tuân thủ; chỉ được ngoại lệ khi có quyết định và lý do được ghi nhận.
- **SHOULD (Nên):** Cách làm mặc định; có thể khác khi có lý do kỹ thuật hợp lý.
- **MAY (Có thể):** Tùy bối cảnh, quy mô và lợi ích thực tế.

## 4. Nguyên tắc cốt lõi

### 4.1. MUST — Runtime behavior và documentation không được mâu thuẫn

Generated API document không được công bố behavior mà runtime không cung cấp, bao gồm:

- endpoint chưa triển khai;
- request field không được runtime tiếp nhận;
- validation không được runtime thực thi;
- response status, header hoặc error code không tồn tại;
- authentication, authorization hoặc CSRF requirement không đúng với security configuration;
- schema khác với serialization behavior thực tế.

Documentation annotation chỉ mô tả ý định. Nó không thay thế runtime implementation hoặc test.

### 4.2. SHOULD — Ưu tiên source-derived metadata

Không lặp lại bằng OpenAPI/Swagger annotation những thông tin mà documentation tool đã suy ra
chính xác từ runtime source.

Ưu tiên để contract được thể hiện bằng:

- HTTP mapping thật;
- request parameter thật;
- request và response type thật;
- DTO hoặc record thật;
- Java type hoặc type system tương đương;
- validation constraint thật;
- serialization configuration thật;
- security configuration thật;
- status và header được runtime thực sự tạo ra.

Chỉ thêm documentation metadata khi generated document còn thiếu, sai hoặc chưa diễn đạt đủ ý
nghĩa cần công bố.

### 4.3. MUST — “Có thể suy ra” phải được kiểm chứng

Không được chỉ nhìn source rồi giả định documentation tool chắc chắn sinh đúng schema.

Khả năng suy ra phụ thuộc vào:

- phiên bản framework và documentation dependency;
- Bean Validation integration;
- serialization configuration;
- nullability;
- generic type;
- polymorphism;
- schema resolver;
- cấu hình toàn cục của dự án.

Chỉ coi metadata là được suy ra chính xác sau khi generated document thực tế đã được kiểm tra. Với
contract quan trọng, phải có test bảo vệ kết quả cần giữ.

## 5. Những gì nên để runtime source tự mô tả

Ví dụ:

```java
@PostMapping(
        value = "/articles",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public ArticleResponse create(
        @Valid @RequestBody ArticleRequest request) {
    // Delegate to the application use case.
}
```

```java
public record ArticleRequest(
        @NotBlank
        @Size(max = 300)
        String title,

        @NotBlank
        String slug
) {
}
```

Documentation tool thường có thể đọc từ source này:

```text
POST /articles

request body
→ ArticleRequest

response body
→ ArticleResponse

title
→ String
→ không được rỗng
→ tối đa 300 ký tự

slug
→ String
→ không được rỗng
```

Nếu generated document đã thể hiện chính xác, không lặp lại:

```java
@Schema(
        type = "string",
        maxLength = 300,
        requiredMode = Schema.RequiredMode.REQUIRED)
String title
```

Việc cùng lưu `300` ở `@Size` và `@Schema` tạo hai nơi sở hữu cùng một quy tắc và làm tăng nguy cơ
contract drift.

## 6. Validation phải là runtime behavior thật

Validation constraint chỉ được xem là public runtime contract khi validation thực sự được kích
hoạt tại boundary phù hợp.

Ví dụ, DTO có `@NotBlank` và `@Size` nhưng controller không kích hoạt Bean Validation có thể tạo ra
tài liệu đúng hình thức nhưng runtime không từ chối request vi phạm.

Khi dùng Spring MVC, boundary thường cần `@Valid` hoặc cơ chế method validation phù hợp:

```java
public ArticleResponse create(
        @Valid @RequestBody ArticleRequest request) {
    // ...
}
```

Quy tắc:

- **MUST NOT** thêm validation constraint chỉ để generated schema đẹp hơn.
- **MUST** bảo đảm constraint được runtime thực thi nếu documentation công bố nó là contract.
- **SHOULD** kiểm tra validation behavior bằng test ở level phù hợp.
- **SHOULD** kiểm tra generated schema nếu requiredness, limit hoặc format là contract quan trọng.

## 7. Khi nào cần thêm documentation metadata

Thêm metadata khi runtime source chưa thể hiện đủ public contract hoặc documentation tool không thể
suy ra chính xác, ví dụ:

- stable `operationId`;
- summary hoặc description có ý nghĩa nghiệp vụ;
- header được xử lý ngoài controller signature;
- security requirement;
- response status đặc biệt;
- error response có semantic riêng;
- response header;
- format hoặc example cần thiết để sử dụng API đúng;
- polymorphic schema;
- quy tắc mà type system hoặc validation không thể biểu diễn đầy đủ.

Ví dụ stable operation identity và mục đích nghiệp vụ:

```java
@Operation(
        operationId = "loginAdmin",
        summary = "Create an Admin session")
```

Ví dụ CSRF header được security filter xử lý và không xuất hiện trong method signature:

```java
@Parameter(
        name = "X-CSRF-TOKEN",
        in = ParameterIn.HEADER,
        required = true,
        description = "Token returned by the CSRF bootstrap operation.")
```

Ví dụ response có header đặc biệt:

```java
@ApiResponse(
        responseCode = "429",
        description = "Login is temporarily locked",
        headers = @Header(
                name = "Retry-After",
                description = "Seconds until another attempt is allowed"))
```

Các metadata trên có giá trị vì Java method signature và Spring MVC mapping không thể hiện đầy đủ
semantic đó.

## 8. Header và parameter

Nếu parameter đã nằm trong runtime method signature, ưu tiên để documentation tool đọc nó:

```java
public ArticleResponse update(
        @RequestHeader("If-Match") String ifMatch,
        @Valid @RequestBody ArticleRequest request) {
    // ...
}
```

Chỉ thêm OpenAPI metadata cho phần chưa rõ, ví dụ format hoặc semantic đặc biệt:

```java
public ArticleResponse update(
        @Parameter(
                description = "Strong decimal revision, for example \"3\".",
                example = "\"3\"")
        @RequestHeader("If-Match") String ifMatch,
        @Valid @RequestBody ArticleRequest request) {
    // ...
}
```

Nếu header được xử lý bởi filter, interceptor, gateway hoặc framework security và không xuất hiện
trong method signature, documentation metadata thường là cần thiết.

## 9. Security configuration và generated documentation

Security configuration là nguồn sự thật của runtime security behavior. Tuy nhiên, documentation
tool thường không thể suy ra đầy đủ security requirement từ filter chain hoặc authorization rule.

Ví dụ, từ một rule như:

```java
requestMatchers("/api/admin/**").authenticated()
```

documentation tool chưa chắc tự sinh đúng:

```yaml
security:
  - adminSession: []
```

Quy tắc:

- **MUST** để runtime security configuration quyết định behavior thật.
- **MUST NOT** làm yếu security runtime chỉ để Swagger UI gọi API thuận tiện hơn.
- **SHOULD** bổ sung OpenAPI security scheme và requirement nếu tool không thể suy ra.
- **MUST** bảo đảm security metadata không mâu thuẫn với runtime behavior.
- **SHOULD** kiểm tra security metadata quan trọng trên generated document.

## 10. Typed runtime model và cấu trúc mơ hồ

Các kiểu như:

```java
JsonNode
Map<String, Object>
Object
```

có thể hợp lệ về runtime nhưng làm documentation tool không suy ra được schema rõ ràng.

Nếu phù hợp với behavior, **SHOULD** dùng typed runtime model thật:

```text
AdminLoginRequest
ApiProblem
ArticleRequest
ArticleResponse
```

Mục tiêu là:

```text
runtime model thật
        +
documentation cùng đọc model đó
```

Không được refactor runtime chỉ để documentation đẹp hơn nếu thay đổi đó làm sai hoặc làm mất
behavior hiện tại, ví dụ:

- không còn từ chối unknown field;
- không còn phân biệt missing và explicit `null`;
- thay đổi error code;
- thay đổi serialization;
- làm lộ secret qua logging hoặc generated method;
- thay đổi validation order.

Model chỉ phục vụ documentation là ngoại lệ, không phải mặc định. Nếu buộc phải dùng vì runtime
structure là dynamic hoặc không thể biểu diễn trực tiếp:

- phải ghi rõ model đó chỉ mô tả wire contract;
- không được giả vờ rằng runtime đang sử dụng model đó;
- phải có test chứng minh schema mô tả đúng behavior thực tế;
- phải xem lại quyết định khi runtime boundary được refactor.

## 11. Quan hệ với explicit runtime configuration

Convention này không yêu cầu bỏ các khai báo runtime có chủ đích chỉ vì documentation tool có thể
suy ra chúng.

Các khai báo như:

- `@GetMapping`, `@PostMapping`, `@PutMapping`;
- `consumes` và `produces`;
- `@RequestHeader`;
- `@ResponseStatus`;
- validation constraint;
- serialization annotation;
- security configuration;

có thể trực tiếp ảnh hưởng hoặc làm rõ runtime behavior. Chúng không phải documentation duplication
chỉ vì Springdoc cũng đọc được chúng.

Điều cần tránh là lặp lại cùng một sự thật bằng metadata OpenAPI riêng khi generated document đã
đọc đúng khai báo runtime đó.

Ví dụ, project policy có thể chủ động khai báo:

```java
@PostMapping(
        value = "/articles",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
```

Không cần lặp lại cùng media type bằng OpenAPI annotation nếu generated document đã chính xác.

Khi convention này và explicit-configuration convention cùng áp dụng:

```text
khai báo runtime behavior quan trọng tại nơi framework thực thi
        +
không sao chép lại bằng documentation metadata nếu không cần
```

## 12. Không để documentation annotation che khuất code chính

Annotation dài hơn method không tự động là vi phạm. Controller thường mỏng trong khi HTTP contract
có thể có nhiều status, header và security rule.

Tuy nhiên, các dấu hiệu sau yêu cầu review:

- controller khó đọc vì metadata quá dài;
- cùng một response documentation bị lặp lại nhiều nơi;
- người đọc khó tìm request mapping, parameter và method body;
- metadata lặp lại type hoặc constraint đã có;
- một thay đổi contract phải sửa cùng giá trị ở nhiều vị trí;
- annotation mô tả behavior nhưng runtime không có.

Khi đó **SHOULD** cân nhắc theo thứ tự:

1. xóa metadata mà tool đã suy ra chính xác;
2. dùng typed runtime model rõ ràng hơn nếu behavior cho phép;
3. đặt security hoặc metadata chung ở cấp controller;
4. tái sử dụng OpenAPI component cho contract ổn định và thực sự dùng chung;
5. dùng customizer cho cross-cutting metadata khi có test bảo vệ;
6. tách API contract interface khỏi implementation khi quy mô đủ lớn.

Không tạo abstraction chỉ để giảm vài dòng annotation. Mọi abstraction phải cải thiện ownership,
khả năng đọc hoặc khả năng duy trì một cách rõ ràng.

## 13. Response và error documentation

Return type thường giúp tool suy ra success response body:

```java
public ArticleResponse get(UUID id)
```

Nhưng tool có thể không biết:

- create trả `201` thay vì `200`;
- response có `Location` hoặc `ETag`;
- `412` biểu thị stale precondition;
- `428` biểu thị thiếu precondition;
- `429` có `Retry-After`;
- exception handler nào áp dụng cho operation nào;
- error code và semantic cụ thể.

Chỉ document các response thực sự có thể xảy ra theo runtime behavior. Không liệt kê status theo
thói quen hoặc sao chép từ endpoint khác.

Response và error dùng chung **MAY** được quản lý bằng reusable component hoặc customizer. Cơ chế
tái sử dụng không được che giấu sự khác biệt giữa các operation và phải được kiểm tra trên generated
document.

## 14. Verification

### 14.1. Không coi annotation là bằng chứng cuối cùng

Annotation chỉ thể hiện ý định. Public document được sinh ở runtime mới là output cần kiểm tra.

Tùy mức độ quan trọng, test có thể xác nhận:

- endpoint có xuất hiện hay không;
- endpoint chưa triển khai không xuất hiện;
- HTTP method và path;
- stable `operationId`;
- request và response schema quan trọng;
- required parameter và header;
- response status hoặc response header đặc biệt;
- security scheme và security requirement.

### 14.2. Không test từng annotation riêng lẻ

Test public output, không khóa chi tiết implementation của annotation.

Không cần assert mọi description, example hoặc thứ tự schema property nếu chúng không phải
compatibility contract. Test quá chi tiết làm dependency upgrade khó khăn mà không tăng độ tin cậy
tương ứng.

### 14.3. Mức test phù hợp

- Unit test có thể kiểm tra customizer hoặc schema-building logic riêng nếu logic đủ phức tạp.
- Integration test nên đọc generated OpenAPI từ application context hoặc HTTP endpoint thật.
- Contract test nên tập trung vào compatibility behavior mà consumer phụ thuộc.

## 15. Quy trình quyết định trước khi thêm annotation

Hỏi theo thứ tự:

1. Thông tin này có thuộc public API contract không?
2. Runtime source hiện đã biểu đạt thông tin này chưa?
3. Generated document thực tế đã suy ra chính xác chưa?
4. Nếu bỏ annotation, document có sai hoặc thiếu thông tin quan trọng không?
5. Có cách biểu đạt rõ hơn bằng runtime mapping, type, validation hoặc serialization không?
6. Annotation có tạo thêm một nguồn sự thật trùng lặp không?
7. Thông tin này có cần test trên generated document không?

Quy tắc quyết định:

```text
Generated document đã đúng từ runtime source
→ không thêm metadata lặp lại

Generated document thiếu semantic mà runtime source không thể hiện
→ bổ sung OpenAPI metadata

Runtime source đang mơ hồ và có thể cải thiện mà không đổi behavior
→ ưu tiên cải thiện runtime source

Runtime source phải giữ cấu trúc dynamic
→ dùng metadata/schema bổ sung có kiểm chứng
```

## 16. Code-first và contract-first

### 16.1. Code-first hoặc source-generated

Operational flow thường là:

```text
runtime source + passing tests
→ generated OpenAPI
→ Swagger UI hoặc consumer tooling
```

Convention này áp dụng trực tiếp.

### 16.2. Contract-first

Operational flow có thể là:

```text
reviewed OpenAPI contract
→ generated interface/client/server stub
→ implementation
→ conformance test
```

Trong trường hợp này, không dùng nguyên tắc “source-derived” để hạ vai trò của contract. Thay vào
đó, source phải conform với contract và pipeline phải phát hiện drift.

Một dự án có thể kết hợp hai hướng, nhưng phải xác định rõ artifact nào sở hữu từng quyết định và
cách thay đổi được đồng bộ.

## 17. Checklist review

### Runtime source

- [ ] HTTP mapping, parameter và media type quan trọng được thể hiện tại runtime boundary.
- [ ] Request và response dùng type rõ ràng khi phù hợp.
- [ ] Validation được runtime thực thi, không chỉ xuất hiện trong documentation.
- [ ] Serialization behavior khớp schema được công bố.
- [ ] Không tạo model giả chỉ để làm Swagger đẹp nếu không có lý do và verification rõ ràng.
- [ ] Không làm yếu runtime security để phục vụ documentation tooling.

### Documentation metadata

- [ ] Chỉ bổ sung phần contract mà generated document chưa suy ra chính xác.
- [ ] Stable `operationId`, security, header và error semantic quan trọng được mô tả rõ.
- [ ] Không lặp type, requiredness hoặc constraint đã được source-derived chính xác.
- [ ] Annotation không che khuất request mapping và logic chính.
- [ ] Metadata dùng chung không bị sao chép ở nhiều nơi nếu có ownership tập trung hợp lý.
- [ ] Không document endpoint hoặc behavior chưa tồn tại.

### Verification

- [ ] Generated OpenAPI đã được kiểm tra, không chỉ suy đoán từ source.
- [ ] Schema quan trọng đúng với runtime model và serialization.
- [ ] Security requirement đúng với runtime behavior.
- [ ] Response status và header đặc biệt đúng với implementation.
- [ ] Contract quan trọng có test ở level phù hợp.
- [ ] Test không khóa chi tiết trình bày không cần thiết.

## 18. Ngoại lệ

Khi không theo một MUST hoặc SHOULD quan trọng, ghi nhận ngắn gọn tại nơi phù hợp:

```text
Decision: <quyết định khác convention>
Reason: <lý do kỹ thuật hoặc sản phẩm>
Runtime truth: <nơi behavior thật được thực thi>
Documentation: <cách public contract được sinh hoặc quản lý>
Verification: <test hoặc review chứng minh hai phía không drift>
Review trigger: <điều kiện cần xem lại quyết định>
```

Không cần tạo tài liệu ngoại lệ riêng cho thay đổi nhỏ. Chỉ ghi nhận khi quyết định ảnh hưởng đến
public contract, security, compatibility hoặc một SHOULD quan trọng.

## 19. Kết luận

Convention này ưu tiên:

```text
Runtime source thể hiện behavior thật.

Documentation tận dụng runtime source đó tối đa.

Metadata riêng chỉ bổ sung phần source chưa đủ để mô tả public contract.

Generated document phải được kiểm tra thay vì được tin theo giả định.
```

Đối với source-generated API documentation, đây là cách giữ tài liệu chính xác mà vẫn bảo vệ khả
năng đọc, giảm duplication và hạn chế contract drift.
