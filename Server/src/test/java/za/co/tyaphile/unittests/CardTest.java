package za.co.tyaphile.unittests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.tyaphile.account.Account;
import za.co.tyaphile.card.Card;
import za.co.tyaphile.user.User;

import static org.junit.jupiter.api.Assertions.*;

public class CardTest {
    private User user;
    private Account account;
    private Card card;

    @BeforeEach
    void setupTest() {
        account = new Account("John", "Doe", "Savings");
        user = new User(account.getName(), account.getSurname(), account.getAccountType());
        card = new Card(user, account);
    }

    @Test
    void testNewCard() {
        assertEquals(3, card.getCVV().length());
        assertEquals(4, card.getCardPin().length());
        assertEquals(16, card.formatCardNumber(card.getCardNumber()).replaceAll("\\s+", "").length());
        assertTrue(card.formatCardNumber(card.getCardNumber()).replaceAll("\\s+", "").startsWith("519629"));
    }

    @Test
    void testLinkCard() {
        assertEquals(account.getAccountNumber(), card.getLinkedAccount());
    }

    @Test
    void testPlaceStop() {
        assertFalse(card.isSTOPPED());
        card.setSTOP(true, "Misplaced card");
        assertTrue(card.isSTOPPED());
        card.setSTOP(false, "Card found");
        assertFalse(card.isSTOPPED());
    }

    @Test
    void testPlaceFraudStop() {
        assertFalse(card.isFRAUD());
        card.setFRAUD(true, "Unknown transaction");
        assertTrue(card.isFRAUD());
        card.setFRAUD(false, "Genuine transaction");
        assertTrue(card.isFRAUD());
    }
}