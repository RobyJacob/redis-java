package redis.server;

import redis.command.CommandRegistry;
import redis.config.ServerConfig;
import redis.storage.DataStore;
import redis.storage.InMemoryDataStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

public class ReplicaServer implements Server {
    private final ServerSocketChannel serverChannel;
    private final EventLoop eventLoop;
    private final ServerConfig config;
    private final DataStore dataStore;
    private final CommandRegistry commandRegistry;

    public ReplicaServer(ServerConfig config) throws IOException {
        this.config = config;
        this.dataStore = new InMemoryDataStore();
        this.commandRegistry = new CommandRegistry();
        this.eventLoop = new EventLoop();

        serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.bind(new InetSocketAddress(config.getPort()));
        eventLoop.register(serverChannel, SelectionKey.OP_ACCEPT, this::acceptClient);

        connectToMaster();

        System.out.println("Replica server started on port " + config.getPort());
    }

    private void connectToMaster() throws IOException {
        SocketChannel masterChannel = SocketChannel.open();
        masterChannel.configureBlocking(false);
        masterChannel.connect(new InetSocketAddress(config.getMasterHost(), config.getMasterPort()));

        ReplicaContext ctx = new ReplicaContext(masterChannel, config, dataStore, commandRegistry);
        eventLoop.register(masterChannel, SelectionKey.OP_CONNECT, ctx);
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
                String response = commandRegistry.execute(tokens, dataStore, config);
                ctx.enqueueWrite(response);
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
