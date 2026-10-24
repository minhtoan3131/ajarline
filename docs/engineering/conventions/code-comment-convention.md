# Code Comment Convention

## 1. Mục đích

Comment giúp người đọc hiểu:

- Vì sao code tồn tại.
- Contract hoặc hành vi quan trọng phải được giữ.
- Quyết định không hiển nhiên.
- Ràng buộc nghiệp vụ hoặc kỹ thuật.
- Failure behavior.
- Side effect.
- Giới hạn bảo mật, hiệu năng hoặc tài nguyên.
- Workaround cần thiết do hệ thống bên ngoài hoặc thư viện.

Comment không được dùng để đọc lại code hoặc bù đắp cho tên class, method và biến không rõ ràng.

## 2. Ngôn ngữ

JavaDoc và code comment phải:

- Viết bằng tiếng Anh đơn giản, rõ ràng.
- Sử dụng câu hoàn chỉnh.
- Tránh từ viết tắt không phổ biến.
- Giữ nguyên tên class, method, field, protocol và thuật ngữ kỹ thuật.
- Không chứa thông tin bí mật, token, mật khẩu hoặc dữ liệu cá nhân.

Ví dụ:

```java
// Do not retry the request because the provider may have processed it
// even when the response was not received.
```

## 3. Khi nào cần JavaDoc

Nên viết JavaDoc cho:

- Public API.
- Interface và port thể hiện contract giữa các thành phần.
- Class có trách nhiệm hoặc giới hạn không thể hiện đầy đủ qua tên.
- Domain model có invariant quan trọng.
- Method có validation, side effect hoặc failure behavior đáng chú ý.
- Logic liên quan đến security, transaction, retry, timeout hoặc concurrency.
- Workaround và hành vi phụ thuộc hệ thống bên ngoài.
- Package có boundary hoặc dependency rule không hiển nhiên.

Không bắt buộc viết JavaDoc cho:

- Getter và setter đơn giản.
- Constructor chỉ gán field.
- Field có tên đã rõ nghĩa.
- Private helper đơn giản.
- Configuration hoặc DTO không có hành vi đặc biệt.
- Method mà JavaDoc chỉ lặp lại tên method.

## 4. JavaDoc cho class

JavaDoc của class nên giải thích:

- Trách nhiệm chính.
- Lý do class tồn tại nếu không hiển nhiên.
- Boundary mà class thuộc về.
- Side effect hoặc dependency bên ngoài.
- Quy tắc quan trọng mà thay đổi sau này phải giữ.

Ví dụ đơn giản:

```java
/**
 * Calculates the final price after applying eligible discounts.
 */
public final class PriceCalculator {
}
```

Ví dụ có contract quan trọng:

```java
/**
 * Stores documents through the external document provider.
 *
 * <p>The adapter streams document content without loading the complete file
 * into memory. A result is accepted only when the provider confirms that the
 * document was stored successfully.</p>
 *
 * <p>The upload is not retried because the provider may have stored the
 * document even when the client did not receive the response.</p>
 */
public final class DocumentStorageAdapter {
}
```

Không nên viết:

```java
/**
 * Document storage adapter.
 */
public final class DocumentStorageAdapter {
}
```

Comment trên không cung cấp thêm thông tin so với tên class.

## 5. JavaDoc cho method

Với method đơn giản, chỉ cần mô tả ngắn nếu tên chưa đủ rõ:

```java
/**
 * Creates a valid order used by the tests.
 */
private Order validOrder() {
    // ...
}
```

Với method có contract phức tạp, JavaDoc nên giải thích:

- Caller phải cung cấp gì.
- Method bảo đảm điều gì khi thành công.
- Side effect nào xảy ra.
- Failure case quan trọng.
- Giới hạn timeout, retry hoặc resource.
- Yêu cầu nullability, format hoặc phạm vi giá trị.

Ví dụ:

```java
/**
 * Reserves the requested quantity for one product.
 *
 * <p>The reservation is created only when sufficient stock is available.
 * A successful result guarantees that the available quantity was reduced
 * atomically.</p>
 *
 * @param productId identifier of the product to reserve
 * @param quantity positive quantity to reserve
 * @return the created reservation
 * @throws ProductNotFoundException when the product does not exist
 * @throws InsufficientStockException when available stock is too low
 */
public Reservation reserve(ProductId productId, int quantity) {
    // ...
}
```

## 6. Quy tắc sử dụng JavaDoc tag

Chỉ sử dụng các tag khi chúng cung cấp thông tin có ích:

- `@param`: format, phạm vi, nullability hoặc ý nghĩa không hiển nhiên.
- `@return`: ý nghĩa của kết quả hoặc điều kiện đặc biệt.
- `@throws`: exception thuộc contract mà caller cần xử lý.
- `@see`: liên kết tới type hoặc contract liên quan khi thực sự cần thiết.

Không cần viết tag chỉ để lặp lại tên:

```java
// Avoid
/**
 * @param orderId the order ID
 * @return the order
 */
```

Không liệt kê mọi unchecked exception có thể phát sinh từ implementation nội bộ.

## 7. Code comment bên trong method

Comment bên trong method phải giải thích **vì sao**, không đọc lại **code đang làm gì**.

Nên comment khi có:

- Quyết định nghiệp vụ không hiển nhiên.
- Safety rule.
- Workaround.
- Protocol detail.
- Thứ tự xử lý bắt buộc.
- Lý do không retry hoặc không rollback.
- Thuật toán khó hiểu.
- Giới hạn của thư viện hoặc framework.
- Hành vi có vẻ dư thừa nhưng không được xóa.

Ví dụ tốt:

```java
// Persist the operation ID before the external call so recovery can
// determine whether the request may already have been processed.
operationRepository.save(operation);
```

```java
// Do not retry here. A timeout does not prove that the provider rejected
// the request, and retrying may create a duplicate side effect.
provider.execute(request);
```

Ví dụ không cần thiết:

```java
// Save the order.
orderRepository.save(order);

// Check whether the customer is active.
if (customer.isActive()) {
    // ...
}
```

## 8. Vị trí và định dạng

Comment phải được đặt ngay trước đoạn code mà nó giải thích:

```java
// The provider requires the checksum to be calculated from the original
// content before compression.
String checksum = checksumCalculator.calculate(content);
```

Quy tắc:

- Dùng `//` cho comment ngắn bên trong method.
- Có thể dùng nhiều dòng `//` cho một giải thích dài.
- Dùng `/** ... */` cho JavaDoc.
- Hạn chế dùng `/* ... */` cho comment implementation.
- Không đặt comment ở cuối một dòng code nếu làm giảm khả năng đọc.
- Không dùng comment để tạo tiêu đề trang trí hoặc phân cách giả.

Tránh:

```java
value++; // Increment value
```

```java
// ==========================================================
// PROCESSING
// ==========================================================
```

Nên tách method hoặc class nếu cần comment phân vùng quá lớn.

## 9. TODO và FIXME

`TODO` chỉ được sử dụng cho công việc thực sự được hoãn lại và phải nói rõ việc còn thiếu.

Ưu tiên kèm mã issue:

```java
// TODO(#142): Replace the temporary in-memory store with persistent storage.
```

`FIXME` biểu thị code đã biết là không đúng hoặc không an toàn:

```java
// FIXME(#187): This comparison ignores the configured customer time zone.
```

Quy tắc:

- Không dùng comment chung chung như `TODO: improve this`.
- Không dùng tên cá nhân làm thông tin duy nhất.
- `FIXME` không nên được merge nếu lỗi ảnh hưởng tính đúng đắn hoặc an toàn.
- Công việc dài hạn phải được quản lý bằng issue, không chỉ bằng comment.

## 10. Commented-out code

Không giữ code đã bị comment:

```java
// oldService.process(request);
// legacyRepository.save(entity);
```

Git đã lưu lịch sử thay đổi. Code không còn sử dụng phải được xóa.

Chỉ giữ đoạn code bị vô hiệu hóa khi có lý do kỹ thuật tạm thời đặc biệt, được giải thích rõ và có issue theo dõi.

## 11. Comment trong test

JavaDoc của test nên giải thích hành vi hoặc quy tắc được bảo vệ:

```java
/**
 * Verifies that an unsuccessful payment does not change the order state.
 */
@Test
void pay_ShouldPreserveOrderState_WhenPaymentFails() {
    // ...
}
```

Không mô tả từng dòng setup hoặc assertion.

Có thể dùng comment `Given`, `When`, `Then` để làm rõ cấu trúc:

```java
@Test
void create_ShouldPersistOrder_WhenRequestIsValid() {
    // Given
    CreateOrderCommand command = validCommand();

    // When
    Order result = service.create(command);

    // Then
    assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
}
```

Không bắt buộc thêm JavaDoc nếu tên test đã mô tả đầy đủ hành vi và test dễ đọc.

## 12. Duy trì comment

Khi thay đổi code, phải kiểm tra và cập nhật các comment liên quan.

Comment phải bị sửa hoặc xóa khi:

- Behavior đã thay đổi.
- Workaround không còn cần thiết.
- Constraint không còn đúng.
- Tên hoặc contract được nhắc tới đã thay đổi.
- Code đã đủ rõ và comment không còn cung cấp giá trị.

Comment lỗi thời nguy hiểm hơn việc không có comment vì nó khiến người đọc hiểu sai hệ thống.

## 13. Checklist

Trước khi thêm hoặc giữ một comment, kiểm tra:

- Comment có cung cấp thông tin mà code chưa thể hiện không?
- Nó giải thích lý do, contract hoặc ràng buộc quan trọng không?
- Có thể làm code rõ hơn bằng cách đổi tên hoặc tách method thay vì thêm comment không?
- Comment có đúng với behavior hiện tại không?
- Nội dung có đủ cụ thể để người khác hiểu không?
- Comment có vô tình chứa dữ liệu nhạy cảm không?
- `TODO` hoặc `FIXME` có issue theo dõi không?
- Comment có còn đúng sau lần thay đổi này không?

