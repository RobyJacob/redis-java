import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket = null;
    private ExecutorService threadPool = null;
    private ServerConfig serverConfig = new ServerConfig();

    Server(ServerConfig config) throws IOException {
        serverConfig = config;
        initServer();
    }

    private void initServer() throws IOException {
        serverSocket = new ServerSocket(serverConfig.getPort());
        serverSocket.setReuseAddress(true);
        threadPool = Executors.newFixedThreadPool(serverConfig.getNumThreads());

        if (serverConfig.isMaster()) {
            serverConfig.setReplicationId(generateRandomReplicationId());
            serverConfig.setReplicationOffset(0);
        } else pollMaster();

        System.out.println("Server started successfully");
    }

    public void respondToClient(Socket clientSocket, String response) throws IOException {
        clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
    }

    public void listen() throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            RespParser parser = new RespParser(serverConfig);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            this.threadPool.execute(() -> {
                try {
                    System.out.println("Server started listening on port " + serverConfig.getPort());
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

    private String generateRandomReplicationId() {
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final int STRING_LENGTH = 40;
        final SecureRandom random = new SecureRandom();

        StringBuilder builder = new StringBuilder(STRING_LENGTH);

        for (int i = 0; i < STRING_LENGTH; i++) {
            int randomIdx = random.nextInt(CHARACTERS.length());
            builder.append(CHARACTERS.charAt(randomIdx));
        }

        return builder.toString();
    }

    private void pollMaster() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverConfig.getMasterHost(), serverConfig.getMasterPort()));

            var out = socket.getOutputStream();
            var in = socket.getInputStream();

            out.write("*1\r\n$4\r\nPING\r\n".getBytes());

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String respone;
            while ((respone = reader.readLine()) != null) {
                if (respone.contains("PONG")) break;
                throw new RuntimeException("Master server is not active/healthy");
            }
        }
    }
}
