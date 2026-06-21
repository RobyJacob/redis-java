package redis.server;

import redis.command.CommandRegistry;
import redis.command.CommandType;
import redis.config.ServerConfig;
import redis.replication.ReplicationManager;
import redis.storage.DataStore;
import redis.storage.InMemoryDataStore;
import redis.util.ReplicationIdGenerator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

public class MasterServer implements Server {
    private final ServerSocketChannel serverChannel;
    private final EventLoop eventLoop;
    private final ServerConfig config;
    private final DataStore dataStore;
    private final CommandRegistry commandRegistry;
    private final ReplicationManager replicationManager;

    public MasterServer(ServerConfig config) throws IOException {
        this.config = config;
        this.dataStore = new InMemoryDataStore();
        this.commandRegistry = new CommandRegistry();
        this.replicationManager = new ReplicationManager();
        this.eventLoop = new EventLoop();

        config.setReplicationId(ReplicationIdGenerator.generate());
        config.setReplicationOffset(0);

        serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.bind(new InetSocketAddress(config.getPort()));

        eventLoop.register(serverChannel, SelectionKey.OP_ACCEPT, this::acceptClient);

        System.out.println("Master server started on port " + config.getPort());
    }

    private void acceptClient(SelectionKey key) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) return;

        System.out.println("Accepted: " + clientChannel.getRemoteAddress());
        clientChannel.configureBlocking(false);

        ConnectionContext ctx = new ConnectionContext(clientChannel);
        SelectionKey clientKey = eventLoop.register(clientChannel, SelectionKey.OP_READ,
                (k) -> handleClient(k, ctx));
        ctx.setKey(clientKey);
    }

    private void handleClient(SelectionKey key, ConnectionContext ctx) throws IOException {
        if (key.isReadable()) {
            List<List<String>> commands = ctx.read();
            for (List<String> tokens : commands) {
                String commandName = tokens.get(0).toLowerCase();
                String response = commandRegistry.execute(tokens, dataStore, config);

                if ("psync".equals(commandName)) {
                    replicationManager.addReplica(ctx);
                }

                if (replicationManager.hasReplicas() && commandRegistry.typeOf(commandName) == CommandType.WRITE) {
                    replicationManager.broadcastWrite(tokens);
                }

                ctx.enqueueWrite(response);

                if (response.contains("FULLRESYNC")) {
                    replicationManager.sendFullResync(ctx);
                }
            }
        }

        if (ctx.hasPendingWrites()) {
            ctx.flushWrites();
        }
    }

    @Override
    public void listen() throws IOException {
        eventLoop.run();
    }

    @Override
    public void close() throws IOException {
        eventLoop.stop();
        serverChannel.close();
    }
}
