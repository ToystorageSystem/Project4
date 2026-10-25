// Keep all UI translations in one place; strings display as Vietnamese in React.
export const receiptText = {
    menu: "Tạo phiếu nhập kho tổng",
    title: "Tạo phiếu nhập hàng cho kho tổng",
    intro: "Chọn nhà cung cấp, kho tổng và các sản phẩm dự kiến nhập. Mã phiếu do Backend tự sinh.",
    loading: "Đang tải dữ liệu từ Backend...",
    loadError: "Không tải được dữ liệu tạo phiếu.",
    retry: "Thử lại",
    supplier: "Nhà cung cấp *",
    warehouse: "Kho tổng nhận hàng *",
    chooseSupplier: "-- Chọn nhà cung cấp --",
    chooseWarehouse: "-- Chọn kho tổng --",
    chooseProduct: "-- Chọn sản phẩm ACTIVE --",
    noSuppliers: "Không có nhà cung cấp ACTIVE.",
    noWarehouses: "Không có kho tổng ACTIVE.",
    noProducts: "Không có sản phẩm ACTIVE.",
    note: "Ghi chú (không bắt buộc)",
    notePlaceholder: "Ví dụ: Giao hàng theo lịch nhận kho...",
    items: "Danh sách sản phẩm dự kiến nhập",
    product: "Sản phẩm *",
    quantity: "Số lượng dự kiến *",
    price: "Đơn giá nhập (VND) *",
    subtotal: "Thành tiền dự kiến",
    addLine: "+ Thêm sản phẩm",
    removeLine: "Xóa dòng",
    loadMore: "Tải thêm sản phẩm ACTIVE",
    loadingMore: "Đang tải thêm...",
    productNote: "Giá nhập gợi ý lấy từ hồ sơ sản phẩm; hãy kiểm tra và điều chỉnh theo phiếu này.",
    noteLimit: "Tối đa 2.000 ký tự.",
    totalLines: "Số mặt hàng",
    totalQuantity: "Tổng số lượng dự kiến",
    total: "Tổng giá trị dự kiến",
    draftNote: "Khi tạo, phiếu có trạng thái CREATED (Chờ xử lý). Chưa cộng tồn kho.",
    autoPoNote: "Backend tự tạo Purchase Order nền khi lưu phiếu nhập hàng.",
    confirmTitle: "Xác nhận tạo phiếu nhập",
    confirmIntro: "Bạn sắp tạo phiếu nhập THẬT và một Purchase Order nền trong MySQL chung của nhóm.",
    confirmCheck: "Tôi đã kiểm tra và đồng ý lưu dữ liệu vào database chung.",
    cancel: "Hủy",
    create: "Tạo phiếu nhập",
    creating: "Đang tạo phiếu...",
    requiredFields: "Vui lòng kiểm tra các trường bắt buộc.",
    invalidSupplier: "Hãy chọn nhà cung cấp ACTIVE.",
    invalidWarehouse: "Hãy chọn kho tổng ACTIVE.",
    invalidProduct: "Hãy chọn sản phẩm ACTIVE.",
    duplicateProduct: "Sản phẩm bị trùng trong phiếu.",
    invalidQuantity: "Số lượng phải là số nguyên từ 1 trở lên.",
    invalidPrice: "Giá nhập phải lớn hơn 0 và tối đa 2 chữ số thập phân.",
    invalidNote: "Ghi chú không được quá 2.000 ký tự.",
    detailTitle: "Chi tiết phiếu nhập hàng",
    back: "← Tạo phiếu khác",
    success: "Tạo phiếu nhập thành công. Phiếu đã được chuyển sang bộ phận kho chờ xử lý.",
    receiptCode: "Mã phiếu nhập",
    receiptStatus: "Trạng thái phiếu",
    created: "Ngày tạo",
    creator: "Người tạo",
    purchaseOrder: "Đơn mua hàng nền",
    itemTable: "Chi tiết hàng dự kiến nhập",
    warehouseNotice: "Phiếu này có thể được bộ phận Kho tổng xem trong danh sách tiếp nhận bằng tài khoản Warehouse có quyền. Business Staff chỉ xem chi tiết phiếu.",
    noItems: "Phiếu không có dòng sản phẩm.",
    createdStatus: "Chờ xử lý",
    receivingStatus: "Đang nhận hàng",
    inspectedStatus: "Đã kiểm nhận",
    confirmedStatus: "Đã xác nhận",
    completedStatus: "Hoàn tất",
    cancelledStatus: "Đã hủy",
    unknown: "Không có dữ liệu",
    savedWithoutId: "Backend xác nhận lưu thành công nhưng không trả ID. Không nhấn tạo lại; hãy xác minh phiếu trên database trước.",
    timeoutWarning: "Nếu request bị mất kết nối, hãy kiểm tra phiếu trong Backend trước khi thử lại, tránh tạo trùng.",
    invalidReceiptId: "Mã ID phiếu không hợp lệ.",
    detailError: "Không tải được chi tiết phiếu nhập.",
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
        ? `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(amount)} đ`
        : "—";
}

export function formatReceiptDate(value) {
    if (!value) return "—";
    const dt = new Date(value);
    return Number.isNaN(dt.getTime()) ? String(value) : dt.toLocaleString("vi-VN");
}
