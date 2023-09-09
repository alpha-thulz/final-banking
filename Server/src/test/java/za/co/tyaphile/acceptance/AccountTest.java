package za.co.tyaphile.acceptance;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import za.co.tyaphile.BankServer;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {
    private static Socket socket;
    private final Gson json = new Gson();
    private static Map<String, Object> john;

    @Test
    void testAccountTransact() throws IOException, ClassNotFoundException {
        john.put("action", "query");
        john.put("search", "Doe");
        john.remove("params");
        john.remove("admin");
        john.remove("search");

        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        String acc = ((ArrayList<Map<?, ?>>)result.get("data")).get(0).get("account_number").toString();

        john.put("action", "transact");
        Map<String, Object> data = new HashMap<>();
        data.put("payer", 100000000);
        data.put("beneficiary", Long.parseLong(acc.replaceAll("-", "").trim()));
        data.put("description", "Testing");
        data.put("type", "ATM");
        data.put("amount", 100);
        data.put("from_account", false);
        john.put("data", data);

        result = json.fromJson(sendRequest(john), Map.class);
        assertEquals("Transaction successful", ((Map<?, ?>) result.get("data")).get("message"));
        assertTrue((Boolean) result.get("successful"));
    }

    @Test
    void testAccountTransactNoDetailsProvidedFails() throws IOException, ClassNotFoundException {
        john.remove("admin");
        john.put("action", "transact");

        Map<?, ?> results = json.fromJson(sendRequest(john), Map.class);
        assertEquals("Failed to process request", results.get("message"));
        assertFalse((Boolean) results.get("successful"));
    }

    @Test
    void testAccountSearchWithParams() throws IOException, ClassNotFoundException {
        john.put("action", "query");
        john.remove("admin");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("surname", "Doe");
        data.put("card", "0");
        data.put("account", "0");
        john.put("params", data);
        john.remove("name");
        john.remove("surname");
        john.remove("type");

        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        assertTrue((Boolean) result.get("successful"));
        assertInstanceOf(List.class, result.get("data"));
    }

    @Test
    void testAccountSearchWithSearch() throws IOException, ClassNotFoundException {
        john.put("action", "query");
        john.put("search", "Doe");
        john.remove("params");
        john.remove("admin");
        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        assertTrue((Boolean) result.get("successful"));
        assertInstanceOf(List.class, result.get("data"));
    }

    @Test
    void testAccountSearchDefault() throws IOException, ClassNotFoundException {
        john.put("action", "query");
        john.remove("admin");
        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        assertTrue((Boolean) result.get("successful"));
        assertInstanceOf(List.class, result.get("data"));
    }


    @Test
    void testOpenAccount() throws IOException, ClassNotFoundException {
        john.put("action", "open");
        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        assertTrue((Boolean) result.get("data"));
        assertTrue((Boolean) result.get("successful"));
    }

    private String sendRequest(Map<String, Object> request) throws IOException, ClassNotFoundException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(request);
        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
        return input.readObject().toString();
    }

    @AfterAll
    static void cleanUp() throws IOException {
        socket.close();

        File file = new File("finance.db");
        if (file.exists()) file.delete();
    }

    @BeforeAll
    static void setupSocket() throws IOException, InterruptedException {
        john = new HashMap<>();

        john.put("admin", "Admin");
        john.put("name", "John");
        john.put("surname", "Doe");
        john.put("type", "Savings");

        BankServer.main();

        Thread.sleep(500);
        socket = new Socket("localhost", 5555);
    }
}
