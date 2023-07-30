package za.co.tyaphile.database;


import za.co.tyaphile.BankServer;
import za.co.tyaphile.account.Account;
import za.co.tyaphile.card.Card;
import za.co.tyaphile.database.Connector.Connect;
import za.co.tyaphile.info.Info;
import za.co.tyaphile.user.User;

import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseManager {
    private static Connection connection;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void createTables() {
        String[] tables = {accountsTable, cardsTable, transactsTable, notesTable};

        for(String sql : tables) {
            PreparedStatement ps = null;
            try {
                setConnection();
                ps = connection.prepareStatement(sql);
                ps.executeUpdate();
            } catch (SQLException e) {
                printStackTrace("Table create error", e);
            } finally {
                try {
                    connection.close();
                    if (ps != null) ps.close();
                } catch (SQLException e) {
                    printStackTrace("Closing connections error", e);
                }
            }
        }
    }

    public static boolean openAccount(User user, String admin) {
        String sql;

        if(!user.getAccount().isOnHold()) {
            sql = "INSERT INTO accounts (account_no, customer_name, customer_surname, account_type) VALUES (?, ?, ?, ?);";
        } else {
            sql = "INSERT INTO accounts (account_no, customer_name, customer_surname, account_type, account_hold) VALUES (?, ?, ?, ?, ?);";
            user.getAccount().setOnHold(user.getAccount().isOnHold(), user.getAccount().getNotes().get(0));
        }
        PreparedStatement ps = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(user.getAccount().getAccountNumber()));
            ps.setString(2, user.getAccount().getName());
            ps.setString(3, user.getAccount().getSurname());
            ps.setString(4, user.getAccount().getAccountType());

            if(user.getAccount().isOnHold()) {
                ps.setBoolean(5, user.getAccount().isOnHold());
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            printStackTrace("SQL Error", e);
            return false;
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }
        return true;
    }

    public static List<Map<String, Object>> getAccounts() {
        List<Map<String, Object>> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts INNER JOIN cards ON account_no=card_linked_account GROUP BY account_no;";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> acc = new HashMap<>();
                acc.put("name", rs.getString("customer_name"));
                acc.put("surname", rs.getString("customer_surname"));
                acc.put("account_number", rs.getString("account_no"));
                acc.put("type", rs.getString("account_type"));
                acc.put("balance", rs.getDouble("balance"));
                accounts.add(acc);
            }
        } catch (SQLException e) {
            printStackTrace("Account fetch error", e);
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
                if (rs != null) rs.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }

        return accounts;
    }

    public static List<Map<String, Object>> getAccounts(String search) {
        List<Map<String, Object>> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts " +
                "INNER JOIN cards ON account_no=card_linked_account " +
                "WHERE (account_no=? OR customer_name=? OR customer_surname=? OR card_no=?) " +
                "GROUP BY account_no;";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);

            String val = search.replaceAll("\\D+", "");
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(val);
            if(matcher.matches()) {
                ps.setLong(1, Long.parseLong(val));
            } else {
                ps.setLong(1, -1);
            }

            ps.setString(2, search);
            ps.setString(3, search);
            ps.setString(4, getFormatCardNumber(search));

            rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> acc = new HashMap<>();
                acc.put("name", rs.getString("customer_name"));
                acc.put("surname", rs.getString("customer_surname"));
                acc.put("account_number", rs.getString("account_no"));
                acc.put("type", rs.getString("account_type"));
                acc.put("balance", rs.getDouble("balance"));
                accounts.add(acc);
            }
        } catch (SQLException e) {
            printStackTrace("Account fetch error", e);
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
                if (rs != null) rs.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }

        return accounts;
    }

    public static boolean setBalance(long from, long to, String desc, String type, double amount) {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
        String sqlTrans = "INSERT INTO transact (transact_account, transact_beneficiary, transact_description, " +
                "transact_type, transaction_amount) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement ps = null;
        try {
            setConnection();
            connection.setAutoCommit(false);

            ps = connection.prepareStatement(sqlTrans);
            ps.setLong(1, from);
            ps.setLong(2, to);
            ps.setString(3, desc);
            ps.setString(4, type);
            ps.setDouble(5, amount);
            ps.executeUpdate();

            ps = connection.prepareStatement(sql);
            ps.setDouble(1, amount);
            ps.setLong(2, to);
            ps.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            printStackTrace("Balance check error", e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                printStackTrace("Error", ex);
            }
            return false;
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }
        return true;
    }

    public static synchronized boolean makeTransaction(long from, long to, String desc, String type, double amount) {
        String sql = "INSERT INTO transact (transact_account, transact_beneficiary, transact_description," +
                " transact_type, transaction_amount) VALUES (?, ?, ?, ?, ?);";
        String sqlUpdatePayer = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
        String sqlUpdateRecipient = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";

        PreparedStatement ps = null;
        try {
            setConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);

            ps.setLong(1, from);
            ps.setLong(2, to);
            ps.setString(3, desc);
            ps.setString(4, type);
            ps.setDouble(5, amount);
            ps.executeUpdate();

            ps = connection.prepareStatement(sqlUpdatePayer);
            ps.setDouble(1, -amount);
            ps.setLong(2, from);
            ps.executeUpdate();

            ps = connection.prepareStatement(sqlUpdateRecipient);
            ps.setDouble(1, amount);
            ps.setLong(2, to);
            ps.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            printStackTrace("Deposit error", e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                printStackTrace("Rollback error", ex);
            }
            return false;
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }
        return true;
    }

    public static boolean issueCard(User user, String admin) {
        if(user.getAccount().isOnHold() || (!user.getAllCards().isEmpty() && !user.getLastCardIssued().isSTOPPED())) {
            return false;
        }

        PreparedStatement ps = null;
        String sql = "INSERT INTO cards (card_no, card_pin, card_cvv, card_linked_account) VALUES (?, ?, ?, ?)";
        if (addNote(user.getAccount().getAccountNumber(), admin, "Issued new card")) {
            try {
                setConnection();
                user.issueCard();
                ps = connection.prepareStatement(sql);
                ps.setString(1, user.getLastCardIssued().formatCardNumber(user.getLastCardIssued().getCardNumber()));
                ps.setString(2, user.getLastCardIssued().getCardPin());
                ps.setString(3, user.getLastCardIssued().getCVV());
                ps.setLong(4, Long.parseLong(user.getAccount().getAccountNumber()));
                ps.executeUpdate();
            } catch (SQLException e) {
                printStackTrace("Card issue error", e);
                return false;
            } finally {
                try {
                    connection.close();
                    if (ps != null) ps.close();
                } catch (SQLException e) {
                    printStackTrace("Closing connections error", e);
                }
            }
        }
        return true;
    }

    public static List<Map<String, Object>> getLinkedCards(String param) {
        String sql = "SELECT * FROM accounts INNER JOIN cards ON card_linked_account=account_no  WHERE account_no=?;";
        List<Map<String, Object>> cards = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, param);
            rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> card = new HashMap<>();
                String cardNumber = rs.getString("card_no");
                card.put("card_no", cardNumber);
                card.put("card_pin", rs.getString("card_pin"));
                card.put("card_cvv", rs.getString("card_cvv"));

                card.put("card_fraud", rs.getBoolean("card_fraud"));
                card.put("card_hold", rs.getBoolean("card_hold"));
                card.put("remarks", getNotes(cardNumber));
                cards.add(card);
            }
        } catch (SQLException e) {
            printStackTrace("Getting cards error", e);
        }  finally {
            try {
                connection.close();
                if (ps != null) ps.close();
                if (rs != null) rs.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }
        return cards;
    }

    public static boolean addNote(String link_to, String admin, String notes) {
        String sql = "INSERT INTO notes (notes_link_to, added_by, notes) VALUES (?, ?, ?);";
        PreparedStatement ps = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, link_to);
            ps.setString(2, admin);
            ps.setString(3, notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            printStackTrace("Note error", e);
            return false;
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }
        return true;
    }

    public static List<String> getNotes(String param) throws SQLException {
        String sql = "SELECT * FROM notes WHERE notes_link_to=?;";
        List<String> notes = new ArrayList<>();
        setConnection();
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, param);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            notes.add("{ " + rs.getString("added_by") + " } " + rs.getString("notes"));
        }
        return notes;
    }

    public static List<Map<String, Object>> getTransactions(long accFrom, long accTo, Timestamp fromDate) {
        String sql = getSql(accFrom, accTo);

        List<Map<String, Object>> transactions = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            setConnection();
            ps = connection.prepareStatement(sql);

            if (accTo == 0 && accFrom == 0) {
                ps.setTimestamp(1, fromDate);
            } else {
                if (accFrom > 0 && accTo == 0) {
                    ps.setLong(1, accFrom);
                    ps.setLong(2, accFrom);
                    ps.setTimestamp(3, fromDate);
                } else if (accTo > 0 && accFrom == 0) {
                    ps.setLong(1, accTo);
                    ps.setLong(2, accTo);
                    ps.setTimestamp(3, fromDate);
                } else {
                    ps.setLong(1, accFrom);
                    ps.setLong(2, accTo);
                    ps.setTimestamp(3, fromDate);
                }
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> trans = new HashMap<>();

                trans.put("transaction_id", rs.getLong("transact_id"));
                trans.put("account_from", rs.getLong("transact_account"));
                trans.put("account_to", rs.getLong("transact_beneficiary"));
                trans.put("description", rs.getString("transact_description"));
                trans.put("transaction_time", rs.getTimestamp("transact_date"));
                trans.put("transaction_type", rs.getString("transact_type"));
                trans.put("amount", rs.getDouble("transaction_amount"));

                if ((accFrom > 0 && accTo == 0) || (accTo > 0 && accFrom == 0)) {
                    trans.put("balance", rs.getLong("Balance"));
                }

                transactions.add(trans);
            }
        } catch (SQLException e) {
            printStackTrace("Get transaction error", e);
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
                if (rs != null) rs.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }

        return transactions;
    }

    private static String getSql(long accFrom, long accTo) {
        String sql;

        if (accFrom == 0 && accTo == 0) {
            sql = "SELECT * FROM transact WHERE transact_date > ? ORDER BY transact_id DESC;";
        } else{
            if (accFrom > 0 && accTo == 0) {
                sql = "SELECT *, sum( CASE " +
                        "WHEN transact_account=" + accFrom + " THEN 0 - transaction_amount " +
                        "WHEN transact_beneficiary=" + accFrom + " THEN 0 + transaction_amount " +
                        "ELSE 0 END) " +
                        "OVER (ORDER BY transact_id RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance FROM transact " +
                        "WHERE (transact_account=? OR transact_beneficiary=?) AND transact_date >= ? ORDER BY transact_id DESC;";
            } else if (accTo > 0 && accFrom == 0) {
                sql = "SELECT *, sum( CASE " +
                        "WHEN transact_account=" + accTo + " THEN 0 - transaction_amount " +
                        "WHEN transact_beneficiary=" + accTo + " THEN 0 + transaction_amount " +
                        "ELSE 0 END) " +
                        "OVER (ORDER BY transact_id RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance FROM transact " +
                        "WHERE (transact_account=? OR transact_beneficiary=?) AND transact_date >= ? ORDER BY transact_id DESC;";
            } else {
                sql = "SELECT * FROM transact WHERE (transact_account=? OR transact_beneficiary=?) AND transact_date >= ? ORDER BY transact_id DESC;";
            }
        }
        return sql;
    }

    public static boolean accountHold(Account account, String admin) {
        String sql = "UPDATE accounts SET account_close=?, account_hold=?  WHERE account_no=?";
        PreparedStatement ps = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);
            ps.setBoolean(1, account.isClosed());
            ps.setBoolean(2, account.isOnHold());
            ps.setString(3, account.getAccountNumber());
            if (addNote(account.getAccountNumber(), admin, account.getCloseReason())) {
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            printStackTrace("Account hold error", e);
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }

        return false;
    }

    public static boolean cardControl(Card card, String admin) {
        PreparedStatement ps = null;
        if (addNote(card.getCardNumber(), admin, card.getStopReason())) {
            try {
                String sql = "UPDATE cards SET card_hold = ?, card_fraud = ? WHERE card_no = ?;";
                setConnection();
                ps = connection.prepareStatement(sql);
                ps.setBoolean(1, card.isSTOPPED());
                ps.setBoolean(2, card.isFRAUD());
                ps.setString(3, card.formatCardNumber(card.getCardNumber()));

                ps.executeUpdate();
            } catch (SQLException e) {
                printStackTrace("Card update error", e);
                return false;
            } finally {
                try {
                    connection.close();
                    if (ps != null) ps.close();
                } catch (SQLException e) {
                    printStackTrace("Closing connections error", e);
                }
            }
        }
        return true;
    }

    private static void setConnection() throws SQLException{
        if(connection == null || connection.isClosed()) {
            if (BankServer.isMySQL()) {
                connection = Connect.getConnection(Info.getDatabaseName(true), Info.getROOT(), Info.getPASSWORD());
            } else {
                connection = Connect.getConnection(Info.getDatabaseName(false));
            }
        }
    }

    public static void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            printStackTrace("Failed to close connection", e);
        }
    }

    private static void printStackTrace(final String description, Exception exception) {
        System.err.println(description + " : " + exception);
        for (StackTraceElement ste : exception.getStackTrace()) {
            System.err.println(description + " : " + ste);
        }
    }

    private static String getFormatCardNumber(String number) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("####,####",symbols);
        number = number.replaceAll("\\D+", "");
        double num = 0;
        if(!number.trim().isEmpty()) { num = Double.parseDouble(number); }
        return df.format(num);
    }

    public DatabaseManager() {
        if(BankServer.isMySQL()) {
            System.out.println("MySQL");
            accountsTable = "CREATE TABLE IF NOT EXISTS accounts (\n" +
                    "\taccount_id BIGINT AUTO_INCREMENT PRIMARY KEY, \n" +
                    "\taccount_no BIGINT UNIQUE NOT NULL, \n" +
                    "\tcustomer_name VARCHAR(255) NOT NULL, \n" +
                    "\tcustomer_surname VARCHAR(255) NOT NULL, \n" +
                    "    account_type VARCHAR(45) NOT NULL,\n" +
                    "\taccount_hold TINYINT(1) DEFAULT 0, \n" +
                    "\taccount_close TINYINT(1) DEFAULT 0, \n" +
                    "\tbalance DECIMAL(13, 2) DEFAULT 0.00, \n" +
                    "    overdraft_balance DECIMAL(13, 2) DEFAULT 0.00, \n" +
                    "    overdraft_limit DECIMAL(13, 2) DEFAULT 0.00, \n" +
                    "\taccount_open_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            cardsTable = "CREATE TABLE IF NOT EXISTS cards (\n" +
                    "\tcard_id BIGINT AUTO_INCREMENT PRIMARY KEY, \n" +
                    "    card_no VARCHAR(45) UNIQUE NOT NULL, " +
                    "    card_linked_account BIGINT NOT NULL, \n" +
                    "    card_pin VARCHAR(4) NOT NULL, \n" +
                    "    card_cvv VARCHAR(3) NOT Null, \n" +
                    "    card_hold TINYINT(1) DEFAULT 0, \n" +
                    "    card_fraud TINYINT(1) DEFAULT 0, " +
                    "    card_issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                    ");";

            transactsTable = "CREATE TABLE IF NOT EXISTS transact (\n" +
                    "\ttransact_id BIGINT AUTO_INCREMENT PRIMARY KEY, \n" +
                    "    transact_account BIGINT NOT NULL, \n" +
                    "    transact_beneficiary BIGINT NOT NULL,\n" +
                    "    transact_description VARCHAR(100) NOT NULL,\n" +
                    "    transact_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    transact_type VARCHAR(20) NOT NULL,\n" +
                    "    transaction_amount DOUBLE NOT NULL" +
                    ");";

            notesTable = "CREATE TABLE IF NOT EXISTS notes (\n" +
                    "\tnotes_id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                    "    notes_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, \n" +
                    "    notes_link_to VARCHAR(70) NOT NULL, \n" +
                    "    added_by VARCHAR(70) NOT NULL, \n" +
                    "    notes BLOB NOT NULL\n" +
                    ");";
        } else {
            System.out.println("SQLite");
            accountsTable = "CREATE TABLE IF NOT EXISTS accounts (\n" +
                    "\taccount_id INTEGER PRIMARY KEY, \n" +
                    "\taccount_no BIGINT UNIQUE NOT NULL, \n" +
                    "\tcustomer_name VARCHAR(255) NOT NULL, \n" +
                    "\tcustomer_surname VARCHAR(255) NOT NULL, \n" +
                    "    account_type VARCHAR(45) NOT NULL,\n" +
                    "\taccount_hold TINYINT(1) DEFAULT 0, \n" +
                    "\taccount_close TINYINT(1) DEFAULT 0, \n" +
                    "\tbalance DECIMAL(13, 2) DEFAULT 0.00, \n" +
                    "    overdraft_balance DECIMAL(13, 2) DEFAULT 0.00, \n" +
                    "    overdraft_limit DECIMAL(13, 2) DEFAULT 0.00, \n" +
                    "\taccount_open_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            cardsTable = "CREATE TABLE IF NOT EXISTS cards (\n" +
                    "\tcard_id INTEGER PRIMARY KEY, \n" +
                    "    card_no VARCHAR(45) UNIQUE NOT NULL, " +
                    "    card_linked_account BIGINT NOT NULL, \n" +
                    "    card_pin VARCHAR(4) NOT NULL, \n" +
                    "    card_cvv VARCHAR(3) NOT Null, \n" +
                    "    card_hold TINYINT(1) DEFAULT 0, \n" +
                    "    card_fraud TINYINT(1) DEFAULT 0, " +
                    "    card_issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                    ");";

            transactsTable = "CREATE TABLE IF NOT EXISTS transact (\n" +
                    "\ttransact_id INTEGER PRIMARY KEY, \n" +
                    "    transact_account BIGINT NOT NULL, \n" +
                    "    transact_beneficiary BIGINT NOT NULL,\n" +
                    "    transact_description VARCHAR(100) NOT NULL,\n" +
                    "    transact_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    transact_type VARCHAR(20) NOT NULL,\n" +
                    "    transaction_amount DOUBLE NOT NULL" +
                    ");";

            notesTable = "CREATE TABLE IF NOT EXISTS notes (\n" +
                    "\tnotes_id INTEGER PRIMARY KEY,\n" +
                    "    notes_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, \n" +
                    "    notes_link_to VARCHAR(70) NOT NULL, \n" +
                    "    added_by VARCHAR(70) NOT NULL, \n" +
                    "    notes BLOB NOT NULL\n" +
                    ");";
        }
    }

    private static String accountsTable;
    private static String cardsTable;
    private static String transactsTable;
    private static String notesTable;
}