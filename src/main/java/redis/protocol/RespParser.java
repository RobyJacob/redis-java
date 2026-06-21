package redis.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RespParser {
    private List<String> tokens;
    private int expectedCount;
    private int currentBulkSize;

    public RespParser() {
        reset();
    }

    public Optional<List<String>> feed(String line) {
        if (line == null || line.isEmpty()) return Optional.empty();

        // Bulk string data must be checked before prefix dispatch
        if (currentBulkSize != -1) {
            if (currentBulkSize != line.length()) {
                throw new IllegalStateException(
                        "Bulk string size mismatch: expected %d, got %d for '%s'"
                                .formatted(currentBulkSize, line.length(), line));
            }
            tokens.add(line);
            currentBulkSize = -1;
            if (expectedCount == -1 || tokens.size() == expectedCount) {
                return complete();
            }
            return Optional.empty();
        }

        char prefix = line.charAt(0);
        String rest = line.substring(1);

        switch (prefix) {
            case '*' -> expectedCount = Integer.parseInt(rest);
            case '$' -> currentBulkSize = Integer.parseInt(rest);
            case '+', '-' -> {
                tokens.add(rest);
                if (expectedCount == -1) return complete();
            }
            default -> throw new IllegalStateException("Unexpected RESP prefix: " + prefix);
        }

        return Optional.empty();
    }

    private Optional<List<String>> complete() {
        List<String> result = List.copyOf(tokens);
        reset();
        return Optional.of(result);
    }

    public void reset() {
        tokens = new ArrayList<>();
        expectedCount = -1;
        currentBulkSize = -1;
    }
}
