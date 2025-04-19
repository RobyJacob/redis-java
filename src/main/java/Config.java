import lombok.Getter;
import lombok.Setter;

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
    private int numThreads;
    
    Config() {
        host = "0.0.0.0";
        port = 6379;
        numThreads = 3;
        isInitialBoot = true;
        isMaster = true;
    }
}
