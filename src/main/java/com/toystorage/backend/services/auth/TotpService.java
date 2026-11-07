
package com.toystorage.backend.services.auth;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalLong;

@Service
public class TotpService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int SECRET_LENGTH_BYTES = 20;

    private static final String BASE32_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static final String ISSUER = "ToyStorageSystem";

    private final SecureRandom secureRandom =
            new SecureRandom();

    /*
     * Tạo secret ngẫu nhiên cho mỗi tài khoản.
     *
     * Secret được mã hóa Base32 để sử dụng
     * với Google Authenticator.
     */
    public String generateSecret() {

        byte[] bytes = new byte[SECRET_LENGTH_BYTES];

        secureRandom.nextBytes(bytes);

        return encodeBase32(bytes);
    }

    /*
     * Tạo URI để sinh mã QR cho Google Authenticator.
     *
     * URI chứa thông tin:
     * - Tên hệ thống
     * - Email tài khoản
     * - Secret
     * - Thuật toán SHA1
     * - Mã OTP 6 chữ số
     * - Chu kỳ 30 giây
     */
    public String createOtpAuthUri(
            String email,
            String secret
    ) {

        if (email == null || email.isBlank()
                || secret == null || secret.isBlank()) {

            throw new IllegalArgumentException(
                    "Email and secret are required"
            );
        }

        String label = URLEncoder.encode(
                ISSUER + ":" + email.trim(),
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + ISSUER
                + "&algorithm=SHA1"
                + "&digits=6"
                + "&period=30";
    }

    /*
     * Kiểm tra mã OTP do người dùng nhập.
     *
     * Giữ nguyên kiểu trả về boolean
     * để tương thích với code và unit test cũ.
     *
     * true  = OTP hợp lệ
     * false = OTP không hợp lệ
     */
    public boolean verifyCode(
            String secret,
            String code
    ) {

        return findMatchingCounter(
                secret,
                code
        ).isPresent();
    }

    /*
     * Xác định chu kỳ 30 giây của mã OTP hợp lệ.
     *
     * Trả về:
     * - OptionalLong chứa counter nếu OTP hợp lệ.
     * - OptionalLong.empty() nếu OTP không hợp lệ.
     *
     * Counter được dùng để so sánh với
     * lastAcceptedStep trong database,
     * giúp ngăn sử dụng lại mã OTP.
     */
    public OptionalLong findMatchingCounter(
            String secret,
            String code
    ) {

        // Kiểm tra dữ liệu đầu vào.
        if (secret == null
                || code == null
                || !code.matches("[0-9]{6}")) {

            return OptionalLong.empty();
        }

        byte[] secretBytes;

        try {

            // Giải mã secret Base32.
            secretBytes = decodeBase32(secret);

        } catch (IllegalArgumentException ex) {

            return OptionalLong.empty();
        }

        // Secret quá ngắn không hợp lệ.
        if (secretBytes.length < 10) {

            return OptionalLong.empty();
        }

        /*
         * Tính chu kỳ hiện tại.
         *
         * Mỗi chu kỳ kéo dài 30 giây.
         */
        long currentCounter =
                Instant.now().getEpochSecond()
                        / TIME_STEP_SECONDS;

        /*
         * Kiểm tra:
         *
         *  0  = Chu kỳ hiện tại
         * -1  = Chu kỳ trước
         * +1  = Chu kỳ tiếp theo
         *
         * Cho phép sai lệch đồng hồ khoảng 30 giây.
         */
        int[] offsets = {0, -1, 1};

        for (int offset : offsets) {

            long counter = currentCounter + offset;

            if (counter < 0) {
                continue;
            }

            // Tính OTP dự kiến tại chu kỳ này.
            String expected = calculateCode(
                    secretBytes,
                    counter
            );

            /*
             * So sánh mã OTP bằng MessageDigest.isEqual.
             */
            boolean matches = MessageDigest.isEqual(
                    expected.getBytes(
                            StandardCharsets.US_ASCII
                    ),
                    code.getBytes(
                            StandardCharsets.US_ASCII
                    )
            );

            if (matches) {

                return OptionalLong.of(counter);
            }
        }

        return OptionalLong.empty();
    }

    /*
     * Tính mã TOTP theo HMAC-SHA1.
     *
     * Đầu vào:
     * - Secret dạng byte[]
     * - Counter của chu kỳ thời gian
     *
     * Đầu ra:
     * - Mã OTP gồm đúng 6 chữ số
     */
    private String calculateCode(
            byte[] secret,
            long counter
    ) {

        try {

            Mac hmac = Mac.getInstance("HmacSHA1");

            hmac.init(
                    new SecretKeySpec(
                            secret,
                            "HmacSHA1"
                    )
            );

            // Chuyển counter thành mảng 8 byte.
            byte[] counterBytes =
                    ByteBuffer.allocate(Long.BYTES)
                            .putLong(counter)
                            .array();

            // Tính HMAC.
            byte[] hash = hmac.doFinal(counterBytes);

            // Dynamic truncation.
            int offset =
                    hash[hash.length - 1] & 0x0f;

            int binary =
                    ((hash[offset] & 0x7f) << 24)
                            | ((hash[offset + 1] & 0xff) << 16)
                            | ((hash[offset + 2] & 0xff) << 8)
                            | (hash[offset + 3] & 0xff);

            // Chuyển thành mã OTP 6 chữ số.
            return String.format(
                    Locale.ROOT,
                    "%0" + DIGITS + "d",
                    binary % 1_000_000
            );

        } catch (GeneralSecurityException ex) {

            throw new IllegalStateException(
                    "TOTP algorithm is unavailable",
                    ex
            );
        }
    }

    /*
     * Chuyển mảng byte thành chuỗi Base32.
     *
     * Sử dụng khi tạo secret mới.
     */
    private String encodeBase32(byte[] data) {

        StringBuilder result = new StringBuilder();

        int buffer = 0;
        int bitCount = 0;

        for (byte b : data) {

            buffer = (buffer << 8) | (b & 0xff);

            bitCount += 8;

            while (bitCount >= 5) {

                bitCount -= 5;

                result.append(
                        BASE32_ALPHABET.charAt(
                                (buffer >>> bitCount) & 31
                        )
                );
            }
        }

        if (bitCount > 0) {

            result.append(
                    BASE32_ALPHABET.charAt(
                            (buffer << (5 - bitCount)) & 31
                    )
            );
        }

        return result.toString();
    }

    /*
     * Giải mã chuỗi Base32 thành mảng byte.
     *
     * Sử dụng khi xác minh mã OTP.
     */
    private byte[] decodeBase32(String text) {

        String input = text.trim()
                .replace("=", "")
                .toUpperCase(Locale.ROOT);

        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        int buffer = 0;
        int bitCount = 0;

        for (int i = 0; i < input.length(); i++) {

            int value = BASE32_ALPHABET.indexOf(
                    input.charAt(i)
            );

            if (value == -1) {

                throw new IllegalArgumentException(
                        "Invalid Base32 secret"
                );
            }

            buffer = (buffer << 5) | value;

            bitCount += 5;

            if (bitCount >= 8) {

                bitCount -= 8;

                bytes.write(
                        (buffer >>> bitCount) & 0xff
                );
            }
        }

        return bytes.toByteArray();
    }
}
