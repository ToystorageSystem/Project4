<<<<<<< HEAD

=======
# Toy Storage System — ứng dụng Flutter

Bản giao diện màu cam #F25D19, logo gốc lấy từ public/logo.svg nhánh frontend (315cccf3b8f4204d7cd1a066dcc26af60f963e9a). Màn đăng nhập dùng chung mọi role: email, mật khẩu, hiện/ẩn mật khẩu và Quên mật khẩu. Không có nút khám phá/demo hoặc ô cấu hình kết nối trên giao diện.

## Chạy trên Mac / iPhone giả lập

Yêu cầu Flutter hỗ trợ mobile_scanner 7.5.0, Xcode, Simulator và CocoaPods nếu Flutter fallback sang Pods. Máy trước đó dùng Flutter 3.44.4; nên giữ bản SDK đang chạy được. Chừa đủ dung lượng đĩa để Xcode build.

Giải nén vào thư mục mới (đừng ghi đè thư mục đang chỉnh), mở Terminal tại thư mục warehouse_staff_flutter:

```bash
flutter pub get
bash run_ios.sh
```

Gói đã có sẵn mã nguồn ios/android/web, iOS Deployment Target 15.5 và quyền camera/ảnh/mạng. Không cần chạy lại flutter create. Đây là mã nguồn app, không phải IPA/APK đã ký. Không cần tải lại Simulator hiện có.

Có thể chạy `bash update_existing.sh` để cấu hình lại quyền, format, analyze và test. `setup.sh` vẫn được giữ cho trường hợp cần tạo lại platform. Nếu dùng Swift Package Manager của Flutter 3.44, không cần tự tạo Podfile; nếu Flutter yêu cầu CocoaPods thì dùng bản CocoaPods bạn đã cài.

Nếu iPhone giả lập có tên khác:

```bash
bash run_ios.sh "iPhone 17 Pro Max"
```

## Máy chủ API

Người dùng cuối không nhập server trong app. Cấu hình tập trung ở lib/app_config.dart hoặc --dart-define=API_BASE_URL.

Debug mặc định: iOS Simulator/web localhost:8080; Android Emulator 10.0.2.2:8080. iOS đã bật NSAllowsLocalNetworking cho phát triển localhost, không bật NSAllowsArbitraryLoads toàn cục. Điện thoại thật dùng HTTPS hostname của server và cấu hình:

```bash
flutter run --dart-define=API_BASE_URL=https://your-real-api.example
```

Release bắt buộc truyền API_BASE_URL thực; nếu thiếu app hiển thị thông báo cần quản trị cấu hình, không âm thầm gọi localhost. Giữ backend đang chạy khi đăng nhập.

## Quên mật khẩu

Đã có giao diện gửi mã qua email, nhập mã, mật khẩu mới, xác nhận mật khẩu và thông báo kết quả. Backend hiện tại CHƯA có API này: cần cài phần bổ sung trong backend_password_reset/ theo README của thư mục đó, tạo bảng và cấu hình SMTP trước khi sử dụng thật. Gói backend không được tự đẩy lên GitHub hoặc tự triển khai.

## Phân role

Login dùng chung, không chọn role tùy ý. authorities từ server quyết định màn tiếp theo. ROLE_WAREHOUSE_STAFF / WAREHOUSE_STAFF mở nghiệp vụ staff. Role khác đăng nhập được và thấy trang thông báo không gian làm việc sẽ bổ sung; không gọi các API staff. Màn nghiệp vụ manager/admin/store chưa nằm trong gói mobile này. Điểm mở rộng ở isWarehouseStaff / RoleWorkspace trong lib/auth.dart. Backend vẫn phải kiểm tra quyền từng endpoint.

## Nghiệp vụ staff được giữ lại

- Tổng quan, danh sách và tìm công việc, kéo để tải lại, lỗi riêng từng danh sách.
- Nhập kho: bắt đầu, ghi thực nhận/hư hỏng, ghi chú chênh lệch, hoàn tất kiểm nhận.
- Cất hàng, soạn hàng: scan hoặc nhập mã, vị trí, số lượng; kiểm tra tiến độ.
- Tạo kiện, scan sản phẩm vào kiện, niêm phong, in QR PDF và gửi quản lý.
- Kiểm kê lần đầu/đếm lại, gửi quản lý; không tự điều chỉnh tồn.
- Hàng trả, ảnh bằng chứng, hoàn tất nhận cập nhật tồn theo backend; báo cáo hỏng, thiếu hàng nhập; xem tồn kho; tra cứu vị trí (chỉ đọc).
- Đăng xuất, cookie session; danh sách kiện đang làm lưu riêng theo server/tài khoản.

Các giới hạn backend vẫn còn, xem BACKEND_GAPS.md và ISSUE_COVERAGE.md. Hàng trả đã nối hoàn tất thật theo backend: cộng tồn ngay và chuyển RECEIVED, có hộp xác nhận. Chưa có API báo sai vị trí/thiếu khi soạn nên các nút nháp đã được bỏ để không nhầm với báo cáo đã gửi. Gói giao diện không thể tự tạo API còn thiếu. Xem API_COVERAGE.md để đối chiếu từng chức năng.

## Kiểm tra

Đã chạy với Flutter 3.44.4 / Dart 3.12.2: phân tích mã không có lỗi và 16 test Flutter đều qua. Các test bao gồm phân role, login/quên mật khẩu, màn hình nhỏ, ghi số đếm 0, đếm lại, kiểm tra barcode/vị trí/số lượng, nhận phản hồi từng dòng, không tải lại receipt sau hoàn tất, lỗi 400/401, bookmark theo tài khoản và xác nhận trước khi hoàn tất hàng trả.

Có kèm test Java cho module khôi phục mật khẩu nhưng chưa chạy Maven. Chưa chạy iOS build trên Xcode, camera/máy in thật, SMTP thật hoặc tích hợp với database đang chạy của bạn. Test API dùng bộ phản hồi mô phỏng theo DTO/controller của backend; không thay thế kiểm thử dữ liệu thực.
>>>>>>> a981f6f (app)
