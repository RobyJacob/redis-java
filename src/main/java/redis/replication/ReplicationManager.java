package redis.replication;

import redis.protocol.RespSerializer;
import redis.util.ExecutorProvider;

import java.io.IOException;
import java.net.Socket;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ReplicationManager {
    private static final String EMPTY_RDB_BASE64 =
            "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";

    private final List<Socket> replicas = new CopyOnWriteArrayList<>();
    private final BlockingQueue<List<String>> writeQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = ExecutorProvider.get();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public void addReplica(Socket socket) {
        replicas.add(socket);
        if (started.compareAndSet(false, true)) {
            startReplicationLoop();
        }
    }

    public void removeReplica(Socket socket) {
        replicas.remove(socket);
    }

    public boolean hasReplicas() {
        return !replicas.isEmpty();
    }

    public void enqueueWrite(List<String> tokens) {
        writeQueue.offer(tokens);
    }

    public void sendFullResync(Socket socket) {
        executor.execute(() -> {
            try {
                byte[] rdbData = Base64.getDecoder().decode(EMPTY_RDB_BASE64);
                var out = socket.getOutputStream();
                out.write(("$" + rdbData.length + "\r\n").getBytes());
                out.write(rdbData);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                removeReplica(socket);
            }
        });
    }

    private void startReplicationLoop() {
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<String> tokens = writeQueue.poll(5, TimeUnit.SECONDS);
                    if (tokens == null) continue;

                    String resp = RespSerializer.array(tokens);
                    broadcast(replica -> {
                        try {
                            var out = replica.getOutputStream();
                            out.write(resp.getBytes());
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                            removeReplica(replica);
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void broadcast(Consumer<Socket> action) {
        for (Socket replica : replicas) {
            action.accept(replica);
        }
    }
}
