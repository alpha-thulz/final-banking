//package za.co.tyaphile.transact;
//
//import za.co.tyaphile.user.User;
//
//import java.math.BigDecimal;
//import java.text.DecimalFormat;
//import java.text.DecimalFormatSymbols;
//import java.util.Date;
//import java.util.Locale;
//
//public class Transactions {
//    private final User user;
//    private boolean deduct;
//
//    public Transactions(final User user) {
//        this.user = user;
//    }
//
//    public User getUser() {
//        return user;
//    }
//
//    public void Deposit(double amount, String description) {
//        user.getAccount().setBalance(amount);
//
//        System.out.println(new Date() + ": '" + description + "' into account '" + user.getAccount().getAccountNumber() + "' amount R " + CurrencyFormat(BigDecimal.valueOf(amount)));
//        System.out.println("Current balance: R" + user.getAccount().getBalance());
//    }
//
//    public void Withdrawal(double amount, String description) {
//        if(user.getLastCardIssued().isSTOPPED()
//                || user.getLastCardIssued().isFRAUD()) {
//            System.out.println("Cannot transact with card: " + user.getLastCardIssued().formatCardNumber(user.getLastCardIssued().getCardNumber()) + " as there is a stop on it");
//            setDeduction(false);
//        } else {
//            if ((user.getAccount().getBalance().doubleValue() + user.getAccount().getOverDraft().doubleValue()) < amount) {
//                System.out.println("You have insufficient funds to perform transaction of R" + CurrencyFormat(BigDecimal.valueOf(amount)));
//                System.out.println("Available balance: R" + CurrencyFormat(user.getAccount().getBalance()));
//                System.out.println("Available overdraft: R" + CurrencyFormat(user.getAccount().getOverDraft()));
//                setDeduction(false);
//            } else {
//                setDeduction(true);
//                user.getAccount().setBalance(-amount);
//                System.out.println(new Date() + ": '" + description + "' from account '" + user.getAccount().getAccountNumber() + "' amount R " + CurrencyFormat(BigDecimal.valueOf(amount)));
//                System.out.println("Remaining balance: R" + CurrencyFormat(user.getAccount().getBalance()));
//                System.out.println("Available overdraft: R" + CurrencyFormat(user.getAccount().getOverDraft()));
//            }
//        }
//    }
//
//    private void setDeduction(boolean val) {
//        deduct = val;
//    }
//
//    public boolean getDeduction() {
//        return deduct;
//    }
//
//    public String getBalance() {
//        return CurrencyFormat(user.getAccount().getBalance());
//    }
//
//    public String getOverDraftLimit() {
//        return CurrencyFormat(user.getAccount().getOverDraft());
//    }
//
//    public void adjustOverDraftLimit(double amount) {
//        user.getAccount().setOverDraftLimit(amount);
//    }
//
//    public String removeSpaces(String val) {
//        return val.replaceAll("\\s","");
//    }
//
//    public String CurrencyFormat(BigDecimal amount) {
//        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.ENGLISH);
//        dfs.setGroupingSeparator(' ');
//        DecimalFormat df = new DecimalFormat("#,##0.00",dfs);
//        return df.format(amount);
//    }
//}
