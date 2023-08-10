package za.co.tyaphile.user;


import za.co.tyaphile.account.Account;
import za.co.tyaphile.card.Card;
import za.co.tyaphile.transact.Transactions;

import java.util.ArrayList;
import java.util.List;

public class User {
    private Card card;
    private final Transactions transactions;
    private final Account account;

    private List<Card> cards = new ArrayList<>();

    public User(String name, String surname, String account_type) {
        account = new Account(name, surname, account_type);
        account.GenerateAccount();
        transactions = new Transactions(this);
    }

    public User(String name, String surname, String account_type, String accountNumber) {
        account = new Account(name, surname, account_type);
        account.setAccountNumber(accountNumber);
        transactions = new Transactions(this);
    }

    public void issueCard() {
//        if (cards.isEmpty() || card.isSTOPPED()) {
//            card = new Card(this, account);
//            cards.add(card);
//        }
    }

    public Account getAccount() {
        return account;
    }

    public void setStop(String reason) {
        card.setSTOP(true, reason);
    }

    public void removeStop(String reason) {
        card.setSTOP(false, reason);
    }

    public void setFraudStop(String reason) {
        card.setFRAUD(true, reason);
    }

    public List<Card> getAllCards() {
        return cards;
    }

    public Card getLastCardIssued() {
        return card;
    }

    public Transactions getTransactions() {
        return transactions;
    }

    public static String returnNumbersOnly(String val) {
        String v = val.replaceAll("(\\s*)(\\^d.)","");
        return  v;
    }
}
