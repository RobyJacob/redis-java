import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = null;
        try {
            server = new Server();
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
