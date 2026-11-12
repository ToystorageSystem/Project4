package com.toystorage.backend.config;

import com.toystorage.backend.enums.inventories.StockCountType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

/**
 * Warehouse stock counts (kiem ke ton kho thuc te) are permitted only in two periods each month:
 * (1) the first week of the month: the first five MON-FRI working days,
 * (2) the mid-month week: the MON-FRI week containing the 15th.
 * END_OF_MONTH is kept only so that plans created before this change can still be completed.
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

    /** First five working days (Mon-Fri) of the month. */
    public static Window beginning(YearMonth month) {
        LocalDate start = month.atDay(1);
        while (isWeekend(start)) start = start.plusDays(1);
        LocalDate end = start;
        int workdays = 1;
        while (workdays < 5) {
            end = end.plusDays(1);
            if (!isWeekend(end)) workdays++;
        }
        return new Window(start, end, StockCountType.BEGINNING_OF_MONTH);
    }

    private static boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /** Legacy period, used only for END_OF_MONTH plans created before the change. */
    public static Window ending(YearMonth month) {
        LocalDate lastFriday = month.atEndOfMonth().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        return new Window(lastFriday.minusDays(4), lastFriday, StockCountType.END_OF_MONTH);
    }

    public static List<Window> windows(YearMonth month) {
        return List.of(beginning(month), middle(month));
    }

    public static Optional<Window> forPlan(StockCountType type, LocalDate scheduledDate) {
        if (type == null || scheduledDate == null) return Optional.empty();
        YearMonth month = YearMonth.from(scheduledDate);
        if (type == StockCountType.MID_MONTH) return Optional.of(middle(month));
        if (type == StockCountType.BEGINNING_OF_MONTH) return Optional.of(beginning(month));
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
