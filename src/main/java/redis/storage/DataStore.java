package redis.storage;

public interface DataStore {
    void set(String key, String value);
    void set(String key, String value, long expiryMilliseconds);
    String get(String key);
}
