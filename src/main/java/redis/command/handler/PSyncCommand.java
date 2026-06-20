package redis.command.handler;

import redis.command.Command;
import redis.config.ServerConfig;
import redis.protocol.RespSerializer;
import redis.storage.DataStore;

import java.util.List;

public class PSyncCommand implements Command {
    @Override
    public String execute(List<String> args, DataStore dataStore, ServerConfig config) {
        String response = "FULLRESYNC %s %d".formatted(config.getReplicationId(), config.getReplicationOffset());
        return RespSerializer.simpleString(response);
    }
}
