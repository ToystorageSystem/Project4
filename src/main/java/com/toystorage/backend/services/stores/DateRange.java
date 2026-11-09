package com.toystorage.backend.services.stores;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.toystorage.backend.exceptions.BadRequest;

/** [from, to) for queries. Days are inclusive; null = open end. */
public record DateRange(LocalDateTime from, LocalDateTime to) {

    private static final LocalDate MIN_DAY = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX_DAY = LocalDate.of(2100, 1, 1);

    public static DateRange ofDays(LocalDate fromDay, LocalDate toDay) {
        if (fromDay != null && toDay != null && fromDay.isAfter(toDay)) {
            throw new BadRequest("from must be before or equal to to");
        }
        LocalDate start = fromDay == null ? MIN_DAY : fromDay;
        LocalDate end = toDay == null ? MAX_DAY : toDay.plusDays(1);   // include the whole "to" day
        return new DateRange(start.atStartOfDay(), end.atStartOfDay());
    }
}
