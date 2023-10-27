package za.co.tyaphile.acceptance;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.co.tyaphile.BankServer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {
    private static Socket socket;
    private final Gson json = new Gson();
    private static Map<String, Object> john;



    @Test
    void testCardIssuePass() throws IOException, ClassNotFoundException {
        john.put("action", "query");
        john.put("search", "Doe");
        john.put("admin", "Admin");
        john.remove("params");
        john.remove("search");

        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        String acc = ((ArrayList<Map<?, ?>>)result.get("data")).get(0).get("account_number").toString().replaceAll("-", "");

        Map<String, Object> data = new HashMap<>();
        data.put("account", acc);
        john.put("data", data);
        john.put("action", "card");
        result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);

        String card = ((Map<?, ?>) ((Map<?, ?>) result.get("data")).get(acc)).get("card").toString().replaceAll("\\s+", "");
        data.put("item", "card");
        data.put("account", card);
        data.put("isHold", false);
        data.put("isStop", true);
        data.put("admin", "Admin");
        data.put("note", "New note");
        john.put("action", "manage");
        john.put("data", data);
        result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        assertTrue((Boolean) result.get("successful"));

        data = new HashMap<>();
        data.put("account", acc);
        john.put("data", data);
        john.put("action", "card");
        sendRequest(john);

        john.put("account", acc.replaceAll("-", ""));
        john.put("action", "issue");
        result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        assertEquals("Card issued successfully", result.get("data"));
        assertTrue((Boolean) result.get("successful"));
    }

    @Test
    void testCardIssueFail() throws IOException, ClassNotFoundException {
        john.put("action", "query");
        john.put("search", "Doe");
        john.put("admin", "Admin");
        john.remove("params");
        john.remove("search");

        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        String acc = ((ArrayList<Map<?, ?>>)result.get("data")).get(0).get("account_number").toString().replaceAll("-", "");

        john.put("account", acc.replaceAll("-", ""));
        john.put("action", "issue");
        result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        assertEquals("Card issue failed", result.get("message"));
        assertFalse((Boolean) result.get("successful"));
    }

    @Test
    void testGetActiveCard() throws IOException, ClassNotFoundException {
        john.put("action", "query");
        john.put("search", "Doe");
        john.put("admin", "Admin");
        john.remove("params");
        john.remove("search");

        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        String acc = ((ArrayList<Map<?, ?>>)result.get("data")).get(0).get("account_number").toString().replaceAll("-", "");

        Map<String, Object> data = new HashMap<>();
        data.put("account", acc);
        john.put("data", data);
        john.put("action", "card");
        result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        assertTrue((Boolean) result.get("successful"));
        assertEquals(1, ((Map<?, ?>) result.get("data")).size());

        for (Map.Entry<?, ?> entry:( (Map<?, ?>) result.get("data")).entrySet()) {
            assertEquals(7, ((Map<?, ?>) entry.getValue()).size());
        }
    }

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
        Gson json = new Gson();
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(json.toJson(request));
        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
        return input.readObject().toString();
    }

    @AfterAll
    static void cleanUp() throws IOException {
        socket.close();

        File file = new File("finance.db");
        if (file.exists()) {
            while(!file.delete()) {
                fail("Failed to delete file");
            }
        }
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
