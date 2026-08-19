package com.bank;

import java.nio.file.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseManagerSQLite {
    private final String dbUrl;
    private Connection conn;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public DatabaseManagerSQLite(String dataDir) throws SQLException {
        try {
            Path base = Paths.get(dataDir);
            if (!Files.exists(base)) Files.createDirectories(base);
        } catch (Exception e) {
            // ignore
        }
        dbUrl = "jdbc:sqlite:" + dataDir + "/bank.db";
        connect();
        init();
    }

    private void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}
        conn = DriverManager.getConnection(dbUrl);
        conn.setAutoCommit(true);
    }

    private void init() throws SQLException {
        String createAccounts = "CREATE TABLE IF NOT EXISTS accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "acc_no TEXT UNIQUE NOT NULL," +
                "title TEXT, first_name TEXT, last_name TEXT," +
                "email TEXT, mobile TEXT, dob TEXT, address TEXT," +
                "id_proof TEXT, pin TEXT, acc_type TEXT," +
                "balance REAL DEFAULT 0" +
                ");";
        String createTx = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "t_id TEXT UNIQUE NOT NULL," +
                "datetime TEXT NOT NULL," +
                "from_acc TEXT, to_acc TEXT," +
                "amount REAL," +
                "description TEXT" +
                ");";
        try (Statement st = conn.createStatement()) {
            st.execute(createAccounts);
            st.execute(createTx);
        }
       
    }

    public String generateNextAccNo() throws SQLException {
        String q = "SELECT MAX(id) as mx FROM accounts";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            int mx = 0;
            if (rs.next()) mx = rs.getInt("mx");
            int next = 200000 + mx + 1;
            return "AC" + next;
        }
    }

    public String generateNextTxId() throws SQLException {
        String q = "SELECT MAX(id) as mx FROM transactions";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            int mx = 0;
            if (rs.next()) mx = rs.getInt("mx");
            return "T" + (1000 + mx + 1);
        }
    }

    public boolean createAccount(Account acc) throws SQLException {
        String sql = "INSERT INTO accounts(acc_no, title, first_name, last_name, email, mobile, dob, address, id_proof, pin, acc_type, balance) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, acc.accNo);
            ps.setString(2, acc.title);
            ps.setString(3, acc.firstName);
            ps.setString(4, acc.lastName);
            ps.setString(5, acc.email);
            ps.setString(6, acc.mobile);
            ps.setString(7, acc.dob);
            ps.setString(8, acc.address);
            ps.setString(9, acc.idProof);
            ps.setString(10, acc.pin);
            ps.setString(11, acc.accType);
            ps.setDouble(12, acc.balance);
            ps.executeUpdate();
        }
        if (acc.balance > 0) {
            String tId = generateNextTxId();
            String time = fmt.format(new Date());
            recordTransaction(new Transaction(tId, time, "SELF", acc.accNo, acc.balance, "Initial Deposit"));
        }
        return true;
    }

    public Account getAccountByAccNo(String accNo) throws SQLException {
        String q = "SELECT * FROM accounts WHERE acc_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, accNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getString("acc_no"),
                            rs.getString("title"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            rs.getString("mobile"),
                            rs.getString("dob"),
                            rs.getString("address"),
                            rs.getString("id_proof"),
                            rs.getString("pin"),
                            rs.getString("acc_type"),
                            rs.getDouble("balance")
                    );
                }
            }
        }
        return null;
    }

    public List<Account> listAllAccounts() throws SQLException {
        List<Account> out = new ArrayList<>();
        String q = "SELECT * FROM accounts ORDER BY id ASC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                out.add(new Account(
                        rs.getString("acc_no"),
                        rs.getString("title"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("mobile"),
                        rs.getString("dob"),
                        rs.getString("address"),
                        rs.getString("id_proof"),
                        rs.getString("pin"),
                        rs.getString("acc_type"),
                        rs.getDouble("balance")
                ));
            }
        }
        return out;
    }

    public boolean updateAccountBalance(String accNo, double newBalance) throws SQLException {
        String u = "UPDATE accounts SET balance = ? WHERE acc_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(u)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, accNo);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean changePin(String accNo, String newPin) throws SQLException {
        String u = "UPDATE accounts SET pin = ? WHERE acc_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(u)) {
            ps.setString(1, newPin);
            ps.setString(2, accNo);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteAccount(String accNo) throws SQLException {
        String dTx = "DELETE FROM transactions WHERE from_acc = ? OR to_acc = ?";
        String dAcc = "DELETE FROM accounts WHERE acc_no = ?";
        try (PreparedStatement p1 = conn.prepareStatement(dTx);
             PreparedStatement p2 = conn.prepareStatement(dAcc)) {
            conn.setAutoCommit(false);
            p1.setString(1, accNo);
            p1.setString(2, accNo);
            p1.executeUpdate();
            p2.setString(1, accNo);
            p2.executeUpdate();
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException ex) {
            conn.rollback();
            conn.setAutoCommit(true);
            throw ex;
        }
    }

    public boolean recordTransaction(Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions(t_id, datetime, from_acc, to_acc, amount, description) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.tId);
            ps.setString(2, t.datetime);
            ps.setString(3, t.fromAcc);
            ps.setString(4, t.toAcc);
            ps.setDouble(5, t.amount);
            ps.setString(6, t.description);
            ps.executeUpdate();
            return true;
        }
    }

    public List<Transaction> listAllTransactions() throws SQLException {
        List<Transaction> out = new ArrayList<>();
        String q = "SELECT * FROM transactions ORDER BY id DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                out.add(new Transaction(
                        rs.getString("t_id"),
                        rs.getString("datetime"),
                        rs.getString("from_acc"),
                        rs.getString("to_acc"),
                        rs.getDouble("amount"),
                        rs.getString("description")
                ));
            }
        }
        return out;
    }

    public List<Transaction> listTransactionsForAccount(String accNo) throws SQLException {
        List<Transaction> out = new ArrayList<>();
        String q = "SELECT * FROM transactions WHERE from_acc = ? OR to_acc = ? ORDER BY id DESC";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, accNo);
            ps.setString(2, accNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Transaction(
                            rs.getString("t_id"),
                            rs.getString("datetime"),
                            rs.getString("from_acc"),
                            rs.getString("to_acc"),
                            rs.getDouble("amount"),
                            rs.getString("description")
                    ));
                }
            }
        }
        return out;
    }

    public boolean transferFunds(String fromAcc, String toAcc, double amount) throws SQLException {
        conn.setAutoCommit(false);
        try {
            Account src = getAccountByAccNo(fromAcc);
            Account dst = getAccountByAccNo(toAcc);
            if (src == null || dst == null) {
                conn.rollback();
                conn.setAutoCommit(true);
                return false;
            }
            if (src.balance < amount) {
                conn.rollback();
                conn.setAutoCommit(true);
                return false;
            }
            double newSrc = src.balance - amount;
            double newDst = dst.balance + amount;
            updateAccountBalance(fromAcc, newSrc);
            updateAccountBalance(toAcc, newDst);
            String tId = generateNextTxId();
            String time = fmt.format(new Date());
            recordTransaction(new Transaction(tId, time, fromAcc, toAcc, amount, "Fund Transfer"));
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException ex) {
            conn.rollback();
            conn.setAutoCommit(true);
            throw ex;
        }
    }

    public boolean authenticateCustomer(String accNo, String pin) throws SQLException {
        String q = "SELECT pin FROM accounts WHERE acc_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, accNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("pin").equals(pin);
                }
            }
        }
        return false;
    }

    public void close() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }
}