package com.example.redisdemo.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import redis.embedded.Redis;
import redis.embedded.RedisCluster;
import redis.embedded.RedisServer;
import redis.embedded.core.PortProvider;
import redis.embedded.core.RedisServerBuilder;
import redis.embedded.model.ReplicationGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static redis.embedded.core.PortProvider.newPredefinedPortProvider;

@Component
public class LocalRedis {

    private final List<Integer> ports = Arrays.asList(6370, 6371, 6372, 6380, 6381, 6382);
    private final PortProvider portProvider = newPredefinedPortProvider(ports);

    private RedisCluster redisCluster;

    @PostConstruct
    public void startRedis() throws IOException {
        redisCluster = new RedisCluster(new ArrayList<>(), buildServers());
        redisCluster.start();

        clustering();
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        redisCluster.stop();
    }

    private void clustering() {
        String scriptPath = Objects.requireNonNull(getClass().getResource("/binary/redis/cluster.sh")).getPath();
        ProcessBuilder pb = new ProcessBuilder(scriptPath);

        try {
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Redis> buildServers() throws IOException {
        final List<ReplicationGroup> groups = new LinkedList<>();
        groups.add(new ReplicationGroup("group1", 1, portProvider));
        groups.add(new ReplicationGroup("group2", 1, portProvider));
        groups.add(new ReplicationGroup("group3", 1, portProvider));

        final List<Redis> servers = new ArrayList<>();
        for (final ReplicationGroup g : groups) {
            servers.add(buildMaster(g));
            buildSlaves(servers, g);
        }
        return servers;
    }

    private void buildSlaves(final List<Redis> servers, ReplicationGroup g) throws IOException {
        for (final Integer slavePort : g.slavePorts) {
            final RedisServer slave = serverBuilder()
                    .port(slavePort)
                    .configFile(getPath(slavePort))
                    .build();
            servers.add(slave);
        }
    }

    private Redis buildMaster(final ReplicationGroup g) throws IOException {
        return serverBuilder().port(g.masterPort)
                .configFile(getPath(g.masterPort))
                .build();
    }

    private Path getPath(Integer slavePort) {
        String filePath = "conf/redis-" + slavePort + "-conf";
        try {
            return Paths.get(getClass().getClassLoader().getResource(filePath).toURI());
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private RedisServerBuilder serverBuilder() {
        RedisServerBuilder redisServerBuilder = new RedisServerBuilder();
        redisServerBuilder.reset();
        return redisServerBuilder;
    }
}
