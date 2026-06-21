package redis.server;

import redis.command.CommandRegistry;
import redis.config.ServerConfig;
import redis.protocol.RespParser;
import redis.protocol.RespSerializer;
import redis.storage.DataStore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Optional;

/**
 * Manages the async connection from a replica to its master.
 * Drives the PSYNC handshake via a state machine, then applies
 * replicated write commands from the master to the local data store.
 */
public class ReplicaContext implements ChannelHandler {

    private enum Phase {
        SEND_PING, AWAIT_PONG,
        SEND_REPLCONF_PORT, AWAIT_REPLCONF_PORT_OK,
        SEND_REPLCONF_CAPA, AWAIT_REPLCONF_CAPA_OK,
        SEND_PSYNC, AWAIT_FULLRESYNC,
        AWAIT_RDB_HEADER, AWAIT_RDB_DATA,
        REPLICATE
    }

    private Phase phase = Phase.SEND_PING;

    private final SocketChannel channel;
    private final ServerConfig config;
    private final DataStore dataStore;
    private final CommandRegistry commandRegistry;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private final StringBuilder lineAccumulator = new StringBuilder();
    private final RespParser parser = new RespParser();

    // Used to send handshake messages one at a time, with partial-write support
    private ByteBuffer pendingSend;
    private int rdbSize;
    private int rdbBytesConsumed;

    public ReplicaContext(SocketChannel channel, ServerConfig config,
                          DataStore dataStore, CommandRegistry commandRegistry) {
        this.channel = channel;
        this.config = config;
        this.dataStore = dataStore;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        if (key.isConnectable()) onConnect(key);
        else if (key.isReadable()) onRead(key);
        else if (key.isWritable()) onWrite(key);
    }

    // ── Connection ──────────────────────────────────────────────────────────

    private void onConnect(SelectionKey key) throws IOException {
        if (channel.finishConnect()) {
            System.out.println("Connected to master");
            key.interestOps(SelectionKey.OP_WRITE); // kick off handshake
        }
    }

    // ── Write (handshake send phases) ───────────────────────────────────────

    private void onWrite(SelectionKey key) throws IOException {
        switch (phase) {
            case SEND_PING         -> send(key, RespSerializer.array(List.of("PING")),            Phase.AWAIT_PONG);
            case SEND_REPLCONF_PORT -> send(key,
                    RespSerializer.array(List.of("REPLCONF", "listening-port", String.valueOf(config.getPort()))),
                    Phase.AWAIT_REPLCONF_PORT_OK);
            case SEND_REPLCONF_CAPA -> send(key,
                    RespSerializer.array(List.of("REPLCONF", "capa", "psync2")),
                    Phase.AWAIT_REPLCONF_CAPA_OK);
            case SEND_PSYNC -> {
                List<String> args = config.isInitialBoot()
                        ? List.of("PSYNC", "?", "-1")
                        : List.of("PSYNC", config.getReplicationId(), String.valueOf(config.getReplicationOffset()));
                send(key, RespSerializer.array(args), Phase.AWAIT_FULLRESYNC);
            }
            default -> key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private void send(SelectionKey key, String message, Phase next) throws IOException {
        if (pendingSend == null) pendingSend = ByteBuffer.wrap(message.getBytes());
        channel.write(pendingSend);
        if (!pendingSend.hasRemaining()) {
            pendingSend = null;
            phase = next;
            key.interestOps(SelectionKey.OP_READ);
        }
        // On partial write: stay in same phase, OP_WRITE keeps firing
    }

    // ── Read (handshake await phases + ongoing replication) ─────────────────

    private void onRead(SelectionKey key) throws IOException {
        readBuffer.clear();
        if (channel.read(readBuffer) == -1) throw new IOException("Master disconnected");
        readBuffer.flip();
        processBuffer(key);
    }

    private void processBuffer(SelectionKey key) throws IOException {
        while (readBuffer.hasRemaining()) {
            switch (phase) {
                case AWAIT_PONG -> {
                    if (readLineAndCheck("PONG")) { transitionToSend(key, Phase.SEND_REPLCONF_PORT); return; }
                }
                case AWAIT_REPLCONF_PORT_OK -> {
                    if (readLineAndCheck("OK")) { transitionToSend(key, Phase.SEND_REPLCONF_CAPA); return; }
                }
                case AWAIT_REPLCONF_CAPA_OK -> {
                    if (readLineAndCheck("OK")) { transitionToSend(key, Phase.SEND_PSYNC); return; }
                }
                case AWAIT_FULLRESYNC -> {
                    if (readLineAndCheck("FULLRESYNC")) phase = Phase.AWAIT_RDB_HEADER;
                    // continue loop to process RDB header if buffered
                }
                case AWAIT_RDB_HEADER -> {
                    if (readRdbHeader()) phase = Phase.AWAIT_RDB_DATA;
                    // continue loop to process RDB data if buffered
                }
                case AWAIT_RDB_DATA -> {
                    if (consumeRdbBytes()) {
                        phase = Phase.REPLICATE;
                        config.setInitialBoot(false);
                        parser.reset();
                        System.out.println("Handshake complete — entering replication mode");
                    }
                }
                case REPLICATE -> { applyReplicatedCommands(); return; }
                default -> { return; }
            }
        }
    }

    private void transitionToSend(SelectionKey key, Phase next) {
        phase = next;
        key.interestOps(SelectionKey.OP_WRITE);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private boolean readLineAndCheck(String expected) {
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b == '\r') continue;
            if (b == '\n') {
                Optional<List<String>> result = parser.feed(lineAccumulator.toString());
                lineAccumulator.setLength(0);
                if (result.isPresent()) {
                    return String.join(" ", result.get()).contains(expected);
                }
            } else {
                lineAccumulator.append((char) b);
            }
        }
        return false;
    }

    private boolean readRdbHeader() {
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b == '\r') continue;
            if (b == '\n') {
                String header = lineAccumulator.toString();
                lineAccumulator.setLength(0);
                if (header.startsWith("$")) {
                    rdbSize = Integer.parseInt(header.substring(1));
                    rdbBytesConsumed = 0;
                    System.out.println("Expecting RDB file: " + rdbSize + " bytes");
                    return true;
                }
            } else {
                lineAccumulator.append((char) b);
            }
        }
        return false;
    }

    private boolean consumeRdbBytes() {
        while (readBuffer.hasRemaining() && rdbBytesConsumed < rdbSize) {
            readBuffer.get();
            rdbBytesConsumed++;
        }
        if (rdbBytesConsumed >= rdbSize) {
            System.out.println("RDB file received: " + rdbSize + " bytes");
            return true;
        }
        return false;
    }

    private void applyReplicatedCommands() {
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b == '\r') continue;
            if (b == '\n') {
                Optional<List<String>> result = parser.feed(lineAccumulator.toString());
                lineAccumulator.setLength(0);
                if (result.isPresent()) {
                    System.out.println("Applying replicated command: " + result.get());
                    commandRegistry.execute(result.get(), dataStore, config);
                }
            } else {
                lineAccumulator.append((char) b);
            }
        }
    }
}
