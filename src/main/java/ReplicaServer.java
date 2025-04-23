import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ReplicaServer implements Server {
    private Socket masterSocket;
    private ServerSocket serverSocket;
    private Config config;
    private ExecutorService executor;
    private Data data;
    private RespParser masterParser;

    ReplicaServer(Config config) throws IOException {
        this.config = config;

        masterSocket = new Socket();
        masterSocket.connect(new InetSocketAddress(this.config.getMasterHost(), this.config.getMasterPort()));
        masterSocket.setReuseAddress(true);

        serverSocket = new ServerSocket(config.getPort());

        data = new Data();

        executor = SharedExecutor.getExecutor();

        masterParser = new RespParser(config, data);

        executor.execute(() -> {
            pollMaster();
            this.config.setInitialBoot(false);
            listenToMaster();
        });
    }

    private boolean sendHandshake(Socket masterSocket, String message, String expectedResponse) {
        boolean result = false;

        try {

            var out = masterSocket.getOutputStream();
            var in = masterSocket.getInputStream();

            out.write(message.getBytes());
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String response;

            masterParser.reset();

            while ((response = reader.readLine()) != null) {
                System.out.println("Replica received handshake response: %s".formatted(response));

                if (!masterParser.feed(response) && !masterParser.getParsedValues().isEmpty()) {
                    String parsedValue = masterParser.getParsedValues().get(0);
                    if (parsedValue.contains(expectedResponse)) {
                        result = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private boolean checkIfRdbFileReceived() {
        try {
            var in = masterSocket.getInputStream();

            byte[] response = in.readNBytes(3);
            String responseString = new String(response);
            if (!responseString.isEmpty() && responseString.startsWith("$")) {
                int rdbFileSize = Integer.parseInt(responseString.substring(1));
                System.out.println("RDB file size: " + rdbFileSize);

                response = in.readNBytes(rdbFileSize);

                if (response.length == rdbFileSize) {
                    System.out.println("Successfully received RDB file");
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void pollMaster() {
        String psyncCommand = "PSYNC ? -1";

        if (!config.isInitialBoot()) {
            psyncCommand = "PSYNC %s %s".formatted(config.getReplicationId(),
                    config.getReplicationOffset());
        }

        boolean isHandshakeSuccessful = sendHandshake(masterSocket,
                Utility.convertToResp("PING", RespParser.Operand.ARRAY), "PONG")
                && sendHandshake(masterSocket,
                        Utility.convertToResp("REPLCONF listening-port %s".formatted(config.getPort()),
                                RespParser.Operand.ARRAY),
                        "OK")
                && sendHandshake(masterSocket, Utility.convertToResp("REPLCONF capa psync2", RespParser.Operand.ARRAY),
                        "OK")
                && sendHandshake(masterSocket, Utility.convertToResp(psyncCommand, RespParser.Operand.ARRAY),
                        "FULLRESYNC")
                && checkIfRdbFileReceived();

        if (!isHandshakeSuccessful)
            System.err.println("Failed to establish connection with master");
    }

    private void listenToMaster() {
        try {
            var in = masterSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String response;

            while (in.available() > 0) {
                in.read();
            }

            masterParser.reset();

            while ((response = reader.readLine()) != null) {
                System.out.println("Replica received: " + response);

                if (masterParser.feed(response)) {
                    masterParser.reset();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (masterSocket != null)
            masterSocket.close();
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

    @Override
    public void listen() throws IOException {
        while (true) {
            Socket clienSocket = serverSocket.accept();
            System.out.println("Replica server started listening on port " + config.getPort());

            executor.execute(() -> {
                try {
                    var in = clienSocket.getInputStream();
                    var out = clienSocket.getOutputStream();
                    RespParser clientParser = new RespParser(config, data);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String input;

                    while ((input = reader.readLine()) != null) {
                        System.out.println("Received input: " + input);

                        if (clientParser.feed(input)) {
                            var parserResponse = clientParser.getResult();
                            out.write(parserResponse.getBytes());
                            out.flush();
                            clientParser.reset();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
