package com.toystorage.backend.services.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /*
     * Upload ảnh bằng chứng thiếu hàng.
     */
    public String uploadImage(
            MultipartFile image
    ) {
        try {
            Map<?, ?> uploadResult =
                    cloudinary
                            .uploader()
                            .upload(
                                    image.getBytes(),
                                    ObjectUtils.asMap(
                                            "folder",
                                            "toystorage/receiving-shortages",
                                            "resource_type",
                                            "image"
                                    )
                            );

            Object secureUrl =
                    uploadResult.get("secure_url");

            if (secureUrl == null) {
                throw new IllegalStateException(
                        "Cloudinary did not return image URL"
                );
            }

            return secureUrl.toString();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to upload evidence image to Cloudinary",
                    exception
            );
        }
    }

    /*
     * Upload file hóa đơn nhà cung cấp:
     * PDF, XML hoặc hình ảnh.
     */
    public String uploadSupplierInvoiceFile(
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Supplier invoice file is required"
            );
        }

        try {
            Map<?, ?> uploadResult =
                    cloudinary
                            .uploader()
                            .upload(
                                    file.getBytes(),
                                    ObjectUtils.asMap(
                                            "folder",
                                            "toystorage/supplier-invoices",
                                            "resource_type",
                                            "auto",
                                            "use_filename",
                                            true,
                                            "unique_filename",
                                            true
                                    )
                            );

            Object secureUrl =
                    uploadResult.get("secure_url");

            if (secureUrl == null) {
                throw new IllegalStateException(
                        "Cloudinary did not return supplier invoice file URL"
                );
            }

            return secureUrl.toString();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to upload supplier invoice file",
                    exception
            );
        }
    }
}