import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data {
    private Map<String, String> bufferMap;
    private Map<String, List<Long>> keyExpiry;

    Data () {
        bufferMap = new HashMap<>();
        keyExpiry = new HashMap<>();
    }

    public void add(String key, String value) {
        bufferMap.put(key, value);
    }

    public void add(String key, String value, Long expiry) {
        add(key, value);

        long currentMilliseconds = System.currentTimeMillis();
        keyExpiry.put(key, List.of(currentMilliseconds, expiry));
    }

    public String get(String key) {
        if (keyExpiry.containsKey(key)) {
            long currentMilliseconds = System.currentTimeMillis();
            long startMilliseconds = keyExpiry.get(key).get(0);
            long expiryMilliseconds = keyExpiry.get(key).get(1);
            
            bufferMap.computeIfPresent(key, (k, v) -> {
                boolean expired = currentMilliseconds - startMilliseconds > expiryMilliseconds;
                return expired ? null : v;
            });
        }

        return bufferMap.getOrDefault(key, null);
    }

    public void saveData(String rdbFileContent) {
        System.out.println("Saved to memory: " + rdbFileContent);
    }
}
