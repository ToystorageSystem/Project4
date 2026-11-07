// Business Operations errors: plain English, no technical details leaked to UI.
export function friendlyBusinessError(error, fallback = "Đã xảy ra lỗi. Vui lòng thử lại.") {
  const status = error?.response?.status;
  const payload = error?.response?.data;
  const raw = typeof payload === "string" ? payload : payload?.message || error?.message;
  const message = typeof raw === "string" ? raw.trim() : "";

  if (status === 401) return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
  if (status === 403) return "Bạn không có quyền thực hiện thao tác này.";
  if (status >= 500) return "Dịch vụ đang tạm thời không khả dụng. Vui lòng thử lại sau.";
  if (!error?.response && !message) return "Không thể kết nối với hệ thống. Vui lòng kiểm tra mạng.";

  const rules = [
    [/bad credentials|invalid (email|password)|password does not match/i, "Email hoặc mật khẩu không đúng."],
    [/only draft or ordered purchase orders can be cancelled/i, "Chỉ có thể hủy đơn nháp hoặc đã đặt."],
    [/legacy pending cancellation request/i, "Đơn có yêu cầu hủy cũ chưa được xử lý. Liên hệ quản trị viên."],
    [/only business operations or admin can cancel purchase orders/i, "Chỉ tài khoản Nghiệp vụ kinh doanh hoặc quản trị viên được hủy đơn."],
    [/supplier cancellation agreement must be confirmed/i, "Vui lòng xác nhận nhà cung cấp đã đồng ý hủy đơn."],
    [/cannot approve or reject your own product change request/i, "Bạn không thể tự duyệt hoặc từ chối yêu cầu sản phẩm do mình tạo."],
    [/purchase order creator cannot accept supplier price changes/i, "Người lập đơn mua hàng không được tự chấp nhận thay đổi giá nhà cung cấp."],
    [/a pending cancellation request already exists/i, "Đơn mua hàng này đã có yêu cầu hủy đang chờ xử lý."],
    [/cancellation request is no longer pending/i, "Yêu cầu hủy này đã được xử lý. Hãy làm mới trang."],
    [/purchase order is no longer ordered/i, "Trạng thái đơn mua hàng đã thay đổi. Hãy làm mới trang trước khi xét duyệt."],
    [/only the purchase order creator can request cancellation/i, "Chỉ người tạo đơn mua hàng mới được gửi yêu cầu hủy."],
    [/a user cannot approve or reject their own cancellation request/i, "Bạn không thể xét duyệt yêu cầu hủy do chính mình gửi."],
    [/you cannot view another user's cancellation requests/i, "Bạn chỉ có thể xem yêu cầu hủy của các đơn mua hàng do mình tạo."],
    [/purchase order has received quantities/i, "Đơn đã nhận hàng vào kho và không thể hủy."],
    [/purchase order has linked goods receipts/i, "Đơn mua hàng có phiếu nhập kho liên quan. Hãy xử lý các phiếu nhập trước khi hủy."],
    [/purchase order has an active supplier invoice/i, "Đơn mua hàng có hóa đơn nhà cung cấp đang hoạt động. Hãy xử lý hóa đơn trước khi hủy."],
    [/auto-generated purchase orders require manual review/i, "Đơn được tạo tự động cần được kiểm tra thủ công trước khi hủy."],
    [/cancellation request not found/i, "Không tìm thấy yêu cầu hủy đơn."],
    [/purchase order not found/i, "Không tìm thấy đơn mua hàng."],
    [/reason must contain/i, "Lý do phải có từ 10 đến 1.000 ký tự."],
    [/decision note is too long/i, "Ghi chú xét duyệt không được vượt quá 1.000 ký tự."],
    [/user is not authenticated|authenticated user not found/i, "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại."],
    [/goods receipt awaiting completion|already has a goods receipt/i, "Đơn mua hàng này đã có phiếu nhập kho đang chờ xử lý. Hãy đợi bộ phận kho hoàn tất trước khi tạo phiếu nhập tiếp theo."],
    [/timeout|network error|failed to fetch/i, "Kết nối bị gián đoạn. Hãy kiểm tra thao tác đã thành công chưa trước khi thử lại."],
  ];
  for (const [pattern, text] of rules) {
    if (pattern.test(message)) return text;
  }
  // Chỉ hiển thị nguyên văn thông báo nghiệp vụ đã là tiếng Việt.
  // Không đưa lỗi kỹ thuật hoặc thông báo tiếng Anh chưa được Việt hóa ra giao diện.
  if (/[À-ỹĐđ]/.test(message) && message.length < 180 && !/sql|exception|stack|at com\.|select\s|constraint/i.test(message)) {
    return message;
  }
  return fallback;
}
