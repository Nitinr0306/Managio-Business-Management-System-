package com.nitin.saas.dashboard.dto;

import java.time.LocalDate;
import java.sql.Date;

public class DailyRevenuePoint {
    public LocalDate date;
    public Long amount;

    public DailyRevenuePoint(Date date, Long amount) {
        this.date = date.toLocalDate();
        this.amount = amount;
    }
}

