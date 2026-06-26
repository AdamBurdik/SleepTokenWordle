package me.adamix.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.adamix.SleepTokenWordle;
import me.adamix.game.WordleGame;
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
            source.getSender().sendMessage("Only players can start a wordle!");
            return;
        }

        plugin.gameService()
                .getGame(player.getUniqueId())
                .ifPresent(_ -> plugin.gameService().endGame(player.getUniqueId()));


        WordleGame game = plugin.gameService().startGame(player);
        player.sendMessage("Worldle started!");

        player.sendMessage("-----------------------------------------");
        player.sendMessage("You have entered interactive chat mode");
        player.sendMessage("All messages will be used as worldle guesses");
        player.sendMessage("-----------------------------------------");
    }
}
