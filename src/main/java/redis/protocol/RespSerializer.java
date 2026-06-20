package redis.protocol;

import java.util.List;

public final class RespSerializer {
    private static final String CRLF = "\r\n";

    private RespSerializer() {}

    public static String simpleString(String message) {
        return "+" + message + CRLF;
    }

    public static String bulkString(String message) {
        return "$" + message.length() + CRLF + message + CRLF;
    }

    public static String nullBulkString() {
        return "$-1" + CRLF;
    }

    public static String error(String message) {
        return "-ERR " + message + CRLF;
    }

    public static String array(List<String> elements) {
        StringBuilder builder = new StringBuilder();
        builder.append("*").append(elements.size()).append(CRLF);
        for (String element : elements) {
            builder.append(bulkString(element));
        }
        return builder.toString();
    }
}
