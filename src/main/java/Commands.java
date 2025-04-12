import java.util.*;

public class Commands {
    private static Map<String, Keywords> commandMap = new HashMap<>();

    private static String argument = "";

    private static Keywords processingCommand;

    enum CommandKeywords implements Keywords {
        GET("get") {
            @Override
            public String process() {
                return "GET invoked";
            }
        },
        SET("set") {
            @Override
            public String process() {
                return "SET invoked";
            }
        },
        ECHO("echo") {
            @Override
            public String process() {
                return argument;
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

    static {
        Arrays.stream(CommandKeywords.values()).forEach(k -> commandMap.put(k.toString(), k));
    }

    public static Boolean isCommand(String comm) {
        return commandMap.containsKey(comm.toLowerCase());
    }

    public static void add(String comm) {
        processingCommand = commandMap.get(comm.toLowerCase());
    }

    public static void addArg(String arg) {
        argument = arg;
    }

    public static String process() {
        return processingCommand.process();
    }
}

interface Keywords {
    public String process();
}