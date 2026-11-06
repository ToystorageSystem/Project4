# Kích hoạt quên mật khẩu trên backend

Nhánh backend được đối chiếu: d9d8b2b511abd6090ec6df0b8dde3c5fbd6d25c2. Hiện chỉ có login/logout; gói này bổ sung hai endpoint dùng chung mọi role, không đổi quyền nghiệp vụ kho.

1. Sao lưu backend hoặc tạo branch. Chạy từ thư mục gói Flutter:

   python3 backend_password_reset/install.py /duong/dan/Project4-backend

   Script thêm spring-boot-starter-mail vào pom.xml, chép mã Java và profile; tạo bản .backup khi thay file. Không sửa SecurityConfig hiện có. Script cũng chép test Java vào src/test để chạy cùng bộ test backend.

2. Chạy backend_password_reset/schema.sql trên đúng MySQL đang dùng. Bảng users phải tồn tại.
3. Cấu hình biến môi trường SMTP_HOST, SMTP_PORT (mặc định 587), SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM. Dùng SMTP thực hỗ trợ STARTTLS; không đặt mật khẩu SMTP trong Git.
4. Thêm password-reset vào các Spring profile đang dùng, ví dụ SPRING_PROFILES_ACTIVE=dev,password-reset, rồi khởi động lại backend. Giữ nguyên profile dev/prod hiện có.
5. Nếu dùng web, RESET_ALLOWED_ORIGINS là danh sách origin thật phân cách bằng dấu phẩy. Native iOS/Android không cần origin trình duyệt.
6. Chạy Maven test/build của backend. Gói này chưa được build hoặc gửi email SMTP thực trong môi trường tạo file.

## Hợp đồng API

POST /api/auth/forgot-password, JSON {"email":"staff@example.com"}
→ 200, thông báo chung dù email không tồn tại. Email chỉ gửi khi tài khoản ACTIVE. Yêu cầu lại trong 60 giây không gửi thêm.

POST /api/auth/reset-password, JSON {"email":"staff@example.com","token":"toan-bo-ma-trong-email","newPassword":"mat-khau-moi-12-ky-tu"}
→ 200 khi thành công; 400 nếu mã sai/hết hạn/đã dùng hoặc dữ liệu không hợp lệ.

Mã ngẫu nhiên 256-bit, chỉ lưu SHA-256 trong DB, hết hạn 15 phút, xóa sau khi đổi mật khẩu trong cùng transaction, BCrypt cho mật khẩu. Mã mới thay mã cũ. Không trả mã trong API hoặc ghi mã ra log.

Đây là khôi phục bằng mã dài để sao chép từ email, không phải OTP 6 số. SMTP cần được cấu hình trước; giao diện không giả vờ đã gửi mã nếu API chưa tồn tại.

Session đăng nhập cũ giữ cơ chế timeout hiện có (15 phút); chức năng này chưa chủ động thu hồi mọi session trên các thiết bị. Nếu triển khai nhiều backend hoặc yêu cầu thu hồi tức thì, tích hợp Spring Session/session registry trước khi phát hành. Thêm giới hạn tần suất IP ở reverse proxy khi đưa ra Internet.

## Kiểm tra tích hợp bắt buộc

- Email hợp lệ nhận mã, email không tồn tại vẫn trả thông báo chung.
- Gửi lại trong 60 giây không gửi thêm email.
- Mã sai/hết hạn bị từ chối; mã đúng dùng được đúng một lần.
- Mật khẩu cũ không đăng nhập được, mật khẩu mới đăng nhập được.
- Tài khoản INACTIVE không nhận mã; đăng nhập/logout và các API staff vẫn giữ quyền.
