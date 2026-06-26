package me.adamix.game;

import org.jetbrains.annotations.NotNull;

public sealed interface GuessResult {
    record Success() implements GuessResult {}
    record InvalidInput() implements GuessResult {}
    record InvalidLength(int expectedLength) implements GuessResult {}
    record WordGuessed(int tries) implements GuessResult {}
    record Lost(@NotNull String word) implements GuessResult {}
    record OnDelay() implements GuessResult {}
    record UnknownWord() implements GuessResult {}

    default boolean isSuccess() {
        return this instanceof Success;
    }
}
