# Chức năng nối với backend hiện tại

Đối chiếu nhánh backend, commit `d9d8b2b511abd6090ec6df0b8dde3c5fbd6d25c2`:
https://github.com/ToystorageSystem/Project4/tree/d9d8b2b511abd6090ec6df0b8dde3c5fbd6d25c2

| Nhóm | Thao tác trên app | API thật |
|---|---|---|
| Tài khoản | Đăng nhập chung role, đăng xuất | POST /api/auth/login, /api/auth/logout |
| Nhập kho | Danh sách phiếu chờ, chi tiết, nhận việc/kiểm lại, lưu từng sản phẩm, kết thúc kiểm nhận | GET /api/warehouse/goods-receipts/receiving; GET /{id}/receiving-detail; PATCH /{id}/start-receiving; PUT /{id}/inspections; PATCH /{id}/finish-inspection |
| Cất hàng | Việc được giao, chi tiết, bắt đầu, quét sản phẩm/vị trí và ghi số lượng, hoàn tất | GET /api/warehouse/putaway/my-tasks; GET /{id}; PATCH /{id}/start, /{id}/items/{itemId}, /{id}/complete |
| Soạn hàng | Danh sách, chi tiết, bắt đầu, quét/ghi số lượng, chuyển đóng gói | GET /api/warehouse/picking/transfers; GET /{id}; PATCH /{id}/start, /{id}/items/{itemId}, /{id}/complete |
| Đóng kiện | Tạo kiện, thêm sản phẩm/số lượng, xem nội dung, niêm phong, hoàn tất đóng gói | POST /api/warehouse/staff/packing/transfers/{id}/packages; POST /packages/{id}/scan; GET /packages/{id}; PATCH /packages/{id}/seal; PATCH /transfers/{id}/complete |
| Gửi đóng hàng | Xem tổng số soạn/đóng, gửi kết quả sang PACKED | GET /api/warehouse/staff/packing-submissions/transfers/{id}; POST /transfers/{id}/submit |
| Nhãn kiện | In/xuất PDF nhãn QR từ packageCode thật | PDF tạo tại app; không gọi API tự tạo shipment hoặc xuất kho |
| Kiểm kê | Danh sách, chi tiết, bắt đầu, đếm lần đầu/lần hai, nhận số 0, gửi kết quả | GET /api/warehouse/staff/stock-counts; GET /{id}; PATCH /{id}/start; PUT /{id}/items; PATCH /{id}/submit |
| Hàng trả | Danh sách chờ, mở bằng ID, chi tiết, nhận việc, quét kiện, ghi thực nhận/tình trạng/ghi chú, tải ảnh, hoàn tất | GET /api/warehouse/staff/store-returns; GET /{id}; PATCH /{id}/start; POST /{id}/scan-package; PUT /{id}/items; POST multipart /{id}/items/{itemId}/evidence; PATCH /{id}/submit |
| Hàng hỏng | Chọn sản phẩm/vị trí từ tồn, số lượng, loại, nguồn, mô tả, ảnh; xem báo cáo của mình và chi tiết | POST multipart /api/warehouse/staff/damaged-goods (request JSON + image); GET danh sách và GET /{reportId} |
| Thiếu khi nhập NCC | Chọn phiếu/sản phẩm đã lưu thực nhận, mô tả và ảnh; xem báo cáo của mình | POST multipart /api/warehouse/receiving-shortages/goods-receipts/{id}; GET /my-reports |
| Tồn kho | Tìm tên/mã/barcode, xem tồn thực tế/giữ chỗ/khả dụng, từng vị trí | GET /api/warehouse/inventory/balances |
| Vị trí | Tìm mã/khu/kệ, quét mã, xem thông tin vị trí (chỉ đọc) | GET /api/warehouse/locations |

Các đường dẫn rút gọn trong cùng ô kế thừa base path ở đầu ô. Màn hình xác thực không có nút demo hoặc nhập server. Dữ liệu mô phỏng chỉ còn dùng trong test, không có lối vào từ app.

## Hành vi cần phân biệt

- **Hàng trả:** service hiện tại hoàn tất sang RECEIVED và cộng tồn ngay. App đã nối nút này, có hộp xác nhận nêu rõ ảnh hưởng tồn. NORMAL vào tồn khả dụng; DAMAGED/EXPIRED/QUARANTINE vào khu cách ly. Đây là hành vi backend, không phải chờ manager. Máy chủ kiểm tra cờ inspected thực tế vì DTO hiện không trả cờ đó.
- **Nhập NCC:** finish-inspection sang INSPECTED, chờ manager; chưa cộng tồn từ app.
- **Kiểm kê:** submit sang PENDING_CONFIRMATION; app không gọi API duyệt/điều chỉnh tồn.
- **Đóng hàng:** complete + submit sang PACKED, chưa xác nhận xuất hàng hoặc giao vận. Khi complete đã thành công nhưng submit lỗi, dùng completedAt để tránh lặp bước complete.
- **Việc có owner:** máy chủ quyết định quyền ghi và có thể từ chối phiếu do staff khác nhận. App hiển thị lỗi 403 thay vì giả vờ hoàn tất.
- **Mất mạng khi ghi:** app không tự retry. Tải lại để xác nhận dữ liệu trước khi gửi lại.

## Giới hạn của nhánh backend

- Chưa có API tạo báo cáo thiếu khi picking/sai vị trí putaway dù có một số DTO request. App không tạo URL giả và không hiển thị nháp cục bộ như báo cáo đã gửi; khi gặp lỗi hãy liên hệ quản lý để điều chỉnh phiếu.
- Chưa có GET toàn bộ phiếu PACKING cho staff. App lưu các ID chuyển sang PACKING theo server/tài khoản/thiết bị; có mở theo ID và luôn tải trạng thái thật khi mở. Không gọi đó là danh sách toàn kho.
- Chưa có API lịch sử hoàn tất chung cho staff. Phiếu hàng trả có thể mở theo ID; không giả lịch sử đã đồng bộ.
- Không có forgot/reset-password trong nhánh gốc. Có kèm module backend bổ sung riêng + SQL + cấu hình SMTP trong backend_password_reset. Phải cài module này mới gửi email thật được.
- Các màn manager/admin/store mobile chưa được triển khai. Login dùng chung, role khác thấy thông báo không gian làm việc sẽ bổ sung và không được chuyển vào staff.
