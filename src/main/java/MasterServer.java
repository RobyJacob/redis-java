import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Base64;

public class MasterServer implements Server {
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private ExecutorService backgroundService;
    private Config config;
    private Data data;

    MasterServer(Config config) throws IOException {
        this.config = config;
        initServer();
    }

    private void initServer() throws IOException {
        serverSocket = new ServerSocket(config.getPort());
        serverSocket.setReuseAddress(true);
        clientThreadPool = Executors.newFixedThreadPool(config.getNumThreads());
        backgroundService = Executors.newFixedThreadPool(config.getNumThreads());
        data = new Data();

        config.setReplicationId(Utility.generateReplicationId());
        config.setReplicationOffset(0);

        System.out.println("Server started successfully");
    }

    @Override
    public void listen() throws IOException {
        while (true) {
            System.out.println("Server started listening on port " + config.getPort());
            Socket clientSocket = serverSocket.accept();

            Connection connection = new Connection(clientSocket);

            startBackgroundService(connection);

            this.clientThreadPool.execute(() -> {
                try {
                    RespParser parser = new RespParser(config, data);
                    var in = connection.getSocket().getInputStream();
                    var out = connection.getSocket().getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String input;

                    while ((input = reader.readLine()) != null) {
                        System.out.println("Received input: " + input);

                        if (parser.feed(input)) {
                            var parserResponse = parser.getResult();
                            out.write(parserResponse.getBytes());
                            out.flush();

                            if (parserResponse.contains("FULLRESYNC")) {
                                connection.setSyncRequired(true);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            });
        }
    }

    @Override
    public void close() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
        if (clientThreadPool != null)
            clientThreadPool.shutdown();
        if (backgroundService != null)
            backgroundService.shutdown();
    }

    private void startBackgroundService(Connection connection) {
        backgroundService.execute(() -> {
            while (true) {
                try {
                    connection.waitForSync();
        
                    byte[] data = Base64.getDecoder().decode(
                            "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==");
                    
                    var out = connection.getSocket().getOutputStream();
                    out.write(("$" + data.length + "\r\n").getBytes());
                    out.write(data);
                    out.flush();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e.getMessage());
                } finally {
                    connection.setSyncRequired(false);
                }
            }
        });
    }
}
