package com.bank;

public class Account {
    public String accNo, title, firstName, lastName, email, mobile, dob, address, idProof, pin, accType;
    public double balance;

    public Account(String accNo, String title, String firstName, String lastName, String email, String mobile,
                   String dob, String address, String idProof, String pin, String accType, double balance) {
        this.accNo = accNo;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobile = mobile;
        this.dob = dob;
        this.address = address;
        this.idProof = idProof;
        this.pin = pin;
        this.accType = accType;
        this.balance = balance;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}