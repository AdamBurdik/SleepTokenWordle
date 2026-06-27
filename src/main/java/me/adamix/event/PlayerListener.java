package me.adamix.event;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.adamix.GameService;
import me.adamix.config.MessageService;
import me.adamix.config.PluginMessages;
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
    private final MessageService<PluginMessages> messageService;

    public PlayerListener(
            @NotNull GameService gameService,
            @NotNull MessageService<PluginMessages> messageService
    ) {
        this.gameService = gameService;
        this.messageService = messageService;
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

        PluginMessages messages = messageService.get();

        gameService.guessWord(uuid, message.toLowerCase()).thenAccept(result -> {
            switch (result) {
                case GuessResult.InvalidLength(int expectedLength) -> event.getPlayer()
                        .sendMessage(messages.invalidLength(expectedLength));
                case GuessResult.InvalidInput() -> event.getPlayer()
                        .sendMessage(messages.invalidInput());
                case GuessResult.WordGuessed(int tries) -> event.getPlayer()
                        .sendMessage(messages.wordGuessed(tries));
                case GuessResult.Lost(String word) -> event.getPlayer()
                        .sendMessage(messages.gameLost(word));
                case GuessResult.UnknownWord() -> event.getPlayer()
                        .sendMessage(messages.unknownWord());

                case GuessResult.Success() -> {}
                case GuessResult.OnDelay onDelay -> {}
            }
        });
    }


    @EventHandler
    public void onDisconnect(@NotNull PlayerQuitEvent event) {
        gameService.endGame(event.getPlayer().getUniqueId());
    }
}
