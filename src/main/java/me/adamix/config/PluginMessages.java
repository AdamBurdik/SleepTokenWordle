package me.adamix.config;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface PluginMessages {
    @NotNull Component onlyPlayersCanStartWordle();
    @NotNull Component wordleStart();
    @NotNull Component interactionMode();

    @NotNull Component invalidLength(int expectedLength);
    @NotNull Component invalidInput();
    @NotNull Component wordGuessed(int tries);
    @NotNull Component gameLost(@NotNull String word);
    @NotNull Component unknownWord();
}