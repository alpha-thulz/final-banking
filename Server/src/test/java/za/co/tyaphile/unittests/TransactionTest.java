package za.co.tyaphile.unittests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.tyaphile.transact.Transactions;
import za.co.tyaphile.user.User;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionTest {
    Transactions transactions;

    @BeforeEach
    void prepare() {
        User user = new User("John", "Doe", "Savings");
        transactions = new Transactions(user);
        user.issueCard();
    }

    @Test
    void testDeposit() {
        assertEquals("0.00", transactions.getBalance());
        transactions.Deposit(200, "ATM deposit");
        assertEquals("200.00", transactions.getBalance());
        transactions.Deposit(200, "ATM deposit");
        assertEquals("400.00", transactions.getBalance());
    }

    @Test
    void testDepositOverdraft() {
        transactions.adjustOverDraftLimit(150);
        assertEquals("0.00", transactions.getBalance());
        transactions.Deposit(200, "ATM deposit");
        assertEquals("200.00", transactions.getBalance());
        assertEquals(150.0, transactions.getUser().getAccount().getOverDraft().doubleValue());
        transactions.Deposit(200, "ATM deposit");
        assertEquals(150.0, transactions.getUser().getAccount().getOverDraft().doubleValue());
        assertEquals("400.00", transactions.getBalance());
    }

    @Test
    void testWithdrawalOverdraft() {
        transactions.adjustOverDraftLimit(500);
        assertEquals(500.0, transactions.getUser().getAccount().getOverDraft().doubleValue());
        assertEquals("0.00", transactions.getBalance());
        transactions.Withdrawal(250, "ATM withdrawal");
        assertEquals(250.0, transactions.getUser().getAccount().getOverDraft().doubleValue());
        assertEquals("0.00", transactions.getBalance());
    }

    @Test
    void testNotEnoughFunds() {
        transactions.Deposit(200, "ATM deposit");
        assertEquals(0.0, transactions.getUser().getAccount().getOverDraft().doubleValue());
        assertEquals("200.00", transactions.getBalance());
        transactions.Withdrawal(250, "ATM withdrawal");
        assertEquals(0.0, transactions.getUser().getAccount().getOverDraft().doubleValue());
        assertEquals("200.00", transactions.getBalance());
    }
}
