package za.co.tyaphile.card;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class Card extends CardGenerator {

    private final CardGenerator cg = new CardGenerator();

    public String getCardNumber() {
        String combine = 519629 + super.getCard();
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
    public static String formatCardNumber(String number) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("####,####",symbols);
        Double num = Double.parseDouble(number);
        return df.format(num);
    }
}
