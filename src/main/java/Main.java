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

            ServerConfig serverConfig = new ServerConfig();

            if (cmd.hasOption("port")) serverConfig.setPort(Integer.parseInt(cmd.getOptionValue("port")));

            if (cmd.hasOption("replicaof")) serverConfig.setMaster(false);

            server = new Server(serverConfig);

            server.listen();
        } catch (IOException | ParseException e) {
            System.out.println("Exception: " + e.getMessage());
        } finally {
            try {
                if (server != null) {
                    server.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
