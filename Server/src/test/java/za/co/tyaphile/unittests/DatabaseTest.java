package za.co.tyaphile.unittests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.co.tyaphile.database.DatabaseManager;
import za.co.tyaphile.user.User;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    private static Map<String, Object> john = new HashMap<>();
    private static Map<String, Object> jane = new HashMap<>();

    @Test
    void testGetAccounts() {
        List<Map<String, Object>> accounts = DatabaseManager.getAccounts();
        assertEquals(2, accounts.size());
    }

    @Test
    void testMakeTransaction() {
        long from = 100000000;
        long to = Long.parseLong(jane.get("account").toString());
        String description = "Account transfer";
        String type = "deposit";

        assertTrue(DatabaseManager.makeTransaction(from , to, description, type, 5000, false));
        assertEquals(1, DatabaseManager.getTransactions(from, to, Timestamp.from(Instant.MIN)).size());
        long beneficiary = Long.parseLong(john.get("account").toString());
        assertTrue(DatabaseManager.makeTransaction(to , beneficiary, description, type, 750, true));
        assertEquals(1, DatabaseManager.getTransactions(from, to, Timestamp.from(Instant.MIN)).size());
        assertEquals(2, DatabaseManager.getTransactions(0, to, Timestamp.from(Instant.MIN)).size());
    }

    @Test
    void testLinkedCards() {
        assertEquals(1, DatabaseManager.getLinkedCards(john.get("account").toString()).size());
    }

    @Test
    void testCurrentCard() {
        assertEquals(1, DatabaseManager.getCurrentCard(john.get("account").toString()).size());
        assertEquals(1, DatabaseManager.getCurrentCard(jane.get("account").toString()).size());
    }

    @Test
    void testCardReIssue()  {
        assertEquals(1, DatabaseManager.getLinkedCards(jane.get("account").toString()).size());
        Map<String, Object> currentCard = null;
        for (int i = 0; i < 4; i++) {
            currentCard = DatabaseManager.getCurrentCard(jane.get("account").toString()).get(jane.get("account").toString());
            assertTrue(DatabaseManager.cardControl(currentCard.get("card").toString(), "Admin", "Lost card", true, false));
            assertTrue(DatabaseManager.issueCard(jane));
        }
        assertEquals(5, DatabaseManager.getLinkedCards(jane.get("account").toString()).size());
        Map<String, Object> newCard = DatabaseManager.getCurrentCard(jane.get("account").toString()).get(jane.get("account").toString());
        assertNotEquals(currentCard.get("card").toString(), newCard.get("card").toString());
    }

    @Test
    void getLinkedCardsWithNotes() throws SQLException {
        List<Map<String, Object>> johnRemarks = DatabaseManager.getLinkedCards(john.get("account").toString());
        List<Map<String, Object>> janeRemarks = DatabaseManager.getLinkedCards(jane.get("account").toString());

        assertEquals(1, ((List<?>) johnRemarks.get(0).get("remarks")).size());
        assertEquals(1, ((List<?>) janeRemarks.get(0).get("remarks")).size());
    }

    @Test
    void testAccountSearch() {
        List<Map<String, Object>> result = DatabaseManager.getAccounts(john.get("account").toString(),
                john.get("name").toString(), john.get("surname").toString(), "", true);

        assertEquals(1, result.size());
        assertEquals(john.get("account").toString(), result.get(0).get("account_number"));
        assertEquals("John", result.get(0).get("name"));
        assertEquals("Doe", result.get(0).get("surname"));
        assertEquals((double) 0, result.get(0).get("balance"));
        assertEquals(16, result.get(0).get("card").toString().replaceAll("\\s+", "").length());
    }

    @BeforeAll
    static void setDatabase() {
        john.put("account", "510000000");
        john.put("admin", "Admin");
        john.put("name", "John");
        john.put("surname", "Doe");
        john.put("type", "Savings");

        jane.put("account", "510000001");
        jane.put("admin", "Admin");
        jane.put("name", "Jane");
        jane.put("surname", "Doe");
        jane.put("type", "Savings");

        File file = new File("finance.db");
        file.delete();

        DatabaseManager.initTables();
        DatabaseManager.createTables();

        DatabaseManager.openAccount(john);
        DatabaseManager.openAccount(jane);
        DatabaseManager.issueCard(john);
        DatabaseManager.issueCard(jane);
    }

    @AfterAll
    static void cleanUpDatabase() {
        DatabaseManager.closeConnection();

//        File file = new File("finance.db");
//        file.delete();
    }
}