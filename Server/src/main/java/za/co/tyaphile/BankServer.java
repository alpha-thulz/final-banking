package za.co.tyaphile;


import com.google.gson.Gson;
import za.co.tyaphile.account.Account;
import za.co.tyaphile.database.Connector.Connect;
import za.co.tyaphile.database.DatabaseManager;
import za.co.tyaphile.info.Info;
import za.co.tyaphile.network.Connector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BankServer implements Executor {
    private final int port = 5555;
    public static boolean MySQL;
    private static Gson json = new Gson();

    public static boolean isMySQL() {
        return MySQL;
    }

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
        Map<String, Object> request = (Map<String, Object>) json.fromJson(input, Map.class);

        System.out.println(">>> " + request);

        try {
            switch (request.get("action").toString().trim().toLowerCase()) {
                case "open":
                    return openAccount(request);
                case "query":
                    return searchAccount(request);
                case "transact":
                    return accountTransact(request);
                case "issue":
                    boolean success = DatabaseManager.issueCard(request);
                    if (success)
                        return getState(true, true);
                    return getState("Card issue failed", false);
                case "card":
                    return getCard(request);
                default:
                    return getState("Failed to process request", false);
            }
        } catch (NullPointerException e) {
            return getState("Failed to process request", false);
        }
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

        long from = Integer.parseInt(data.get("payer").toString());
        long to = Integer.parseInt(data.get("beneficiary").toString());
        String desc = data.get("description").toString();
        String type = data.get("type").toString();
        double amount = Double.parseDouble(data.get("amount").toString());
        boolean fromAccount = (Boolean) data.get("from_account");

        return getState(DatabaseManager.makeTransaction(from, to, desc, type, amount, fromAccount), true);
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

    public static void main(String... args) throws IOException { new BankServer(); }

    @Override
    public void execute(Runnable command) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(command);
    }

    private void printStackTrace(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        for (StackTraceElement ste:e.getStackTrace()) {
            System.err.println(ste);
        }
    }
}