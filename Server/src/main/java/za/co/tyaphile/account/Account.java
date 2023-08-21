package za.co.tyaphile.account;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Account extends AccountGenerate {

    private String name, surname, accountType, holdReason, closeReason;
    private BigDecimal balance = new BigDecimal(0);
    private BigDecimal overDraft = new BigDecimal(0);
    private BigDecimal overDraftLimit = new BigDecimal(0);
    private boolean closed, onHold;
    private List<String> notes = new ArrayList<>();

    public Account(String name, String surname, String accountType) {
        this.name = name;
        this.surname = surname;
        this.accountType = accountType;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public synchronized void setBalance(double amount) {
        double diff = 0;
        if (amount > 0) {
            if (overDraft.doubleValue() < overDraftLimit.doubleValue()) {
                diff = overDraftLimit.doubleValue() - overDraft.doubleValue();
            }
            setOverDraft(diff);
            balance = balance.add(BigDecimal.valueOf(Math.abs(amount - diff)));
        } else {
            if (Math.abs(amount) > balance.doubleValue()) {
                diff = amount - balance.doubleValue();
            }
            setOverDraft(diff);
            balance = balance.add(BigDecimal.valueOf(amount - diff));
        }
    }

    public BigDecimal getOverDraft() {
        return overDraft;
    }

    public void setOverDraft(double amount) {
        overDraft = overDraft.add(BigDecimal.valueOf(amount));
    }

    public BigDecimal getOverDraftLimit() {
        return overDraftLimit;
    }

    public void setOverDraftLimit(double amount) {
        this.overDraftLimit = overDraftLimit.add(BigDecimal.valueOf(amount));
        setOverDraft(amount);
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed, String reason) {
        if (!isClosed()) {
            this.closeReason = reason;
            notes.add(reason);
            this.closed = closed;
        }
    }

    public boolean isOnHold() {
        return onHold;
    }

    public void setOnHold(boolean onHold, String reason) {
        this.holdReason = reason;
        notes.add(reason);
        this.onHold = onHold;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public List<String> getNotes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(this.getAccountNumber(), account.getAccountNumber());
    }
}