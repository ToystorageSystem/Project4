package com.toystorage.backend.dto.response.receipts.store;

import java.util.List;

/** GET /manifests/{code}, and the result of F1 scan / complete-receiving */
public record ManifestDetailResponse(
        ManifestSummaryResponse manifest,
        List<PackageStatusResponse> packages,
        List<ReceiptBriefResponse> receipts) {
}