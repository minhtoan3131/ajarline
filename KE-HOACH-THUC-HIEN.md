# KẾ HOẠCH THỰC HIỆN DỰ ÁN (BÀI TẬP LỚN)

**Đồ án chuyên ngành - Học kỳ 7 - Năm học 2026-2027**

- **Tên đề tài/dự án:** Xây dựng website chia sẻ học liệu và tổng hợp tin tức công nghệ
- **Lớp học phần:** Đồ án chuyên ngành - N09
- **Mã nhóm:** 6

## Thông tin nhân sự

| STT | Họ và tên | Mã sinh viên | Vai trò trong nhóm | Nhiệm vụ chính phụ trách |
|---:|---|---|---|---|
| 1 | Nguyễn Minh Toàn | 20232703 | Nhóm trưởng, thành viên | Chịu trách nhiệm 100% dự án. |

## I. TỔNG QUAN DỰ ÁN (PROJECT OVERVIEW)

### 1. Lý do thực hiện dự án

- Website được xây dựng từ nhu cầu học và tra cứu kiến thức công nghệ bằng tiếng Việt theo cách có hệ thống. Nội dung công nghệ hiện thường nằm rải rác ở nhiều nguồn, khó xác định trình tự học và khó tìm lại đúng tài liệu khi người đọc diễn đạt cùng một vấn đề bằng các cách khác nhau. Dự án hướng tới xây dựng một website tập trung học liệu có cấu trúc và tin tức công nghệ được tổng hợp thủ công, đồng thời chú trọng khả năng truy xuất nội dung phù hợp thay vì chỉ tìm theo từ khóa đơn giản.
- Đối tượng hưởng lợi chính là sinh viên CNTT, người mới học lập trình, người chuyển ngành và lập trình viên ở giai đoạn đầu cần học hoặc tra cứu kiến thức kỹ thuật bằng tiếng Việt.

### 2. Mục tiêu dự án theo tiêu chí SMART

- **S (Specific - Cụ thể):** Xây dựng phiên bản website có thể sử dụng thực tế, gồm hai phạm vi chính: học liệu công nghệ và tin tức công nghệ tổng hợp thủ công; hỗ trợ tổ chức học liệu theo Series - Module - Article, đọc nội dung Markdown, quản trị/xuất bản nội dung và truy xuất nội dung trên toàn hệ thống.
- **M (Measurable - Đo lường được):** Hoàn thành các luồng chính từ tạo/xuất bản nội dung đến người đọc tìm và mở được Article, Module, Series hoặc News; có bộ lọc theo loại nội dung; xây dựng bộ truy vấn kiểm thử để so sánh chất lượng tìm kiếm; triển khai được website bằng tên miền thật trên môi trường self-host.
- **A (Achievable - Khả thi):** Dự án được thực hiện cá nhân với stack: Java, Spring Boot, React và PostgreSQL; ưu tiên công nghệ mã nguồn mở/công cụ miễn phí, phạm vi được giới hạn trong 8 tuần và có phương án giảm cấp về tìm kiếm toàn văn nếu phần ngữ nghĩa gặp khó khăn.
- **R (Relevant - Thực tế):** Vận dụng trực tiếp các nội dung của một phần mềm: phân tích yêu cầu, thiết kế kiến trúc và dữ liệu, xây dựng ứng dụng web/API, xử lý dữ liệu và truy xuất thông tin, kiểm thử, triển khai và vận hành sản phẩm.
- **T (Time-bound - Thời hạn):** Kế hoạch thực hiện từ 14/10/2026 đến thời điểm nộp báo cáo từ 07/12/2026; thi vấn đáp theo lịch Khảo thí.

## II. KẾ HOẠCH TRIỂN KHAI VÀ PHÂN CHIA CÔNG VIỆC (WBS)

| STT | Giai đoạn & Công việc cụ thể | Người phụ trách | Thời gian bắt đầu | Thời gian kết thúc | Kết quả đầu ra dự kiến (Deliverables) | Trạng thái |
|---:|---|---|---|---|---|---|
| 1 | Xây dựng đề cương | Nguyễn Minh Toàn | 14/10/2026 | 15/10/2026 | Đề cương hoàn chỉnh | Xong |
| 2 | Nộp đề cương | Nguyễn Minh Toàn | 15/10/2026 | 16/10/2026 | Đề cương đã nộp | Xong |
| 3 | Bảo vệ đề cương | Nguyễn Minh Toàn | 16/10/2026 | 19/10/2026 | Góp ý và nội dung cần điều chỉnh sau bảo vệ | Xong |
| 4 | Khảo sát, phân tích yêu cầu và thiết kế hệ thống | Nguyễn Minh Toàn | 20/10/2026 | 26/10/2026 | Yêu cầu và thiết kế hệ thống | Đang làm |
| 5 | Hoàn thiện thiết kế kỹ thuật và xây dựng các chức năng chính | Nguyễn Minh Toàn | 27/10/2026 | 23/11/2026 | Thiết kế kỹ thuật và các chức năng chính | Chưa làm |
| 6 | Kiểm thử, hoàn thiện sản phẩm và viết báo cáo | Nguyễn Minh Toàn | 24/11/2026 | 30/11/2026 | Sản phẩm đã kiểm thử và bản thảo báo cáo | Chưa làm |
| 7 | Hoàn thiện Bài tập lớn theo ý kiến của giảng viên | Nguyễn Minh Toàn | 30/11/2026 | 07/12/2026 | Bài tập lớn hoàn thiện theo góp ý | Chưa làm |
| 8 | Nộp Báo cáo | Nguyễn Minh Toàn | 07/12/2026 | - | Báo cáo chính thức đã nộp | Chưa làm |
| 9 | Thi vấn đáp | Nguyễn Minh Toàn | Theo lịch Khảo thí | Theo lịch Khảo thí | Hoàn thành thi vấn đáp | Chưa làm |

## III. CÁC MỐC KIỂM TRA TIẾN ĐỘ QUAN TRỌNG (MILESTONES)

- **Mốc 1 (14/10/2026 - 19/10/2026):** Xây dựng, nộp và bảo vệ đề cương.
- **Mốc 2 (20/10/2026 - 26/10/2026):** Khảo sát, phân tích yêu cầu và thiết kế hệ thống.
- **Mốc 3 (27/10/2026 - 23/11/2026):** Hoàn thiện thiết kế kỹ thuật và xây dựng các chức năng chính.
- **Mốc 4 (24/11/2026 - 30/11/2026):** Kiểm thử, hoàn thiện sản phẩm và viết báo cáo.
- **Mốc 5 (30/11/2026 - 07/12/2026):** Hoàn thiện Bài tập lớn theo ý kiến của giảng viên và nộp báo cáo.
- **Mốc 6 (Theo lịch Khảo thí):** Thi vấn đáp.

## IV. QUẢN LÝ RỦI RO (RISK MANAGEMENT)

| Tình huống rủi ro có thể xảy ra | Mức độ ảnh hưởng | Giải pháp phòng ngừa / Xử lý của nhóm |
|---|---|---|
| Phạm vi dự án hoặc phần truy xuất nâng cao quá lớn, không kịp trong 8 tuần. | Cao | Khóa phạm vi tối thiểu theo từng mốc; ưu tiên luồng học liệu, tin tức và tìm kiếm. Tính năng phụ được cắt trước khi ảnh hưởng deadline. |
| Semantic/hybrid retrieval cho chất lượng chưa ổn định hoặc tích hợp model gặp lỗi. | Cao | Làm full-text search trước; xây bộ truy vấn kiểm thử sớm; luôn duy trì fallback lexical để sản phẩm vẫn hoạt động. |
| Máy self-host, Docker, mạng hoặc dữ liệu gặp sự cố trước buổi demo. | Vừa | Lưu source trên Git, tài liệu trên Drive, backup PostgreSQL; chuẩn bị script khởi động/kiểm tra và phương án demo local nếu đường public gặp sự cố. |

## V. TIÊU CHÍ ĐÁNH GIÁ ĐÓNG GÓP (KPI & PEER REVIEW)

Dự án được thực hiện cá nhân nên không áp dụng đánh giá chéo giữa các thành viên. Người thực hiện chịu trách nhiệm 100% đối với việc phân tích, thiết kế, lập trình, kiểm thử, triển khai, nội dung và báo cáo.
