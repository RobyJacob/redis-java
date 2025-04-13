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
    private int size;
    private Commands commands;
    private String resultResp;

    RespParser() {
        parsedValues = new ArrayList<>();
        expectedNumArgs = -1;
        size = -1;
        commands = new Commands();
    }

    public boolean feed(String respString) {
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

    public String getResult() {
        return resultResp;
    }

    private void processParsedValues() {
        String command = parsedValues.get(0);
        boolean withArg = expectedNumArgs > 1;
        List<String> arg = withArg ? parsedValues.subList(1, parsedValues.size()) : Collections.emptyList();

        if (commands.isCommand(command)) {
            commands.add(command); 

            if (withArg) commands.addArg(arg);
            
            resultResp = commands.process();
        } else {
            throw new RuntimeException("Invalid command found: %s".formatted(command));
        }

        parsedValues.clear();
        expectedNumArgs = -1;
        size = -1;
    }
}
