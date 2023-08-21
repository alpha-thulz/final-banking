//package za.co.tyaphile.unittests;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import za.co.tyaphile.account.Account;
//
//import java.math.BigDecimal;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class AccountTest {
//    private static Account account;
//
//    @BeforeEach
//    void createAccount() {
//        account = new Account("John", "Doe", "Savings");
//    }
//
//    @Test
//    void testAccountValid() {
//        assertNotNull(account);
//        assertEquals("Savings", account.getAccountType());
//        assertEquals(BigDecimal.ZERO, account.getBalance());
//        assertEquals(BigDecimal.ZERO, account.getOverDraft());
//        assertEquals(BigDecimal.ZERO, account.getOverDraftLimit());
//    }
//
//    @Test
//    void testAccountBalance() {
//        account.setBalance(200);
//        assertEquals(BigDecimal.valueOf(200).doubleValue(), account.getBalance().doubleValue());
//        assertEquals(BigDecimal.ZERO.doubleValue(), account.getOverDraft().doubleValue());
//        assertEquals(BigDecimal.ZERO.doubleValue(), account.getOverDraftLimit().doubleValue());
//    }
//
//    @Test
//    void testOverDraft() {
//        assertEquals(BigDecimal.ZERO, account.getBalance());
//        assertEquals(BigDecimal.ZERO, account.getOverDraft());
//        assertEquals(BigDecimal.ZERO, account.getOverDraftLimit());
//        account.setOverDraftLimit(200);
//        assertEquals(BigDecimal.ZERO.doubleValue(), account.getBalance().doubleValue());
//        assertEquals(BigDecimal.valueOf(200).doubleValue(), account.getOverDraft().doubleValue());
//        assertEquals(BigDecimal.valueOf(200).doubleValue(), account.getOverDraftLimit().doubleValue());
//    }
//
//    @Test
//    void testOverDraftOverflow() {
//        assertEquals(BigDecimal.ZERO, account.getOverDraft());
//        assertEquals(BigDecimal.ZERO, account.getOverDraftLimit());
//        account.setOverDraftLimit(200);
//        account.setBalance(20);
//        assertEquals(BigDecimal.valueOf(20).doubleValue(), account.getBalance().doubleValue());
//        assertEquals(BigDecimal.valueOf(200).doubleValue(), account.getOverDraft().doubleValue());
//        assertEquals(BigDecimal.valueOf(200).doubleValue(), account.getOverDraftLimit().doubleValue());
//    }
//
//    @Test
//    void increaseOverDraft() {
//        assertEquals(BigDecimal.ZERO, account.getOverDraft());
//        assertEquals(BigDecimal.ZERO, account.getOverDraftLimit());
//        account.setOverDraftLimit(200);
//        assertEquals(BigDecimal.valueOf(200).doubleValue(), account.getOverDraft().doubleValue());
//        assertEquals(BigDecimal.valueOf(200).doubleValue(), account.getOverDraftLimit().doubleValue());
//        account.setOverDraftLimit(50);
//        assertEquals(BigDecimal.valueOf(250).doubleValue(), account.getOverDraft().doubleValue());
//        assertEquals(BigDecimal.valueOf(250).doubleValue(), account.getOverDraftLimit().doubleValue());
//        account.setOverDraft(-50);
//        assertEquals(BigDecimal.valueOf(200).doubleValue(), account.getOverDraft().doubleValue());
//        assertEquals(BigDecimal.valueOf(250).doubleValue(), account.getOverDraftLimit().doubleValue());
//    }
//
//    @Test
//    void testAccountOnhold() {
//        assertFalse(account.isOnHold());
//        account.setOnHold(true, "testing");
//        assertTrue(account.isOnHold());
//        account.setOnHold(false, "testing");
//        assertFalse(account.isOnHold());
//    }
//
//    @Test
//    void testAccountClosure() {
//        assertFalse(account.isClosed());
//        account.setClosed(true, "testing");
//        assertTrue(account.isClosed());
//        account.setClosed(false, "testing");
//        assertTrue(account.isClosed());
//    }
//
//    @Test
//    void testAccountNumberMatch() {
//        Pattern pattern = Pattern.compile("\\d{2}-?(\\d{3}-?){2}\\d{3}");
//        Matcher matches = pattern.matcher(account.getFormattedAccount());
//        assertEquals(10, account.getAccountNumber().length());
//        assertTrue(matches.matches());
//    }
//
//    @Test
//    void testMultipleAccounts() {
//        for (int i = 0; i < 1_000; i++) {
//            assertNotEquals(account, new Account("John", "Doe", "Savings"));
//        }
//    }
//}
