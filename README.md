# ToyStorage — Unified Frontend

Bản ghép 3 nhánh, dùng `frontend` làm nền chính:

| Nhánh | Commit nguồn | Phần được ghép |
| --- | --- | --- |
| frontend | c1a2c9c344da67b84de277ef808bf1eb7ce50d0d | Đăng nhập/MFA, giao diện chung, quản lý kho, nhân viên kho, giao hàng |
| phuc_frontend | 4d57e09849480efa500bebabace3c459bef5b63c | Dashboard Admin, tài khoản, role/quyền, kho/cửa hàng, nhật ký, cảnh báo |
| feature/business-fe-integration | 6c7e8a31ea226ba10564578846cd20df90e8ecb7 | Sản phẩm, NCC, mua hàng, hóa đơn, phiếu nhập, điều chuyển, vận chuyển, tồn kho, báo cáo |

Không cần chép đè thêm code từ nhánh khác vào thư mục này.

## Chạy trên máy

Node.js 22.12 trở lên. Mở terminal trong thư mục chứa package.json:

```bash
npm ci
npm run dev
```

Backend mặc định: http://localhost:8080. Có thể thay API_PROXY_TARGET trong .env.local theo .env.example. Mở đúng URL Vite in ra. Không cần chạy riêng frontend Admin hay frontend Business.

```bash
npm test
npm run build
```

## Đăng nhập và phân quyền

Một AuthProvider duy nhất đọc phiên từ GET /api/auth/status. Không tin tài khoản lưu trong sessionStorage. Giữ luồng CHANGE_PASSWORD → SETUP_2FA hoặc VERIFY_2FA → AUTHENTICATED; không bỏ OTP hoặc lấy quyền từ dữ liệu demo.

| Role | Trang sau xác thực |
| --- | --- |
| ADMIN / ROLE_ADMIN / ROLE_ROLE-ADMIN | /admin/dashboard |
| BIZ_OPS / BUSINESS_OPERATIONS / ROLE_ROLE-BIZ-OPS | /business/dashboard |
| BIZ_MANAGER / BUSINESS_MANAGER / BIZ_STAFF / BUSINESS_STAFF | /business/dashboard (bí danh role cũ; API vẫn kiểm tra quyền) |
| WH_MANAGER / WAREHOUSE_MANAGER | /warehouse/dashboard |
| WH_STAFF / WAREHOUSE_STAFF | /warehouse/dashboard, menu thực thi |
| DELIVERY / DELIVERY_STAFF | /warehouse/dashboard, menu chuyến giao |
| Role khác không có trong 3 nhánh | /workspace, thông báo chưa gán khu vực |

Tiền tố ROLE_ lặp, dấu gạch ngang và gạch dưới được chuẩn hóa. Nếu tài khoản có nhiều role: Admin → Business → Warehouse là thứ tự chọn trang mặc định; menu vẫn hiển thị các khu vực thực sự được cấp. Đường dẫn trực tiếp cũng đi qua guard. Backend là nơi quyết định quyền và trạng thái nghiệp vụ.

Các URL /business-staff/* và /business-manager/* được giữ dưới dạng alias. Theo đúng nhánh business nguồn, các URL requests/rejected/approvals cũ chuyển tới màn hình Products hoặc Purchase Orders đã hợp nhất. Code của các trang cũ vẫn nằm trong ZIP, không tạo thêm quy trình phê duyệt thay cho hành vi hiện tại của nhánh nguồn.

## Chức năng

Admin: xem/tạo/sửa tài khoản, role và quyền, khóa/mở khóa/ngừng hoạt động, reset mật khẩu, quản lý kho/cửa hàng và gán nhân viên/quản lý, xuất nhật ký CSV, điều tra và xử lý cảnh báo bảo mật. Các điều kiện quyền và bước xác nhận trong nhánh phuc_frontend được giữ.

Business: dashboard, sản phẩm, danh mục, ảnh và nhập Excel, lịch sử sản phẩm, nhà cung cấp và liên kết sản phẩm/giá, purchase orders, phản hồi nhà cung cấp và so sánh giá, supplier invoices và tệp đính kèm, phiếu nhập, điều chuyển, theo dõi chuyến đang vận chuyển, manifests, báo cáo tồn kho/giao dịch/hoạt động/chênh lệch.

Warehouse: dashboard, nhận/kiểm hàng, duyệt/kiểm lại, chênh lệch, hàng trả/hàng hỏng, putaway, picking, packing/seal/barcode, bàn giao xuất, lịch sử/manifest, tồn kho/vị trí, kiểm kê quản lý và nhân viên, chuyến giao.

## Giao diện

Logo ToyStorage gốc và tông cam #f25d19; nhãn tiếng Anh. Lời chào sáng/chiều/tối dùng giờ máy người dùng và tên từ phiên đăng nhập. Các khu vực dùng chung sidebar, tìm trang, header, logout và Compact view. Dashboard kho giữ lối tắt cá nhân. Tùy chọn chỉ lưu riêng từng tài khoản trên trình duyệt, không thay quyền.

CSS Admin được giới hạn trong @scope (.admin-workspace), tránh ảnh hưởng bảng/menu kho. Business giữ các thành phần nghiệp vụ và dùng layout chung. Không còn form đăng nhập Admin lồng trong màn hình kho/cửa hàng.

## Backend cần có

Đây là bản ghép FRONTEND, không phải backend ghép. Backend phải cung cấp đồng thời:

- Auth dạng stage hiện tại của nhánh frontend: /api/auth/status, /csrf, /login, /change-password, /2fa/setup, /2fa/confirm, /2fa/verify, /2fa/recover, /logout và forgot/reset password.
- API Admin /api/admin/*, API Business /api/business-staff/*, /api/business-manager/*, /api/products, /api/categories, và các API kho tương ứng.
- Quyền/role thực trong database và cookie session hợp lệ.

Axios và các API fetch của Admin đều lấy CSRF mới trước mutation, dùng credentials và không tự gửi lại mutation. Phiên 401 ở API nghiệp vụ sẽ thoát về đăng nhập.

Lưu ý: snapshot integration/backend công khai tại lúc kiểm tra vẫn dùng LoginResponse cũ không có stage, không phải bản AuthService MFA mới. Không thể mặc định nó tương thích chỉ bằng cách ghép frontend. Dùng backend đã tích hợp cả chức năng và hợp đồng auth phù hợp. Không tắt CSRF/2FA để che lỗi không tương thích.

Thư mục integration/backend-files và backend-auth.patch được giữ từ nhánh frontend làm tài liệu cho backend gốc 0f9c984. Chỉ dùng khi backend đã có AuthService/MfaRequests/SetupResponse tương ứng. Không chép hai file này đè lên một backend khác hợp đồng.

## Kiểm tra

40 tests qua, gồm các kiểm tra cũ và kiểm tra role Admin/Business/Kho, stage MFA, truy cập chéo role, CSRF/payload/không retry cho cả Axios và fetch, điều hướng toàn ứng dụng, bảo vệ Admin khỏi tự khóa tài khoản, phiên giả/hết hạn, workflow kho và tùy chọn cá nhân. Build production thành công.

Đã xem trực quan dashboard Admin/Business, danh sách tài khoản/sản phẩm và khung mobile 390px bằng fixture có nhãn Design QA · sample records only. Chưa kiểm thử tạo/sửa/xóa dữ liệu trên MySQL/SMTP/backend đang chạy. Không khẳng định mọi endpoint backend đã chạy end-to-end.

tests/visual/integrated.html chỉ phục vụ QA trong dev, không được import vào entry production. Production dùng API thật, không có tài khoản đăng nhập demo hay chế độ bỏ xác thực.

## Triển khai

npm run build tạo dist/. Máy chủ cần trả index.html cho đường dẫn SPA và reverse proxy /api, /ws tới backend. Vite proxy chỉ dùng khi phát triển.

Chi tiết ghép và xử lý trùng file: MERGE_REPORT.md. Không thay đổi hoặc push các nhánh GitHub gốc.
