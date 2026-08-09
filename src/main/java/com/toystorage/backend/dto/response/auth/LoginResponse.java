package com.toystorage.backend.dto.response.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class LoginResponse {

    private Long id;

    private String userCode;

    private String name;

    private String email;

    private Set<String> authorities;
}