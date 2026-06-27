package me.adamix.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.adamix.SleepTokenWordle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WordleCommand implements BasicCommand {
    private final SleepTokenWordle plugin;

    public WordleCommand(@NotNull SleepTokenWordle plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(
            @NotNull CommandSourceStack source,
            String @NotNull [] args
    ) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(
                    plugin.messageService().get().onlyPlayersCanStartWordle()
            );
            return;
        }

        plugin.gameService()
                .getGame(player.getUniqueId())
                .ifPresent(_ -> plugin.gameService().endGame(player.getUniqueId()));


        plugin.gameService().startGame(player);
        player.sendMessage(
                plugin.messageService().get().wordleStart()
        );
        player.sendMessage(
                plugin.messageService().get().interactionMode()
        );
    }
}
