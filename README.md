# Boot master server with default port
```sh
./spawn_redis_server.sh
```

# Boot master server with custom port
```sh
./spawn_redis_server.sh --port 6420
```

# Boot replica server
```sh
./spawn_redis_server.sh --port 6379 --replicaof "localhost 6380"
```