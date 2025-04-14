import java.io.IOException;
import java.util.Map;

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

            if (cmd.hasOption("port")) server = new Server(Integer.parseInt(cmd.getOptionValue("port")));
            else server = new Server();

            if (cmd.hasOption("replicaof")) {
                String[] masterAddr = cmd.getOptionValue("replicaof").split(" ");

                Server.setMaster(Map.of(masterAddr[0], masterAddr[1]));
            }

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
