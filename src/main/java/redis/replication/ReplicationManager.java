package redis.replication;

import redis.protocol.RespSerializer;
import redis.server.ConnectionContext;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Tracks replica connections and broadcasts write commands.
 * All methods are called on the single event-loop thread — no locking needed.
 */
public class ReplicationManager {
    private static final String EMPTY_RDB_BASE64 =
            "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";

    private final List<ConnectionContext> replicas = new ArrayList<>();

    public void addReplica(ConnectionContext ctx) {
        replicas.add(ctx);
    }

    public void removeReplica(ConnectionContext ctx) {
        replicas.remove(ctx);
    }

    public boolean hasReplicas() {
        return !replicas.isEmpty();
    }

    public void broadcastWrite(List<String> tokens) {
        byte[] resp = RespSerializer.array(tokens).getBytes();
        List<ConnectionContext> dead = new ArrayList<>();
        for (ConnectionContext replica : replicas) {
            try {
                replica.enqueueWrite(resp.clone());
            } catch (Exception e) {
                System.err.println("Replica write error: " + e.getMessage());
                dead.add(replica);
            }
        }
        replicas.removeAll(dead);
    }

    public void sendFullResync(ConnectionContext ctx) {
        byte[] rdbData = Base64.getDecoder().decode(EMPTY_RDB_BASE64);
        ctx.enqueueWrite(("$" + rdbData.length + "\r\n").getBytes());
        ctx.enqueueWrite(rdbData);
    }
}
