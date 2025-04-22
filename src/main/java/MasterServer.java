import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class MasterServer implements Server {
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private Config config;
    private Data data;
    private BlockingQueue<String> replicationQueue;
    private boolean replicationStarted;

    MasterServer(Config config) throws IOException {
        this.config = config;
        initServer();
    }

    private void initServer() throws IOException {
        serverSocket = new ServerSocket(config.getPort());
        serverSocket.setReuseAddress(true);
        data = new Data();

        executor = SharedExecutor.getExecutor();
        replicationQueue = new LinkedBlockingQueue<>();

        config.setReplicationId(Utility.generateReplicationId());
        config.setReplicationOffset(0);

        replicationStarted = false;

        System.out.println("Server started successfully");
    }

    @Override
    public void listen() throws IOException {
        while (true) {
            System.out.println("Server started listening on port " + config.getPort());
            Socket clientSocket = serverSocket.accept();

            executor.execute(() -> {
                try {
                    RespParser parser = new RespParser(config, data);
                    var in = clientSocket.getInputStream();
                    var out = clientSocket.getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String input;

                    synchronized (this) {
                        if (!replicationStarted && !config.getReplicas().isEmpty()) {
                            startReplication();
                            // SharedExecutor.getScheduledExecutor().scheduleAtFixedRate(() -> pollReplicaConnections(), 0, 10, TimeUnit.SECONDS);
                            replicationStarted = true;
                        }
                    }

                    while ((input = reader.readLine()) != null) {
                        System.out.println("Received input: " + input);

                        if (parser.feed(input)) {
                            if (!config.getReplicas().isEmpty()) {
                                if ("write".equalsIgnoreCase(parser.commandType())) {
                                    var parsedValues = parser.getParsedValues();
                                    replicationQueue.offer(String.join(" ", parsedValues));
                                }
                            }

                            var parserResponse = parser.getResult();

                            out.write(parserResponse.getBytes());
                            out.flush();

                            parser.reset();

                            if (parserResponse.contains("FULLRESYNC")) {
                                startFullResync(clientSocket);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void close() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void startFullResync(Socket connection) {
        executor.execute(() -> {
            try {
                config.addReplica(connection);

                byte[] data = Base64.getDecoder().decode(
                        "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==");

                var out = connection.getOutputStream();

                out.write(("$" + data.length + "\r\n").getBytes());
                out.write(data);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                config.removeReplica(connection);
            }
        });
    }

    private void startReplication() {
        executor.execute(() -> {
            while (true) {
                try {
                    String data = replicationQueue.poll(5, TimeUnit.SECONDS);
                    if (data == null) {
                        continue;
                    }

                    String respData = Utility.convertToResp(data, RespParser.Operand.ARRAY);

                    iterateReplicas((replica) -> {
                        try {
                            var out = replica.getOutputStream();
                            out.write(respData.getBytes());
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                            config.removeReplica(replica);
                        }
                    });
                } catch (InterruptedException e) { 
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void iterateReplicas(Consumer<Socket> action) {
        for (Socket replica : config.getReplicas()) {
            action.accept(replica);
        }
    }

    private void pollReplicaConnections() {
        iterateReplicas(replica -> {
            try {
                var out = replica.getOutputStream();
                out.write(Utility.convertToResp("PING", RespParser.Operand.ARRAY).getBytes());
                out.flush();
            } catch (IOException e) {
                System.err.println("Removing disconnected replica: " + replica.getRemoteSocketAddress());
                config.removeReplica(replica);
            }
        });
    }
}
