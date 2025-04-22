import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SharedExecutor {
    private static final ExecutorService executor = Executors.newFixedThreadPool(Config.getThreadPoolSize());
    private static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(Config.getThreadPoolSize());

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }
}