package com.toystorage.backend.services.products;

import com.toystorage.backend.dto.request.products.CreateProductRequest;
import com.toystorage.backend.dto.request.suppliers.CreateSupplierProductRequest;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.repository.suppliers.SupplierRepository;
import com.toystorage.backend.services.suppliers.SupplierProductService;
import com.toystorage.backend.dto.response.products.ProductResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

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

    private final SupplierRepository supplierRepository;

    private final SupplierProductService supplierProductService;

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
                        "Excel file does not contain a data worksheet"
                );
            }

            Sheet sheet = workbook.getSheetAt(0);

            validateHeader(sheet.getRow(0));

            List<ProductImportRowResponse> rows =
                    readRows(sheet);

            if (rows.isEmpty()) {
                throw new BadRequest(
                        "Excel file contains no product rows"
                );
            }

            validateDuplicateValuesInFile(rows);
            parseSupplierSheet(workbook, rows);

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
                    "Unable to read Excel file"
            );
        } catch (Exception exception) {
            throw new BadRequest(
                    "Invalid Excel file format"
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
                    "Excel file contains "
                            + preview.getInvalidRows()
                            + " invalid rows. "
                            + "Correct the errors and preview again before importing"
            );
        }

        // Doc het lien ket NCC truoc khi bat dau ghi database.
        List<SupplierExcelLine> supplierLines;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            supplierLines = parseSupplierSheet(workbook, preview.getRows());
        } catch (IOException ex) {
            throw new BadRequest("Unable to read ProductSuppliers worksheet");
        } catch (BadRequest ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequest("Invalid ProductSuppliers worksheet");
        }

        // Mot transaction: loi o Supplier association => rollback ca Product import.
        List<ProductResponse> createdProducts = new ArrayList<>();
        Map<String, Long> createdProductIds = new HashMap<>();
        for (ProductImportRowResponse row : preview.getRows()) {
            ProductResponse response = productService.createProduct(toCreateProductRequest(row));
            createdProducts.add(response);
            createdProductIds.put(normalizeCode(response.getProductCode()), response.getId());
        }
        for (SupplierExcelLine supplierLine : supplierLines) {
            CreateSupplierProductRequest link = new CreateSupplierProductRequest();
            link.setProductId(createdProductIds.get(supplierLine.productCode()));
            link.setSupplierId(supplierLine.supplierId());
            link.setSupplierProductCode(supplierLine.supplierProductCode());
            link.setPurchasePrice(supplierLine.purchasePrice());
            link.setMinimumOrderQuantity(supplierLine.minimumOrderQuantity());
            link.setLeadTimeDays(supplierLine.leadTimeDays());
            link.setDefaultSupplier(supplierLine.isDefault());
            supplierProductService.create(link);
        }

        return ProductImportResultResponse.builder()
                .totalRows(preview.getTotalRows())
                .importedRows(createdProducts.size())
                .products(createdProducts)
                .build();
    }

    // Giu sheet Products 10 cot cu. Sheet thu hai la tuy chon.
    // Mỗi dong ProductSuppliers luu mot quan he product - supplier.
    private record SupplierExcelLine(
            String productCode,
            Long supplierId,
            String supplierProductCode,
            BigDecimal purchasePrice,
            int minimumOrderQuantity,
            int leadTimeDays,
            boolean isDefault) {}

    private static final List<String> SUPPLIER_HEADERS = List.of(
            "productCode", "supplierCode", "supplierProductCode", "purchasePrice",
            "minimumOrderQuantity", "leadTimeDays", "isDefault"
    );

    private List<SupplierExcelLine> parseSupplierSheet(
            Workbook workbook, List<ProductImportRowResponse> productRows) {
        Sheet sheet = workbook.getSheet("ProductSuppliers");
        if (sheet == null) return List.of();
        Row heading = sheet.getRow(0);
        if (heading == null) throw new BadRequest("ProductSuppliers sheet is missing headers");
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < SUPPLIER_HEADERS.size(); i++) {
            if (!SUPPLIER_HEADERS.get(i).equals(getCellValue(heading, i, formatter))) {
                throw new BadRequest("ProductSuppliers column " + (i+1)
                        + " must be named: " + SUPPLIER_HEADERS.get(i));
            }
        }
        Map<String, ProductImportRowResponse> byCode = new LinkedHashMap<>();
        for (ProductImportRowResponse product : productRows) {
            byCode.put(normalizeCode(product.getProductCode()), product);
        }
        Set<String> seenPairs = new HashSet<>();
        Set<String> seenProducts = new HashSet<>();
        Set<String> seenDefault = new HashSet<>();
        List<SupplierExcelLine> links = new ArrayList<>();
        for (int rowNo = 1; rowNo <= sheet.getLastRowNum(); rowNo++) {
            Row row = sheet.getRow(rowNo);
            boolean empty = row == null;
            if (row != null) {
                empty = true;
                for (int c = 0; c < SUPPLIER_HEADERS.size(); c++) {
                    if (!getCellValue(row, c, formatter).isBlank()) { empty = false; break; }
                }
            }
            if (empty) continue;
            String productCode = normalizeCode(getCellValue(row, 0, formatter));
            String supplierCode = normalizeCode(getCellValue(row, 1, formatter));
            String supplierItemCode = toNullable(getCellValue(row, 2, formatter));
            ProductImportRowResponse owningProduct = byCode.get(productCode);
            if (owningProduct == null) {
                throw new BadRequest("ProductSuppliers row " + (rowNo + 1)
                        + " references productCode not found in Products sheet: " + productCode);
            }
            List<String> errors = new ArrayList<>();
            String prefix = "ProductSuppliers row " + (rowNo + 1) + ": ";
            if (supplierCode == null || supplierCode.isBlank()) errors.add("supplierCode is required");
            if (supplierItemCode != null && supplierItemCode.length() > 100) errors.add("supplierProductCode exceeds 100 characters");
            BigDecimal price = parseDecimal(getCellValue(row, 3, formatter), "purchasePrice", errors);
            if (price != null && (price.compareTo(new BigDecimal("0.01")) < 0 || price.scale() > 2)) {
                errors.add("purchasePrice must be positive with at most 2 decimal places");
            }
            Long min = parseLong(getCellValue(row, 4, formatter), "minimumOrderQuantity", true, errors);
            Long lead = parseLong(getCellValue(row, 5, formatter), "leadTimeDays", true, errors);
            if (min != null && (min < 1 || min > Integer.MAX_VALUE)) errors.add("minimumOrderQuantity must be between 1 and 2147483647");
            if (lead != null && (lead < 0 || lead > Integer.MAX_VALUE)) errors.add("leadTimeDays must be between 0 and 2147483647");
            String defaultText = getCellValue(row, 6, formatter).trim().toUpperCase(Locale.ROOT);
            if (!"TRUE".equals(defaultText) && !"FALSE".equals(defaultText)) errors.add("isDefault must be TRUE or FALSE");
            boolean isDefault = "TRUE".equals(defaultText);
            // Logic moi: moi Product Code moi co mot Supplier duy nhat.
            // Muon nhap cung Barcode tu NCC khac, su dung mot dong Product Code khac.
            if (supplierCode != null && !supplierCode.isBlank()) {
                if (!seenPairs.add(productCode + "|" + supplierCode)) errors.add("duplicate supplier for this product");
                if (!seenProducts.add(productCode)) errors.add("Each product code must have only one supplier");
                var supplier = supplierRepository.findBySuppliersCodeIgnoreCase(supplierCode);
                if (supplier.isEmpty() || supplier.get().getStatus() != CommonStatus.ACTIVE) {
                    errors.add("supplierCode does not identify an active supplier: " + supplierCode);
                }
                if (errors.isEmpty()) links.add(new SupplierExcelLine(
                        productCode, supplier.get().getId(), supplierItemCode, price,
                        min.intValue(), lead.intValue(), isDefault));
            }
            if (isDefault && !seenDefault.add(productCode)) errors.add("more than one default supplier for this product");
            for (String error : errors) addError(owningProduct, prefix + error);
        }
        for (ProductImportRowResponse product : productRows) {
            product.setValid(product.getErrors().isEmpty());
        }
        return links;
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
                    "productCode: Product code already exists"
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
                    "barcode: Barcode already exists"
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
                    "categoryId: Category does not exist "
                            + "or is inactive"
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
                    "brandId: Brand does not exist "
                            + "or is inactive"
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
                        "productCode: Duplicate product code "
                                + "in the Excel file"
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
                        "barcode: Duplicate barcode "
                                + "in the Excel file"
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
                    "Please select an Excel file"
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new BadRequest(
                    "Invalid Excel filename"
            );
        }

        String lowerCaseName =
                fileName.toLowerCase(Locale.ROOT);

        if (
                !lowerCaseName.endsWith(".xlsx")
                        && !lowerCaseName.endsWith(".xls")
        ) {
            throw new BadRequest(
                    "Only .xlsx or .xls Excel files are supported"
            );
        }
    }

    private void validateHeader(Row headerRow) {
        if (headerRow == null) {
            throw new BadRequest(
                    "Excel file is missing the header row"
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
                        "Excel column "
                                + (index + 1)
                                + " must be named: "
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
                        fieldName + ": Required field"
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
                    fieldName + ": Must be a valid integer"
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
                    fieldName + ": Required field"
            );

            return null;
        }

        try {
            return new BigDecimal(
                    value.trim().replace(",", "")
            );
        } catch (NumberFormatException exception) {
            errors.add(
                    fieldName + ": Must be a valid number"
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