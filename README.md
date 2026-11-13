# Chapter — website bán sách

Ứng dụng Java 21 / Spring Boot 4.1.2, giao diện Thymeleaf tiếng Việt, PostgreSQL 18.7.

## Chạy trên máy Windows

Yêu cầu JDK 21 và Docker Desktop đang chạy Linux containers. Từ thư mục dự án:

```powershell
.\Run-Local.ps1
```

Script tạo cấu hình local nếu chưa có, dùng mật khẩu ngẫu nhiên, bật PostgreSQL rồi chạy ứng
dụng. Nó đọc `.env` như dữ liệu thuần, không thực thi nội dung file. Không in mật khẩu ra log.
Lần đầu cần mạng để Maven Wrapper tải dependency. Không cần cài Maven toàn máy.

- Cửa hàng: <http://127.0.0.1:8080/books>
- Tạo tài khoản khách: <http://127.0.0.1:8080/register>; đăng nhập: <http://127.0.0.1:8080/login>.
- Quản trị: <http://127.0.0.1:8080/admin/login>
- Tài khoản/mật khẩu local: `CHAPTER_ADMIN_USERNAME` và `CHAPTER_ADMIN_PASSWORD` trong `.env`.
- Database: `127.0.0.1:15432`; cổng mặc định 5432 bị Windows chặn trên máy đã kiểm tra.
- Dừng app bằng Ctrl+C; dừng database bằng `docker compose stop db`. Volume được giữ nguyên.

Nếu PowerShell chặn script do execution policy, có thể dùng lệnh chỉ áp dụng cho tiến trình đó:

```powershell
powershell -ExecutionPolicy Bypass -File .\Run-Local.ps1
```

`.env` được ignore và không được commit. Không dùng dữ liệu khách hàng thật cho demo.
Profile `local` chỉ bind localhost và tạo 16 sách hư cấu khi catalog đang trống; không thêm lại
hay ghi đè dữ liệu đã có. Tài khoản quản trị chỉ được bootstrap nếu username đó chưa tồn tại;
đổi password trong `.env` không đổi mật khẩu của tài khoản đã tạo trong database.

## Chức năng

- Tìm tên sách/tác giả, lọc thể loại, phân trang; giá VND, trạng thái còn/hết hàng.
- Giỏ hàng theo phiên, kiểm tra số lượng và tồn kho, tổng tiền bằng số nguyên có chặn tràn.
- Đặt hàng khách vãng lai, COD, phí giao hàng 0 ₫ trong MVP; xác nhận riêng tư theo phiên.
- Transaction và khóa PostgreSQL chống bán quá tồn, tạo đơn trùng và hoàn kho nhiều lần.
- Quản trị có đăng nhập, giới hạn thử sai, CSRF, sửa/ẩn sách, quản lý thể loại, tồn kho và đơn.

Trạng thái đơn: PENDING → CONFIRMED → SHIPPING → COMPLETED. Có thể hủy trước SHIPPING.
Giá/tên sách trong đơn được lưu cố định, không đổi khi sửa sản phẩm.

## Tài khoản khách hàng

- Đăng ký bằng họ tên, email và mật khẩu.
- Đăng nhập, xem thông tin tài khoản và đăng xuất.
- Giữ giỏ sách khi đăng nhập trong cùng phiên trình duyệt.

Mật khẩu khách dài 12–64 ký tự Unicode, giữ nguyên khoảng trắng, lưu bằng PBKDF2 có salt.
Email không phân biệt hoa thường; đăng ký thành công chuyển sang đăng nhập, không tự đăng nhập.
Trang `/account` hiển thị họ tên và email của chính bạn. Khách hàng và quản trị dùng hai nguồn
xác thực riêng. Đăng nhập sai 5 lần trong 5 phút với cùng email/địa chỉ nguồn sẽ tạm chặn 5 phút.

Có thể đăng nhập cùng tài khoản trên nhiều thiết bị. Mỗi phiên có giỏ, thông tin giao hàng và
quyền xem biên nhận riêng. Đăng xuất có bước xác nhận và xóa dữ liệu mua hàng của phiên hiện tại;
các thiết bị khác vẫn hoạt động. Đăng nhập giữ giỏ và draft đã lưu trong cùng trình duyệt.
Thông tin đang gõ chưa gửi ở trang thanh toán chưa được lưu tự động. Bạn vẫn mua sách không cần
tài khoản. Chưa có xác minh email, quên mật khẩu, sửa hồ sơ, lịch sử đơn hay đồng bộ giỏ.

## Kiểm thử

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd -Pbrowser-tests test-compile exec:java '-Dexec.mainClass=com.microsoft.playwright.CLI' '-Dexec.classpathScope=test' '-Dexec.args=install chromium'
.\mvnw.cmd -Pbrowser-tests verify
.\mvnw.cmd -Pperformance-tests verify
```

Surefire chạy unit `*Test`; Failsafe chạy integration `*IT` trên PostgreSQL Testcontainers
riêng, không dùng dữ liệu Compose. Docker không chạy thì integration test phải thất bại,
không bỏ qua. Profile browser chạy thêm Chromium headless; profile performance đo riêng
1.000 sách và 20 browser context độc lập. Không có frontend build hay Node runtime cho app.

Báo cáo kiểm thử được tạo trong `target/` sau khi chạy các lệnh trên;
ảnh browser trong `target/screenshots/`, số đo tải trong `target/performance/catalog.json`.
Tài liệu Spec Kit và hướng dẫn AI được giữ cục bộ, không đưa vào repository.

## Lưu ý khi vận hành

Cookie mặc định Secure/HttpOnly/SameSite=Lax; chỉ profile local/test tắt Secure để dùng HTTP.
Môi trường khác cần HTTPS và các biến `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`; bootstrap quản trị cần hai biến CHAPTER_ADMIN ở trên. Mật khẩu
bootstrap tối thiểu 12 ký tự và tối đa 72 byte UTF-8. Không có tài khoản production mặc định.

Phiên hết hạn sau 30 phút không hoạt động; mất phiên thì mất giỏ và quyền xem xác nhận khách
vãng lai. Hãy lưu mã đơn để liên hệ cửa hàng. MVP chạy một instance; bộ đếm thử sai nằm trong
bộ nhớ và được đặt lại khi khởi động. Chưa có gửi email, thanh toán online, tích hợp vận chuyển,
hay tra cứu đơn công khai. Cần quy định lưu/xóa
dữ liệu khách trước khi nhận đơn thật. Dự án chưa được triển khai lên hosting.
