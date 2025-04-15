import java.util.*;

public class Commands {
    private Map<String, Keywords> commandMap;

    private List<String> arguments;

    private Keywords processingCommand;

    private Data data;

    private ServerConfig serverConfig;

    Commands(ServerConfig serverConfig) {
        commandMap = new HashMap<>();
        arguments = new ArrayList<>();
        data = new Data();
        this.serverConfig = serverConfig;
        Arrays.stream(CommandKeywords.values()).forEach(k -> commandMap.put(k.toString(), k));
    }

    enum CommandKeywords implements Keywords {
        GET("get") {
            @Override
            public String process(List<String> args, Data data, ServerConfig config) {
                String key = data.get(args.get(0));
                
                if (key == null)
                    return Utility.convertToResp("1", RespParser.Operand.ERROR);

                return Utility.convertToResp(key, RespParser.Operand.BULKSTRING);
            }
        },

        SET("set") {
            @Override
            public String process(List<String> args, Data data, ServerConfig config) {
                String key = args.get(0);
                String val = args.get(1);

                data.add(key, val);

                if (args.size() > 2 && "px".equals(args.get(2).toLowerCase())) {
                    long expiryMilliseconds = Long.valueOf(args.get(3));
                    data.add(key, val, expiryMilliseconds);
                }

                return Utility.convertToResp("OK", RespParser.Operand.STRING);
            }
        },

        ECHO("echo") {
            @Override
            public String process(List<String> args, Data data, ServerConfig config) {
                String echoedString = args.get(0);
                return Utility.convertToResp(echoedString, RespParser.Operand.BULKSTRING);
            }
        },

        PING("ping") {
            @Override
            public String process(List<String> args, Data data, ServerConfig config) {
                return Utility.convertToResp("PONG", RespParser.Operand.BULKSTRING);
            }
        },
        
        INFO("info") {
            @Override
            public String process(List<String> args, Data data, ServerConfig config) {
                String arg = args.get(0).toLowerCase();
                String response = "";
                String infoMessage = "role:master\nmaster_replid:%s\nmaster_repl_offset:%d"
                    .formatted(config.getReplicationId(), config.getReplicationOffset());

                switch (arg) {
                    case "replication":
                        if (!config.isMaster()) infoMessage = "role:slave";
                        
                        response = Utility.convertToResp(infoMessage, RespParser.Operand.BULKSTRING);
                        break;
                }

                return response;
            }
        }, 
        
        REPLCONF("replconf") {
            @Override
            public String process(List<String> args, Data data, ServerConfig config) {
                return Utility.convertToResp("OK", RespParser.Operand.STRING);
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
        String result = processingCommand.process(arguments, data, serverConfig);
        clearArgs();
        return result;
    }

    private void clearArgs() {
        arguments.clear();
    }
}

interface Keywords {
    public String process(List<String> args, Data data, ServerConfig serverConfig);
}