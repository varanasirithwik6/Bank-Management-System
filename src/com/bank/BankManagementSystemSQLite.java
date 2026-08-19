package com.bank;

import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BankManagementSystemSQLite extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private DatabaseManagerSQLite db;
    private Account loggedInAccount = null;

    private java.util.HashMap<String, String> admins = new java.util.HashMap<>();
    private String loggedInAdmin = null;

    private DefaultTableModel customerTableModel;
    private DefaultTableModel transactionLogsTableModel;
    private DefaultTableModel historyTableModel;
    private JTextArea summaryInfoArea;
    private JLabel fromAccountLabel;

    public BankManagementSystemSQLite() {
        setTitle("Bank Management System");
        setSize(1024, 768);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            db = new DatabaseManagerSQLite("data");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB init failed: " + ex.getMessage());
            System.exit(1);
        }

        admins.put("admin", "1234");

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createMainMenuPanel(), "MAIN");
        mainPanel.add(createAdminLoginPanel(), "ADMIN_LOGIN");
        mainPanel.add(createAdminDashboardPanel(), "ADMIN_DASHBOARD");
        mainPanel.add(createCustomerLoginPanel(), "CUSTOMER_LOGIN");
        mainPanel.add(createCustomerDashboardPanel(), "CUSTOMER_DASH");
        mainPanel.add(createTransactionPanel(), "TRANSACTION");
        mainPanel.add(createAccountSummaryPanel(), "SUMMARY");
        mainPanel.add(createTransactionHistoryPanel(), "HISTORY");
        mainPanel.add(createFundTransferPanel(), "TRANSFER");
        mainPanel.add(createChangePinPanel(), "PIN");

        add(mainPanel);
    }

    private void styleButton(JButton button, int width, int height, int fontSize) {
        button.setPreferredSize(new Dimension(width, height));
        button.setFont(new Font("Arial", Font.BOLD, fontSize));
        button.setBackground(new Color(60, 90, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }


    private JPanel createMainMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        JLabel title = new JLabel("Bank Management System ", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        panel.add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(3, 1, 15, 15));
        buttons.setBorder(BorderFactory.createEmptyBorder(20, 300, 20, 300));
        JButton customerLoginBtn = new JButton("Customer Login");
        JButton adminLoginBtn = new JButton("Admin Login");
        JButton exitBtn = new JButton("Exit");

        styleButton(customerLoginBtn, 200, 50, 16);
        styleButton(adminLoginBtn, 200, 50, 16);
        styleButton(exitBtn, 200, 50, 16);

        customerLoginBtn.addActionListener(e -> cardLayout.show(mainPanel, "CUSTOMER_LOGIN"));
        adminLoginBtn.addActionListener(e -> cardLayout.show(mainPanel, "ADMIN_LOGIN"));
        exitBtn.addActionListener(e -> System.exit(0));

        buttons.add(customerLoginBtn);
        buttons.add(adminLoginBtn);
        buttons.add(exitBtn);
        panel.add(buttons, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAdminLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(150, 200, 150, 200));
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton login = new JButton("Login");
        JButton back = new JButton("Back");

        styleButton(login, 120, 35, 14);
        styleButton(back, 120, 35, 14);

        panel.add(new JLabel("Admin Username:"));
        panel.add(user);
        panel.add(new JLabel("Password:"));
        panel.add(pass);
        panel.add(login);
        panel.add(back);

        login.addActionListener(e -> {
            String username = user.getText();
            String password = new String(pass.getPassword());
            if (admins.containsKey(username) && admins.get(username).equals(password)) {
                loggedInAdmin = username;
                JOptionPane.showMessageDialog(this, "Admin Login Successful!");
                cardLayout.show(mainPanel, "ADMIN_DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials!");
            }
        });
        back.addActionListener(e -> cardLayout.show(mainPanel, "MAIN"));
        return panel;
    }

    private JPanel createAdminDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.add("Customer Management", createCustomerManagementPanel());
        tabbedPane.add("Transaction Logs & Reports", createTransactionLogsPanel());
        tabbedPane.add("Admin & Security", createAdminSecurityPanel());

        JButton logoutBtn = new JButton("Logout");
        styleButton(logoutBtn, 100, 30, 12);
        logoutBtn.addActionListener(e -> {
            loggedInAdmin = null;
            cardLayout.show(mainPanel, "MAIN");
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(logoutBtn);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) {
                refreshCustomerTable();
            } else if (selectedIndex == 1) {
                refreshTransactionLogsTable();
            }
        });
        return panel;
    }

    private JPanel createCustomerManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columnNames = {"Acc No", "Name", "Email", "Mobile", "Balance"};
        customerTableModel = new DefaultTableModel(columnNames, 0);
        JTable customerTable = new JTable(customerTableModel);
        panel.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton btnAdd = new JButton("Create New Customer");
        JButton btnDelete = new JButton("Delete Selected");
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnDelete);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> showCreateCustomerDialog());

        btnDelete.addActionListener(e -> {
            int selectedRow = customerTable.getSelectedRow();
            if (selectedRow >= 0) {
                String accNo = (String) customerTableModel.getValueAt(selectedRow, 0);
                int choice = JOptionPane.showConfirmDialog(this, "Delete account " + accNo + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        db.deleteAccount(accNo);
                        refreshCustomerTable();
                        JOptionPane.showMessageDialog(this, "Account deleted.");
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error deleting account: " + ex.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a customer to delete.");
            }
        });
        return panel;
    }

    private void refreshCustomerTable() {
        customerTableModel.setRowCount(0);
        try {
            List<Account> list = db.listAllAccounts();
            for (Account acc : list) {
                Object[] row = {acc.accNo, acc.getFullName(), acc.email, acc.mobile, String.format("%.2f", acc.balance)};
                customerTableModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }

    private void showCreateCustomerDialog() {
    JDialog dialog = new JDialog(this, "Create New Customer Account", true);
    dialog.setSize(500, 600);
    dialog.setLayout(new BorderLayout(10,10));
    dialog.setLocationRelativeTo(this);

    JPanel formPanel = new JPanel(new GridLayout(12, 2, 10, 10));
    formPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

    JComboBox<String> title = new JComboBox<>(new String[]{"Mr.", "Mrs.", "Ms."});
    JTextField fName = new JTextField(), lName = new JTextField(), email = new JTextField(),
            phone = new JTextField(), dob = new JTextField(), address = new JTextField(),
            idProof = new JTextField(), initialDeposit = new JTextField();
    JPasswordField pin = new JPasswordField();
    JComboBox<String> accountTypeBox = new JComboBox<>(new String[]{"Savings", "Current"});

    formPanel.add(new JLabel("Title:")); formPanel.add(title);
    formPanel.add(new JLabel("First Name:")); formPanel.add(fName);
    formPanel.add(new JLabel("Last Name:")); formPanel.add(lName);
    formPanel.add(new JLabel("Email:")); formPanel.add(email);
    formPanel.add(new JLabel("Phone:")); formPanel.add(phone);
    formPanel.add(new JLabel("DOB (yyyy-mm-dd):")); formPanel.add(dob);
    formPanel.add(new JLabel("Address:")); formPanel.add(address);
    formPanel.add(new JLabel("ID Proof No:")); formPanel.add(idProof);
    formPanel.add(new JLabel("Initial Deposit:")); formPanel.add(initialDeposit);
    formPanel.add(new JLabel("Set Customer PIN:")); formPanel.add(pin);
    formPanel.add(new JLabel("Account Type:")); formPanel.add(accountTypeBox);

    JButton createBtn = new JButton("Create Account");
    createBtn.addActionListener(ae -> {
        try {
            double deposit = Double.parseDouble(initialDeposit.getText().isEmpty() ? "0" : initialDeposit.getText());
            String accNo = db.generateNextAccNo();
            Account acc = new Account(accNo, (String) title.getSelectedItem(), fName.getText(), lName.getText(), email.getText(),
                    phone.getText(), dob.getText(), address.getText(), idProof.getText(),
                    new String(pin.getPassword()), (String) accountTypeBox.getSelectedItem(), deposit);
            db.createAccount(acc);
            JOptionPane.showMessageDialog(dialog, "Account Created!\nAccount No: " + accNo);
            refreshCustomerTable();
            dialog.dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(dialog, "Invalid Initial Deposit amount.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(dialog, "DB error: " + ex.getMessage());
        }
    });

    dialog.add(formPanel, BorderLayout.CENTER);
    dialog.add(createBtn, BorderLayout.SOUTH);
    dialog.setVisible(true);
}


    private JPanel createTransactionLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("From:"));
        JTextField fromDate = new JTextField("yyyy-mm-dd", 10);
        filterPanel.add(fromDate);
        filterPanel.add(new JLabel("To:"));
        JTextField toDate = new JTextField("yyyy-mm-dd", 10);
        filterPanel.add(toDate);
        filterPanel.add(new JLabel("Account No:"));
        JTextField accNoFilter = new JTextField(10);
        filterPanel.add(accNoFilter);
        JButton filterBtn = new JButton("Filter");
        filterPanel.add(filterBtn);
        JButton exportBtn = new JButton("Export CSV");
        filterPanel.add(exportBtn);
        panel.add(filterPanel, BorderLayout.NORTH);

        String[] columnNames = {"Transaction ID", "Date & Time", "From", "To", "Amount", "Description"};
        transactionLogsTableModel = new DefaultTableModel(columnNames, 0);
        JTable logsTable = new JTable(transactionLogsTableModel);
        panel.add(new JScrollPane(logsTable), BorderLayout.CENTER);

        exportBtn.addActionListener(e -> {
            try {
                java.io.FileWriter csv = new java.io.FileWriter("transaction_logs.csv");
                for (int i = 0; i < transactionLogsTableModel.getColumnCount(); i++) {
                    csv.write(transactionLogsTableModel.getColumnName(i) + ",");
                }
                csv.write("\n");
                for (int i = 0; i < transactionLogsTableModel.getRowCount(); i++) {
                    for (int j = 0; j < transactionLogsTableModel.getColumnCount(); j++) {
                        csv.write(transactionLogsTableModel.getValueAt(i, j).toString() + ",");
                    }
                    csv.write("\n");
                }
                csv.close();
                JOptionPane.showMessageDialog(this, "Exported to transaction_logs.csv");
            } catch (java.io.IOException ioException) {
                JOptionPane.showMessageDialog(this, "Error exporting file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return panel;
    }

    private void refreshTransactionLogsTable() {
        transactionLogsTableModel.setRowCount(0);
        try {
            List<Transaction> all = db.listAllTransactions();
            for (Transaction t : all) {
                Object[] row = {t.tId, t.datetime, t.fromAcc, t.toAcc, String.format("%.2f", t.amount), t.description};
                transactionLogsTableModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }

    private JPanel createAdminSecurityPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel createAdminPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        createAdminPanel.setBorder(BorderFactory.createTitledBorder("Create New Admin"));

        JTextField newAdminUser = new JTextField();
        JPasswordField newAdminPass = new JPasswordField();
        JComboBox<String> privilegeLevel = new JComboBox<>(new String[]{"Normal", "Auditor"});
        JButton createAdminBtn = new JButton("Create Admin");

        createAdminPanel.add(new JLabel("New Admin Username:")); createAdminPanel.add(newAdminUser);
        createAdminPanel.add(new JLabel("Password:")); createAdminPanel.add(newAdminPass);
        createAdminPanel.add(new JLabel("Privilege Level:")); createAdminPanel.add(privilegeLevel);
        createAdminPanel.add(createAdminBtn); createAdminPanel.add(new JLabel(""));

        createAdminBtn.addActionListener(e -> {
            String user = newAdminUser.getText();
            String pass = new String(newAdminPass.getPassword());
            if(!user.isEmpty() && !pass.isEmpty()) {
                admins.put(user, pass);
                JOptionPane.showMessageDialog(this, "Admin '" + user + "' created.");
                newAdminUser.setText("");
                newAdminPass.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel changePassPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        changePassPanel.setBorder(BorderFactory.createTitledBorder("Change Own Password"));

        JPasswordField oldPass = new JPasswordField();
        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();
        JButton changePassBtn = new JButton("Change Password");

        changePassPanel.add(new JLabel("Old Password:")); changePassPanel.add(oldPass);
        changePassPanel.add(new JLabel("New Password:")); changePassPanel.add(newPass);
        changePassPanel.add(new JLabel("Confirm Password:")); changePassPanel.add(confirmPass);
        changePassPanel.add(changePassBtn); changePassPanel.add(new JLabel(""));

        changePassBtn.addActionListener(e -> {
            String oldP = new String(oldPass.getPassword());
            String newP = new String(newPass.getPassword());
            String confirmP = new String(confirmPass.getPassword());

        if (loggedInAdmin != null && admins.get(loggedInAdmin).equals(oldP)) {
                if (newP.equals(confirmP)) {
                    admins.put(loggedInAdmin, newP);
                    JOptionPane.showMessageDialog(this, "Password changed successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Old password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(createAdminPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(changePassPanel);

        return panel;
    }

    private JPanel createCustomerLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(150, 200, 150, 200));
        JTextField accNo = new JTextField();
        JPasswordField pin = new JPasswordField();
        JButton login = new JButton("Login"), back = new JButton("Back");

        styleButton(login, 120, 35, 14);
        styleButton(back, 120, 35, 14);

        panel.add(new JLabel("Account Number:"));
        panel.add(accNo);
        panel.add(new JLabel("PIN:"));
        panel.add(pin);
        panel.add(login);
        panel.add(back);

        login.addActionListener(e -> {
            String id = accNo.getText();
            String p = new String(pin.getPassword());
            try {
                if (db.authenticateCustomer(id, p)) {
                    Account ar = db.getAccountByAccNo(id);
                    loggedInAccount = ar;
                    JOptionPane.showMessageDialog(this, "Welcome " + loggedInAccount.getFullName() + "!");
                    cardLayout.show(mainPanel, "CUSTOMER_DASH");
                } else JOptionPane.showMessageDialog(this, "Invalid Account or PIN!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
            }
        });
        back.addActionListener(e -> cardLayout.show(mainPanel, "MAIN"));
        return panel;
    }

    private JPanel createCustomerDashboardPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(80, 250, 80, 250));
        JButton summary = new JButton("Account Summary");
        JButton history = new JButton("Transaction History");
        JButton transfer = new JButton("Fund Transfer");
        JButton transaction = new JButton("Deposit / Withdraw");
        JButton changePin = new JButton("Change PIN");
        JButton logout = new JButton("Logout");

        styleButton(summary, 200, 45, 16);
        styleButton(history, 200, 45, 16);
        styleButton(transfer, 200, 45, 16);
        styleButton(transaction, 200, 45, 16);
        styleButton(changePin, 200, 45, 16);
        styleButton(logout, 200, 45, 16);

        summary.addActionListener(e -> {
            updateAccountSummary();
            cardLayout.show(mainPanel, "SUMMARY");
        });
        history.addActionListener(e -> {
            updateTransactionHistory();
            cardLayout.show(mainPanel, "HISTORY");
        });
        transfer.addActionListener(e -> {
            updateFundTransferPanel();
            cardLayout.show(mainPanel, "TRANSFER");
        });
        transaction.addActionListener(e -> cardLayout.show(mainPanel, "TRANSACTION"));
        changePin.addActionListener(e -> cardLayout.show(mainPanel, "PIN"));
        logout.addActionListener(e -> {
            loggedInAccount = null;
            cardLayout.show(mainPanel, "MAIN");
        });

        panel.add(summary); panel.add(history); panel.add(transfer);
        panel.add(transaction); panel.add(changePin); panel.add(logout);
        return panel;
    }

    private void updateAccountSummary() {
        if (loggedInAccount != null) {
            summaryInfoArea.setText(" Name: \t\t" + loggedInAccount.getFullName() +
                    "\n\n Account No: \t" + loggedInAccount.accNo +
                    "\n\n Account Type: \t" + loggedInAccount.accType +
                    "\n\n Email: \t\t" + loggedInAccount.email +
                    "\n\n Mobile: \t\t" + loggedInAccount.mobile +
                    "\n\n ID Proof: \t" + loggedInAccount.idProof +
                    "\n\n Address: \t" + loggedInAccount.address +
                    "\n\n----------------------------------------------------------" +
                    "\n\n Current Balance: \t₹ " + String.format("%.2f", loggedInAccount.balance));
        }
    }

    private JPanel createAccountSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        summaryInfoArea = new JTextArea();
        summaryInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        summaryInfoArea.setEditable(false);
        summaryInfoArea.setOpaque(false);
        summaryInfoArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton back = new JButton("Back");
        styleButton(back, 100, 35, 14);
        back.addActionListener(e -> cardLayout.show(mainPanel, "CUSTOMER_DASH"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(back);

        panel.add(new JScrollPane(summaryInfoArea), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void updateTransactionHistory() {
        historyTableModel.setRowCount(0);
        if (loggedInAccount != null) {
            try {
                List<Transaction> txs = db.listTransactionsForAccount(loggedInAccount.accNo);
                for (Transaction t : txs) {
                    String type = loggedInAccount.accNo.equals(t.fromAcc) ? "Debit" : "Credit";
                    historyTableModel.addRow(new String[]{t.datetime, type, "₹" + String.format("%.2f", t.amount), ""});
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
            }
        }
    }

    private JPanel createTransactionHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        String[] cols = {"Date/Time", "Type", "Amount", "Balance"};
        historyTableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(historyTableModel);
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        JButton back = new JButton("Back");
        styleButton(back, 100, 35, 14);
        back.addActionListener(e -> cardLayout.show(mainPanel, "CUSTOMER_DASH"));

        JPanel buttons = new JPanel();
        buttons.add(back);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void updateFundTransferPanel() {
        if (loggedInAccount != null) {
            fromAccountLabel.setText(loggedInAccount.accNo);
        }
    }

    private JPanel createFundTransferPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(100, 150, 100, 150));
        JTextField toAcc = new JTextField(), amt = new JTextField();
        fromAccountLabel = new JLabel("");
        fromAccountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JButton transfer = new JButton("Transfer"), back = new JButton("Back");

        styleButton(transfer, 120, 35, 14);
        styleButton(back, 120, 35, 14);

        panel.add(new JLabel("From Account:")); panel.add(fromAccountLabel);
        panel.add(new JLabel("To Account Number:")); panel.add(toAcc);
        panel.add(new JLabel("Amount (₹):")); panel.add(amt);
        panel.add(transfer); panel.add(back);

        transfer.addActionListener(e -> {
            if (loggedInAccount == null) return;
            String to = toAcc.getText();
            try {
                double amount = Double.parseDouble(amt.getText());
                boolean ok = db.transferFunds(loggedInAccount.accNo, to, amount);
                if (ok) {
                    loggedInAccount = db.getAccountByAccNo(loggedInAccount.accNo);
                    JOptionPane.showMessageDialog(this, "₹" + amount + " transferred successfully!");
                    toAcc.setText("");
                    amt.setText("");
                } else JOptionPane.showMessageDialog(this, "Transfer failed (insufficient balance or beneficiary missing).");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Amount!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
            }
        });
        back.addActionListener(e -> cardLayout.show(mainPanel, "CUSTOMER_DASH"));
        return panel;
    }

    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(150, 200, 150, 200));
        JTextField amt = new JTextField();
        JButton deposit = new JButton("Deposit"), withdraw = new JButton("Withdraw"), back = new JButton("Back");

        styleButton(deposit, 120, 35, 14);
        styleButton(withdraw, 120, 35, 14);
        styleButton(back, 120, 35, 14);

        panel.add(new JLabel("Amount (₹):")); panel.add(amt);
        panel.add(deposit); panel.add(withdraw);
        panel.add(back); panel.add(new JLabel(""));

        deposit.addActionListener(e -> {
            try {
                double a = Double.parseDouble(amt.getText());
                Account ar = db.getAccountByAccNo(loggedInAccount.accNo);
                ar.balance += a;
                db.updateAccountBalance(loggedInAccount.accNo, ar.balance);
                db.recordTransaction(new Transaction(db.generateNextTxId(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), "SELF", loggedInAccount.accNo, a, "Deposit"));
                loggedInAccount = ar;
                JOptionPane.showMessageDialog(this, "Deposited ₹" + a);
                amt.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
            }
        });

        withdraw.addActionListener(e -> {
            try {
                double a = Double.parseDouble(amt.getText());
                Account ar = db.getAccountByAccNo(loggedInAccount.accNo);
                if (ar.balance >= a) {
                    ar.balance -= a;
                    db.updateAccountBalance(loggedInAccount.accNo, ar.balance);
                    db.recordTransaction(new Transaction(db.generateNextTxId(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), loggedInAccount.accNo, "SELF", a, "Withdrawal"));
                    loggedInAccount = ar;
                    JOptionPane.showMessageDialog(this, "Withdrawn ₹" + a);
                    amt.setText("");
                } else JOptionPane.showMessageDialog(this, "Insufficient balance!");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
            }
        });
        back.addActionListener(e -> cardLayout.show(mainPanel, "CUSTOMER_DASH"));
        return panel;
    }

    private JPanel createChangePinPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(150, 200, 150, 200));
        JPasswordField oldPin = new JPasswordField(), newPin = new JPasswordField();
        JButton change = new JButton("Change PIN"), back = new JButton("Back");

        styleButton(change, 120, 35, 14);
        styleButton(back, 120, 35, 14);

        panel.add(new JLabel("Old PIN:")); panel.add(oldPin);
        panel.add(new JLabel("New PIN:")); panel.add(newPin);
        panel.add(change); panel.add(back);

        change.addActionListener(e -> {
            if (loggedInAccount == null) return;
            try {
                if (loggedInAccount.pin.equals(new String(oldPin.getPassword()))) {
                    db.changePin(loggedInAccount.accNo, new String(newPin.getPassword()));
                    loggedInAccount.pin = new String(newPin.getPassword());
                    JOptionPane.showMessageDialog(this, "PIN changed successfully!");
                    oldPin.setText("");
                    newPin.setText("");
                } else JOptionPane.showMessageDialog(this, "Old PIN incorrect!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
            }
        });
        back.addActionListener(e -> cardLayout.show(mainPanel, "CUSTOMER_DASH"));
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankManagementSystemSQLite().setVisible(true));
    }
}