package com.toystorage.backend.services.products;

import com.toystorage.backend.dto.request.products.CreateProductRequest;
import com.toystorage.backend.dto.response.products.ProductChangeRequestResponse;
import com.toystorage.backend.dto.response.products.ProductImportPreviewResponse;
import com.toystorage.backend.dto.response.products.ProductImportResultResponse;
import com.toystorage.backend.dto.response.products.ProductImportRowResponse;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.products.BrandRepository;
import com.toystorage.backend.repository.products.CategoryRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductExcelImportService {

    private static final List<String> EXPECTED_HEADERS = List.of(
            "productCode",
            "barcode",
            "imageUrl",
            "name",
            "categoryId",
            "brandId",
            "baseUnit",
            "purchasePrice",
            "sellingPrice",
            "requestReason"
    );

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    private final BrandRepository brandRepository;

    private final Validator validator;

    private final ProductService productService;

    public ProductImportPreviewResponse preview(
            MultipartFile file
    ) {
        validateFile(file);

        try (
                Workbook workbook = WorkbookFactory.create(
                        file.getInputStream()
                )
        ) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequest(
                        "File Excel không có sheet dữ liệu"
                );
            }

            Sheet sheet = workbook.getSheetAt(0);

            validateHeader(sheet.getRow(0));

            List<ProductImportRowResponse> rows =
                    readRows(sheet);

            if (rows.isEmpty()) {
                throw new BadRequest(
                        "File Excel không có dòng dữ liệu sản phẩm"
                );
            }

            validateDuplicateValuesInFile(rows);

            int validRows = (int) rows.stream()
                    .filter(ProductImportRowResponse::isValid)
                    .count();

            return ProductImportPreviewResponse.builder()
                    .totalRows(rows.size())
                    .validRows(validRows)
                    .invalidRows(rows.size() - validRows)
                    .rows(rows)
                    .build();

        } catch (BadRequest exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequest(
                    "Không thể đọc file Excel"
            );
        } catch (Exception exception) {
            throw new BadRequest(
                    "File Excel không đúng định dạng"
            );
        }
    }

    @Transactional
    public ProductImportResultResponse importProducts(
            MultipartFile file
    ) {
        ProductImportPreviewResponse preview = preview(file);

        if (preview.getInvalidRows() > 0) {
            throw new BadRequest(
                    "File Excel còn "
                            + preview.getInvalidRows()
                            + " dòng không hợp lệ. "
                            + "Vui lòng sửa lỗi và preview lại trước khi nhập"
            );
        }

        List<ProductChangeRequestResponse> changeRequests =
                new ArrayList<>();

        for (ProductImportRowResponse row : preview.getRows()) {
            CreateProductRequest request =
                    toCreateProductRequest(row);

            ProductChangeRequestResponse response =
                    productService.createProduct(request);

            changeRequests.add(response);
        }

        return ProductImportResultResponse.builder()
                .totalRows(preview.getTotalRows())
                .importedRows(changeRequests.size())
                .changeRequests(changeRequests)
                .build();
    }

    private CreateProductRequest toCreateProductRequest(
            ProductImportRowResponse row
    ) {
        CreateProductRequest request =
                new CreateProductRequest();

        request.setProductCode(row.getProductCode());
        request.setBarcode(row.getBarcode());
        request.setImageUrl(row.getImageUrl());
        request.setName(row.getName());
        request.setCategoryId(row.getCategoryId());
        request.setBrandId(row.getBrandId());
        request.setBaseUnit(row.getBaseUnit());
        request.setPurchasePrice(row.getPurchasePrice());
        request.setSellingPrice(row.getSellingPrice());
        request.setRequestReason(row.getRequestReason());

        return request;
    }

    private List<ProductImportRowResponse> readRows(
            Sheet sheet
    ) {
        List<ProductImportRowResponse> rows =
                new ArrayList<>();

        DataFormatter formatter = new DataFormatter();

        for (
                int index = 1;
                index <= sheet.getLastRowNum();
                index++
        ) {
            Row row = sheet.getRow(index);

            if (isEmptyRow(row, formatter)) {
                continue;
            }

            rows.add(
                    readAndValidateRow(
                            row,
                            index + 1,
                            formatter
                    )
            );
        }

        return rows;
    }

    private ProductImportRowResponse readAndValidateRow(
            Row row,
            int rowNumber,
            DataFormatter formatter
    ) {
        List<String> errors = new ArrayList<>();

        String productCode =
                getCellValue(row, 0, formatter);

        String barcode =
                getCellValue(row, 1, formatter);

        String imageUrl =
                toNullable(
                        getCellValue(row, 2, formatter)
                );

        String name =
                getCellValue(row, 3, formatter);

        Long categoryId = parseLong(
                getCellValue(row, 4, formatter),
                "categoryId",
                true,
                errors
        );

        Long brandId = parseLong(
                getCellValue(row, 5, formatter),
                "brandId",
                false,
                errors
        );

        String baseUnit =
                getCellValue(row, 6, formatter);

        BigDecimal purchasePrice = parseDecimal(
                getCellValue(row, 7, formatter),
                "purchasePrice",
                errors
        );

        BigDecimal sellingPrice = parseDecimal(
                getCellValue(row, 8, formatter),
                "sellingPrice",
                errors
        );

        String requestReason =
                toNullable(
                        getCellValue(row, 9, formatter)
                );

        CreateProductRequest request =
                new CreateProductRequest();

        request.setProductCode(productCode);
        request.setBarcode(barcode);
        request.setImageUrl(imageUrl);
        request.setName(name);
        request.setCategoryId(categoryId);
        request.setBrandId(brandId);
        request.setBaseUnit(baseUnit);
        request.setPurchasePrice(purchasePrice);
        request.setSellingPrice(sellingPrice);
        request.setRequestReason(requestReason);

        Set<ConstraintViolation<CreateProductRequest>>
                violations = validator.validate(request);

        violations.stream()
                .map(violation ->
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage()
                )
                .sorted()
                .forEach(errors::add);

        validateDatabaseValues(
                productCode,
                barcode,
                categoryId,
                brandId,
                errors
        );

        return ProductImportRowResponse.builder()
                .rowNumber(rowNumber)
                .productCode(normalizeCode(productCode))
                .barcode(normalizeText(barcode))
                .imageUrl(imageUrl)
                .name(normalizeText(name))
                .categoryId(categoryId)
                .brandId(brandId)
                .baseUnit(normalizeUppercase(baseUnit))
                .purchasePrice(purchasePrice)
                .sellingPrice(sellingPrice)
                .requestReason(requestReason)
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private void validateDatabaseValues(
            String productCode,
            String barcode,
            Long categoryId,
            Long brandId,
            List<String> errors
    ) {
        if (
                productCode != null
                        && !productCode.isBlank()
                        && productRepository
                                .existsByProductsCodeIgnoreCase(
                                        productCode.trim()
                                )
        ) {
            errors.add(
                    "productCode: Mã sản phẩm đã tồn tại"
            );
        }

        if (
                barcode != null
                        && !barcode.isBlank()
                        && productRepository.existsByBarcode(
                                barcode.trim()
                        )
        ) {
            errors.add(
                    "barcode: Barcode đã tồn tại"
            );
        }

        if (
                categoryId != null
                        && categoryRepository
                                .findByIdAndStatus(
                                        categoryId,
                                        CommonStatus.ACTIVE
                                )
                                .isEmpty()
        ) {
            errors.add(
                    "categoryId: Danh mục không tồn tại "
                            + "hoặc không hoạt động"
            );
        }

        if (
                brandId != null
                        && brandRepository
                                .findByIdAndStatus(
                                        brandId,
                                        CommonStatus.ACTIVE
                                )
                                .isEmpty()
        ) {
            errors.add(
                    "brandId: Thương hiệu không tồn tại "
                            + "hoặc không hoạt động"
            );
        }
    }

    private void validateDuplicateValuesInFile(
            List<ProductImportRowResponse> rows
    ) {
        Map<String, Integer> productCodeCounts =
                new HashMap<>();

        Map<String, Integer> barcodeCounts =
                new HashMap<>();

        for (ProductImportRowResponse row : rows) {
            String productCode =
                    normalizeCode(row.getProductCode());

            String barcode =
                    normalizeText(row.getBarcode());

            if (
                    productCode != null
                            && !productCode.isBlank()
            ) {
                productCodeCounts.merge(
                        productCode,
                        1,
                        Integer::sum
                );
            }

            if (
                    barcode != null
                            && !barcode.isBlank()
            ) {
                barcodeCounts.merge(
                        barcode,
                        1,
                        Integer::sum
                );
            }
        }

        for (ProductImportRowResponse row : rows) {
            String productCode =
                    normalizeCode(row.getProductCode());

            String barcode =
                    normalizeText(row.getBarcode());

            if (
                    productCode != null
                            && productCodeCounts.getOrDefault(
                                    productCode,
                                    0
                            ) > 1
            ) {
                addError(
                        row,
                        "productCode: Mã sản phẩm bị trùng "
                                + "trong file Excel"
                );
            }

            if (
                    barcode != null
                            && barcodeCounts.getOrDefault(
                                    barcode,
                                    0
                            ) > 1
            ) {
                addError(
                        row,
                        "barcode: Barcode bị trùng "
                                + "trong file Excel"
                );
            }

            row.setValid(row.getErrors().isEmpty());
        }
    }

    private void addError(
            ProductImportRowResponse row,
            String error
    ) {
        if (!row.getErrors().contains(error)) {
            row.getErrors().add(error);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequest(
                    "Vui lòng chọn file Excel"
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new BadRequest(
                    "Tên file Excel không hợp lệ"
            );
        }

        String lowerCaseName =
                fileName.toLowerCase(Locale.ROOT);

        if (
                !lowerCaseName.endsWith(".xlsx")
                        && !lowerCaseName.endsWith(".xls")
        ) {
            throw new BadRequest(
                    "Chỉ chấp nhận file Excel .xlsx hoặc .xls"
            );
        }
    }

    private void validateHeader(Row headerRow) {
        if (headerRow == null) {
            throw new BadRequest(
                    "File Excel thiếu dòng tiêu đề"
            );
        }

        DataFormatter formatter = new DataFormatter();

        for (
                int index = 0;
                index < EXPECTED_HEADERS.size();
                index++
        ) {
            String actualHeader = getCellValue(
                    headerRow,
                    index,
                    formatter
            );

            String expectedHeader =
                    EXPECTED_HEADERS.get(index);

            if (!expectedHeader.equals(actualHeader)) {
                throw new BadRequest(
                        "Cột Excel thứ "
                                + (index + 1)
                                + " phải có tên: "
                                + expectedHeader
                );
            }
        }
    }

    private Long parseLong(
            String value,
            String fieldName,
            boolean required,
            List<String> errors
    ) {
        if (value == null || value.isBlank()) {
            if (required) {
                errors.add(
                        fieldName + ": Không được để trống"
                );
            }

            return null;
        }

        try {
            BigDecimal number =
                    new BigDecimal(value.trim());

            return number.longValueExact();
        } catch (Exception exception) {
            errors.add(
                    fieldName + ": Phải là số nguyên hợp lệ"
            );

            return null;
        }
    }

    private BigDecimal parseDecimal(
            String value,
            String fieldName,
            List<String> errors
    ) {
        if (value == null || value.isBlank()) {
            errors.add(
                    fieldName + ": Không được để trống"
            );

            return null;
        }

        try {
            return new BigDecimal(
                    value.trim().replace(",", "")
            );
        } catch (NumberFormatException exception) {
            errors.add(
                    fieldName + ": Phải là số hợp lệ"
            );

            return null;
        }
    }

    private boolean isEmptyRow(
            Row row,
            DataFormatter formatter
    ) {
        if (row == null) {
            return true;
        }

        for (
                int index = 0;
                index < EXPECTED_HEADERS.size();
                index++
        ) {
            if (
                    !getCellValue(
                            row,
                            index,
                            formatter
                    ).isBlank()
            ) {
                return false;
            }
        }

        return true;
    }

    private String getCellValue(
            Row row,
            int columnIndex,
            DataFormatter formatter
    ) {
        if (row == null || row.getCell(columnIndex) == null) {
            return "";
        }

        return formatter.formatCellValue(
                row.getCell(columnIndex)
        ).trim();
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUppercase(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private String toNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}