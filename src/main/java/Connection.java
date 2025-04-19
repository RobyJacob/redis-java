import java.net.Socket;

public class Connection {
    private final Socket socket;
    private boolean syncRequired;

    Connection(Socket socket) {
        this.socket = socket;
        this.syncRequired = false;
    }

    public synchronized boolean syncRequired() {
        return syncRequired;
    }

    public synchronized void setSyncRequired(boolean syncRequired) {
        this.syncRequired = syncRequired;
        if (syncRequired) {
            notifyAll();
        }
    }

    public synchronized void waitForSync() throws InterruptedException {
        while (!syncRequired) {
            wait();
        }
    }

    public Socket getSocket() {
        return socket;
    }
}