import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket = null;
    private int port = 6379;
    private ExecutorService threadPool = null;
    private final int NUM_THREADS = 5;

    Server() throws IOException {
        this.initServer();
    }

    Server(int port) throws IOException {
        this.port = port;
        this.initServer();
    }

    private void initServer() throws IOException {
        this.serverSocket = new ServerSocket(this.port);
        this.serverSocket.setReuseAddress(true);
        this.threadPool = Executors.newFixedThreadPool(this.NUM_THREADS);
        System.out.println("Server started successfully");
    }

    public void respondToClient(Socket clientSocket) throws IOException {
        String response = "+PONG\r\n";

        clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
    }

    public void listen() throws IOException {
        while (true) {
            Socket clientSocket = this.serverSocket.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            this.threadPool.execute(() -> {
                try {
                    System.out.println("Server started listening on port " + this.port);
                    String input = "";
                    while ((input = reader.readLine()) != null) {
                        System.out.println("Received input: " + input);
                        if (input.contains("ping"))
                            this.respondToClient(clientSocket);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public  void close() throws IOException {
        if (this.serverSocket != null) this.serverSocket.close();
        if (this.threadPool != null) this.threadPool.shutdown();
    }
}
