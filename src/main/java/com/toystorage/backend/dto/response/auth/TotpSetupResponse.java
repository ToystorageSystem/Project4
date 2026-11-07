
package com.toystorage.backend.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpSetupResponse {

    // URI dung de tao QR Code cho Google Authenticator.
    private String otpAuthUri;

    // Khoa bi mat de nguoi dung nhap thu cong.
    private String manualEntryKey;
}
