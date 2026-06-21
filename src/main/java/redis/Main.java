package redis;

import org.apache.commons.cli.*;
import redis.config.ServerConfig;
import redis.server.MasterServer;
import redis.server.ReplicaServer;
import redis.server.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = null;
        try {
            Options options = new Options();
            options.addOption("port", "port", true, "Port to listen on");
            options.addOption("replicaof", "replicaof", true, "Master host and port (e.g. 'localhost 6380')");

            CommandLineParser parser = new DefaultParser(false);
            CommandLine cmd = parser.parse(options, args);

            ServerConfig.Builder configBuilder = ServerConfig.builder();

            if (cmd.hasOption("port")) {
                configBuilder.port(Integer.parseInt(cmd.getOptionValue("port")));
            }

            if (cmd.hasOption("replicaof")) {
                String[] parts = cmd.getOptionValue("replicaof").split(" ");
                String masterHost = "localhost".equals(parts[0]) ? "127.0.0.1" : parts[0];
                configBuilder.master(false)
                             .masterHost(masterHost)
                             .masterPort(Integer.parseInt(parts[1]));
            }

            ServerConfig config = configBuilder.build();
            server = config.isMaster() ? new MasterServer(config) : new ReplicaServer(config);
            server.listen();

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try { server.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }
}
