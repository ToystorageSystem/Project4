package com.toystorage.backend.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class QRCodeUtil {

    public static String generateQRImage(String text) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();

        BitMatrix matrix = writer.encode(
                text,
                BarcodeFormat.QR_CODE,
                250,
                250
        );

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        MatrixToImageWriter.writeToStream(
                matrix,
                "PNG",
                stream
        );

        return Base64.getEncoder()
                .encodeToString(stream.toByteArray());
    }
}