package za.co.tyaphile.card;


import za.co.tyaphile.account.Account;
import za.co.tyaphile.user.User;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Card extends CardGenerator {
    private int bin_number = 519629;
    private String cardNum, stopReason;

    private boolean STOP, FRAUD;
    private List<String> notes = new ArrayList<>();

    private final CardGenerator cg = new CardGenerator();
    private User user;
    private final String account;

    public Card(String account) {
        this.account = account;
//        super.generateCard();
    }

    public String getCardNumber() {
        String combine = bin_number + super.getCard();
        return String.valueOf(Double.parseDouble(combine));
    }

    public String getCVV() {
        DecimalFormat df = new DecimalFormat("000");
        return df.format(cg.getCvv());
    }

    public String getCardPin() {
        DecimalFormat df = new DecimalFormat("0000");
        return df.format(cg.getPin());
    }

    public String getStopReason() {
        return stopReason;
    }

    public static String formatCardNumber(String number) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("####,####",symbols);
        Double num = Double.parseDouble(number);
        return df.format(num);
    }

    public String getLinkedAccount() {
        return account;
    }

    public boolean isSTOPPED() {
        return STOP;
    }

    public void setSTOP(boolean STOP, String reason) {
        if (!isFRAUD()) {
            this.stopReason = reason;
            notes.add(reason);
            this.STOP = STOP;
        }
    }

    public boolean isFRAUD() {
        return FRAUD;
    }

    public void setFRAUD(boolean FRAUD, String reason) {
        if(!isFRAUD()) {
            if (FRAUD) setSTOP(true, reason);
            this.FRAUD = FRAUD;
        }
    }

    public List<String> getNotes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(this.cardNum, card.cardNum) && Objects.equals(this.getCardPin(), card.getCardPin())
                && Objects.equals(this.getCVV(), card.getCVV());
    }
}
