package me.adamix.event;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.adamix.GameService;
import me.adamix.game.GuessResult;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerListener implements Listener {
    private final GameService gameService;

    public PlayerListener(@NotNull GameService gameService) {
        this.gameService = gameService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(@NotNull AsyncChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (!gameService.hasActiveGame(uuid)) {
            return;
        }

        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText()
                .serialize(event.message());

        gameService.guessWord(uuid, message.toLowerCase()).thenAccept(result -> {
            switch (result) {
                case GuessResult.InvalidLength(int expectedLength) -> event.getPlayer()
                        .sendMessage("The word must be exactly %d characters long!".formatted(expectedLength));
                case GuessResult.InvalidInput() -> event.getPlayer()
                        .sendMessage("Your guess is invalid! Please use only alphabetic characters");
                case GuessResult.Success() -> {
                }
                case GuessResult.WordGuessed(int tries) -> event.getPlayer()
                        .sendMessage("You have successfully guessed the word after %d attempts!".formatted(tries));
                case GuessResult.Lost(String word) -> event.getPlayer()
                        .sendMessage("You have ran out of attempts! The word was " + word);
                case GuessResult.OnDelay onDelay -> {}
                case GuessResult.UnknownWord() -> {
                    event.getPlayer()
                        .sendMessage("Unknown word! Please enter valid sleep token song");
                }
            }
        });
    }


    @EventHandler
    public void onDisconnect(@NotNull PlayerQuitEvent event) {
        gameService.endGame(event.getPlayer().getUniqueId());
    }
}
