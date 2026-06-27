package me.adamix.config;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public sealed interface MessageLoadResult {
    record Success() implements MessageLoadResult {}
    record MissingValues(String @NotNull [] keys) implements MessageLoadResult {}
    record InstanceInitializationFailed(@NotNull Throwable e) implements MessageLoadResult {}
    record CreateDirectoriesFailed(@NotNull IOException e) implements MessageLoadResult {}

    default boolean isSuccess() {
        return this instanceof MessageLoadResult.Success;
    }
}
