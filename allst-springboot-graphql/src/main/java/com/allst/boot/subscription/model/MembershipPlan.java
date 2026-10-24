package com.allst.boot.subscription.model;

public enum MembershipPlan {
    MONTHLY(30, 18D),
    QUARTERLY(90, 48D),
    YEARLY(365, 168D);

    private final int days;
    private final double price;

    MembershipPlan(int days, double price) {
        this.days = days;
        this.price = price;
    }

    public int getDays() {
        return days;
    }

    public double getPrice() {
        return price;
    }
}
