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

                    if (parsedValues.size() == expectedNumArgs) {
                        String command = parsedValues.get(0);
                        String arg = parsedValues.get(1);
                        if (Commands.isCommand(command)) {
                            Commands.add(command); 
                            Commands.addArg(arg);
                        }
                        parsedValues.clear();
                        expectedNumArgs = -1;
                        size = -1;
                        return true;
                    }
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
}
