# Bank-Management-System
# Bank Management System - Java Swing + SQLite

A desktop-based Bank Management System developed using Java Swing and SQLite.

The application provides separate customer and administrator functionality for managing bank accounts, transactions, fund transfers, PINs, and transaction records.

## Features

### Customer Features

- Customer login using Account Number and PIN
- View account details
- Check account balance
- Deposit money
- Withdraw money
- Transfer funds
- Change PIN
- View transaction history
- Generate/view transaction details

### Admin Features

- Admin login
- Customer management
- Create new customer accounts
- Delete customer accounts
- View all customer accounts
- View transaction logs
- Manage customer information
- Admin dashboard

## Technologies Used

- Java
- Java Swing
- SQLite
- JDBC
- Object-Oriented Programming
- SQL

## Project Structure

```text
BankManagementSystemSQLite/
│
├── README.md
├── .gitignore
│
├── src/
│   └── com/
│       └── bank/
│           ├── Account.java
│           ├── BankManagementSystemSQLite.java
│           ├── DatabaseManagerSQLite.java
│           └── Transaction.java
│
├── data/
│   └── bank.db
│
└── lib/
    └── sqlite-jdbc-3.50.3.0.jar
