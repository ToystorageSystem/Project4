// Business Operations errors: plain English, no technical details leaked to UI.
export function friendlyBusinessError(error, fallback = "An error occurred. Please try again.") {
  const status = error?.response?.status;
  const payload = error?.response?.data;
  const raw = typeof payload === "string" ? payload : payload?.message || error?.message;
  const message = typeof raw === "string" ? raw.trim() : "";

  if (status === 401) return "Your session has expired. Please sign in again.";
  if (status === 403) return "You do not have permission to perform this action.";
  if (status >= 500) return "The service is temporarily unavailable. Please try again later.";
  if (!error?.response && !message) return "Unable to connect to the system. Check your network connection.";

  const rules = [
    [/bad credentials|invalid (email|password)|password does not match/i, "Incorrect email or password."],
    [/only draft or ordered purchase orders can be cancelled/i, "Only draft or placed purchase orders can be cancelled."],
    [/legacy pending cancellation request/i, "This order has an unresolved legacy cancellation request. Contact an administrator."],
    [/only business operations or admin can cancel purchase orders/i, "Only Business Operations users or administrators can cancel purchase orders."],
    [/supplier cancellation agreement must be confirmed/i, "Confirm that the supplier has agreed to cancel the order."],
    [/cannot approve or reject your own product change request/i, "You cannot approve or reject your own product change request."],
    [/purchase order creator cannot accept supplier price changes/i, "Purchase order creators cannot approve their own supplier price changes."],
    [/a pending cancellation request already exists/i, "This purchase order already has a pending cancellation request."],
    [/cancellation request is no longer pending/i, "This cancellation request has already been processed. Refresh the page."],
    [/purchase order is no longer ordered/i, "The purchase order status has changed. Refresh before reviewing."],
    [/only the purchase order creator can request cancellation/i, "Only the purchase order creator can request cancellation."],
    [/a user cannot approve or reject their own cancellation request/i, "You cannot review your own cancellation request."],
    [/you cannot view another user's cancellation requests/i, "You can only view cancellation requests for your own purchase orders."],
    [/purchase order has received quantities/i, "Goods have already been received against this order. Cancellation is not allowed."],
    [/purchase order has linked goods receipts/i, "This purchase order has related goods receipts. Resolve the receipts before cancelling."],
    [/purchase order has an active supplier invoice/i, "This purchase order has an active supplier invoice. Resolve the invoice before cancelling."],
    [/auto-generated purchase orders require manual review/i, "Automatically generated purchase orders require manual review before cancellation."],
    [/cancellation request not found/i, "Cancellation request not found."],
    [/purchase order not found/i, "Purchase order not found."],
    [/reason must contain/i, "Reason must be between 10 and 1,000 characters."],
    [/decision note is too long/i, "Review notes cannot exceed 1,000 characters."],
    [/user is not authenticated|authenticated user not found/i, "Your session is invalid. Please sign in again."],
    [/goods receipt awaiting completion|already has a goods receipt/i, "This purchase order already has a goods receipt awaiting processing. Wait for the warehouse to complete it before creating another receipt."],
    [/timeout|network error|failed to fetch/i, "Connection was interrupted. Check whether the action succeeded before retrying."],
  ];
  for (const [pattern, text] of rules) {
    if (pattern.test(message)) return text;
  }
  // Chi hien thi thong bao nghiep vu an toan tu Backend (4xx).
  // Khong hien thi stack trace, SQL, HTML hoac thong tin loi noi bo.
  const safeBusinessMessage =
    [400, 404, 409, 422].includes(status) &&
    message.length > 0 && message.length <= 200 &&
    !/[<>\r\n]/.test(message) &&
    !/\b(sql|sqlstate|exception|stack|trace|constraint|query|select|insert|update|delete|table|column|jdbc|hibernate|internal server error)\b|java\.|org\.|com\.toystorage|\bat\s+com\./i.test(message);
  if (safeBusinessMessage) return message;
  return fallback;
}
