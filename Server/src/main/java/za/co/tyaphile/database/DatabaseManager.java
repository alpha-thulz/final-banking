package za.co.tyaphile.database;


import za.co.tyaphile.BankServer;
import za.co.tyaphile.card.Card;
import za.co.tyaphile.database.Connector.Connect;
import za.co.tyaphile.info.Info;

import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseManager {

    private static String accountsTable;
    private static String cardsTable;
    private static String transactsTable;
    private static String notesTable;
    private static Connection connection;

    public static boolean accountHold(Map<String, Object> request) {
        String sql = "UPDATE accounts SET account_close=?, account_hold=?  WHERE account_no=?";
        PreparedStatement ps = null;
        boolean isHold = (Boolean) request.get("hold");
        boolean isClose = (Boolean) request.get("close");
        String account = request.get("account").toString(), admin = request.get("admin").toString();
        String note = request.get("note").toString();

        try {
            if (addNote(account, admin, note)) {
                setConnection();
                ps = connection.prepareStatement(sql);
                ps.setBoolean(1, isClose);
                ps.setBoolean(2, isHold);
                ps.setString(3, account);
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

    public static boolean cardControl(String card, String admin, String note, boolean isHold, boolean isStop) {
        PreparedStatement ps = null;

        if (addNote(card, admin, note)) {
            try {
                String sql = "UPDATE cards SET card_hold = ?, card_fraud = ? WHERE card_no = ?;";
                setConnection();
                ps = connection.prepareStatement(sql);
                ps.setBoolean(1, isHold);
                ps.setBoolean(2, isStop);
                ps.setString(3, card);

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

    private static String getFormatCardNumber(String number) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("####,####",symbols);
        number = number.replaceAll("\\D+", "");
        double num = 0;
        if(!number.trim().isEmpty()) { num = Double.parseDouble(number); }
        return df.format(num);
    }

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

    public static boolean updateLimit(String account, double amount) {
        String sql;
        PreparedStatement ps = null;
        boolean update = false;
        try {
            double current = amount - DatabaseManager.getBalance(account).get("overdraft_limit");
            setConnection();
            sql = "UPDATE accounts SET overdraft_balance = overdraft_balance + ?, overdraft_limit = ? WHERE account_no = ?";
            ps = connection.prepareStatement(sql);
            ps.setDouble(1, current);
            ps.setDouble(2, amount);
            ps.setString(3, account);
            update =  ps.executeUpdate() > 0;
        } catch (SQLException e) {
            printStackTrace("Update limit failure", e);
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                printStackTrace("Unable to close connection", e);
            }
        }
        return update;
    }

    public static Map<String, Double> getBalance(String account) throws SQLException {
        Map<String, Double> balance = new HashMap<>();
        String sql = "SELECT balance, overdraft_balance, overdraft_limit FROM accounts WHERE account_no = ?";

        setConnection();

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, account);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            balance.put("balance", rs.getDouble("balance"));
            balance.put("overdraft", rs.getDouble("overdraft_balance"));
            balance.put("overdraft_limit", rs.getDouble("overdraft_limit"));
        }

        return balance;
    }

    private static boolean setBalance(long account, double amount) throws SQLException {
        String sql = "UPDATE accounts SET balance = balance + ?, " +
                "overdraft_balance = overdraft_balance + ? WHERE account_no = ?";
        PreparedStatement ps = connection.prepareStatement(sql);

        Map<String, Double> balances = getBalance(String.valueOf(account));
        double balance = balances.get("balance");
        double overdraft = balances.get("overdraft");
        double overdraft_limit = balances.get("overdraft_limit");

        double difference = 0;
        if (amount > 0) {
            if (overdraft < overdraft_limit) {
                difference = overdraft_limit - overdraft;
            }
        } else {
            if (Math.abs(amount) > (balance + overdraft)) return false;

            if ((balance - Math.abs(amount)) < 0) {
                difference = balance - Math.abs(amount);
            }
        }
        ps.setDouble(1, amount - difference);
        ps.setDouble(2, difference);
        ps.setLong(3, account);
        ps.executeUpdate();
        return true;
    }

    public static synchronized boolean makeTransaction(long from, long to, String desc, String type, double amount, boolean fromAccount) {
        boolean successful = false;

        String sql = "INSERT INTO transact (transact_account, transact_beneficiary, transact_description," +
                " transact_type, transaction_amount) VALUES (?, ?, ?, ?, ?);";

        PreparedStatement ps = null;

        try {

            String card = null;

            try {
                card = getCurrentCard(String.valueOf(from)).get(String.valueOf(from)).get("card").toString();
            } catch (NullPointerException ignored) {}


            if (fromAccount &&
                    (Boolean) getAccounts(String.valueOf(from), null, null, card, false)
                            .get(0)
                            .get("on_hold")) {
                return false;
            }

            setConnection();
            connection.setAutoCommit(false);
            if (fromAccount) {
                if (setBalance(from, -amount) && setBalance(to, amount)) {
                    ps = connection.prepareStatement(sql);

                    ps.setLong(1, from);
                    ps.setLong(2, to);
                    ps.setString(3, desc);
                    ps.setString(4, type);
                    ps.setDouble(5, amount);
                    ps.executeUpdate();
                } else {
                    return false;
                }
            } else {
                if (setBalance(to, amount)) {
                    ps = connection.prepareStatement(sql);

                    ps.setLong(1, from);
                    ps.setLong(2, to);
                    ps.setString(3, desc);
                    ps.setString(4, type);
                    ps.setDouble(5, amount);
                    ps.executeUpdate();
                } else {
                    return false;
                }
            }
            connection.commit();
            successful = true;
        } catch (SQLException e) {
            printStackTrace("Deposit error", e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                printStackTrace("Rollback error", ex);
            }
        } finally {
            try {
                connection.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                printStackTrace("Closing connections error", e);
            }
        }
        return successful;
    }

    public static List<Map<String, Object>> getTransactions(long accFrom, long accTo, Timestamp fromDate) {
        String sql = getTransactionsSql(accFrom, accTo);

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

    public static boolean openAccount(Map<String, Object> request) {
        String sql, account = request.get("account").toString(), admin = request.get("admin").toString();
        boolean isOnHold = request.get("hold") != null && (Boolean) request.get("hold");

        if(!isOnHold) {
            sql = "INSERT INTO accounts (account_no, customer_name, customer_surname, account_type) VALUES (?, ?, ?, ?);";
        } else {
            sql = "INSERT INTO accounts (account_no, customer_name, customer_surname, account_type, account_hold) VALUES (?, ?, ?, ?, ?);";
            addNote(account, admin, request.get("note").toString());
        }
        PreparedStatement ps = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(account));
            ps.setString(2, request.get("name").toString());
            ps.setString(3, request.get("surname").toString());
            ps.setString(4, request.get("type").toString());

            if(isOnHold) {
                ps.setBoolean(5, true);
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
        addNote(account, admin, "New account opened");
        issueCard(request);
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
                acc.put("account_number", formatAccount(rs.getString("account_no")));
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

    public static List<Map<String, Object>> getAccounts(String account, String name, String surname, String card, boolean match) {
        List<Map<String, Object>> accounts = new ArrayList<>();

        String sql;
        if (match) {
            sql = "SELECT * FROM accounts " +
                    "INNER JOIN cards ON account_no=card_linked_account " +
                    "WHERE account_no=? OR (customer_name=? AND customer_surname=?) OR card_no=?" +
                    "GROUP BY account_no ORDER BY card_issue_date DESC;";
        } else {
            sql = "SELECT * FROM accounts " +
                    "INNER JOIN cards ON account_no=card_linked_account " +
                    "WHERE (account_no=? OR customer_name=? OR customer_surname=? OR card_no=?) " +
                    "GROUP BY account_no ORDER BY card_issue_date DESC;";
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);

            String val = account.replaceAll("\\D+", "");
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(val);
            if(matcher.matches()) {
                ps.setLong(1, Long.parseLong(val));
            } else {
                ps.setLong(1, -1);
            }

            ps.setString(2, name);
            ps.setString(3, surname);
            ps.setString(4, getFormatCardNumber(card));

            rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> acc = new HashMap<>();
                acc.put("account_number", formatAccount(rs.getString("account_no")));
                acc.put("name", rs.getString("customer_name"));
                acc.put("surname", rs.getString("customer_surname"));
                acc.put("type", rs.getString("account_type"));
                acc.put("on_hold", rs.getBoolean("account_hold"));
                acc.put("on_close", rs.getBoolean("account_close"));
                acc.put("balance", rs.getDouble("balance"));
                acc.put("overdraft_balance", rs.getDouble("overdraft_balance"));
                acc.put("overdraft_limit", rs.getDouble("overdraft_limit"));
                acc.put("open_date", rs.getTimestamp("account_open_date"));
                acc.put("card", rs.getString("card_no"));
                acc.put("card_hold", rs.getBoolean("card_hold"));
                acc.put("card_fraud", rs.getBoolean("card_fraud"));
                acc.put("issue_date", rs.getTimestamp("card_issue_date"));
                acc.put("notes", getNotes(rs.getString("account_no")));
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

    public static boolean issueCard(Map<String, Object> request) {
        String account = request.get("account").toString(), admin = request.get("admin").toString();
        boolean isOnHold = request.get("hold") != null && (Boolean) request.get("hold");

        boolean isCardHold = getCurrentCard(account).values().stream()
                .anyMatch(x -> ((Boolean) x.get("card_hold") || (Boolean) x.get("card_fraud")));

        if(isOnHold || (!isCardHold && !getCurrentCard(account).isEmpty())) {
            return false;
        }
        PreparedStatement ps = null;
        String sql = "INSERT INTO cards (card_no, card_pin, card_cvv, card_linked_account) VALUES (?, ?, ?, ?)";
        if (addNote(account, admin, "Issued new card")) {
            Card card;
            try {
                setConnection();
                card = new Card();
                ps = connection.prepareStatement(sql);
                ps.setString(1, Card.formatCardNumber(card.getCardNumber()));
                ps.setString(2, card.getCardPin());
                ps.setString(3, card.getCVV());
                ps.setLong(4, Long.parseLong(account));
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
            addNote(Card.formatCardNumber(card.getCardNumber()), admin, account + " linked with card " + Card.formatCardNumber(card.getCardNumber()));
        }
        return true;
    }

    public static Map<String, Map<String, Object>> getCurrentCard(String param) {
        String sql = "SELECT * FROM accounts INNER JOIN cards ON card_linked_account=account_no  WHERE account_no=? " +
                "ORDER BY card_id DESC LIMIT 1;";
        Map<String, Map<String, Object>> card = new HashMap<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            setConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, param);
            rs = ps.executeQuery();
            while (rs.next()) {
                String cardNumber = rs.getString("card_no");
                Map<String, Object> cardDetails = new HashMap<>();
                cardDetails.put("card", cardNumber);
                cardDetails.put("card_pin", rs.getString("card_pin"));
                cardDetails.put("card_cvv", rs.getString("card_cvv"));

                cardDetails.put("card_fraud", rs.getBoolean("card_fraud"));
                cardDetails.put("card_hold", rs.getBoolean("card_hold"));
                cardDetails.put("remarks", getNotes(cardNumber));
                card.put(param, cardDetails);
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
        return card;
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
            notes.add("{" + rs.getString("added_by") + "} " + rs.getString("notes"));
        }
        return notes;
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

    public static void closeConnection() throws SQLException {
        connection.close();
    }

    private static void printStackTrace(final String description, Exception exception) {
        System.err.println(description + " : " + exception);
        for (StackTraceElement ste : exception.getStackTrace()) {
            System.err.println(description + " : " + ste);
        }
    }

    private static String getTransactionsSql(long accFrom, long accTo) {
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

    public static String formatAccount(String account) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator('-');
        DecimalFormat df = new DecimalFormat("00,000,000,000", symbols);
        return df.format(Double.parseDouble(account));
    }

    public static void initTables() {
        if(BankServer.isMySQL()) {
//            System.out.println("MySQL");
            accountsTable = "CREATE TABLE IF NOT EXISTS accounts (" +
                    "account_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "account_no BIGINT UNIQUE NOT NULL, " +
                    "customer_name VARCHAR(255) NOT NULL, " +
                    "customer_surname VARCHAR(255) NOT NULL, " +
                    "account_type VARCHAR(45) NOT NULL, " +
                    "account_hold TINYINT(1) DEFAULT 0, " +
                    "account_close TINYINT(1) DEFAULT 0, " +
                    "balance DECIMAL(13, 2) NOT NULL DEFAULT 0.00, " +
                    "overdraft_balance DECIMAL(13, 2) NOT NULL DEFAULT 0.00, " +
                    "overdraft_limit DECIMAL(13, 2) NOT NULL DEFAULT 0.00, " +
                    "account_open_date TIMESTAMP DEFAULT (datetime('now','localtime'))" +
                    ");";

            cardsTable = "CREATE TABLE IF NOT EXISTS cards (\n" +
                    "\tcard_id BIGINT AUTO_INCREMENT PRIMARY KEY, \n" +
                    "    card_no VARCHAR(45) UNIQUE NOT NULL, " +
                    "    card_linked_account BIGINT NOT NULL, \n" +
                    "    card_pin VARCHAR(4) NOT NULL, \n" +
                    "    card_cvv VARCHAR(3) NOT Null, \n" +
                    "    card_hold TINYINT(1) DEFAULT 0, \n" +
                    "    card_fraud TINYINT(1) DEFAULT 0, " +
                    "    card_issue_date TIMESTAMP DEFAULT (datetime('now','localtime'))\n" +
                    ");";

            transactsTable = "CREATE TABLE IF NOT EXISTS transact (\n" +
                    "\ttransact_id BIGINT AUTO_INCREMENT PRIMARY KEY, \n" +
                    "    transact_account BIGINT NOT NULL, \n" +
                    "    transact_beneficiary BIGINT NOT NULL,\n" +
                    "    transact_description VARCHAR(100) NOT NULL,\n" +
                    "    transact_date TIMESTAMP DEFAULT (datetime('now','localtime')),\n" +
                    "    transact_type VARCHAR(20) NOT NULL,\n" +
                    "    transaction_amount DOUBLE NOT NULL" +
                    ");";

            notesTable = "CREATE TABLE IF NOT EXISTS notes (\n" +
                    "\tnotes_id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                    "    notes_date TIMESTAMP DEFAULT (datetime('now','localtime')), \n" +
                    "    notes_link_to VARCHAR(70) NOT NULL, \n" +
                    "    added_by VARCHAR(70) NOT NULL, \n" +
                    "    notes BLOB NOT NULL\n" +
                    ");";
        } else {
//            System.out.println("SQLite");
            accountsTable = "CREATE TABLE IF NOT EXISTS accounts (" +
                    "account_id INTEGER PRIMARY KEY, " +
                    "account_no BIGINT UNIQUE NOT NULL, " +
                    "customer_name VARCHAR(255) NOT NULL, " +
                    "customer_surname VARCHAR(255) NOT NULL, " +
                    "account_type VARCHAR(45) NOT NULL, " +
                    "account_hold TINYINT(1) DEFAULT 0, " +
                    "account_close TINYINT(1) DEFAULT 0, " +
                    "balance DECIMAL(13, 2) NOT NULL DEFAULT 0.00, " +
                    "overdraft_balance DECIMAL(13, 2) NOT NULL DEFAULT 0.00, " +
                    "overdraft_limit DECIMAL(13, 2) NOT NULL DEFAULT 0.00, " +
                    "account_open_date TIMESTAMP DEFAULT (datetime('now','localtime'))" +
                    ");";

            cardsTable = "CREATE TABLE IF NOT EXISTS cards (\n" +
                    "\tcard_id INTEGER PRIMARY KEY, \n" +
                    "    card_no VARCHAR(45) UNIQUE NOT NULL, " +
                    "    card_linked_account BIGINT NOT NULL, \n" +
                    "    card_pin VARCHAR(4) NOT NULL, \n" +
                    "    card_cvv VARCHAR(3) NOT Null, \n" +
                    "    card_hold TINYINT(1) DEFAULT 0, \n" +
                    "    card_fraud TINYINT(1) DEFAULT 0, " +
                    "    card_issue_date TIMESTAMP DEFAULT (datetime('now','localtime'))\n" +
                    ");";

            transactsTable = "CREATE TABLE IF NOT EXISTS transact (\n" +
                    "\ttransact_id INTEGER PRIMARY KEY, \n" +
                    "    transact_account BIGINT NOT NULL, \n" +
                    "    transact_beneficiary BIGINT NOT NULL,\n" +
                    "    transact_description VARCHAR(100) NOT NULL,\n" +
                    "    transact_date TIMESTAMP DEFAULT (datetime('now','localtime')),\n" +
                    "    transact_type VARCHAR(20) NOT NULL,\n" +
                    "    transaction_amount DOUBLE NOT NULL" +
                    ");";

            notesTable = "CREATE TABLE IF NOT EXISTS notes (\n" +
                    "\tnotes_id INTEGER PRIMARY KEY,\n" +
                    "    notes_date TIMESTAMP DEFAULT (datetime('now','localtime')), \n" +
                    "    notes_link_to VARCHAR(70) NOT NULL, \n" +
                    "    added_by VARCHAR(70) NOT NULL, \n" +
                    "    notes BLOB NOT NULL\n" +
                    ");";
        }
    }
}