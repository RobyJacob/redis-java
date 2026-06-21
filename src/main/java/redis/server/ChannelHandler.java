package redis.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;

@FunctionalInterface
public interface ChannelHandler {
    void handle(SelectionKey key) throws IOException;
}
