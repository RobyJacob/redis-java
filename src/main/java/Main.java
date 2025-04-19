import java.io.IOException;
import org.apache.commons.cli.*;

public class Main {

    public static void main(String[] args) {
        Server server = null;

        try {
            Options option = new Options();
            option.addOption("port", "port", true, "port");
            option.addOption("replicaof", "replicaof", true, "replicaOf");

            CommandLineParser parser = new DefaultParser(false);
            CommandLine cmd = parser.parse(option, args);

            Config config = new Config();
            
            if (cmd.hasOption("port")) config.setPort(Integer.parseInt(cmd.getOptionValue("port")));

            if (cmd.hasOption("replicaof")) {
                String[] masterAddr = cmd.getOptionValue("replicaof").split(" ");

                config.setMasterHost(masterAddr[0] == "localhost" ? "127.0.0.1" : masterAddr[0]);
                config.setMasterPort(Integer.valueOf(masterAddr[1]));
                config.setMaster(false);

                server = new ReplicaServer(config);
            } else server = new MasterServer(config);

            server.listen();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                if (server != null) server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
