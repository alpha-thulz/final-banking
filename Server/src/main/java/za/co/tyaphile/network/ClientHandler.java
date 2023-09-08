package za.co.tyaphile.network;

import za.co.tyaphile.BankServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        System.out.println("Connection established on " + socket.getPort());
    }

    @Override
    public void run() {
        while (true) {
            try {
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                Object object = input.readObject();
                sendMessage(BankServer.processRequest(object.toString()));
            } catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
                printStackTrace("Class error", e);
            }
        }
    }


    private void sendMessage(Object response) throws IOException {
        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        output.writeObject(response);
        output.flush();
    }

    private void printStackTrace(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        for (StackTraceElement ste:e.getStackTrace()) {
            System.err.println(ste);
        }
    }
}
