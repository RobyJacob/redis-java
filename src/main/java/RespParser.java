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
    private String commandType;
    private String resultResp;

    RespParser(Config config, Data data) {
        parsedValues = new ArrayList<>();
        expectedNumArgs = -1;
        size = -1;
        commands = new Commands(config, data);
        resultResp = new String();
    }

    public boolean feed(String respString) {
        Character prefix = respString.charAt(0);

        switch (prefix) {
            case '*':
                expectedNumArgs = Integer.parseInt(respString.substring(1));
                break;
            case '$':
                size = Integer.parseInt(respString.substring(1));
                break;
            case '+':
                parsedValues.add(respString.substring(1));
                break;

            default:
                if (size == respString.length()) {
                    parsedValues.add(respString);

                    if (expectedNumArgs != -1 && parsedValues.size() == expectedNumArgs) {
                        processParsedValues();

                        return true;
                    }
                } else throw new RuntimeException("Size does not match: %s".formatted(respString));
        }

        return false;
    }

    public String getResult() {
        return resultResp;
    }

    public List<String> getParsedValues() {
        return parsedValues;
    }

    private void processParsedValues() {
        String command = parsedValues.get(0);
        boolean withArg = expectedNumArgs > 1;
        List<String> arg = withArg ? parsedValues.subList(1, parsedValues.size()) : Collections.emptyList();

        if (commands.isCommand(command)) {
            commands.add(command);

            commandType = getCommandType();

            if (withArg) commands.addArg(arg);

            resultResp = commands.process();
        } else {
            throw new RuntimeException("Invalid command found: %s".formatted(command));
        }
    }

    private String getCommandType() {
        if (parsedValues.isEmpty()) return "unknown";

        String command = parsedValues.get(0).toLowerCase();
            
        if (Commands.readCommands.contains(command)) return "read";
        else if (Commands.writeCommands.contains(command)) return "write";
        else if (Commands.adminCommands.contains(command)) return "admin";
        else return "unknown";
    }

    public String commandType() {
        return commandType;
    }

    public void reset() {
        parsedValues.clear();
        expectedNumArgs = -1;
        size = -1;
    }
}
