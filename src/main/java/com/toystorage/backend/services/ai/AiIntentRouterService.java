package com.toystorage.backend.services.ai;

import com.toystorage.backend.enums.ai.AiIntent;
import com.toystorage.backend.exceptions.BadRequest;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AiIntentRouterService {

    private static final Pattern DIACRITICS_PATTERN =
            Pattern.compile("\\p{M}+");


    // =========================================================
    // SPECIFIC PRODUCT REFERENCE
    // THAM CHIẾU SẢN PHẨM CỤ THỂ
    //
    // ENGLISH NOTE:
    // Detect questions such as:
    //
    // product 7
    // product id 7
    // san pham 7
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Nhận diện câu hỏi có chỉ định một product cụ thể.
    // =========================================================

    private static final Pattern PRODUCT_ID_REFERENCE_PATTERN =
            Pattern.compile(
                    "\\b(?:product|san pham)\\s+(?:id\\s*)?\\d+\\b"
            );


    // =========================================================
    // ROUTE INTENT
    // XÁC ĐỊNH INTENT
    //
    // ENGLISH NOTE:
    // Route the user's natural-language question
    // to the appropriate analytics module.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Phân loại câu hỏi tự nhiên của user
    // vào đúng nhóm analytics.
    // =========================================================

    public AiIntent route(
            String message
    ) {

        if (
                message == null
                        || message.isBlank()
        ) {

            // ENGLISH NOTE:
            // The user must provide a question.
            //
            // GHI CHÚ TIẾNG VIỆT:
            // User bắt buộc phải nhập câu hỏi.

            throw new BadRequest(
                    "AI question must not be blank."
            );
        }


        String normalized =
                normalize(
                        message
                );


        // =====================================================
        // PRIORITY 1: FORECAST
        // ƯU TIÊN 1: DỰ BÁO
        //
        // ENGLISH NOTE:
        // Forecast must be checked before PRODUCT and INVENTORY.
        //
        // Example:
        // "Forecast inventory demand for product 7"
        //
        // must be FORECAST instead of PRODUCT or INVENTORY.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // FORECAST phải được kiểm tra trước PRODUCT và INVENTORY.
        //
        // Ví dụ:
        // "Dự báo tồn kho sản phẩm 7"
        //
        // phải thuộc FORECAST.
        // =====================================================

        if (
                containsAny(
                        normalized,

                        "du bao",
                        "forecast",
                        "demand forecast",
                        "sales forecast",
                        "nhu cau tuong lai",
                        "nhu cau sap toi",
                        "du kien nhu cau",
                        "stock risk",
                        "nguy co het hang",
                        "nguy co thieu hang",
                        "het hang trong",
                        "du kien ban"
                )
        ) {

            return AiIntent.FORECAST;
        }


        // =====================================================
        // PRIORITY 2: SUPPLIER
        // ƯU TIÊN 2: NHÀ CUNG CẤP
        // =====================================================

        if (
                containsAny(
                        normalized,

                        "nha cung cap",
                        "ncc",
                        "supplier",
                        "supplier performance",
                        "hieu suat nha cung cap",
                        "phan hoi nha cung cap",
                        "supplier response",
                        "do chinh xac nha cung cap",
                        "giao hang dung han",
                        "on time delivery"
                )
        ) {

            return AiIntent.SUPPLIER;
        }


        // =====================================================
        // PRIORITY 3: PURCHASE ORDER
        // ƯU TIÊN 3: ĐƠN MUA HÀNG
        // =====================================================

        if (
                containsAny(
                        normalized,

                        "purchase order",
                        "don mua hang",
                        "don dat hang",
                        "po ",
                        " po",
                        "po-",
                        "tien do nhan hang",
                        "don mua",
                        "qua han giao hang",
                        "purchase"
                )
        ) {

            return AiIntent.PURCHASE_ORDER;
        }


        // =====================================================
        // PRIORITY 4: STOCK TRANSFER
        // ƯU TIÊN 4: ĐIỀU CHUYỂN HÀNG
        // =====================================================

        if (
                containsAny(
                        normalized,

                        "stock transfer",
                        "dieu chuyen",
                        "phieu dieu chuyen",
                        "transfer",
                        "chuyen kho",
                        "chuyen hang",
                        "shortage",
                        "surplus",
                        "thieu hang khi dieu chuyen",
                        "du hang khi dieu chuyen",
                        "shipment overdue",
                        "receipt overdue"
                )
        ) {

            return AiIntent.STOCK_TRANSFER;
        }


        // =====================================================
        // PRIORITY 5: INVENTORY
        // ƯU TIÊN 5: TỒN KHO
        //
        // ENGLISH NOTE:
        // Inventory must be checked before product analytics.
        //
        // Example:
        // "What is the stock level of product 7?"
        //
        // should be INVENTORY, not PRODUCT.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // INVENTORY phải được kiểm tra trước PRODUCT.
        //
        // Ví dụ:
        // "Product 7 còn bao nhiêu tồn kho?"
        //
        // phải thuộc INVENTORY.
        // =====================================================

        if (
                containsAny(
                        normalized,

                        "ton kho",
                        "inventory",
                        "hang ton",
                        "ton hien tai",
                        "ton kha dung",
                        "available",
                        "reserved",
                        "giu cho",
                        "slow moving",
                        "slow-moving",
                        "luan chuyen cham",
                        "hang cham luan chuyen",
                        "bat thuong ton kho",
                        "inventory anomaly",
                        "available quantity",
                        "reserved quantity",
                        "stock level",
                        "in stock"
                )
        ) {

            return AiIntent.INVENTORY;
        }


        // =====================================================
        // PRIORITY 6: PRODUCT-FOCUSED ANALYTICS
        // ƯU TIÊN 6: PHÂN TÍCH TẬP TRUNG VÀO SẢN PHẨM
        //
        // ENGLISH NOTE:
        // This block MUST be before SALES.
        //
        // A question may contain "sales" or "revenue"
        // but still be asking specifically about one product.
        //
        // Example:
        //
        // "How is product 7 performing in sales?"
        //
        // Expected:
        // PRODUCT
        //
        // Not:
        // SALES
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Block này PHẢI nằm trước SALES.
        //
        // Một câu có thể chứa chữ "sales" hoặc "revenue",
        // nhưng mục tiêu chính vẫn là phân tích một sản phẩm.
        // =====================================================

        if (
                isProductFocusedQuestion(
                        normalized
                )
        ) {

            return AiIntent.PRODUCT;
        }


        // =====================================================
        // PRIORITY 7: SALES
        // ƯU TIÊN 7: BÁN HÀNG
        //
        // ENGLISH NOTE:
        // This block handles overall sales analytics:
        //
        // - total revenue
        // - sales trends
        // - completed orders
        // - total units sold
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Block này xử lý phân tích bán hàng tổng thể:
        //
        // - tổng doanh thu
        // - xu hướng bán hàng
        // - đơn hoàn tất
        // - tổng số lượng đã bán
        // =====================================================

        if (
                containsAny(
                        normalized,

                        "doanh thu",
                        "sales",
                        "revenue",
                        "don ban",
                        "don hang ban",
                        "ban hang",
                        "tong doanh thu",
                        "doanh so",
                        "sales trend",
                        "xu huong ban",
                        "xu huong doanh thu",
                        "ban duoc bao nhieu",
                        "so luong da ban",
                        "units sold",
                        "quantity sold",
                        "sold today",
                        "total sales",
                        "total revenue"
                )
        ) {

            return AiIntent.SALES;
        }


        // =====================================================
        // PRIORITY 8: PRODUCT
        // ƯU TIÊN 8: SẢN PHẨM
        //
        // ENGLISH NOTE:
        // General product/category questions reach this block.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Các câu hỏi chung về sản phẩm hoặc category
        // sẽ được xử lý tại đây.
        // =====================================================

        if (
                containsAny(
                        normalized,

                        "san pham",
                        "product",
                        "products",
                        "sku",
                        "item",
                        "ban chay",
                        "best selling",
                        "best-selling",
                        "top selling",
                        "ban cham",
                        "slow selling",
                        "slow-selling",
                        "khong co doanh so",
                        "product performance",
                        "hieu suat san pham",
                        "danh muc",
                        "category"
                )
        ) {

            return AiIntent.PRODUCT;
        }


        // =====================================================
        // FALLBACK: GENERAL
        // MẶC ĐỊNH: TỔNG QUÁT
        //
        // ENGLISH NOTE:
        // If no supported analytics intent is detected,
        // route the question to GENERAL.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Nếu không xác định được nhóm analytics cụ thể,
        // trả về GENERAL.
        // =====================================================

        return AiIntent.GENERAL;
    }


    // =========================================================
    // PRODUCT-FOCUSED QUESTION
    // CÂU HỎI TẬP TRUNG VÀO SẢN PHẨM
    //
    // ENGLISH NOTE:
    // Determine whether the user is asking about the
    // performance of a product rather than overall sales.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Xác định user đang hỏi hiệu suất của sản phẩm
    // thay vì hỏi doanh số tổng thể.
    // =========================================================

    private boolean isProductFocusedQuestion(
            String normalized
    ) {

        if (
                normalized == null
                        || normalized.isBlank()
        ) {

            return false;
        }


        // =====================================================
        // PRODUCT CONTEXT
        // NGỮ CẢNH SẢN PHẨM
        // =====================================================

        boolean hasProductContext =
                containsAny(
                        normalized,

                        "san pham",
                        "product",
                        "products",
                        "sku",
                        "item",
                        "category",
                        "danh muc"
                );


        if (!hasProductContext) {

            return false;
        }


        // =====================================================
        // PRODUCT PERFORMANCE LANGUAGE
        // CỤM TỪ PHÂN TÍCH HIỆU SUẤT SẢN PHẨM
        //
        // ENGLISH NOTE:
        // These phrases strongly indicate PRODUCT analytics.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Các từ này cho thấy user đang muốn phân tích PRODUCT.
        // =====================================================

        boolean hasPerformanceContext =
                containsAny(
                        normalized,

                        "performance",
                        "performing",
                        "product performance",
                        "sales performance",

                        "best selling",
                        "best-selling",
                        "top selling",

                        "slow selling",
                        "slow-selling",

                        "product revenue",
                        "revenue of product",
                        "sales of product",
                        "product sales",

                        "hieu suat",
                        "hieu suat san pham",

                        "ban chay",
                        "ban cham",

                        "doanh thu san pham",
                        "doanh thu cua san pham",

                        "khong co doanh so"
                );


        if (hasPerformanceContext) {

            return true;
        }


        // =====================================================
        // SPECIFIC PRODUCT + SALES/REVENUE
        // SẢN PHẨM CỤ THỂ + SALES/REVENUE
        //
        // ENGLISH NOTE:
        // Example:
        //
        // "How much revenue did product 7 generate?"
        //
        // Because product 7 is explicitly specified,
        // this should be PRODUCT analytics.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Ví dụ:
        //
        // "Product 7 tạo ra bao nhiêu doanh thu?"
        //
        // Vì user chỉ định product 7 cụ thể,
        // câu hỏi phải đi vào PRODUCT analytics.
        // =====================================================

        boolean hasSpecificProductReference =
                PRODUCT_ID_REFERENCE_PATTERN
                        .matcher(
                                normalized
                        )
                        .find();


        boolean hasSalesContext =
                containsAny(
                        normalized,

                        "sales",
                        "revenue",
                        "sold",
                        "selling",

                        "doanh thu",
                        "doanh so",
                        "ban duoc",
                        "so luong ban"
                );


        return hasSpecificProductReference
                && hasSalesContext;
    }


    // =========================================================
    // NORMALIZE TEXT
    // CHUẨN HÓA TEXT
    //
    // Example / Ví dụ:
    //
    // "Dự báo nhu cầu SẢN PHẨM"
    //
    // ->
    //
    // "du bao nhu cau san pham"
    //
    // ENGLISH NOTE:
    // Lowercase text, remove Vietnamese diacritics,
    // and normalize spaces.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Chuyển về chữ thường, bỏ dấu tiếng Việt
    // và chuẩn hóa khoảng trắng.
    // =========================================================

    private String normalize(
            String value
    ) {

        String lowerCase =
                value
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();


        String normalized =
                Normalizer.normalize(
                        lowerCase,
                        Normalizer.Form.NFD
                );


        normalized =
                DIACRITICS_PATTERN
                        .matcher(
                                normalized
                        )
                        .replaceAll(
                                ""
                        );


        // ENGLISH NOTE:
        // Vietnamese đ / Đ is not removed by NFD.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Chữ đ / Đ không bị loại bỏ bởi NFD.

        normalized =
                normalized.replace(
                        'đ',
                        'd'
                );


        // ENGLISH NOTE:
        // Normalize multiple spaces into one space.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Chuẩn hóa nhiều khoảng trắng thành một khoảng trắng.

        normalized =
                normalized.replaceAll(
                        "\\s+",
                        " "
                );


        return normalized;
    }


    // =========================================================
    // KEYWORD MATCH
    // KIỂM TRA TỪ KHÓA
    //
    // ENGLISH NOTE:
    // Return true when the text contains
    // at least one keyword.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Trả true nếu text chứa ít nhất một keyword.
    // =========================================================

    private boolean containsAny(

            String text,

            String... keywords
    ) {

        if (
                text == null
                        || text.isBlank()
                        || keywords == null
        ) {

            return false;
        }


        for (
                String keyword
                : keywords
        ) {

            if (
                    keyword != null
                            && !keyword.isBlank()
                            && text.contains(
                                    keyword
                            )
            ) {

                return true;
            }
        }


        return false;
    }
}