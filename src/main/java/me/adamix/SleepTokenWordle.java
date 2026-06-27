package me.adamix;

import me.adamix.command.WordleCommand;
import me.adamix.config.MessageService;
import me.adamix.config.PluginMessages;
import me.adamix.event.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class SleepTokenWordle extends JavaPlugin {
    private GameService gameService;
    private final NamespacedKey key = new NamespacedKey(this, "is_board_part");

    private MessageService<PluginMessages> messageService;

    public void deleteBoardEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (TextDisplay entity : world.getEntitiesByClass(TextDisplay.class)) {
                var data = entity.getPersistentDataContainer();
                if (data.has(key)) {
                    entity.remove();
                }
            }
        }
    }

    @Override
    public void onEnable() {
        deleteBoardEntities();

        this.messageService = new MessageService<>(
                this,
                PluginMessages.class,
                "messages.yml",
                getDataPath().resolve("messages.yml")
        );

        gameService = new GameService(this, key, getComponentLogger());

        registerCommand("worldle", new WordleCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerListener(gameService, messageService), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public @NotNull GameService gameService() { return gameService; }

    public @NotNull MessageService<PluginMessages> messageService() { return messageService; }
}
