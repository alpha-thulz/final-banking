package za.co.tyaphile.acceptance;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import za.co.tyaphile.BankServer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountTest {
    private static Socket socket;
    private static ExecutorService service;
    private final Gson json = new Gson();
    private static Map<String, Object> john;

    @Test
    void testAccountSearchWithParams() throws IOException, ClassNotFoundException {
        john.put("action", "query");
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
        Map<?, ?> result = (Map<?, ?>) json.fromJson(sendRequest(john), Map.class);
        System.out.println("result: " + result);
        assertTrue((Boolean) result.get("successful"));
        assertInstanceOf(List.class, result.get("data"));
    }

    @Test
    void testAccountSearchDefault() throws IOException, ClassNotFoundException {
        john.put("action", "query");
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
    static void cleanUp() throws InterruptedException, IOException {
        socket.close();
        if (!service.awaitTermination(2, TimeUnit.SECONDS)) {
            service.shutdownNow();
        }

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

        service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            try {
                BankServer.main();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(500);
        socket = new Socket("localhost", 5555);
    }
}
