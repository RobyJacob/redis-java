package redis.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class ExecutorProvider {
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(POOL_SIZE);
    private static final ScheduledExecutorService SCHEDULED = Executors.newScheduledThreadPool(POOL_SIZE);

    private ExecutorProvider() {}

    public static ExecutorService get() {
        return EXECUTOR;
    }

    public static ScheduledExecutorService getScheduled() {
        return SCHEDULED;
    }
}
