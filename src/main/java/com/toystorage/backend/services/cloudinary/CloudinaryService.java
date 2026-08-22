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


    // =====================================================
    // UPLOAD RECEIVING SHORTAGE IMAGE
    // =====================================================

    /*
     * Chức năng cũ của project.
     *
     * Dùng để upload ảnh bằng chứng cho receiving shortage.
     *
     * Không thay đổi logic cũ để tránh ảnh hưởng
     * đến các task Receiving hiện tại.
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


    // =====================================================
    // UPLOAD SUPPLIER INVOICE FILE
    // =====================================================

    /*
     * Task #10 - Supplier Invoice Management.
     *
     * Hóa đơn nhà cung cấp có thể là:
     *
     * - Image
     * - PDF
     * - XML
     *
     * Vì vậy resource_type = "auto" để Cloudinary
     * tự nhận diện loại file.
     */
    public String uploadSupplierInvoiceFile(
            MultipartFile file
    ) {

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

                                            /*
                                             * Giữ tên file gốc làm một phần
                                             * trong public id trên Cloudinary.
                                             */
                                            "use_filename",
                                            true,

                                            /*
                                             * Vẫn tạo tên unique để tránh
                                             * hai invoice file ghi đè nhau.
                                             */
                                            "unique_filename",
                                            true
                                    )
                            );


            Object secureUrl =
                    uploadResult.get(
                            "secure_url"
                    );


            if (secureUrl == null) {

                throw new RuntimeException(
                        "Cloudinary did not return supplier invoice file URL"
                );
            }


            return secureUrl.toString();


        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to upload supplier invoice file to Cloudinary",
                    e
            );
        }
    }
}