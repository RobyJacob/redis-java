package redis.command.handler;

import redis.command.Command;
import redis.config.ServerConfig;
import redis.protocol.RespSerializer;
import redis.storage.DataStore;

import java.util.List;

public class InfoCommand implements Command {
    @Override
    public String execute(List<String> args, DataStore dataStore, ServerConfig config) {
        if (args.isEmpty() || !"replication".equalsIgnoreCase(args.get(0))) {
            return RespSerializer.bulkString("");
        }

        String info = config.isMaster()
                ? "role:master\nmaster_replid:%s\nmaster_repl_offset:%d"
                        .formatted(config.getReplicationId(), config.getReplicationOffset())
                : "role:slave";

        return RespSerializer.bulkString(info);
    }
}
