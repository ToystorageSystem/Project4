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
                    uploadResult.get(
                            "secure_url"
                    );

            if (secureUrl == null) {

                throw new RuntimeException(
                        "Cloudinary did not return image URL"
                );
            }

            return secureUrl.toString();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to upload evidence image to Cloudinary",
                    e
            );
        }
    }

    /** Upload invoice PDF, XML, or image without changing the existing shortage-image upload path. */
    public String uploadSupplierInvoiceFile(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "toystorage/supplier-invoices",
                            "resource_type", "auto",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );
            Object url = result.get("secure_url");
            if (url == null) {
                throw new IllegalStateException("Cloudinary did not return supplier invoice file URL");
            }
            return url.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to upload supplier invoice file", ex);
        }
    }
}