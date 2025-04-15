import lombok.Getter;
import lombok.Setter;

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
}
