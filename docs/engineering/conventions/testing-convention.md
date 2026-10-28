# Testing Convention

## 1. Mục đích và phạm vi

Tài liệu này quy định cách đặt tên, tổ chức và trình bày test trong các dự án Java.

Tài liệu chỉ quy định hình thức thống nhất. Việc lựa chọn test case, test level, test double, môi
trường kiểm thử và chiến lược kiểm thử thuộc các tài liệu hướng dẫn riêng.

Các tài liệu hướng dẫn liên quan:

- 1-test-design-and-implementation-guide.md.
- 2-integration-testing-environment-guide.md.
- 3-concurrency-testing-environment-guide.md.

## 2. Phân loại và hậu tố

Chỉ sử dụng hai hậu tố cho test class và test file:

| Loại | Hậu tố bắt buộc | Ví dụ |
| --- | --- | --- |
| Unit Test | `*Test` | `PriceCalculatorTest` |
| Integration Test | `*IT` | `OrderPersistenceAdapterIT` |

Không tạo thêm hậu tố riêng như:

```text
*IntegrationTest
*ComponentTest
*ApiTest
*E2ETest
*SystemTest
```

API test, component test, end-to-end test và system test sử dụng hậu tố `*IT` khi chúng thuộc luồng
Integration Test.

## 3. Tách luồng build

Build bắt buộc tách Unit Test và Integration Test thành hai luồng độc lập:

```text
Unit Test         → chỉ chạy *Test
Integration Test  → chỉ chạy *IT
Full verification → chạy cả hai luồng
```

Build phải bảo đảm:

- `*IT` không chạy lẫn trong luồng Unit Test.
- Có thể chạy riêng từng luồng.
- Full verification thất bại nếu một trong hai luồng có test thất bại.
- Hậu tố class hoặc file là nguồn phân loại chính.

Công cụ build cụ thể phải được cấu hình theo quy ước này. Cách cấu hình Maven, Gradle hoặc công cụ
khác thuộc tài liệu hướng dẫn của từng hệ sinh thái.

## 4. Vị trí và package

Test code phải nằm trong test source set của dự án, ví dụ:

```text
src/test/java
src/test/resources
```

Package của test phải phản ánh package hoặc boundary đang được kiểm tra:

```text
Production:
src/main/java/com/example/order/application/OrderService.java

Test:
src/test/java/com/example/order/application/OrderServiceTest.java
```

Test support dùng chung phải nằm trong package có tên rõ ràng:

```text
testsupport
fixture
```

Không đặt test utility vào package production.

## 5. Tên test class

### 5.1. Unit Test

Sử dụng cấu trúc:

```text
<ProductionTypeOrBehavior>Test
```

Ví dụ:

```java
PriceCalculatorTest
EmailAddressTest
OrderCancellationPolicyTest
```

### 5.2. Integration Test

Sử dụng cấu trúc:

```text
<BehaviorOrBoundary>IT
```

Ví dụ:

```java
OrderPersistenceAdapterIT
CheckoutApiIT
PaymentTransactionIT
CheckoutFlowIT
```

Tên class phải thể hiện đối tượng, behavior hoặc boundary được kiểm tra. Không dùng tên theo số thứ
tự, work package, sprint hoặc tên người thực hiện.

## 6. Tên test method

Sử dụng cấu trúc:

```text
<operation>_Should<ExpectedResult>_When<Condition>
```

Quy tắc:

- `<operation>` bắt đầu bằng chữ thường.
- `Should`, `When` và các thành phần tiếp theo dùng PascalCase.
- Tên phải dùng tiếng Anh đơn giản và rõ nghĩa.
- Không thêm tiền tố `test`.
- Không kết thúc tên bằng số thứ tự.
- Không mô tả private method hoặc lời gọi nội bộ.

Ví dụ đúng:

```java
calculate_ShouldReturnDiscountedPrice_WhenCustomerIsEligible()
create_ShouldRejectRequest_WhenEmailIsInvalid()
save_ShouldRollback_WhenTransactionFails()
findById_ShouldReturnEmpty_WhenRecordDoesNotExist()
```

Ví dụ không đúng:

```java
testCalculate()
testCase1()
shouldCallRepositoryOnce()
create_WhenRequestIsValid_ShouldPersistOrder()
```

## 7. Bố cục Given-When-Then

Test method phải trình bày theo thứ tự:

```text
Given → When → Then
```

Mỗi phần được phân tách bằng một dòng trống:

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

Quy ước comment phân đoạn:

```java
// Given
// When
// Then
```

Khi hành động được thực hiện trực tiếp trong assertion, sử dụng:

```java
// When / Then
```

Ví dụ:

```java
@Test
void create_ShouldRejectRequest_WhenEmailIsInvalid() {
    // Given
    CreateCustomerCommand command = commandWithEmail("invalid");

    // When / Then
    assertThatThrownBy(() -> service.create(command))
            .isInstanceOf(InvalidEmailException.class)
            .hasMessage("Email format is invalid");
}
```

## 8. Assertion

Trong cùng một dự án, test phải sử dụng thống nhất một assertion library. Đối với dự án Java, mặc
định sử dụng AssertJ trừ khi dự án đã quy định công cụ khác.

Ưu tiên assertion trực tiếp trên object, collection và kiểu dữ liệu có cấu trúc:

```java
assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);

assertThat(results)
        .extracting(OrderResult::id)
        .containsExactly(expectedId);
```

Exception assertion phải thể hiện ít nhất loại exception:

```java
assertThatThrownBy(() -> service.execute(command))
        .isInstanceOf(InvalidCommandException.class);
```

Không trộn nhiều assertion style trong cùng một test class nếu không có lý do bắt buộc từ framework.

## 9. Tên test data

Test data class sử dụng cấu trúc:

```text
<DomainType>TestData
<DomainType>Fixture
<DomainType>TestBuilder
```

Ví dụ:

```java
CustomerTestData
OrderFixture
PaymentTestBuilder
```

Factory method phải mô tả trạng thái dữ liệu:

```java
validCustomer()
activeOrder()
expiredPayment()
commandWithEmail(String email)
```

Không dùng tên chung chung:

```java
createData()
getObject()
buildTest1()
sample()
```

## 10. Tên test double

Tên test double phải thể hiện vai trò:

```text
Fake<Type>
Stub<Type>
Recording<Type>
```

Ví dụ:

```java
FakeOrderRepository
StubExchangeRateProvider
RecordingEventPublisher
```

Chỉ sử dụng tiền tố `Mock` khi object thực sự là mock hoặc được tạo để đại diện cho mock được chia sẻ.

## 11. Tên Integration Test support

Base class cho Integration Test sử dụng cấu trúc:

```text
Abstract<ContextOrBoundary>IT
```

Ví dụ:

```java
AbstractDatabaseIT
AbstractWebIT
AbstractApplicationIT
```

Helper dành riêng cho test phải dùng tên mô tả trách nhiệm:

```java
DatabaseCleaner
TestClock
TestIdGenerator
HttpResponseRecorder
```

Tránh các tên chung chung như `TestUtils`, `CommonTestHelper` hoặc `BaseTest`.

## 12. Parameterized Test

Parameterized Test vẫn sử dụng quy ước tên method thông thường:

```java
@ParameterizedTest
@ValueSource(strings = {"", "invalid", "example@"})
void parse_ShouldRejectValue_WhenEmailIsInvalid(String value) {
    // When / Then
    assertThatThrownBy(() -> EmailAddress.parse(value))
            .isInstanceOf(InvalidEmailException.class);
}
```

Khi khai báo display name, nội dung phải ngắn và dùng tiếng Anh:

```java
@ParameterizedTest(name = "[{index}] rejects {0}")
```

Không dùng display name để thay thế một tên method không rõ nghĩa.

## 13. Các tên bị cấm

Không sử dụng:

```text
Test1
TestClass
BaseTest
CommonTest
TestUtils
testMethod1
shouldWork
shouldPass
happyPath
w1Test
sprint2Test
```

Tên test phải giữ được ý nghĩa sau khi task, sprint hoặc giai đoạn triển khai kết thúc.

## 14. Checklist

- Class hoặc file có dùng đúng hậu tố `*Test` hoặc `*IT` không?
- Test có chạy trong đúng luồng build không?
- Package test có phản ánh production package hoặc boundary không?
- Tên class có nói rõ đối tượng hoặc behavior được kiểm tra không?
- Tên method có đúng mẫu `<operation>_Should<ExpectedResult>_When<Condition>` không?
- Test có được trình bày theo Given-When-Then không?
- Assertion style có thống nhất không?
- Test data và test double có tên thể hiện đúng vai trò không?
- Có tên chung chung, tên theo số thứ tự hoặc tên theo task không?
