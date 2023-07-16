package za.co.tyaphile.database;


import za.co.tyaphile.BankServer;
import za.co.tyaphile.account.Account;
import za.co.tyaphile.database.Connector.Connect;
import za.co.tyaphile.info.Info;
import za.co.tyaphile.user.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

public class DatabaseManager {
    private static Connection connection;
    private static SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

    public static void createTables() {
        String[] tables = {accountsTable, cardsTable, transactsTable, notesTable};

        for(String sql : tables) {
            try {
                setConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.executeUpdate();
            } catch (SQLException e) {
                printStackTrace("Table create error", e);
            }
        }
    }

    public static List<Map<String, Object>> getTransactions() {
        String sql = "SELECT * FROM transact";
        List<Map<String, Object>> transactions = new ArrayList<>();
        PreparedStatement ps;

        try {
            setConnection();
            ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Map<String, Object> transaction = new HashMap<>();

                transaction.put("transaction_id", rs.getLong("transact_id"));
                transaction.put("account_from", rs.getLong("transact_account"));
                transaction.put("account_to", rs.getLong("transact_beneficiary"));
                transaction.put("description", rs.getString("transact_description"));
                transaction.put("transaction_time", rs.getTimestamp("transact_date"));
                transaction.put("transaction_type", rs.getString("transact_type"));
                transaction.put("amount", rs.getDouble("transaction_amount"));

                transactions.add(transaction);
            }
        } catch (SQLException e) {
            printStackTrace("Transaction get error", e);
        }

        return transactions;
    }

//    public static List<Map<String, Object>> getTransactions(Map<String, Object> transaction) {
//        String sql;
//
//        if (transaction.getAccountTo() == 0 && transaction.getAccountFrom() == 0) {
//            sql = "SELECT * FROM transact WHERE transact_date > ?;";
//        } else if (transaction.getAccountFrom() == transaction.getAccountTo()) {
//            sql = "SELECT *, CASE \n" +
//                    "WHEN transact_account=? THEN (@s := @s - transaction_amount) \n" +
//                    "WHEN transact_beneficiary=? THEN (@s := @s + transaction_amount) \n" +
//                    "END AS Balance\n" +
//                    "FROM finance_db.transact\n" +
//                    "INNER JOIN (SELECT @s := 0) p \n" +
//                    "WHERE (transact_account=? OR transact_beneficiary=?) AND transact_date > ?;";
//        } else {
//            if(transaction.getAccountFrom() > 0 && transaction.getAccountTo() == 0) {
//                sql = "SELECT *, CASE \n" +
//                        "WHEN transact_account=? THEN (@s := @s - transaction_amount) \n" +
//                        "WHEN transact_beneficiary=? THEN (@s := @s + transaction_amount) \n" +
//                        "END AS Balance\n" +
//                        "FROM finance_db.transact\n" +
//                        "INNER JOIN (SELECT @s := 0) p \n" +
//                        "WHERE (transact_account=? AND transact_date > ?);";
//            } else if (transaction.getAccountTo() > 0 && transaction.getAccountFrom() == 0) {
//                sql = "SELECT *, CASE \n" +
//                        "WHEN transact_account=? THEN (@s := @s - transaction_amount) \n" +
//                        "WHEN transact_beneficiary=? THEN (@s := @s + transaction_amount) \n" +
//                        "END AS Balance\n" +
//                        "FROM finance_db.transact\n" +
//                        "INNER JOIN (SELECT @s := 0) p \n" +
//                        "WHERE (transact_beneficiary=? AND transact_date > ?);";
//            } else {
//                sql = "SELECT *, CASE \n" +
//                        "WHEN transact_account=? THEN (@s := @s - transaction_amount) \n" +
//                        "WHEN transact_beneficiary=? THEN (@s := @s + transaction_amount) \n" +
//                        "END AS Balance\n" +
//                        "FROM finance_db.transact\n" +
//                        "INNER JOIN (SELECT @s := 0) p \n" +
//                        "WHERE (transact_account=? AND transact_beneficiary=?) AND transact_date > ?;";
//            }
//        }
//
//        List<Map<String, Object>> transactions = new ArrayList<>();
//
//        try {
//            setConnection();
//            System.out.println(sql);
//            PreparedStatement ps = connection.prepareStatement(sql);
//
//            if (transaction.getAccountTo() == 0 && transaction.getAccountFrom() == 0) {
//                ps.setTimestamp(1, transaction.getTransactionDate());
//            } else if (transaction.getAccountFrom() == transaction.getAccountTo()) {
//                ps.setLong(3, transaction.getAccountFrom());
//                ps.setLong(4, transaction.getAccountTo());
//                ps.setTimestamp(5, transaction.getTransactionDate());
//            } else {
//                if(transaction.getAccountFrom() > 0 && transaction.getAccountTo() == 0) {
//                    ps.setLong(3, transaction.getAccountFrom());
//                    ps.setTimestamp(4, transaction.getTransactionDate());
//                } else if (transaction.getAccountTo() > 0 && transaction.getAccountFrom() == 0) {
//                    ps.setLong(3, transaction.getAccountTo());
//                    ps.setTimestamp(4, transaction.getTransactionDate());
//                } else {
//                    ps.setLong(3, transaction.getAccountFrom());
//                    ps.setLong(4, transaction.getAccountTo());
//                    ps.setTimestamp(5, transaction.getTransactionDate());
//                }
//            }
//
//            if (!(transaction.getAccountTo() == 0 && transaction.getAccountFrom() == 0)) {
//                ps.setLong(1, transaction.getAccountFrom());
//                ps.setLong(2, transaction.getAccountTo());
//            }
//
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                Map<String, Object> trans = new HashMap<>();
//
//                trans.put("transaction_id", rs.getLong("transact_id"));
//                trans.put("account_from", rs.getLong("transact_account"));
//                trans.put("account_to", rs.getLong("transact_beneficiary"));
//                trans.put("description", rs.getString("transact_description"));
//                trans.put("transaction_time", rs.getTimestamp("transact_date"));
//                trans.put("transaction_type", rs.getString("transact_type"));
//                trans.put("amount", rs.getDouble("transaction_amount"));
//
//                if (!(transaction.getAccountTo() == 0 && transaction.getAccountFrom() == 0)) {
//                    trans.setBalance(rs.getLong("Balance"));
//                }
//
//                transactions.add(trans);
//            }
//            System.out.println();
//        } catch (SQLException e) {
//            printStackTrace("Get transaction error", e);
//        }
//
//
//        return transactions;
//    }

    public static boolean setBalance(Map<String, Object> transact, double amount) {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
        String sqlTrans = "INSERT INTO transact (transact_account, transact_beneficiary, transact_description, transact_type, transaction_amount) VALUES (?, ?, ?, ?, ?);";
        try {
            setConnection();
            connection.setAutoCommit(false);

            PreparedStatement ps = connection.prepareStatement(sqlTrans);
            ps.setLong(1, (long) transact.get("account_from"));
            ps.setLong(2, (long) transact.get("account_to"));
            ps.setString(3, transact.get("description").toString());
            ps.setString(4, transact.get("transaction_type").toString());
            ps.setDouble(5, amount);
            ps.executeUpdate();

            ps = connection.prepareStatement(sql);
            ps.setDouble(1, (Double) transact.get("amount"));
            ps.setLong(2, (long) transact.get("account_to"));
            ps.executeUpdate();

            connection.commit();
            return true;
        } catch (SQLException e) {
            printStackTrace("Balance check error", e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                printStackTrace("Error", ex);
            }
        }
        return false;
    }

//    public static synchronized boolean makeTransaction(Transaction transact) {
//        String sql = "INSERT INTO transact (transact_account, transact_beneficiary, transact_description, transact_type, transaction_amount) VALUES (?, ?, ?, ?, ?);";
//        String sqlUpdatePayee = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
//        String sqlUpdateRecipient = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
//
//        try {
//            setConnection();
//            connection.setAutoCommit(false);
//            PreparedStatement ps = connection.prepareStatement(sql);
//
//            ps.setLong(1, transact.getAccountFrom());
//            ps.setLong(2, transact.getAccountTo());
//            ps.setString(3, transact.getTransactionDescription());
//            ps.setDouble(5, transact.getAmount());
//            if(transact.isDeposit()) {
//                ps.setString(4, "Deposit");
//            } else if (transact.isWithdrawal()) {
//                ps.setString(4, "Withdrawal");
//            } else {
//                ps.setString(4, transact.getTransactionType());
//            }
//            ps.executeUpdate();
//
//            ps = connection.prepareStatement(sqlUpdatePayee);
//            ps.setDouble(1, -transact.getAmount());
//            ps.setLong(2, transact.getAccountFrom());
//            ps.executeUpdate();
//
//            ps = connection.prepareStatement(sqlUpdateRecipient);
//            ps.setDouble(1, transact.getAmount());
//            ps.setLong(2, transact.getAccountTo());
//            ps.executeUpdate();
//
//            connection.commit();
//            return true;
//        } catch (SQLException e) {
//            printStackTrace("Deposit error", e);
//            try {
//                connection.rollback();
//            } catch (SQLException ex) {
//                printStackTrace("Rollback error", ex);
//            }
//        }
//        return false;
//    }

//    public static List<Account> getAccounts() {
//        List<Account> accounts = new ArrayList<>();
//        String sql = "SELECT * FROM accounts";
//
//        try {
//            setConnection();
//            PreparedStatement ps = connection.prepareStatement(sql);
//            ResultSet rs = ps.executeQuery();
//
//            while (rs.next()) {
//                Account account = new Account(rs.getString("customer_name"), rs.getString("customer_surname"),
//                        rs.getString("account_no"), rs.getString("account_type"));
//                account.setBalance(rs.getDouble("balance"));
//                accounts.add(account);
//            }
//        } catch (SQLException e) {
//            printStackTrace("Account fetch error", e);
//        }
//
//        return accounts;
//    }

//    public static List<za.co.tyaphile.user.User> getAccounts(String search, String accountType) {
//        List<za.co.tyaphile.user.User> accounts = new ArrayList<>();
//        String sqlAcc;
//        Connection conn;
//        PreparedStatement prep;
//
//        if(accountType.equals("All accounts")) {
//            sqlAcc = "SELECT * FROM accounts " +
//                    "INNER JOIN cards ON account_no=card_linked_account " +
//                    "WHERE account_no=? OR customer_name=? OR customer_surname=? OR card_no=? GROUP BY account_no;";
//        } else {
//            sqlAcc = "SELECT * FROM accounts " +
//                    "INNER JOIN cards ON account_no=card_linked_account " +
//                    "WHERE (account_no=? OR customer_name=? OR customer_surname=? OR card_no=?) AND account_type=? GROUP BY account_no;";
//        }
//
//        try {
//            if(BankServer.isMySQL()) {
//                conn = Connect.getConnection(Info.getDatabaseName(), Info.getROOT(), Info.getPASSWORD());
//            } else {
//                conn = Connect.getConnection(Info.getDatabaseName());
//            }
//            prep = conn.prepareStatement(sqlAcc);
//
//            String val = search.replaceAll("\\D+", "");
//            Pattern pattern = Pattern.compile("\\d+");
//            Matcher matcher = pattern.matcher(val);
//            if(matcher.matches()) {
//                prep.setLong(1, Long.parseLong(val));
//            } else {
//                prep.setLong(1, -1);
//            }
//
//            prep.setString(2, search);
//            prep.setString(3, search);
//            prep.setString(4, getFormatCardNumber(search));
//
//            if(!accountType.equals("All accounts")) {
//                prep.setString(5, accountType);
//            }
//
//            ResultSet rs = prep.executeQuery();
//            while (rs.next()) {
//                Account account = new Account(rs.getString("customer_name"), rs.getString("customer_surname"),
//                        rs.getString("account_no"), rs.getString("account_type"));
//                account.setBalance(rs.getDouble("balance"));
//                account.setOverDraft(rs.getDouble("overdraft_balance"));
//                account.setOverDraftLimit(rs.getDouble("overdraft_limit"));
//
//                List<String> notes = getNotes(rs.getString("account_no"));
//                if(!(notes.size() < 1)) {
//                    account.getNotes().addAll(notes);
//                    account.setClosed(rs.getBoolean("account_close"), notes.get(notes.size() - 1));
//                    account.setOnHold(rs.getBoolean("account_hold"), notes.get(notes.size() - 1));
//                }
//                List<Card> cards = getLinkedCards(account.getAccountNumber());
//                account.getNotes().clear();
//                account.getNotes().addAll(notes);
//                za.co.tyaphile.user.User user = new za.co.tyaphile.user.User(cards.get(cards.size() - 1), account, cards);
//                accounts.add(user);
//            }
//        } catch (SQLException e) {
//            printStackTrace("Account retrieval", e);
//        }
//
//        return accounts;
//    }

    public static boolean openAccount(Account account, String admin) {
        User user = new User(account.getName(), account.getSurname(), account.getAccountType());

        String sql;

        if(!account.isOnHold()) {
            sql = "INSERT INTO accounts (account_no, customer_name, customer_surname, account_type) VALUES (?, ?, ?, ?);";
        } else {
            sql = "INSERT INTO accounts (account_no, customer_name, customer_surname, account_type, account_hold) VALUES (?, ?, ?, ?, ?);";
            user.getAccount().setOnHold(account.isOnHold(), account.getNotes().get(0));
        }

        try {
            setConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(user.getAccount().getAccountNumber()));
            ps.setString(2, user.getAccount().getName());
            ps.setString(3, user.getAccount().getSurname());
            ps.setString(4, user.getAccount().getAccountType());

            if(account.isOnHold()) {
                ps.setBoolean(5, user.getAccount().isOnHold());
            }

            ps.executeUpdate();

            issueCard(user, admin);

            return true;
        } catch (SQLException e) {
            printStackTrace("SQL Error", e);
        }
        return false;
    }

//    public static boolean accountHold(Account account) {
//        String sql = "UPDATE accounts SET account_hold=? WHERE account_no=?";
//
//        try {
//            setConnection();
//            PreparedStatement ps = connection.prepareStatement(sql);
//            ps.setBoolean(1, account.isOnHold());
//            ps.setString(2, account.getAccountNumber());
//            ps.executeUpdate();
//            addNote(account.getAccountNumber(), account.getAdmin(), account.getHoldReason());
//            return true;
//        } catch (SQLException e) {
//            printStackTrace("Account hold error", e);
//        }
//
//        return false;
//    }

//    public static boolean accountClose(Account account) {
//        String sql = "UPDATE accounts SET account_close=? WHERE account_no=?";
//
//        try {
//            setConnection();
//            PreparedStatement ps = connection.prepareStatement(sql);
//            ps.setBoolean(1, account.isClosed());
//            ps.setString(2, account.getAccountNumber());
//            ps.executeUpdate();
//            addNote(account.getAccountNumber(), account.getAdmin(), account.getCloseReason());
//            return true;
//        } catch (SQLException e) {
//            printStackTrace("Account hold error", e);
//        }
//
//        return false;
//    }

//    public static boolean cardManager(Card card) {
//        String sqlAcc = "SELECT card_linked_account, account_no, customer_name, customer_surname, account_type FROM cards " +
//                "INNER JOIN accounts ON account_no=card_linked_account " +
//                "WHERE card_no=?";
//
//        try {
//            setConnection();
//            PreparedStatement ps = connection.prepareStatement(sqlAcc);
//            ps.setString(1, card.getCardNumber());
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                User user = new User(rs.getString("customer_name"), rs.getString("customer_surname"), rs.getString("account_type"));
//                user.getAccount().setAccountNumber(rs.getString("account_no"));
//
//                if(card.isIssue()) {
//                    cardControl(card);
//                    issueCard(user, card.getAdmin());
//                } else if (card.isFRAUD()) {
//                    cardControl(card);
//                    List<Card> cards = getLinkedCards(user.getAccount().getAccountNumber());
//                    Card latest = cards.get(cards.size() - 1);
//                    if(card.getCardNumber().equals(latest.getCardNumber())) {
//                        issueCard(user, card.getAdmin());
//                    }
//                } else {
//                    cardControl(card);
//                }
//            }
//            return true;
//        } catch (SQLException e) {
//            printStackTrace("Card control error", e);
//        }
//        return false;
//    }

//    private static void cardControl(Card card) throws SQLException {
//        String sql = "UPDATE cards SET card_hold = ?, card_fraud = ? WHERE card_no = ?;";
//        setConnection();
//        PreparedStatement ps = connection.prepareStatement(sql);
//        ps.setBoolean(1, card.isSTOP());
//        ps.setBoolean(2, card.isFRAUD());
//        ps.setString(3, card.getCardNumber());
//        ps.executeUpdate();
//        addNote(card.getCardNumber(), card.getAdmin(), card.getStopReason());
//    }

    private static void issueCard(User user, String admin) throws SQLException {
        String sql = "INSERT INTO cards (card_no, card_pin, card_cvv, card_linked_account) VALUES (?, ?, ?, ?)";
        setConnection();
        user.issueCard();
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, user.getLastCardIssued().formatCardNumber(user.getLastCardIssued().getCardNumber()));
        ps.setString(2, user.getLastCardIssued().getCardPin());
        ps.setString(3, user.getLastCardIssued().getCVV());
        ps.setLong(4, Long.parseLong(user.getAccount().getAccountNumber()));

        ps.executeUpdate();

        if(user.getAccount().isOnHold()) {
            addNote(user.getAccount().getAccountNumber(), admin, user.getAccount().getHoldReason());
        }
    }

//    private static List<Card> getLinkedCards(String param) throws SQLException {
//        String sql = "SELECT * FROM accounts INNER JOIN cards ON card_linked_account=account_no  WHERE account_no=?;";
//        List<Card> cards = new ArrayList<>();
//        setConnection();
//        PreparedStatement ps = connection.prepareStatement(sql);
//        ps.setString(1, param);
//        ResultSet rs = ps.executeQuery();
//        while (rs.next()) {
//            Card card = new Card(rs.getString("card_no"), rs.getString("card_pin"), rs.getString("card_cvv"));
//            card.setFRAUD(rs.getBoolean("card_fraud"));
//            card.setSTOP(rs.getBoolean("card_hold"));
//            cards.add(card);
//        }
//
//        for (Card c : cards) {
//            c.getNotes().addAll(getNotes(c.getCardNumber()));
//        }
//
//        return cards;
//    }

    private static void addNote(String link_to, String admin, String notes) {
        String sql = "INSERT INTO notes (notes_link_to, added_by, notes) VALUES (?, ?, ?);";
        try {
            setConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, link_to);
            ps.setString(2, admin);
            ps.setString(3, notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            printStackTrace("Note error", e);
        }
    }

    private static List<String> getNotes(String param) throws SQLException {
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

    private static void setConnection() throws SQLException{
        if(connection == null) {
            if (BankServer.isMySQL()) {
                connection = Connect.getConnection(Info.getDatabaseName(), Info.getROOT(), Info.getPASSWORD());
            } else {
                connection = Connect.getConnection(Info.getDatabaseName());
            }
        }
    }

    public static void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("Failed to close connection");
            e.printStackTrace();
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

    private static String getFormattedDate(double milliseconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE ':' dd MMM yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("CAT"));
        return sdf.format(new Date((long) milliseconds));
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