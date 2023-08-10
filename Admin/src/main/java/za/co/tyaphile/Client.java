package za.co.tyaphile;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 5555;
    public static void main(String... args) {
        Scanner scan = new Scanner(System.in);
        try {
            Socket socket = new Socket(HOST, PORT);

            Map<String, Object> request = new HashMap<>();
            request.put("admin", "Admin");
            request.put("name", "John");
            request.put("surname", "Doe");
            request.put("type", "Savings");
            request.put("action", "open");

            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(request);
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
