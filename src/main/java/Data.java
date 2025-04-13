import java.util.HashMap;
import java.util.Map;

public class Data {
    private static Map<String, String> bufferMap = new HashMap<>();

    public static void add(String key, String value) {
        bufferMap.put(key, value);
    }

    public static String get(String key) {
        return bufferMap.getOrDefault(key, "-1");
    }
}
