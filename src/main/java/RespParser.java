import java.util.*;

public class RespParser {
    enum Operand {
        ARRAY('*'),
        BULKSTRING('$'),
        INTEGER(':'),
        ERROR('-'),
        STRING('+');

        private Character typeChar;

        Operand(Character typeChar) {
            this.typeChar = typeChar;
        }

        @Override
        public String toString() {
            return typeChar.toString();
        }
    }

    private List<String> parsedValues;
    private int expectedNumArgs;
    private final String CRLF = "\r\n";
    private int size;

    RespParser() {
        parsedValues = new ArrayList<>();
        expectedNumArgs = -1;
        size = -1;
    }

    boolean feed(String respString) {
        Character prefix = respString.charAt(0);
        
        switch(prefix) {
            case '*':
                expectedNumArgs = Integer.parseInt(respString.substring(1));
                break;
            case '$':
                size = Integer.parseInt(respString.substring(1));
                break;
            
            default:
                if (size == respString.length()) {
                    parsedValues.add(respString);

                    if (expectedNumArgs != -1 && parsedValues.size() == expectedNumArgs) {
                        processParsedValues();
                        return true;
                    }
                } else {
                    throw new RuntimeException("Size does not match: %s".formatted(respString));
                }
        }

        return false;
    }

    String convertToResp(String str) {
        StringBuilder builder = new StringBuilder();

        builder.append(Operand.BULKSTRING)
                .append(str.length())
                .append(CRLF)
                .append(str)
                .append(CRLF);

        return builder.toString();
    }

    private void processParsedValues() {
        String command = parsedValues.get(0);
        boolean withArg = expectedNumArgs > 1;
        String arg = withArg ? parsedValues.get(1) : "";
        if (Commands.isCommand(command)) {
            Commands.add(command); 
            if (withArg) Commands.addArg(arg);
        } else {
            throw new RuntimeException("Invalid command found: %s".formatted(command));
        }
        parsedValues.clear();
        expectedNumArgs = -1;
        size = -1;
    }
}
