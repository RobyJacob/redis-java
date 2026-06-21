package redis.command.handler;

import redis.command.Command;
import redis.config.ServerConfig;
import redis.protocol.RespSerializer;
import redis.storage.DataStore;

import java.util.List;

public class GetCommand implements Command {
    @Override
    public String execute(List<String> args, DataStore dataStore, ServerConfig config) {
        String value = dataStore.get(args.get(0));
        return value != null ? RespSerializer.bulkString(value) : RespSerializer.nullBulkString();
    }
}
