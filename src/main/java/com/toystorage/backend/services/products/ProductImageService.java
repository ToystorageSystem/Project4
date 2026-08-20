package com.toystorage.backend.services.products;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.toystorage.backend.dto.response.products.ProductImageUploadResponse;
import com.toystorage.backend.exceptions.BadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final String PRODUCT_FOLDER =
            "toystorage/products";

    private final Cloudinary cloudinary;

    /**
     * Upload ảnh sản phẩm lên Cloudinary.
     *
     * Method này chỉ upload ảnh và trả về URL.
     * Không tạo hoặc cập nhật Product trong database.
     */
    public ProductImageUploadResponse uploadImage(
            MultipartFile image
    ) {

        validateImage(image);

        try {

            Map<?, ?> uploadResult =
                    cloudinary
                            .uploader()
                            .upload(
                                    image.getBytes(),
                                    ObjectUtils.asMap(
                                            "folder",
                                            PRODUCT_FOLDER,
                                            "resource_type",
                                            "image"
                                    )
                            );

            Object secureUrl =
                    uploadResult.get("secure_url");

            if (secureUrl == null) {
                throw new RuntimeException(
                        "Cloudinary did not return image URL"
                );
            }

            return ProductImageUploadResponse
                    .builder()
                    .imageUrl(
                            secureUrl.toString()
                    )
                    .build();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Không thể tải ảnh sản phẩm lên Cloudinary",
                    e
            );
        }
    }

    /**
     * Kiểm tra file trước khi upload.
     */
    private void validateImage(
            MultipartFile image
    ) {

        if (image == null || image.isEmpty()) {

            throw new BadRequest(
                    "Ảnh sản phẩm không được để trống"
            );
        }

        String contentType =
                image.getContentType();

        if (contentType == null
                || !contentType.startsWith("image/")) {

            throw new BadRequest(
                    "File tải lên phải là hình ảnh"
            );
        }
    }
}