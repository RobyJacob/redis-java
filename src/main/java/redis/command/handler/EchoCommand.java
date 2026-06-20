package redis.command.handler;

import redis.command.Command;
import redis.config.ServerConfig;
import redis.protocol.RespSerializer;
import redis.storage.DataStore;

import java.util.List;

public class EchoCommand implements Command {
    @Override
    public String execute(List<String> args, DataStore dataStore, ServerConfig config) {
        return RespSerializer.bulkString(args.get(0));
    }
}
