import java.util.*;

public class Commands {
    private Map<String, Keywords> commandMap;

    private List<String> arguments;

    private Keywords processingCommand;

    Commands() {
        commandMap = new HashMap<>();
        arguments = new ArrayList<>();
        Arrays.stream(CommandKeywords.values()).forEach(k -> commandMap.put(k.toString(), k));
    }

    enum CommandKeywords implements Keywords {
        GET("get") {
            @Override
            public String process(List<String> args) {
                String fetchedData = Data.get(args.get(0));
                return Utility.convertToResp(fetchedData, RespParser.Operand.BULKSTRING);
            }
        },

        SET("set") {
            @Override
            public String process(List<String> args) {
                Data.add(args.get(0), args.get(1));
                return Utility.convertToResp("OK", RespParser.Operand.STRING);
            }
        },

        ECHO("echo") {
            @Override
            public String process(List<String> args) {
                String echoedString = args.get(0);
                return Utility.convertToResp(echoedString, RespParser.Operand.BULKSTRING);
            }
        },
        
        PING("ping") {
            @Override
            public String process(List<String> args) {
                return Utility.convertToResp("PONG", RespParser.Operand.BULKSTRING);
            }
        };

        private final String keyword;

        CommandKeywords(String keyword) {
            this.keyword = keyword;
        }

        @Override
        public String toString() {
            return keyword;
        }
    }

    public Boolean isCommand(String comm) {
        return commandMap.containsKey(comm.toLowerCase());
    }

    public void add(String comm) {
        processingCommand = commandMap.get(comm.toLowerCase());
    }

    public void addArg(List<String> arg) {
        arguments.addAll(arg);
    }

    public String process() {
        String result = processingCommand.process(arguments);
        clearArgs();
        return result;
    }

    private void clearArgs() {
        arguments.clear();
    }
}

interface Keywords {
    public String process(List<String> args);
}