package za.co.tyaphile.card;

import java.util.Random;

public class CardGenerator {

    private final long num;
    private final int cvv, pin;
    private final long min = 0, max = 999_999_999;

    CardGenerator() {
        Random r = new Random();
        num = max + ((long) (r.nextDouble() * (max - min)));
        cvv = r.nextInt(1000);
        pin = r.nextInt(10000);
    }

    public String getCard() {
        return String.valueOf(num);
    }

    public int getCvv() {
        return cvv;
    }

    public int getPin() { return pin; }
}
