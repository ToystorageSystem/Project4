package com.toystorage.backend.dto.request.users;

import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserRolesRequest {

    /*
     * User bắt buộc phải còn ít nhất 1 Role.
     */
    @NotEmpty(
            message = "Người dùng phải có ít nhất một vai trò"
    )
    private List<Long> roleIds;
}