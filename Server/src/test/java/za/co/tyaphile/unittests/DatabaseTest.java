package za.co.tyaphile.unittests;

import org.junit.jupiter.api.*;
import za.co.tyaphile.database.DatabaseManager;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    private static Map<String, Object> john, jane;

    @Test
    void testAccountOnHold() {
        long accountFrom = Integer.parseInt(john.get("account").toString());
        long accountTo = Integer.parseInt(jane.get("account").toString());
        String description = "Capital";
        String type = "EFT";
        john.put("hold", true);
        john.put("close", false);
        john.put("note", "Hold account test");

        assertTrue(DatabaseManager.makeTransaction(100000000, accountFrom, description, type, 500, false));
        assertTrue(DatabaseManager.makeTransaction(accountFrom, accountTo, description, type, 100, true));
        assertTrue(DatabaseManager.accountHold(john));
        assertFalse(DatabaseManager.makeTransaction(accountFrom, accountTo, description, type, 100, true));

        john.put("hold", false);
        assertTrue(DatabaseManager.accountHold(john));
        assertTrue(DatabaseManager.makeTransaction(accountFrom, accountTo, description, type, 100, true));
    }

    @Test
    void testCardReIssue() {
        String account = john.get("account").toString();
        Map<?, ?> request = DatabaseManager.getCurrentCard(account).get(account);
        String originalCard = request.get("card").toString();
        assertNotNull(originalCard);
        assertFalse(DatabaseManager.issueCard(john));
        assertTrue(DatabaseManager.cardControl(originalCard, "Admin", "Stop test", true, false));
        assertTrue(DatabaseManager.issueCard(john));
        request = DatabaseManager.getCurrentCard(account).get(account);
        String newCard = request.get("card").toString();
        assertNotNull(newCard);
        assertNotEquals(originalCard, newCard);
    }

    @Test
    void testTransactWithOverdraft() throws SQLException {
        assertTrue(DatabaseManager.updateLimit(john.get("account").toString(), 100));

        Map<String, Double> balances = DatabaseManager.getBalance(john.get("account").toString());
        assertEquals(Double.valueOf(0), balances.get("balance"));
        assertEquals(Double.valueOf(100), balances.get("overdraft"));
        assertEquals(Double.valueOf(100), balances.get("overdraft_limit"));

        long accountFrom = Integer.parseInt(john.get("account").toString());
        long accountTo = Integer.parseInt(jane.get("account").toString());
        String description = "Capital";
        String type = "EFT";

        assertTrue(DatabaseManager.makeTransaction(accountFrom, accountTo, description, type, 50, true));

        balances = DatabaseManager.getBalance(john.get("account").toString());
        assertEquals(Double.valueOf(0), balances.get("balance"));
        assertEquals(Double.valueOf(50), balances.get("overdraft"));
        assertEquals(Double.valueOf(100), balances.get("overdraft_limit"));

        assertTrue(DatabaseManager.updateLimit(john.get("account").toString(), 0));

        balances = DatabaseManager.getBalance(john.get("account").toString());
        assertEquals(Double.valueOf(0), balances.get("balance"));
        assertEquals(Double.valueOf(-50), balances.get("overdraft"));
        assertEquals(Double.valueOf(0), balances.get("overdraft_limit"));

        assertTrue(DatabaseManager.makeTransaction(100000000,
                Long.valueOf(john.get("account").toString()), "deposit", "EFT", 50, false));

        balances = DatabaseManager.getBalance(john.get("account").toString());
        assertEquals(Double.valueOf(0), balances.get("balance"));
        assertEquals(Double.valueOf(0), balances.get("overdraft"));
        assertEquals(Double.valueOf(0), balances.get("overdraft_limit"));
    }

    @Test
    void testUpdateLimits() throws SQLException {
        assertTrue(DatabaseManager.updateLimit(jane.get("account").toString(), 100));

        Map<String, Double> balances = DatabaseManager.getBalance(jane.get("account").toString());
        assertEquals(Double.valueOf(0), balances.get("balance"));
        assertEquals(Double.valueOf(100), balances.get("overdraft"));
        assertEquals(Double.valueOf(100), balances.get("overdraft_limit"));

        assertTrue(DatabaseManager.updateLimit(jane.get("account").toString(), 200));
        balances = DatabaseManager.getBalance(jane.get("account").toString());
        assertEquals(Double.valueOf(0), balances.get("balance"));
        assertEquals(Double.valueOf(200), balances.get("overdraft"));
        assertEquals(Double.valueOf(200), balances.get("overdraft_limit"));

        assertTrue(DatabaseManager.updateLimit(jane.get("account").toString(), 100));
        balances = DatabaseManager.getBalance(jane.get("account").toString());
        assertEquals(Double.valueOf(0), balances.get("balance"));
        assertEquals(Double.valueOf(100), balances.get("overdraft"));
        assertEquals(Double.valueOf(100), balances.get("overdraft_limit"));
    }

    @Test
    void testCurrentCard() {
        assertInstanceOf(Map.class, DatabaseManager.getCurrentCard(jane.get("account").toString()));
        Map<String, Map<String, Object>> result = DatabaseManager.getCurrentCard(jane.get("account").toString());
        assertEquals(1, result.size());
        for(Map.Entry<String, Map<String, Object>> entry:result.entrySet()) {
            assertEquals(jane.get("account").toString(), entry.getKey());
            assertEquals(9, entry.getKey().length());
        }
    }

    @Test
    void testLinkedCards() {
        assertInstanceOf(List.class, DatabaseManager.getLinkedCards(john.get("account").toString()));
        List<Map<String, Object>> result = DatabaseManager.getLinkedCards(john.get("account").toString());
        assertEquals(1, result.size());
        Map<String, Object> cardInfo = result.get(0);
        assertEquals(16, cardInfo.get("card_no").toString().replaceAll("\\s+", "").length());
        assertEquals(4, cardInfo.get("card_pin").toString().length());
        assertEquals(3, cardInfo.get("card_cvv").toString().length());
        assertFalse((Boolean) cardInfo.get("card_fraud"));
        assertFalse((Boolean) cardInfo.get("card_hold"));
        assertInstanceOf(List.class, cardInfo.get("remarks"));
        assertEquals(1, ((List<?>) cardInfo.get("remarks")).size());
    }

    @Test
    void testGetAllAccounts() {
        List<Map<String, Object>> accounts = DatabaseManager.getAccounts();
        assertEquals(2, accounts.size());
    }

    @Test
    void testGetSpecificAccount() {
        List<Map<String, Object>> result = DatabaseManager.getAccounts(john.get("account").toString(),
                john.get("name").toString(), john.get("surname").toString(), "", true);

        assertEquals(1, result.size());
        assertEquals("00-510-000-000", result.get(0).get("account_number"));
        assertEquals("John", result.get(0).get("name"));
        assertEquals("Doe", result.get(0).get("surname"));
        assertEquals((double) 0, result.get(0).get("balance"));
        assertEquals(16, result.get(0).get("card").toString().replaceAll("\\s+", "").length());
    }

    @Test
    void testMakePaymentInsufficientFunds() {
        long accountFrom = Integer.parseInt(john.get("account").toString());
        long accountTo = Integer.parseInt(jane.get("account").toString());
        String description = "Capital";
        String type = "EFT";
        double amount = 50_000;
        boolean fromAccount = true;

        assertFalse(DatabaseManager.makeTransaction(accountFrom, accountTo, description, type, amount, fromAccount));
    }

    @Test
    void testMakePaymentFunds() throws SQLException {
        long accountFrom = Integer.parseInt(john.get("account").toString());
        long accountTo = Integer.parseInt(jane.get("account").toString());
        String description = "Capital";
        String type = "EFT";

        assertFalse(DatabaseManager.makeTransaction(accountFrom, accountTo, description, type, 1000, true));
        Map<String, Double> balances = DatabaseManager.getBalance(john.get("account").toString());

        assertEquals(Double.valueOf(0), balances.get("balance"));
        assertEquals(Double.valueOf(0), DatabaseManager.getBalance(jane.get("account").toString()).get("balance"));
        assertEquals(Double.valueOf(0), balances.get("overdraft_limit"));
        assertEquals(Double.valueOf(0), balances.get("overdraft"));

        assertTrue(DatabaseManager.makeTransaction(100000000, accountFrom, description, type, 1000, false));
        balances = DatabaseManager.getBalance(john.get("account").toString());
        assertEquals(Double.valueOf(1000), balances.get("balance"));
        assertEquals(Double.valueOf(0), DatabaseManager.getBalance(jane.get("account").toString()).get("balance"));
        assertEquals(Double.valueOf(0), balances.get("overdraft_limit"));
        assertEquals(Double.valueOf(0), balances.get("overdraft"));

        assertTrue(DatabaseManager.makeTransaction(accountFrom, accountTo, description, type, 350, true));
        balances = DatabaseManager.getBalance(john.get("account").toString());
        assertEquals(Double.valueOf(650), balances.get("balance"));
        assertEquals(Double.valueOf(350), DatabaseManager.getBalance(jane.get("account").toString()).get("balance"));
        assertEquals(Double.valueOf(0), balances.get("overdraft_limit"));
        assertEquals(Double.valueOf(0), balances.get("overdraft"));
    }

    @Test
    void testDepositCash() {
        long accountFrom = 100000000;
        long accountTo = Integer.parseInt(jane.get("account").toString());
        String description = "Capital";
        String type = "EFT";
        double amount = 25_000;
        boolean fromAccount = false;

        assertTrue(DatabaseManager.makeTransaction(accountFrom, accountTo, description, type, amount, fromAccount));
    }

    @BeforeEach
    void setDatabase() {
        john = new HashMap<>();
        jane = new HashMap<>();

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

        deleteFile();

        DatabaseManager.initTables();
        DatabaseManager.createTables();

        assertTrue(DatabaseManager.openAccount(john));
        assertTrue(DatabaseManager.openAccount(jane));
    }

    @AfterEach
    void cleanUpDatabase() {
        DatabaseManager.closeConnection();
        deleteFile();
    }

    private void deleteFile() {
        File file = new File("finance.db");
        if (file.exists()) file.delete();
    }
}