import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Getter 
@Setter
public class ServerConfig {
    private boolean isMaster = true;
    private String replicationId;
    private int replicationOffset;
    private String host = "localhost";
    private int port = 6379;
    private int numThreads = 5;
    private String masterHost = "localhost";
    private int masterPort = 6379;
    private List<ServerConfig> replicas;

    ServerConfig() {
        replicas = new ArrayList<>();
    }

    public void setReplicas(List<ServerConfig> replicas) {
        this.replicas.addAll(replicas);
    }
}
