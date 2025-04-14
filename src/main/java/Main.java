import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = null;

        try {
            if (args.length > 0) {
                if (args[0].equals("--port")) {
                    server = new Server(Integer.parseInt(args[1]));
                } else {
                    server = new Server();
                }
            } else {
                server = new Server();
            }

            server.listen();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
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
