package com.bank;

public class Transaction {
    public String tId;
    public String datetime;
    public String fromAcc;
    public String toAcc;
    public double amount;
    public String description;

    public Transaction(String tId, String datetime, String fromAcc, String toAcc, double amount, String description) {
        this.tId = tId;
        this.datetime = datetime;
        this.fromAcc = fromAcc;
        this.toAcc = toAcc;
        this.amount = amount;
        this.description = description;
    }
}