package com.toystorage.backend.mapper.suppliers;

import com.toystorage.backend.dto.request.suppliers.CreateSupplierRequest;
import com.toystorage.backend.dto.request.suppliers.UpdateSupplierRequest;
import com.toystorage.backend.dto.response.suppliers.SupplierResponse;
import com.toystorage.backend.entity.suppliers.Suppliers;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public Suppliers toEntity(
            CreateSupplierRequest request
    ) {
        Suppliers supplier = new Suppliers();

        supplier.setName(request.getName());

        supplier.setTaxCode(
                request.getTaxCode()
        );

        supplier.setContactPerson(
                request.getContactPerson()
        );

        supplier.setContactPosition(
                request.getContactPosition()
        );

        supplier.setPhone(
                request.getPhone()
        );

        supplier.setEmail(
                request.getEmail()
        );

        supplier.setAddress(
                request.getAddress()
        );

        supplier.setNote(
                request.getNote()
        );

        return supplier;
    }

    public void updateEntity(
            UpdateSupplierRequest request,
            Suppliers supplier
    ) {
        supplier.setName(
                request.getName()
        );

        supplier.setTaxCode(
                request.getTaxCode()
        );

        supplier.setContactPerson(
                request.getContactPerson()
        );

        supplier.setContactPosition(
                request.getContactPosition()
        );

        supplier.setPhone(
                request.getPhone()
        );

        supplier.setEmail(
                request.getEmail()
        );

        supplier.setAddress(
                request.getAddress()
        );

        supplier.setNote(
                request.getNote()
        );
    }

    public SupplierResponse toResponse(
            Suppliers supplier
    ) {
        return SupplierResponse.builder()

                .id(
                        supplier.getId()
                )

                .supplierCode(
                        supplier.getSuppliersCode()
                )

                .name(
                        supplier.getName()
                )

                .taxCode(
                        supplier.getTaxCode()
                )

                .contactPerson(
                        supplier.getContactPerson()
                )

                .contactPosition(
                        supplier.getContactPosition()
                )

                .phone(
                        supplier.getPhone()
                )

                .email(
                        supplier.getEmail()
                )

                .address(
                        supplier.getAddress()
                )

                .note(
                        supplier.getNote()
                )

                .status(
                        supplier.getStatus().name()
                )

                .createdAt(
                        supplier.getCreatedAt()
                )

                .updatedAt(
                        supplier.getUpdatedAt()
                )

                .build();
    }
}