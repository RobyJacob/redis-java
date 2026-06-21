package redis.command;

import redis.config.ServerConfig;
import redis.storage.DataStore;

import java.util.List;

public interface Command {
    String execute(List<String> args, DataStore dataStore, ServerConfig config);
}
