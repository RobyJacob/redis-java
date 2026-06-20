package redis.util;

import java.io.IOException;
import java.io.InputStream;

public final class StreamUtils {
    private StreamUtils() {}

    public static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                in.read(); // discard '\n'
                return sb.toString();
            }
            sb.append((char) b);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
