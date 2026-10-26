package com.toystorage.backend.stockcount;

import com.toystorage.backend.enums.inventories.StockCountType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

/**
 * Warehouse stock counts are permitted only during two MON-FRI workweeks:
 * (1) the calendar week containing the 15th,
 * (2) the last MON-FRI week of the month (week containing the month's last Friday).
 * All dates are evaluated using the warehouse timezone by callers.
 */
public final class StockCountWindowPolicy {
    private StockCountWindowPolicy() {}

    public record Window(LocalDate start, LocalDate end, StockCountType type) {
        public boolean contains(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end)
                    && date.getDayOfWeek() != DayOfWeek.SATURDAY
                    && date.getDayOfWeek() != DayOfWeek.SUNDAY;
        }
    }

    public static Window middle(YearMonth month) {
        LocalDate monday = month.atDay(15).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Window(monday, monday.plusDays(4), StockCountType.MID_MONTH);
    }

    public static Window ending(YearMonth month) {
        LocalDate lastFriday = month.atEndOfMonth().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        return new Window(lastFriday.minusDays(4), lastFriday, StockCountType.END_OF_MONTH);
    }

    public static List<Window> windows(YearMonth month) {
        return List.of(middle(month), ending(month));
    }

    public static Optional<Window> forPlan(StockCountType type, LocalDate scheduledDate) {
        if (type == null || scheduledDate == null) return Optional.empty();
        YearMonth month = YearMonth.from(scheduledDate);
        if (type == StockCountType.MID_MONTH) return Optional.of(middle(month));
        if (type == StockCountType.END_OF_MONTH) return Optional.of(ending(month));
        // For explicitly created FULL/AD_HOC/CYCLE plans, the scheduled day selects
        // the period, and any working day of that period may be used for Start.
        return windows(month).stream().filter(window -> window.contains(scheduledDate)).findFirst();
    }

    public static boolean canCount(StockCountType type, LocalDate scheduledDate, LocalDate today) {
        return today != null && forPlan(type, scheduledDate)
                .map(window -> window.contains(today)).orElse(false);
    }

    /** Today when allowed, otherwise the first day of the next available period. */
    public static LocalDate nextEligibleDay(LocalDate today) {
        YearMonth month = YearMonth.from(today);
        for (int offset = 0; offset < 4; offset++) {
            for (Window window : windows(month.plusMonths(offset))) {
                if (window.contains(today)) return today;
                if (today.isBefore(window.start())) return window.start();
            }
        }
        throw new IllegalStateException("Cannot find the next warehouse stock count workweek");
    }
}
