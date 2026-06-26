package me.adamix.game;

import me.adamix.SleepTokenWordle;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WordleGame {
    private static final Random random = new Random();

    private final SleepTokenWordle plugin;
    private final NamespacedKey key;

    private final Player player;
    private final Location rootLocation;
    private final String[] history;
    private final String target;
    private final Tile[][] entityGrid;
    private int currentGuess = 0;
    private final Map<Character, Integer> characterBag;
    private @NotNull WordleState state = WordleState.ACTIVE;
    private volatile boolean canGuess = true;

    record Tile(
            @NotNull TextDisplay letter,
            @NotNull TextDisplay background,
            @NotNull TextDisplay state
    ) {

    }

    public WordleGame(
            @NotNull SleepTokenWordle plugin,
            @NotNull NamespacedKey key,
            @NotNull Player player,
            @NotNull Location rootLocation,
            @NotNull String target,
            int guessAmount
    ) {
        this.plugin = plugin;
        this.key = key;

        this.player = player;
        this.rootLocation = rootLocation;
        this.history = new String[guessAmount];
        this.target = target;
        this.entityGrid = new Tile[guessAmount][target.length()];
        this.characterBag = createBag(target);

        for (int i = 0; i < guessAmount; i++) {
            history[i] = target;
        }
    }

    private @NotNull Map<Character, Integer> createBag(@NotNull String target) {
        Map<Character, Integer> map = new HashMap<>();
        for (char c : target.toCharArray()) {
            int current = map.computeIfAbsent(c, k -> 0);
            map.put(c, current + 1);
        }
        return map;
    }

    private void genericMetadata(@NotNull TextDisplay textDisplay) {
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setSeeThrough(false);
        textDisplay.setBillboard(Display.Billboard.FIXED);
        textDisplay.setVisibleByDefault(false);
        var data = textDisplay.getPersistentDataContainer();
        data.set(this.key, PersistentDataType.BOOLEAN, true);
    }

    public void spawnBoard() {
        World world = rootLocation.getWorld();

        for (int y = 0; y < history.length; y++) {
            Tile[] row = new Tile[target.length()];

            for (int x = 0; x < target.length(); x++) {
                var location = rootLocation.clone();
                location.setPitch(0);
                location.addRotation(180, 0);

                Vector forward = location.getDirection();

                Vector up = new Vector(0, 1, 0);
                Vector right = forward.crossProduct(up).normalize();

                Location leftLocation = location.subtract(right.multiply(x))
                        .subtract(up.multiply(y * 1.1));

                int scale = 3;
                int bgScale = 4;

                Location bgLocation = leftLocation.clone().subtract(leftLocation.getDirection().normalize().multiply(0.01));
                Location stateLocation = leftLocation.clone().subtract(leftLocation.getDirection().normalize().multiply(0.009));

                var entity = world.spawn(leftLocation, TextDisplay.class, CreatureSpawnEvent.SpawnReason.CUSTOM, textDisplay -> {
                    genericMetadata(textDisplay);
                    textDisplay.text(Component.text(""));
                    textDisplay.setDefaultBackground(false);
                    textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    textDisplay.setTransformation(new Transformation(
                            new Vector3f(0f, -0.4f, 0f),
                            new AxisAngle4f(0, 0, 0, 0),
                            new Vector3f(scale, scale, scale),
                            new AxisAngle4f(0, 0, 0, 0)
                    ));
                });


                var bgEntity = world.spawn(bgLocation, TextDisplay.class, CreatureSpawnEvent.SpawnReason.CUSTOM, textDisplay -> {
                    genericMetadata(textDisplay);
                    textDisplay.text(Component.text("  "));
                    textDisplay.setBackgroundColor(Color.fromARGB(150, 0, 0, 0));
                    textDisplay.setTransformation(new Transformation(
                            new Vector3f(0f, -0.5f, 0f),
                            new AxisAngle4f(0, 0, 0, 0),
                            new Vector3f(bgScale, bgScale, bgScale),
                            new AxisAngle4f(0, 0, 0, 0)
                    ));
                });

                var stateEntity = world.spawn(stateLocation, TextDisplay.class, CreatureSpawnEvent.SpawnReason.CUSTOM, textDisplay -> {
                    genericMetadata(textDisplay);
                    textDisplay.setBillboard(Display.Billboard.FIXED);
                    textDisplay.text(Component.text("  "));
                    textDisplay.setBackgroundColor(Color.fromARGB(150, 0, 0, 0));
                    textDisplay.setTransformation(new Transformation(
                            new Vector3f(0f, -0.5f, 0f),
                            new AxisAngle4f(0, 0, 0, 0),
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f(0, 0, 0, 0)
                    ));
                });

                player.showEntity(plugin, entity);
                player.showEntity(plugin, bgEntity);
                player.showEntity(plugin, stateEntity);

                row[x] = new Tile(entity, bgEntity, stateEntity);
            }

            entityGrid[y] = row;
        }
    }

    public void spawnFirework() {
        Vector offset = new Vector(
                random.nextInt(-5, 5),
                random.nextInt(-5, 5),
                random.nextInt(-5, 5)
        );

        player.getWorld()
                .spawn(player.getLocation().clone().add(offset), Firework.class, firework -> {
                    FireworkMeta meta = firework.getFireworkMeta();
                    meta.addEffect(
                            FireworkEffect.builder()
                                    .trail(true)
                                    .build()
                    );
                });
    }

    public @NotNull GuessResult verifyWord(@NotNull String word) {
        if (word.length() != target.length()) {
            return new GuessResult.InvalidLength(target.length());
        }
        return new GuessResult.Success();
    }

    public void displayWord(@NotNull String word) {
        canGuess = false;
        Tile[] row = entityGrid[currentGuess];

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                2f,
                0f
        );

        for (int i = 0; i < word.length(); i++) {
            Tile tile = row[i];
            char character = word.charAt(i);
            tile.letter.text(Component.text(character));
        }
    }

    public @NotNull GuessResult guessWord(@NotNull String word) throws InterruptedException {
        Map<Character, Integer> bag = new HashMap<>(this.characterBag);

        boolean correctGuess = true;

        // This array represents states for this guess
        // 0 = Not in word (gray)
        // 1 = In this position (green)
        // 2 = Somewhere else in the word (yellow)
        int[] states = new int[target.length()];

        // Check green letters
        for (int i = 0; i < target.length(); i++) {
            char expected = target.charAt(i);
            char actual = word.charAt(i);

            if (expected == actual) {
                states[i] = 1;
                bag.computeIfPresent(expected, (_, value) -> value - 1);
            } else {
                correctGuess = false;
            }
        }

        // Check for yellows
        for (int i = 0; i < target.length(); i++) {
            char expected = target.charAt(i);
            char actual = word.charAt(i);

            if (expected == actual) continue;
            int amount = bag.getOrDefault(actual, 0);
            if (amount > 0) {
                states[i] = 2;
                bag.computeIfPresent(actual, (_, value) -> value - 1);
            }
        }

        history[currentGuess] = word;

        Tile[] row = entityGrid[currentGuess];
        int i = 0;
        for (Tile tile : row) {
            char character = word.charAt(i);
            int state = states[i];

            Color bgColor = switch (state) {
                case 0 -> Color.GRAY;
                case 1 -> Color.fromRGB(21, 133, 21);
                case 2 -> Color.fromRGB(187, 191, 61);
                default -> throw new IllegalStateException("Unexpected value: " + state);
            };

            tile.letter.text(Component.text(character));
            tile.state.setBackgroundColor(bgColor);

            final int index = i;

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        Transformation transformation = tile.state.getTransformation();

                        transformation.getScale().set(4f, 4f, 4f);

                        tile.state.setTransformation(transformation);

                        tile.state.setInterpolationDuration(8);
                        tile.state.setInterpolationDelay(index * 3);
                    },
                    1
            );

            i++;
        }

        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> this.canGuess = true,
                        history.length * 3L + 15
                );

        currentGuess++;

        if (correctGuess) {
            this.state = WordleState.WON;
            return new GuessResult.WordGuessed(currentGuess);
        }

        if (currentGuess >= history.length) {
            this.state = WordleState.LOST;
            return new GuessResult.Lost(target);
        }

        return new GuessResult.Success();
    }

    private void wonTranslation(@NotNull TextDisplay textDisplay, float scale) {
        Transformation transformation = textDisplay.getTransformation();

        transformation.getScale().set(scale, scale, scale);

        textDisplay.setTransformation(transformation);

        textDisplay.setInterpolationDelay(0);
        textDisplay.setInterpolationDuration(3);
    }

    private void undoWonTranslation(@NotNull TextDisplay textDisplay, float scale) {
        Transformation transformation = textDisplay.getTransformation();

        transformation.getScale().set(scale, scale, scale);

        textDisplay.setTransformation(transformation);

        textDisplay.setInterpolationDelay(0);
        textDisplay.setInterpolationDuration(3);
    }

    public void wonAnimation() {
        Tile[] row = entityGrid[currentGuess - 1];

        for (Tile tile : row) {
            wonTranslation(tile.letter, 3f + 1.2f);
            wonTranslation(tile.state, 4f + 1.2f);
        }
    }

    public void undoWonAnimation() {
        Tile[] row = entityGrid[currentGuess - 1];

        for (Tile tile : row) {
            undoWonTranslation(tile.letter, 3f);
            undoWonTranslation(tile.state, 4f);
        }
    }

    public void deleteBoard() {
        for (Tile[] row : entityGrid) {
            for (Tile tile : row) {
                tile.letter.remove();
                tile.background.remove();
                tile.state.remove();
            }
        }
    }

    public @NotNull WordleState state() {
        return state;
    }

    public boolean canGuess() {
        return canGuess;
    }
}
