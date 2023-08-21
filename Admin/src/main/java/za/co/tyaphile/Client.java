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
    private static final int PORT = 5555;
    public static void main(String... args) {
        Gson json = new Gson();
        try {
            Socket socket = new Socket(HOST, PORT);

            Map<String, Object> request = new HashMap<>();
            Map<String, Object> data = new HashMap<>();

            data.put("account", "3991509595");
//            data.put("list", "all");

//            request.put("name", "Jane");
//            request.put("surname", "Doe");
//            request.put("type", "Savings");
            request.put("action", "card");
            request.put("admin", "Admin");
            request.put("data", data);

            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(json.toJson(request));
            output.flush();

            while (!socket.isClosed()) {
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                Object object = inputStream.readObject();
                System.out.println(object.toString());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
