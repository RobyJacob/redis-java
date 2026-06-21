package redis.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore implements DataStore {
    private final Map<String, String> store = new ConcurrentHashMap<>();
    private final Map<String, long[]> expiry = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value) {
        store.put(key, value);
        expiry.remove(key);
    }

    @Override
    public void set(String key, String value, long expiryMilliseconds) {
        store.put(key, value);
        expiry.put(key, new long[]{System.currentTimeMillis(), expiryMilliseconds});
    }

    @Override
    public String get(String key) {
        long[] times = expiry.get(key);
        if (times != null) {
            long elapsed = System.currentTimeMillis() - times[0];
            if (elapsed > times[1]) {
                store.remove(key);
                expiry.remove(key);
                return null;
            }
        }
        return store.get(key);
    }
}
