package com.toystorage.backend.services.inventories.discrepancy;

import com.toystorage.backend.enums.inventories.DiscrepancySeverity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DiscrepancySeverityPolicy {

    private final int minorMaxQuantity;
    private final double minorMaxPercent;
    private final double majorDamagedPercent;

    public DiscrepancySeverityPolicy(
            @Value("${warehouse.discrepancy.minor.max-quantity:2}") int minorMaxQuantity,
            @Value("${warehouse.discrepancy.minor.max-percent:3}") double minorMaxPercent,
            @Value("${warehouse.discrepancy.major.damaged-percent:5}") double majorDamagedPercent
    ) {
        this.minorMaxQuantity = minorMaxQuantity;
        this.minorMaxPercent = minorMaxPercent;
        this.majorDamagedPercent = majorDamagedPercent;
    }

    public DiscrepancySeverity determine(
            Integer expectedQuantity,
            Integer actualQuantity,
            int damagedQuantity
    ) {
        int expected = expectedQuantity == null ? 0 : Math.max(expectedQuantity, 0);
        int actual = actualQuantity == null ? 0 : Math.max(actualQuantity, 0);
        int damaged = Math.max(damagedQuantity, 0);

        int difference = Math.abs(actual - expected);

        double differencePercent = expected > 0
                ? (difference * 100.0) / expected
                : (difference > 0 ? 100.0 : 0.0);

        double damagedPercent = actual > 0
                ? (damaged * 100.0) / actual
                : (damaged > 0 ? 100.0 : 0.0);

        boolean quantityMajor =
                difference > minorMaxQuantity
                        && differencePercent > minorMaxPercent;

        boolean damagedMajor =
                damaged > 0
                        && damagedPercent >= majorDamagedPercent;

        return quantityMajor || damagedMajor
                ? DiscrepancySeverity.MAJOR
                : DiscrepancySeverity.MINOR;
    }

    public boolean isBlocking(DiscrepancySeverity severity) {
        return severity == DiscrepancySeverity.MAJOR;
    }
}
