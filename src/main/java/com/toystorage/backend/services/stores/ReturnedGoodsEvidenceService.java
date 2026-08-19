package com.toystorage.backend.services.stores;

import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.stores.WarehouseReturnItemRepository;

import com.toystorage.backend.services.cloudinary.CloudinaryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReturnedGoodsEvidenceService {

    private final WarehouseReturnItemRepository
            returnItemRepository;

    private final ReturnedGoodsInspectionValidationService
            validationService;

    private final CloudinaryService
            cloudinaryService;


    @Transactional
    public String uploadEvidence(
            Long returnId,
            Long itemId,
            MultipartFile image
    ) {

        if (image == null
                || image.isEmpty()) {

            throw new BadRequest(
                    "Image is required"
            );
        }


        Users staff =
                validationService.getCurrentUser();


        StoreReturns storeReturn =
                validationService.getReturn(
                        returnId
                );


        validationService.validateWarehouse(
                staff,
                storeReturn
        );


        validationService.validateEditable(
                storeReturn
        );


        StoreReturnItems item =
                returnItemRepository
                        .findById(
                                itemId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Store return item not found"
                                )
                        );


        if (!item
                .getStoreReturn()
                .getId()
                .equals(returnId)) {

            throw new BadRequest(
                    "Item does not belong to store return"
            );
        }


        String imageUrl =
                cloudinaryService.uploadImage(
                        image
                );


        item.setEvidenceImageUrl(
                imageUrl
        );


        returnItemRepository.save(
                item
        );


        return imageUrl;
    }
}