package redis.server;

import redis.protocol.RespParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class ConnectionContext {
    private static final int READ_BUFFER_SIZE = 8192;

    final SocketChannel channel;
    private SelectionKey key;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
    private final Deque<ByteBuffer> writeQueue = new ArrayDeque<>();
    private final StringBuilder lineAccumulator = new StringBuilder();
    private final RespParser parser = new RespParser();

    public ConnectionContext(SocketChannel channel) {
        this.channel = channel;
    }

    public void setKey(SelectionKey key) {
        this.key = key;
    }

    public List<List<String>> read() throws IOException {
        readBuffer.clear();
        int n = channel.read(readBuffer);
        if (n == -1) throw new IOException("Connection closed by peer");

        readBuffer.flip();
        List<List<String>> commands = new ArrayList<>();

        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b == '\r') continue;
            if (b == '\n') {
                Optional<List<String>> result = parser.feed(lineAccumulator.toString());
                lineAccumulator.setLength(0);
                result.ifPresent(commands::add);
            } else {
                lineAccumulator.append((char) b);
            }
        }

        return commands;
    }

    public void enqueueWrite(String data) {
        enqueueWrite(data.getBytes());
    }

    public void enqueueWrite(byte[] data) {
        writeQueue.add(ByteBuffer.wrap(data));
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    public void flushWrites() throws IOException {
        while (!writeQueue.isEmpty()) {
            ByteBuffer buf = writeQueue.peek();
            channel.write(buf);
            if (buf.hasRemaining()) return; // partial write — keep OP_WRITE set
            writeQueue.poll();
        }
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    public boolean hasPendingWrites() {
        return !writeQueue.isEmpty();
    }
}
