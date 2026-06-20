package redis.server;

import redis.command.CommandRegistry;
import redis.command.CommandType;
import redis.config.ServerConfig;
import redis.protocol.RespParser;
import redis.replication.ReplicationManager;
import redis.storage.DataStore;
import redis.storage.InMemoryDataStore;
import redis.util.ExecutorProvider;
import redis.util.ReplicationIdGenerator;
import redis.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MasterServer implements Server {
    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final ServerConfig config;
    private final DataStore dataStore;
    private final CommandRegistry commandRegistry;
    private final ReplicationManager replicationManager;

    public MasterServer(ServerConfig config) throws IOException {
        this.config = config;
        this.dataStore = new InMemoryDataStore();
        this.commandRegistry = new CommandRegistry();
        this.replicationManager = new ReplicationManager();
        this.executor = ExecutorProvider.get();
        this.serverSocket = new ServerSocket(config.getPort());
        this.serverSocket.setReuseAddress(true);

        config.setReplicationId(ReplicationIdGenerator.generate());
        config.setReplicationOffset(0);

        System.out.println("Master server started on port " + config.getPort());
    }

    @Override
    public void listen() throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.execute(() -> handleClient(clientSocket));
        }
    }

    private void handleClient(Socket socket) {
        try {
            RespParser parser = new RespParser();
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            String line;

            while ((line = StreamUtils.readLine(in)) != null) {
                System.out.println("Received: " + line);
                Optional<List<String>> result = parser.feed(line);

                if (result.isPresent()) {
                    List<String> tokens = result.get();
                    String commandName = tokens.get(0).toLowerCase();

                    String response = commandRegistry.execute(tokens, dataStore, config);

                    if ("psync".equals(commandName)) {
                        replicationManager.addReplica(socket);
                    }

                    if (replicationManager.hasReplicas() && commandRegistry.typeOf(commandName) == CommandType.WRITE) {
                        replicationManager.enqueueWrite(tokens);
                    }

                    out.write(response.getBytes());
                    out.flush();

                    if (response.contains("FULLRESYNC")) {
                        replicationManager.sendFullResync(socket);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (serverSocket != null) serverSocket.close();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
