# Phạm vi backend

Bản này nối các API warehouse staff tồn tại trên backend tại commit d9d8b2b511abd6090ec6df0b8dde3c5fbd6d25c2. Xem API_COVERAGE.md để biết từng thao tác và endpoint.

Hàng trả đã mở hoàn tất thật, có xác nhận vì backend cộng tồn và chuyển RECEIVED ngay. Các giới hạn còn lại: không có API báo thiếu picking/sai vị trí; không có danh sách PACKING/lịch sử chung; không có forgot-password trong nhánh gốc. Không hiển thị thao tác nháp như đã gửi.

API nghiệp vụ và kiểm tra owner/warehouse vẫn do server quyết định; nhiều annotation PreAuthorize trong nhánh gốc đang comment. UI phân role không thay thế kiểm tra quyền phía server.

Quên mật khẩu có gói backend bổ sung trong backend_password_reset, cần cài SQL/module và SMTP thật. Không được tự triển khai bởi việc giải nén app.
