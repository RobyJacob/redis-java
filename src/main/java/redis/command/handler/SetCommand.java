package redis.command.handler;

import redis.command.Command;
import redis.config.ServerConfig;
import redis.protocol.RespSerializer;
import redis.storage.DataStore;

import java.util.List;

public class SetCommand implements Command {
    @Override
    public String execute(List<String> args, DataStore dataStore, ServerConfig config) {
        String key = args.get(0);
        String value = args.get(1);

        if (args.size() > 2 && "px".equalsIgnoreCase(args.get(2))) {
            dataStore.set(key, value, Long.parseLong(args.get(3)));
        } else {
            dataStore.set(key, value);
        }

        return RespSerializer.simpleString("OK");
    }
}
