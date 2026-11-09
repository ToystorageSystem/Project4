# ToyStorage frontend — UI refresh

Bản cập nhật từ nhánh `frontend`, commit gốc `532f5f2880dc5939c7efef399c1862b827bb4154`.

- Icon sidebar riêng cho từng chức năng.
- Dashboard Kho, Business Operations, Admin và Store có biểu đồ cột / donut dựa trên API hiện có.
- Login gọn, giữ luồng đổi mật khẩu và 2FA. Bỏ phần giới thiệu và chữ phụ.
- Bỏ Compact view trên toàn bộ khung ứng dụng, bỏ áp dụng lựa chọn compact đã lưu.
- Mật khẩu mới tối thiểu 6 ký tự, nhận cả chữ, số, ký tự đặc biệt; tối đa 72 byte UTF-8 theo hợp đồng backend.
- Quên mật khẩu hiển thị thông báo nhận mật khẩu tạm qua email.
- Bỏ ghi chú phụ trên các trang; giữ lỗi, dữ liệu mô tả, ghi chú đơn hàng và nội dung người dùng nhập.
- Store Manager có menu Dashboard và đăng nhập chuyển tới `/store/dashboard`.

## Chạy

Dùng Node.js 22.12 trở lên (khuyến nghị Node 24). Mở Terminal tại thư mục có `package.json`:

```bash
npm ci
npm run dev
```

Backend mặc định `http://localhost:8080`. Nếu khác, tạo `.env.local` theo `.env.example`, đặt `API_PROXY_TARGET`, rồi khởi động lại Vite. Mở URL Vite in ra.

## Mật khẩu và backend

Frontend nhận mật khẩu từ 6 ký tự. Nếu đã áp dụng gói backend trước đây có regex `[0-9]{6}` hoặc `[0-9]{6,72}`, hãy áp dụng gói `ToyStorage_Backend_Password_6Characters.zip` kèm theo vào backend `dat_backend` và khởi động lại. Bản đó thay ràng buộc mật khẩu mới bằng `@Size(min=6,max=72)`; OTP và mật khẩu tạm vẫn 6 số. Không cần thay backend nếu server của bạn đã nhận mật khẩu từ 6 ký tự bất kỳ.

## Dữ liệu biểu đồ

Biểu đồ lấy dữ liệu API, không có số liệu mẫu trong entry production. Biểu đồ trạng thái có chữ `Loaded` dùng các dòng đã tải trong trang dữ liệu; không đại diện tổng lịch sử. Biểu đồ cột đếm từng queue độc lập, không cộng các queue chồng lấn. Lỗi API hiển thị unavailable, không đổi thành số 0. Biểu đồ doanh số gần nhất hiển thị đơn vị VND, không dựng lịch sử tuần/tháng khi API chưa có dữ liệu đó.

## Kiểm tra

```bash
npm test
npm run build
```

Đã kiểm tra bằng build, 47 bài test và xem trực quan login / dashboard Kho, Business, Store, Admin với fixture nội bộ. Chưa kiểm thử với MySQL/SMTP/backend thật. File fixture dùng khi soạn giao diện không nằm trong bản đóng gói này.

Các file `integration/backend-files` và `integration/backend-auth.patch` có sẵn từ repository gốc, không phải bản sửa validation kèm lần này. Không chép các file đó vào backend đang chạy nếu chưa đối chiếu hợp đồng auth.
