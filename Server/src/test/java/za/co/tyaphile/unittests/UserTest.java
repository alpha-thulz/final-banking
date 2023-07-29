package za.co.tyaphile.unittests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.tyaphile.card.Card;
import za.co.tyaphile.user.User;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {
    private User user;

    @BeforeEach
    void createUser() {
        user = new User("John", "Doe", "Savings");
    }

    @Test
    void testAccountNumber() {
        assertNotNull(user.getAccount());
        assertEquals(10, user.getAccount().getAccountNumber().length());
    }

    @Test
    void testCardIssue() {
        assertEquals(0, user.getAllCards().size());
        assertNull(user.getLastCardIssued());
        user.issueCard();
        assertEquals(1, user.getAllCards().size());
        assertNotNull(user.getLastCardIssued());
        user.issueCard();
        assertEquals(1, user.getAllCards().size());
        user.getLastCardIssued().setSTOP(true, "Lost");
        user.issueCard();
        assertEquals(2, user.getAllCards().size());
    }

    @Test
    void testCardStoppage() {
        assertNull(user.getLastCardIssued());
        user.issueCard();
        assertFalse(user.getLastCardIssued().isSTOPPED());
        user.setStop("Lost");
        assertTrue(user.getLastCardIssued().isSTOPPED());
        user.removeStop("Found");
        assertFalse(user.getLastCardIssued().isSTOPPED());
        Card temp = user.getLastCardIssued();
        user.issueCard();
        assertEquals(temp, user.getLastCardIssued());
        user.setStop("Lost");
        user.issueCard();
        assertNotEquals(temp, user.getLastCardIssued());
    }
}
