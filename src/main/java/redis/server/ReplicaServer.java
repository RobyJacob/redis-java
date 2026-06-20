package redis.server;

import redis.command.CommandRegistry;
import redis.config.ServerConfig;
import redis.protocol.RespParser;
import redis.protocol.RespSerializer;
import redis.storage.DataStore;
import redis.storage.InMemoryDataStore;
import redis.util.ExecutorProvider;
import redis.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ReplicaServer implements Server {
    private final ServerSocket serverSocket;
    private volatile Socket masterSocket;
    private final ServerConfig config;
    private final ExecutorService executor;
    private final DataStore dataStore;
    private final CommandRegistry commandRegistry;

    public ReplicaServer(ServerConfig config) throws IOException {
        this.config = config;
        this.dataStore = new InMemoryDataStore();
        this.commandRegistry = new CommandRegistry();
        this.executor = ExecutorProvider.get();
        this.serverSocket = new ServerSocket(config.getPort());
        executor.execute(this::connectToMaster);
    }

    private void connectToMaster() {
        try {
            masterSocket = new Socket();
            masterSocket.connect(new InetSocketAddress(config.getMasterHost(), config.getMasterPort()));
            masterSocket.setReuseAddress(true);

            InputStream in = masterSocket.getInputStream();
            OutputStream out = masterSocket.getOutputStream();

            boolean ok = performHandshake(in, out);
            config.setInitialBoot(false);

            if (ok) {
                listenToMaster(in);
            } else {
                System.err.println("Handshake with master failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean performHandshake(InputStream in, OutputStream out) throws IOException {
        RespParser parser = new RespParser();

        List<String> psyncArgs = config.isInitialBoot()
                ? List.of("PSYNC", "?", "-1")
                : List.of("PSYNC", config.getReplicationId(), String.valueOf(config.getReplicationOffset()));

        return sendAndExpect(out, in, parser, RespSerializer.array(List.of("PING")), "PONG")
                && sendAndExpect(out, in, parser,
                        RespSerializer.array(List.of("REPLCONF", "listening-port", String.valueOf(config.getPort()))), "OK")
                && sendAndExpect(out, in, parser,
                        RespSerializer.array(List.of("REPLCONF", "capa", "psync2")), "OK")
                && sendAndExpect(out, in, parser,
                        RespSerializer.array(psyncArgs), "FULLRESYNC")
                && receiveRdbFile(in);
    }

    private boolean sendAndExpect(OutputStream out, InputStream in,
                                   RespParser parser, String message, String expected) throws IOException {
        out.write(message.getBytes());
        out.flush();
        parser.reset();

        String line;
        while ((line = StreamUtils.readLine(in)) != null) {
            System.out.println("Handshake response: " + line);
            Optional<List<String>> result = parser.feed(line);
            if (result.isPresent()) {
                return String.join(" ", result.get()).contains(expected);
            }
        }
        return false;
    }

    private boolean receiveRdbFile(InputStream in) throws IOException {
        String header = StreamUtils.readLine(in);
        if (header == null || !header.startsWith("$")) return false;

        int size = Integer.parseInt(header.substring(1));
        byte[] rdb = in.readNBytes(size);
        System.out.println("Received RDB file: " + rdb.length + " bytes");
        return rdb.length == size;
    }

    private void listenToMaster(InputStream in) throws IOException {
        RespParser parser = new RespParser();
        String line;

        while ((line = StreamUtils.readLine(in)) != null) {
            System.out.println("Master sent: " + line);
            Optional<List<String>> result = parser.feed(line);
            if (result.isPresent()) {
                commandRegistry.execute(result.get(), dataStore, config);
            }
        }
    }

    @Override
    public void listen() throws IOException {
        System.out.println("Replica server listening on port " + config.getPort());
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
                System.out.println("Client sent: " + line);
                Optional<List<String>> result = parser.feed(line);
                if (result.isPresent()) {
                    String response = commandRegistry.execute(result.get(), dataStore, config);
                    out.write(response.getBytes());
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (masterSocket != null) masterSocket.close();
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
