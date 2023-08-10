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

        try {
            if (request.get("action").toString().equalsIgnoreCase("open")) {
                String name = request.get("name").toString();
                String surname =  request.get("surname").toString();
                String type =  request.get("type").toString();

                Account account = new Account(name, surname, type);
                request.put("account", account.getAccountNumber());
                return getState(DatabaseManager.openAccount(request), true);
            }
        } catch (NullPointerException e) {
            return getState("Failed to process request", false);
        }

        return json.toJson(request);
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