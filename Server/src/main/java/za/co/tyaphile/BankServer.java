package za.co.tyaphile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import za.co.tyaphile.account.Account;
import za.co.tyaphile.database.Connector.Connect;
import za.co.tyaphile.database.DatabaseManager;
import za.co.tyaphile.info.Info;
import za.co.tyaphile.network.Connector;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BankServer implements Executor {
    private final int port = 5555;
    public static boolean MySQL;
    private static final Gson json = new GsonBuilder().create();

    public static boolean isMySQL() {
        return MySQL;
    }
    private static String[] type = {"Private", "Premier", "Gold", "Silver", "Platinum"};

    public BankServer() throws IOException {
        init();
        execute(new Connector(port));
    }

    private void init() {
//        new ThemeSetup();
        DatabaseManager.initTables();
        if (MySQL) {
            Connect.createDatabase(Info.getDatabaseName(true), Info.getROOT(), Info.getPASSWORD());
        }
        DatabaseManager.createTables();
    }

    public static String processRequest(String input) {

        try {
            Map<String, Object> request = (Map<String, Object>) json.fromJson(input, Map.class);
            switch (request.get("action").toString().trim().toLowerCase()) {
                case "open":
                    return openAccount(request);
                case "query":
                    return searchAccount(request);
                case "transact":
                    return accountTransact(request);
                case "card":
                    return getCard(request);
                case "issue":
                    boolean success = DatabaseManager.issueCard(request);
                    if (success)
                        return getState("Card issued successfully", true);
                    return getState("Card issue failed", false);
                case "manage":
                    String item = ((Map<String, Object>) request.get("data")).get("item").toString();
                    if (item.equalsIgnoreCase("card")) {
                        return manageCard((Map<String, Object>) request.get("data"));
                    } else if (item.equalsIgnoreCase("account")) {
                        return manageAccount((Map<String, Object>) request.get("data"));
                    } else {
                        return getState("Need to define Card or Account management", false);
                    }
                default:
                    return getState("Invalid option", false);
            }
        } catch (NullPointerException e) {
            printStackTrace("Failed to process request", e);
            return getState("Failed to process request", false);
        }
    }

    private static String manageAccount(Map<String, Object> request) throws NullPointerException {
        System.out.println(request);
        boolean success = DatabaseManager.issueCard((Map<String, Object>) request.get("data"));

        if (success) return getState("Account action successful", true);
        return getState("Action failed, cannot manage account", false);
    }

    private static String manageCard(Map<String, Object> request) throws NullPointerException {
        DecimalFormat df = new DecimalFormat("0");
        String admin = request.get("admin").toString();
        String card = df.format(Double.parseDouble(request.get("account").toString()));
        String note = request.get("note").toString();
        boolean isHold = request.containsKey("isHold") ? (Boolean) request.get("isHold") : false;
        boolean isStop = request.containsKey("isStop") ? (Boolean) request.get("isStop") : false;
        boolean isFraud = request.containsKey("isFraud") ? (Boolean) request.get("isFraud") : false;

        boolean success = DatabaseManager.cardControl(card, admin, note, isHold, isStop, isFraud);

        if (success) return getState("Card action successful", true);
        return getState("Action failed, cannot manage card", false);
    }

    private static String getCard(Map<String, Object> request) throws NullPointerException {
        Map<?, ?> data = (Map<?, ?>) request.get("data");
        String account = data.get("account").toString();
        String list = data.containsKey("list") ? data.get("list").toString() : "current";
        if (list.equalsIgnoreCase("all"))
            return getState(DatabaseManager.getLinkedCards(account), true);
        return getState(DatabaseManager.getCurrentCard(account), true);
    }

    private static String accountTransact(Map<String, Object> request) throws NullPointerException {
        Map<?, ?> data = (Map<?, ?>) request.get("data");

        BigDecimal accFrom = new BigDecimal(Double.valueOf(data.get("payer").toString()),
                new MathContext(0));
        BigDecimal accTo = new BigDecimal(Double.valueOf(data.get("beneficiary").toString()),
                new MathContext(0));

        long from = accFrom.longValue();
        long to = accTo.longValue();
        String desc = data.get("description").toString();
        String type = data.get("type").toString();
        double amount = Double.parseDouble(data.get("amount").toString());
        boolean fromAccount = (Boolean) data.get("from_account");

        boolean success = DatabaseManager.makeTransaction(from, to, desc, type, amount, fromAccount);
        Map<String, String> result = new HashMap<>();

        if (success) result.put("message", "Transaction successful") ;
        else result.put("message", "Transaction failed");

        return getState(result, success);
    }

    private static String searchAccount(Map<String, Object> request) throws NullPointerException {
        if (request.containsKey("params") && request.containsKey("params")) {
            Map<?, ?> data = (Map<?, ?>) request.get("params");
            String name = data.get("name").toString();
            String surname = data.get("surname").toString();
            String card = data.get("card").toString();
            String account = data.get("account").toString();
            return getState(DatabaseManager.getAccounts(account, name, surname, card, true), true);
        } else if (request.containsKey("search")) {
            String search = request.get("search").toString();
            return getState(DatabaseManager.getAccounts(search, search, search, search, false), true);
        }
        return getState(DatabaseManager.getAccounts(), true);
    }

    private static String openAccount(Map<String, Object> request) throws NullPointerException {
        String name = request.get("name").toString();
        String surname =  request.get("surname").toString();
        String type =  request.get("type").toString();

        Account account = new Account(name, surname, type);
        request.put("account", account.getAccountNumber());
        return getState(DatabaseManager.openAccount(request), true);
    }

    private static String getState(Object result, boolean success) {
        Map<String, Object> results = new HashMap<>();
        results.put("successful", success);
        if (success) {
            results.put("data", result);
        } else {
            results.put("message", result);
        }
        return json.toJson(results);
    }

    public static void main(String... args) throws IOException {
        new BankServer();
        System.out.println("Server started...");
        if (DatabaseManager.getAccounts().isEmpty()) {
            Random random = new Random();
            outer:
            for (int i = 0; i < generateTestAccounts().length; i++) {
                for (int j = 0; j < i + 1; i++) {
                    if (generateTestAccounts().length == i) break outer;
                    Map<String, Object> account = new HashMap<>(Map.of(
                            "name", generateTestAccounts()[i][0],
                            "surname",generateTestAccounts()[i][1],
                            "type", type[random.nextInt(type.length)],
                            "admin", "Admin",
                            "action", "open"
                    ));
                    openAccount(account);
                }
            }
            System.out.println(DatabaseManager.getAccounts());
        }
    }

    private static String[][] generateTestAccounts() {
        return new String[][]{
                {"Zachary", "Pierce"},
                {"Safia", "Sutherland"},
                {"Bailey", "Owen"},
                {"Callum", "Ballard"},
                {"Nathan", "Walls"},
                {"Malik", "Knowles"},
                {"Alfie", "Barber"},
                {"Bibi", "Copeland"},
                {"Denzel", "Hilton"},
                {"Aiza", "Meza"},
                {"Benjamin", "Berry"},
                {"Jac", "Garrett"},
                {"Tamsin", "O'Doherty"},
                {"Philippa", "Swanson"},
                {"Seamus", "Daugherty"},
                {"John", "Doe"},
                {"Jane", "Doe"}
        };
    }

    @Override
    public void execute(@NotNull Runnable command) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(command);
    }

    public static String[] getAccountTypes() {
        return type;
    }

    private static void printStackTrace(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        for (StackTraceElement ste:e.getStackTrace()) {
            System.err.println(ste);
        }
    }
}