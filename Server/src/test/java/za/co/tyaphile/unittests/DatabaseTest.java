package za.co.tyaphile.unittests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.co.tyaphile.database.DatabaseManager;
import za.co.tyaphile.user.User;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseTest {
    private static DatabaseManager dbm = new DatabaseManager();
    private static User john, jane;

//    @Test
//    void testOpenAccount() throws SQLException {
//        assertTrue(DatabaseManager.openAccount(john, "Admin"));
//        assertTrue(DatabaseManager.openAccount(jane, "Admin"));
//        assertTrue(DatabaseManager.issueCard(john, "Admin"));
//        assertTrue(DatabaseManager.issueCard(jane, "Admin"));
//    }

    @Test
    void testGetAccounts() {
        List<Map<String, Object>> accounts = DatabaseManager.getAccounts();
        assertEquals(2, accounts.size());
    }

    @Test
    void testSetBalance() {
        Map<String, Object> transact = new HashMap<>();
        transact.put("account_from", 1000000000);
        transact.put("account_to", john.getAccount().getAccountNumber());
        transact.put("description", "Account transfer");
        transact.put("transaction_type", "payment");
        transact.put("amount", 5000);

        assertTrue(DatabaseManager.setBalance(transact));
    }

    @Test
    void testMakeTransaction() {
        Map<String, Object> transact = new HashMap<>();
        transact.put("account_from", john.getAccount().getAccountNumber());
        transact.put("account_to", jane.getAccount().getAccountNumber());
        transact.put("description", "Account transfer");
        transact.put("transaction_type", "payment");
        transact.put("amount", 2500);

        assertTrue(DatabaseManager.makeTransaction(transact));
    }

    @Test
    void getLinkedCards() throws SQLException {
        assertEquals(1, DatabaseManager.getLinkedCards(john.getAccount().getAccountNumber()).size());
        assertEquals(1, DatabaseManager.getLinkedCards(jane.getAccount().getAccountNumber()).size());
    }

    @Test
    void getLinkedCardsWithNotes() throws SQLException {
        List<Map<String, Object>> johnRemarks = DatabaseManager.getLinkedCards(john.getAccount().getAccountNumber());
        List<Map<String, Object>> janeRemarks = DatabaseManager.getLinkedCards(john.getAccount().getAccountNumber());

        assertEquals(10, ((List<?>) johnRemarks.get(0).get("remarks")).size());
        assertEquals(10, ((List<?>) janeRemarks.get(0).get("remarks")).size());
    }

    @BeforeAll
    static void setDatabase() throws SQLException {
        john = new User("John", "Doe", "Savings");
        jane = new User("Jane", "Doe", "Savings");

        File file = new File("finance_db");
        file.delete();

        DatabaseManager.createTables();

        DatabaseManager.openAccount(john, "Admin");
        DatabaseManager.openAccount(jane, "Admin");
        DatabaseManager.issueCard(john, "Admin");
        DatabaseManager.issueCard(jane, "Admin");

        for (int i = 1; i < 11; i++) {
            DatabaseManager.addNote(john.getLastCardIssued().formatCardNumber(john.getLastCardIssued().getCardNumber()), "Admin", "John note " + i);
            DatabaseManager.addNote(jane.getLastCardIssued().formatCardNumber(jane.getLastCardIssued().getCardNumber()), "Admin", "Jane note " + i);
        }
    }

    @AfterAll
    static void cleanUpDatabase() {
        dbm = null;
        DatabaseManager.closeConnection();

        File file = new File("finance_db");
        file.delete();
    }
}