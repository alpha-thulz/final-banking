package za.co.tyaphile.network;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Connector implements Runnable, Executor {
    private ServerSocket ss;
    private boolean running;

    public Connector(final int PORT) throws IOException {
        ss = new ServerSocket(PORT);
        running = true;
    }

    @Override
    public void run() {
        while(running) {
            try {
                execute(new ClientHandler(ss.accept()));
            } catch (IOException e) {
                e.getStackTrace();
                running = false;
            }
        }
    }

    @Override
    public void execute(@NotNull Runnable command) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(command);
    }
}
