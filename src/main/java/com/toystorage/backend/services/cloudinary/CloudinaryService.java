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
}