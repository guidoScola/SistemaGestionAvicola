package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.time.LocalDate;

final class DateRange {

    private DateRange() {
    }

    static void requireComplete(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidInputException("Date range is required");
        }
        requireOrdered(from, to);
    }

    static void requireOrdered(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidInputException("Start date must not be after end date");
        }
    }
}
