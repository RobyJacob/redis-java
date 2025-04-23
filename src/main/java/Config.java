import lombok.Getter;
import lombok.Setter;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class Config {
    private boolean isMaster;
    private String host;
    private int port;
    private String masterHost;
    private int masterPort;
    private boolean isInitialBoot;
    private String replicationId;
    private int replicationOffset;
    private static int threadPoolSize;
    private List<Socket> replicas;
    
    static {
        threadPoolSize = Runtime.getRuntime().availableProcessors();
    }

    Config() {
        host = "0.0.0.0";
        port = 6379;
        isInitialBoot = true;
        isMaster = true;
        replicas = new CopyOnWriteArrayList<>();
    }

    public static int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void addReplica(Socket socket) {
        replicas.add(socket);
    }
    
    public void removeReplica(Socket socket) {
        replicas.remove(socket);
    }

    public static void setThreadPoolSize(int threadPoolSize) {
        Config.threadPoolSize = threadPoolSize;
    }
}
