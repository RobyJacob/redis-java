import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket = null;
    private int port = 6379;
    private ExecutorService threadPool = null;
    private int num_threads = 5;
    private static Map<String, String> masterAddr = new HashMap<>();

    Server() throws IOException {
        initServer();
    }

    Server(int port, int num_threads) throws IOException {
        this.port = port;
        this.num_threads = num_threads;
        initServer();
    }

    Server(int port) throws IOException {
        this.port = port;
        initServer();
    }

    private void initServer() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        threadPool = Executors.newFixedThreadPool(this.num_threads);
        System.out.println("Server started successfully");
    }

    public void respondToClient(Socket clientSocket, String response) throws IOException {
        clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
    }

    public void listen() throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            RespParser parser = new RespParser();
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            this.threadPool.execute(() -> {
                try {
                    System.out.println("Server started listening on port " + port);
                    String input;

                    while ((input = reader.readLine()) != null) {
                        System.out.println("Received input: " + input);

                        if (parser.feed(input)) {
                            respondToClient(clientSocket, parser.getResult());
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public void close() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
        if (threadPool != null)
            threadPool.shutdown();
    }

    public static void setMaster(Map<String, String> masterAddr) {
        Server.masterAddr.putAll(masterAddr);
    }

    public static Map<String, String> getMaster() {
        return masterAddr;
    }
}
