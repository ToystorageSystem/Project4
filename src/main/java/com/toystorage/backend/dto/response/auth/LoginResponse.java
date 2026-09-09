package com.toystorage.backend.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Long id;

    private String userCode;

    private String name;

    private String email;

    private Set<String> authorities;

    /*
     * true:
     * user bắt buộc đổi password trước
     * khi sử dụng các chức năng khác.
     */
    private boolean mustChangePassword;
}