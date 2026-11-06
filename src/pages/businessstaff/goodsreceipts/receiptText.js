
 // Nội dung tiếng Việt cho giao diện phiếu nhập kho tổng.
 // Chỉ thay đổi văn bản hiển thị, không thay đổi nghiệp vụ Backend.

 export const receiptText = {
     menu: "Tạo phiếu nhập kho tổng",
     title: "Tạo phiếu nhập hàng cho kho tổng",
     intro: "Chọn đơn mua hàng đã gửi nhà cung cấp để lập phiếu nhận hàng. Không tạo thêm đơn mua hàng mới.",
     loading: "Đang tải dữ liệu...",
     loadError: "Không thể tải dữ liệu phiếu nhập kho.",
     retry: "Thử lại",

     supplier: "Nhà cung cấp",
     warehouse: "Kho tổng nhận hàng",
     chooseSupplier: "-- Chọn nhà cung cấp --",
     chooseWarehouse: "-- Chọn kho tổng --",
     chooseProduct: "-- Chọn sản phẩm đang hoạt động --",
     noSuppliers: "Không có nhà cung cấp đang hoạt động.",
     noWarehouses: "Không có kho tổng đang hoạt động.",
     noProducts: "Không có sản phẩm đang hoạt động.",

     selectPurchaseOrder: "Thông tin đơn mua hàng",
     purchaseOrderRequired: "Đơn mua hàng *",
     choosePurchaseOrder: "-- Chọn đơn đã gửi hoặc đã nhận một phần --",
     purchaseOrderStatus: "Trạng thái đơn mua hàng",
     purchaseOrderNote: "Ghi chú của đơn mua hàng",
     noPurchaseOrders: "Không có đơn mua hàng đủ điều kiện để tạo phiếu nhập.",
     invalidOrder: "Đơn mua hàng này không đủ điều kiện để tạo phiếu nhập.",
     noRemainingProducts: "Tất cả sản phẩm trong đơn này đã nhận đủ.",

     ordered: "Đã đặt",
     received: "Đã nhận",
     remaining: "Còn lại",

     note: "Ghi chú đơn mua hàng",
     notePlaceholder: "Ví dụ: Giao hàng theo lịch đã thống nhất...",
     items: "Danh sách sản phẩm dự kiến nhập",
     product: "Sản phẩm",
     quantity: "Số lượng dự kiến",
     quantityInput: "Số lượng dự kiến nhận (nhập 0 để bỏ qua)",
     price: "Đơn giá trong đơn mua hàng (đồng)",
     subtotal: "Thành tiền dự kiến",
     addLine: "Thêm sản phẩm",
     removeLine: "Xóa sản phẩm",
     loadMore: "Tải thêm sản phẩm",
     loadingMore: "Đang tải thêm...",

     productNote:
         "Sản phẩm và đơn giá lấy từ đơn mua hàng. Nhập số lượng dự kiến nhận trong lần này; nhập 0 nếu không nhận sản phẩm đó.",

     noteLimit: "Tối đa 2.000 ký tự.",

     totalLines: "Số mặt hàng trong phiếu",
     totalQuantity: "Tổng số lượng dự kiến nhập",
     total: "Tổng giá trị dự kiến",

     draftNote:
         "Sau khi tạo, phiếu sẽ chờ bộ phận kho kiểm tra. Số lượng tồn kho chỉ tăng khi kho xác nhận đã nhận hàng.",

     fromPurchaseOrderNote:
         "Phiếu nhận hàng gắn với đơn mua hàng đã có. Chỉ lập phiếu khi đã thống nhất lịch giao hàng với nhà cung cấp.",

     autoPoNote: "Phiếu nhận hàng dùng đơn mua hàng đã có.",

     confirmTitle: "Xác nhận tạo phiếu nhập kho",

     confirmIntro:
         "Phiếu nhận hàng sẽ được lưu vào hệ thống. Số lượng hàng trong kho chưa thay đổi ở bước này.",

     confirmCheck:
         "Tôi đã kiểm tra thông tin và đồng ý tạo phiếu nhận hàng.",

     cancel: "Hủy",
     create: "Tạo phiếu nhập",
     creating: "Đang tạo phiếu nhập...",

     requiredFields: "Vui lòng kiểm tra các trường bắt buộc.",
     invalidSupplier: "Vui lòng chọn nhà cung cấp đang hoạt động.",
     invalidWarehouse: "Vui lòng chọn kho tổng đang hoạt động.",
     invalidProduct: "Vui lòng chọn sản phẩm hợp lệ.",
     duplicateProduct: "Sản phẩm đã có trong phiếu nhập.",

     invalidQuantity:
         "Số lượng mỗi sản phẩm phải từ 0 đến số lượng chưa nhận trong đơn. Cần nhập ít nhất một sản phẩm có số lượng lớn hơn 0.",

     invalidPrice:
         "Đơn giá phải lớn hơn 0 và có tối đa 2 chữ số thập phân.",

     invalidNote: "Ghi chú không được vượt quá 2.000 ký tự.",

     detailTitle: "Chi tiết phiếu nhập kho",
     back: "← Tạo phiếu nhập khác",

     success:
         "Tạo phiếu nhập thành công. Phiếu đang chờ bộ phận kho xử lý.",

     receiptCode: "Mã phiếu nhập",
     receiptStatus: "Trạng thái phiếu nhập",
     created: "Ngày tạo",
     creator: "Người tạo đơn mua hàng",
     purchaseOrder: "Đơn mua hàng liên kết",
     itemTable: "Danh sách sản phẩm dự kiến nhập",

     warehouseNotice:
         "Bộ phận kho sẽ tiếp nhận và xử lý phiếu này. Nhân viên kinh doanh có thể theo dõi tiến độ nhận hàng.",

     noItems: "Phiếu nhập chưa có sản phẩm.",

     createdStatus: "Chờ xử lý",
     receivingStatus: "Đang nhận hàng",
     inspectedStatus: "Đã kiểm nhận",
     confirmedStatus: "Đã xác nhận",
     completedStatus: "Hoàn tất",
     cancelledStatus: "Đã hủy",
     unknown: "Không có dữ liệu",

     savedWithoutId:
         "Chưa thể xác nhận mã phiếu vừa tạo. Vui lòng kiểm tra danh sách phiếu trước khi tạo lại để tránh bị trùng.",

     timeoutWarning:
         "Nếu mất kết nối, hãy kiểm tra phiếu đã được tạo hay chưa trước khi thử lại, tránh tạo trùng.",

     invalidReceiptId: "Mã phiếu nhận hàng không hợp lệ.",
     detailError: "Không thể tải chi tiết phiếu nhập.",
 };

 export const receiptStatusLabels = {
     CREATED: receiptText.createdStatus,
     RECEIVING: receiptText.receivingStatus,
     INSPECTED: receiptText.inspectedStatus,
     CONFIRMED: receiptText.confirmedStatus,
     COMPLETED: receiptText.completedStatus,
     CANCELLED: receiptText.cancelledStatus,
 };

 export function formatReceiptMoney(value) {
     const amount = Number(value);

     return Number.isFinite(amount)
         ? `${new Intl.NumberFormat("vi-VN", {
               maximumFractionDigits: 2,
           }).format(amount)} đ`
         : "—";
 }

 export function formatReceiptDate(value) {
     if (!value) return "—";

     const dt = new Date(value);

     return Number.isNaN(dt.getTime())
         ? String(value)
         : dt.toLocaleString("vi-VN");
 }
