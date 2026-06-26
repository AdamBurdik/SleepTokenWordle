package me.adamix;

import me.adamix.command.WordleCommand;
import me.adamix.event.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

public final class SleepTokenWordle extends JavaPlugin {
    private GameService gameService;
    private final NamespacedKey key = new NamespacedKey(this, "is_board_part");

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

        gameService = new GameService(this, key, getComponentLogger());

        registerCommand("worldle", new WordleCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerListener(gameService), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public GameService gameService() { return gameService; }
}
