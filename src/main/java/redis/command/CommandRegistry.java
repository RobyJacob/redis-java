package redis.command;

import redis.command.handler.*;
import redis.config.ServerConfig;
import redis.storage.DataStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, CommandType> types = new HashMap<>();

    public CommandRegistry() {
        register("get",      new GetCommand(),      CommandType.READ);
        register("set",      new SetCommand(),      CommandType.WRITE);
        register("echo",     new EchoCommand(),     CommandType.READ);
        register("ping",     new PingCommand(),     CommandType.READ);
        register("info",     new InfoCommand(),     CommandType.READ);
        register("replconf", new ReplConfCommand(), CommandType.ADMIN);
        register("psync",    new PSyncCommand(),    CommandType.ADMIN);
    }

    public void register(String name, Command command, CommandType type) {
        String key = name.toLowerCase();
        commands.put(key, command);
        types.put(key, type);
    }

    public boolean isKnown(String name) {
        return commands.containsKey(name.toLowerCase());
    }

    public String execute(List<String> tokens, DataStore dataStore, ServerConfig config) {
        String name = tokens.get(0).toLowerCase();
        Command command = commands.get(name);
        if (command == null) throw new IllegalArgumentException("Unknown command: " + name);
        List<String> args = tokens.size() > 1 ? tokens.subList(1, tokens.size()) : List.of();
        return command.execute(args, dataStore, config);
    }

    public CommandType typeOf(String name) {
        return types.getOrDefault(name.toLowerCase(), CommandType.UNKNOWN);
    }
}
