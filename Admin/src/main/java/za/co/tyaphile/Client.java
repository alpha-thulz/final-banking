package za.co.tyaphile;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Client {
    private static final String HOST = "localhost";
    private Socket socket = null;
    private final String ADMIN = "Administrator";
    private static final int PORT = 5555;
    private final Gson json = new Gson();

    Client() {
        try {
            socket = new Socket(HOST, PORT);
            listenSession().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Thread listenSession() {
        return new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                    Object object = input.readObject();
                    System.out.println(object);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendRequest(Object obj) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(json.toJson(obj));
    }

    public static void main(String... args) {
        new Client();
    }

    private Map<String, Object> controlCard(String card, String note, boolean isHold, boolean isStop, boolean isFraud) {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> details = new HashMap<>();

        details.put("item", "card");
        details.put("account", card);
        details.put("isHold", isHold);
        details.put("isStop", isStop);
        details.put("isFraud", isFraud);
        details.put("admin", ADMIN);
        details.put("note", note);

        request.put("action", "manage");
        request.put("data", details);
        return request;
    }

    private Map<String, Object> getActiveCard(String account) {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> details = new HashMap<>();

        details.put("account", account);

        request.put("action", "card");
        request.put("data", details);
        return request;
    }

    private Map<String, Object> makeTransaction(String from, String to, String description, String type, String amount, boolean isAccount) {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> details = new HashMap<>();

        details.put("payer", from);
        details.put("beneficiary", to);
        details.put("description", description);
        details.put("type", type);
        details.put("amount", amount);
        details.put("from_account", isAccount);

        request.put("action", "transact");
        request.put("data", details);
        return request;
    }

    private Map<String, String> searchAccount() {
        Map<String, String> request = new HashMap<>();

        request.put("action", "query");

        return request;
    }

    private Map<String, String> searchAccount(String param) {
        Map<String, String> request = new HashMap<>();

        request.put("action", "query");
        request.put("search", param);

        return request;
    }

    private Map<String, String> searchAccount(String name, String surname, String account, String card) {
        Map<String, String> request = new HashMap<>();

        request.put("action", "query");
        request.put("name", name);
        request.put("surname", surname);
        request.put("account", account);
        request.put("card", card);

        return request;
    }

    private Map<String, String> openAccount(String name, String surname, String accountType) {
        Map<String, String> request = new HashMap<>();

        request.put("name", name);
        request.put("surname", surname);
        request.put("type", accountType);
        request.put("action", "open");
        request.put("admin", ADMIN);

        return request;
    }
}
