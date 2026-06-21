package redis.config;

public class ServerConfig {
    private final String host;
    private final int port;
    private final boolean master;
    private final String masterHost;
    private final int masterPort;
    private boolean initialBoot;
    private String replicationId;
    private int replicationOffset;

    private ServerConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.master = builder.master;
        this.masterHost = builder.masterHost;
        this.masterPort = builder.masterPort;
        this.initialBoot = true;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isMaster() { return master; }
    public String getMasterHost() { return masterHost; }
    public int getMasterPort() { return masterPort; }
    public boolean isInitialBoot() { return initialBoot; }
    public String getReplicationId() { return replicationId; }
    public int getReplicationOffset() { return replicationOffset; }

    public void setInitialBoot(boolean initialBoot) { this.initialBoot = initialBoot; }
    public void setReplicationId(String replicationId) { this.replicationId = replicationId; }
    public void setReplicationOffset(int replicationOffset) { this.replicationOffset = replicationOffset; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String host = "0.0.0.0";
        private int port = 6379;
        private boolean master = true;
        private String masterHost;
        private int masterPort;

        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder master(boolean master) { this.master = master; return this; }
        public Builder masterHost(String masterHost) { this.masterHost = masterHost; return this; }
        public Builder masterPort(int masterPort) { this.masterPort = masterPort; return this; }

        public ServerConfig build() { return new ServerConfig(this); }
    }
}
