package redis.server;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class EventLoop {
    private final Selector selector;
    private volatile boolean running;

    public EventLoop() throws IOException {
        selector = Selector.open();
    }

    public SelectionKey register(SelectableChannel channel, int ops, ChannelHandler handler) throws IOException {
        channel.configureBlocking(false);
        return channel.register(selector, ops, handler);
    }

    public void run() {
        running = true;
        while (running) {
            try {
                selector.select();
            } catch (IOException e) {
                if (running) e.printStackTrace();
                break;
            }
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (!key.isValid()) continue;
                ChannelHandler handler = (ChannelHandler) key.attachment();
                try {
                    handler.handle(key);
                } catch (IOException e) {
                    System.err.println("Channel closed: " + e.getMessage());
                    key.cancel();
                    try { key.channel().close(); } catch (IOException ignored) {}
                }
            }
        }
    }

    public void stop() {
        running = false;
        selector.wakeup();
    }
}
