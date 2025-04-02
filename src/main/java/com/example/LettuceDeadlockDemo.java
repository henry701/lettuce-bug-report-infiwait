package com.example;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolver;
import io.lettuce.core.resource.SocketAddressResolver;
import io.lettuce.core.internal.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LettuceDeadlockDemo {

    private static final Logger logger = LoggerFactory.getLogger(LettuceDeadlockDemo.class);

    public static void main(String[] args) {
        logger.info("Starting Lettuce Deadlock Demonstration");
        try {
            List<RedisURI> redisURIs = Arrays.asList(
                    // Valid URI (although it points to a non-existent server)
                    RedisURI.create("localhost", 6379),
                    // Invalid URI with unparseable port - this will cause the deadlock
                    RedisURI.create("redis://localhost:$(INVALID_DATA):CONFIG")
            );
            // Configure client with custom resources that use our problematic socket address resolver
            ClientResources clientResources = ClientResources.builder()
                    .socketAddressResolver(new ProblematicSocketAddressResolver())
                    .build();
            RedisClusterClient clusterClient = RedisClusterClient.create(clientResources, redisURIs);
            // Set topology refresh options
            ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                    .enableAllAdaptiveRefreshTriggers()
                    .enablePeriodicRefresh(Duration.ofSeconds(30))
                    .build();
            clusterClient.setOptions(ClusterClientOptions.builder()
                    .topologyRefreshOptions(topologyRefreshOptions)
                    .build());
            logger.info("Attempting to connect to the Redis cluster with invalid nodes...");
            // This call will hang indefinitely due to the deadlock bug
            logger.info("Getting partitions - this should hang due to the bug");
            clusterClient.getPartitions();
            // We'll never reach here due to the deadlock
            logger.info("Successfully retrieved partitions (we shouldn't see this message)");
        } catch (Exception e) {
            // This should be what happens, with an exception thrown from the getPartitions() call
            logger.error("Error occurred: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Custom SocketAddressResolver that will throw an exception when encountering invalid port format.
     * This simulates the behavior described in the bug report.
     */
    static class ProblematicSocketAddressResolver extends SocketAddressResolver {
        
        public ProblematicSocketAddressResolver() {
            // Call the parent constructor with a custom DNS resolver
            super(DnsResolver.unresolved());
        }
        
        @Override
        public SocketAddress resolve(RedisURI redisURI) {
            String uriString = redisURI.toString();
            logger.info("uriString: {}", uriString);
            // Simulate the error when we encounter the invalid port pattern
            if (uriString.contains("INVALID_DATA")) {
                throw new IllegalArgumentException("Cannot parse port number: $(INVALID_DATA):CONFIG");
            }
            return super.resolve(redisURI);
        }
    }
}
