# Lettuce Redis Client Deadlock Reproduction

This repository demonstrates a deadlock issue in the Lettuce Redis client when using a custom SocketAddressResolver that throws an exception. The issue occurs when attempting to connect to a Redis cluster with invalid node configuration.

## Prerequisites

- Java 8 or higher

## Running the Reproduction

Using Maven Wrapper:

```bash
./mvnw compile exec:java
```

## Expected Behavior

The application will compile and then hang indefinitely due to a deadlock in the Lettuce client's connection initialization process. This occurs because:

1. The application attempts to connect to a Redis cluster
2. A custom SocketAddressResolver throws an exception due to invalid port configuration
3. The connection's CompletableFuture is not properly completed in the error path
4. The main thread becomes deadlocked waiting for cluster partitions

## Issue Reference

For more details about this issue, please refer to:
https://github.com/redis/lettuce/issues/3240
