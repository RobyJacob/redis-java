import java.io.IOException;

public interface Server {
    public void listen() throws IOException;

    public void close() throws IOException;
}
