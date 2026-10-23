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

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(
            max = 200,
            message = "Tên nhà cung cấp không được vượt quá 200 ký tự"
    )
    private String name;

    @Size(
            max = 50,
            message = "Mã số thuế không được vượt quá 50 ký tự"
    )
    private String taxCode;

    @Size(
            max = 150,
            message = "Tên người liên hệ không được vượt quá 150 ký tự"
    )
    private String contactPerson;

    @Size(
            max = 100,
            message = "Chức vụ người liên hệ không được vượt quá 100 ký tự"
    )
    private String contactPosition;

    @Pattern(
            regexp = "^$|^\\+?[0-9]{9,15}$",
            message = "Số điện thoại phải có từ 9 đến 15 chữ số"
    )
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(
            max = 150,
            message = "Email không được vượt quá 150 ký tự"
    )
    private String email;

    @Size(
            max = 255,
            message = "Địa chỉ không được vượt quá 255 ký tự"
    )
    private String address;

    private String note;
}