# Testing Convention

## 1. Mục đích

Tài liệu này quy định các nguyên tắc chung để thiết kế, tổ chức và viết test.

Mục tiêu của bộ test là:

- Phát hiện khi hành vi đúng của hệ thống bị phá vỡ.
- Không bị lỗi chỉ vì implementation nội bộ được refactor.
- Dễ đọc, dễ chạy và dễ xác định nguyên nhân khi thất bại.
- Bảo vệ các business rule, contract và invariant quan trọng.
- Tránh test trùng lặp hoặc phụ thuộc quá mức vào chi tiết triển khai.

## 2. Nguyên tắc thiết kế

### 2.1. Test hành vi, không test chi tiết triển khai

Test nên kiểm tra những gì có thể quan sát được:

- Giá trị trả về.
- Exception hoặc lỗi được công bố.
- Trạng thái dữ liệu sau xử lý.
- State transition.
- Dữ liệu được lưu.
- Event hoặc request gửi ra bên ngoài.
- Contract của API.
- Invariant phải luôn được bảo toàn.

Không nên mặc định kiểm tra:

- Private method.
- Thứ tự gọi các method nội bộ.
- Một dependency được gọi chính xác bao nhiêu lần.
- Cách thuật toán được chia thành các class.
- Vòng lặp, stream hoặc design pattern được sử dụng bên trong.

Chỉ kiểm tra interaction khi chính interaction đó là một phần quan trọng của contract.

### 2.2. Test phải chịu được refactor

Nếu hành vi bên ngoài vẫn đúng thì test không nên thất bại chỉ vì:

- Class hoặc method được đổi tên.
- Một class được tách hoặc gộp.
- Thuật toán bên trong thay đổi.
- Dependency nội bộ được tổ chức lại.
- Implementation thay đổi nhưng contract không đổi.

### 2.3. Phân tích theo ràng buộc

Mỗi hành vi cần được phân tích theo ba nhóm:

- **Pre-condition**: điều kiện phải đúng trước khi thực hiện.
- **Post-condition**: kết quả phải đúng sau khi thực hiện thành công.
- **Invariant**: điều kiện luôn phải đúng dù xử lý thành công hay thất bại.

Không cần tạo một test riêng cho mọi dòng code. Chỉ tạo test cho các trường hợp có khả năng bảo
vệ một hành vi hoặc ràng buộc có ý nghĩa.

## 3. Phân loại và tách luồng chạy test

Build bắt buộc tách Unit Test và Integration Test thành hai luồng chạy độc lập.

| Loại test | Hậu tố bắt buộc | Luồng chạy |
| --- | --- | --- |
| Unit Test | `*Test` | Luồng unit test nhanh |
| Integration Test | `*IT` | Luồng integration test |

Chỉ sử dụng hai hậu tố `*Test` và `*IT`. Không tạo thêm hậu tố riêng cho API test, component test,
system test hoặc end-to-end test. Các test có sử dụng hạ tầng thật, application context, database,
network hoặc nhiều thành phần tích hợp phải dùng hậu tố `*IT`.

Build phải bảo đảm:

- Luồng unit test chỉ chạy các file hoặc class kết thúc bằng `Test`.
- Luồng integration test chỉ chạy các file hoặc class kết thúc bằng `IT`.
- Integration Test không bị chạy lẫn trong luồng unit test.
- Có thể chạy riêng từng luồng và chạy cả hai luồng trong quá trình xác minh đầy đủ.
- Build đầy đủ phải thất bại nếu một trong hai luồng có test thất bại.

### 3.1. Unit Test

Unit Test kiểm tra một đơn vị hành vi nhỏ, chạy nhanh và không sử dụng tài nguyên bên ngoài thật.

Một unit không nhất thiết tương ứng với một class. Nó có thể bao gồm nhiều class cùng thực hiện một
hành vi.

### 3.2. Integration Test

Integration Test kiểm tra sự tích hợp với thành phần thật hoặc gần với môi trường thật, ví dụ:

- Database.
- Message broker.
- HTTP server.
- Serialization.
- Dependency injection container.
- Transaction.
- Persistence mapping.
- Luồng hoàn chỉnh qua nhiều thành phần của hệ thống.

Không mock thành phần đang là mục tiêu chính của Integration Test.

## 4. Quy ước đặt tên

### 4.1. Tên class hoặc file

```text
<ProductionTypeOrBehavior>Test
<BehaviorOrBoundary>IT
```

Ví dụ:

```java
PriceCalculatorTest
OrderPersistenceAdapterIT
CheckoutApiIT
CheckoutFlowIT
```

### 4.2. Tên test method

Sử dụng cấu trúc:

```text
<operation>_Should<ExpectedResult>_When<Condition>
```

Ví dụ:

```java
calculate_ShouldReturnDiscountedPrice_WhenCustomerIsEligible()
create_ShouldRejectRequest_WhenEmailIsInvalid()
save_ShouldRollbackChanges_WhenTransactionFails()
```

Tên test phải mô tả hành vi quan sát được, không mô tả implementation:

```java
// Avoid
create_ShouldCallRepositoryOnce_WhenRequestIsValid()

// Prefer
create_ShouldPersistOrder_WhenRequestIsValid()
```

## 5. Cấu trúc test

Mỗi test nên có một hành động chính và tuân theo bố cục Given-When-Then:

```java
@Test
void calculate_ShouldReturnDiscountedPrice_WhenCustomerIsEligible() {
    // Given
    Customer customer = eligibleCustomer();
    Money originalPrice = Money.of("100.00");

    // When
    Money result = calculator.calculate(customer, originalPrice);

    // Then
    assertThat(result).isEqualTo(Money.of("90.00"));
}
```

Quy tắc:

- `Given`: chuẩn bị dữ liệu và trạng thái cần thiết.
- `When`: thực hiện một hành động chính.
- `Then`: kiểm tra kết quả và các side effect quan trọng.
- Không đặt logic điều kiện hoặc vòng lặp phức tạp trong test.
- Không gom nhiều kịch bản không liên quan vào cùng một test.

## 6. Test double

Ưu tiên lựa chọn theo thứ tự:

1. Giá trị hoặc object thật.
2. Fake.
3. Stub.
4. Mock.

Sử dụng:

- **Fake** khi cần implementation đơn giản có trạng thái, ví dụ repository trong bộ nhớ.
- **Stub** khi dependency chỉ cần trả về dữ liệu đã xác định.
- **Mock** khi interaction với dependency chính là contract cần kiểm tra.

Tên test double nên thể hiện đúng vai trò:

```java
FakeOrderRepository
StubExchangeRateProvider
RecordingEventPublisher
```

Không lạm dụng mock để kiểm tra mọi lời gọi nội bộ. Việc verify quá nhiều interaction làm test dễ
vỡ khi refactor.

## 7. Assertion

Assertion phải tập trung vào kết quả có ý nghĩa:

```java
assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
assertThat(savedOrder.total()).isEqualTo(expectedTotal);
```

Khi kiểm tra lỗi, nên kiểm tra cả loại lỗi và thông tin quan trọng:

```java
assertThatThrownBy(() -> service.create(command))
        .isInstanceOf(InvalidOrderException.class)
        .hasMessageContaining("quantity");
```

Ưu tiên assertion trên object, collection hoặc kiểu dữ liệu có cấu trúc thay vì so sánh chuỗi thô.
Thông báo thất bại phải giúp xác định ràng buộc nào đã bị phá vỡ.

## 8. Quy tắc cho Integration Test

Integration Test phải:

- Sử dụng dependency thật hoặc gần với môi trường thật.
- Chạy độc lập với thứ tự test.
- Không phụ thuộc dữ liệu còn lại từ test trước.
- Chuẩn bị và dọn dữ liệu một cách rõ ràng.
- Không dùng chung trạng thái có thể thay đổi nếu chưa có cơ chế cô lập.
- Không mặc định dùng transaction rollback nếu cần kiểm tra commit, lock hoặc transaction thật.

Với database, ưu tiên database cùng loại với production khi SQL, transaction hoặc mapping có ý
nghĩa. Có thể sử dụng container để tạo môi trường tái lập.

## 9. Dữ liệu test

Dữ liệu test phải:

- Chỉ chứa thông tin cần thiết cho kịch bản.
- Thể hiện rõ giá trị nào quan trọng đối với hành vi đang kiểm tra.
- Không phụ thuộc thời gian, ID ngẫu nhiên hoặc môi trường nếu chưa kiểm soát được.
- Không sử dụng fixture quá lớn cho mọi test.

Có thể dùng factory hoặc builder:

```java
Order order = OrderTestData.validOrder()
        .withQuantity(2)
        .build();
```

Các dependency không ổn định như clock, ID generator và random generator nên được đưa vào qua
abstraction để test có thể kiểm soát.

## 10. Tính độc lập và ổn định

Mỗi test phải có thể:

- Chạy riêng lẻ.
- Chạy cùng toàn bộ test suite.
- Chạy lặp lại và cho cùng kết quả.
- Chạy theo thứ tự bất kỳ.
- Chạy song song nếu dự án hỗ trợ.

Không khắc phục flaky test bằng cách retry vô điều kiện. Phải tìm nguyên nhân như shared state,
thời gian, race condition, network hoặc dữ liệu không được dọn sạch.

## 11. Coverage

Coverage chỉ là tín hiệu hỗ trợ, không phải mục tiêu chất lượng độc lập.

Ưu tiên coverage cho:

- Business rule quan trọng.
- Nhánh lỗi có ý nghĩa.
- State transition.
- Transaction và rollback.
- Security rule.
- Contract với hệ thống bên ngoài.
- Các lỗi đã từng xảy ra.

Không viết test không có giá trị chỉ để tăng tỷ lệ coverage.

## 12. Checklist

- Test đang bảo vệ hành vi hoặc ràng buộc nào?
- Happy path và failure path quan trọng đã được kiểm tra chưa?
- Giá trị biên có ảnh hưởng đến kết quả không?
- Test có phụ thuộc implementation detail không?
- Test có còn đúng sau khi refactor nội bộ không?
- Mỗi test có đúng một hành động chính không?
- Tên test có thể hiện operation, kết quả và điều kiện không?
- Test có dùng đúng hậu tố `*Test` hoặc `*IT` không?
- Test có được chạy trong đúng luồng build không?
- Test có chạy độc lập và lặp lại ổn định không?
- Test double có được sử dụng đúng mục đích không?
- Integration Test có sử dụng dependency phù hợp với mục tiêu kiểm tra không?
- Khi test thất bại, nguyên nhân có dễ nhận biết không?
