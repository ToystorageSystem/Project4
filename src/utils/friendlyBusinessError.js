// Thong bao cho nguoi dung. Khong sua ma loi, API hay quy tac nghiep vu.
export function friendlyBusinessError(error, fallback = "Có lỗi xảy ra. Vui lòng thử lại.") {
    const status = error?.response?.status;
    const payload = error?.response?.data;
    const raw = typeof payload === "string" ? payload : payload?.message || error?.message;
    const message = typeof raw === "string" ? raw.trim() : "";

    if (status === 401) return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    if (status === 403) return "Tài khoản chưa được cấp quyền thực hiện thao tác này.";
    if (status >= 500) return "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.";
    if (!error?.response && !message) return "Không thể kết nối hệ thống. Vui lòng kiểm tra kết nối mạng.";

    const rules = [
        [/bad credentials|invalid (email|password)|password does not match/i, "Email hoặc mật khẩu không đúng."],
        [/only ordered purchase orders can request cancellation/i, "Chỉ có thể đề nghị hủy đơn đã gửi nhà cung cấp."],
        [/supplier cancellation agreement must be confirmed/i, "Vui lòng xác nhận nhà cung cấp đã đồng ý hủy đơn."],
        [/a pending cancellation request already exists/i, "Đơn mua hàng này đã có đề nghị hủy đang chờ xét duyệt."],
        [/cancellation request is no longer pending/i, "Đề nghị hủy này đã được xử lý. Vui lòng tải lại danh sách."],
        [/purchase order is no longer ordered/i, "Trạng thái đơn đã thay đổi nên không thể duyệt hủy. Vui lòng tải lại."],
        [/only the purchase order creator can request cancellation/i, "Chỉ người lập đơn mới có thể gửi đề nghị hủy."],
        [/a user cannot approve or reject their own cancellation request/i, "Bạn không thể tự xét duyệt đề nghị hủy do mình gửi."],
        [/you cannot view another user's cancellation requests/i, "Bạn chỉ có thể xem đề nghị hủy của đơn mình phụ trách."],
        [/purchase order has received quantities/i, "Đơn đã có hàng được nhận nên không thể hủy."],
        [/purchase order has linked goods receipts/i, "Đơn đã có phiếu nhận hàng liên quan. Vui lòng xử lý phiếu trước khi đề nghị hủy."],
        [/purchase order has an active supplier invoice/i, "Đơn đã có hóa đơn nhà cung cấp. Vui lòng xử lý hóa đơn trước khi đề nghị hủy."],
        [/auto-generated purchase orders require manual review/i, "Đơn được tạo tự động cần được kiểm tra riêng trước khi hủy."],
        [/cancellation request not found/i, "Không tìm thấy đề nghị hủy này."],
        [/purchase order not found/i, "Không tìm thấy đơn mua hàng."],
        [/reason must contain/i, "Lý do phải có ít nhất 10 ký tự và không vượt quá 1.000 ký tự."],
        [/decision note is too long/i, "Ghi chú xét duyệt không được vượt quá 1.000 ký tự."],
        [/user is not authenticated|authenticated user not found/i, "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại."],
        [/timeout|network error|failed to fetch/i, "Kết nối bị gián đoạn. Vui lòng kiểm tra dữ liệu trước khi thử lại."],
    ];
    for (const [pattern, text] of rules) {
        if (pattern.test(message)) return text;
    }
    // Giu lai thong bao tieng Viet co san; khong dua chi tiet ky thuat tieng Anh ra giao dien.
    if (/[\u00c0-\u1ef9]/i.test(message)) return message;
    return fallback;
}
