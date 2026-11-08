package com.toystorage.backend.dto.request.suppliers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSupplierRequest {

    @NotBlank(message = "Supplier name is required")
    @Size(
            max = 200,
            message = "Supplier name cannot exceed 200 characters"
    )
    private String name;

    @Size(
            max = 50,
            message = "Tax ID cannot exceed 50 characters"
    )
    private String taxCode;

    @Size(
            max = 150,
            message = "Contact person's name cannot exceed 150 characters"
    )
    private String contactPerson;

    @Size(
            max = 100,
            message = "Contact position cannot exceed 100 characters"
    )
    private String contactPosition;

    @Pattern(
            regexp = "^$|^\\+?[0-9]{9,15}$",
            message = "Phone number must contain 9 to 15 digits"
    )
    private String phone;

    @Email(message = "Invalid email address format")
    @Size(
            max = 150,
            message = "Email cannot exceed 150 characters"
    )
    private String email;

    @Size(
            max = 255,
            message = "Address cannot exceed 255 characters"
    )
    private String address;

    private String note;
}