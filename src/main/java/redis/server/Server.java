package redis.server;

import java.io.IOException;

public interface Server {
    void listen() throws IOException;
    void close() throws IOException;
}
