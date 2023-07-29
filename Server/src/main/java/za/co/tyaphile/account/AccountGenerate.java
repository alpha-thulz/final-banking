package za.co.tyaphile.account;

import java.text.DecimalFormat;
import java.util.Random;

public class AccountGenerate {
    private double account;
    private final double min = 0, max = 2_147_483_647;
    private DecimalFormat df = new DecimalFormat("0");

    public AccountGenerate() {
        GenerateAccount();
    }

    public void GenerateAccount() {
        Random r = new Random();
        account = max + ((long) (r.nextDouble() * (max - min)));
    }

    public void setAccountNumber(String accountNumber) {
        account = Double.parseDouble(accountNumber);
    }

    public String getAccountNumber() {

        return df.format(account);
    }
}