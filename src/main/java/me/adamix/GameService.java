package me.adamix;

import me.adamix.game.GuessResult;
import me.adamix.game.WordleGame;
import me.adamix.game.WordleState;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameService {
    private static final Random random = new Random();
    private static final HashSet<String> words = new HashSet<>(Arrays.asList(
            // EP: One
            "threadtheneedle",
            "fieldsofelation",
            "whentheboughbreaks",

            // EP: Two
            "calcutta",
            "nazareth",
            "jericho",

            // Singles
            "jaws",
            "heyya",
            "thewaythatyouwere",

            // Sundowning
            "thenightdoesnotbelongtogod",
            "theoffering",
            "levitate",
            "darksigns",
            "higher",
            "takeaim",
            "give",
            "gods",
            "sugar",
            "saythatyouwill",
            "dragmeunder",
            "bloodsport",

            // Sundowning Deluxe bonus tracks
            "whenthepartysover",
            "iwannadancewithsomebody",
            "shelter",

            // This Place Will Become Your Tomb
            "atlantic",
            "hypnosis",
            "mine",
            "likethat",
            "theloveyouwant",
            "fallforme",
            "alkaline",
            "distraction",
            "descending",
            "telomeres",
            "highwater",
            "missinglimbs",

            // Single
            "isitreallyyou",

            // Take Me Back to Eden
            "chokehold",
            "thesummoning",
            "granite",
            "aquaregia",
            "vore",
            "ascensionism",
            "areyoureallyokay",
            "theapparition",
            "dywtylm",
            "rain",
            "takemebacktoeden",
            "euclid",

            // Even in Arcadia
            "looktowindward",
            "emergence",
            "pastself",
            "dangerous",
            "caramel",
            "eveninarcadia",
            "provider",
            "damocles",
            "gethsemane",
            "infinitebaths"
    ));
    private static final String[] wordArray = words.toArray(new String[0]);

    private final int GUESS_AMOUNT = 6;

    private final SleepTokenWordle plugin;
    private final NamespacedKey key;

    private final ComponentLogger log;
    private final Map<UUID, WordleGame> activeGames = new HashMap<>();

    public GameService(
            @NotNull SleepTokenWordle plugin,
            @NotNull NamespacedKey key,
            @NotNull ComponentLogger log
    ) {
        this.plugin = plugin;
        this.key = key;
        this.log = log;
    }

    private @NotNull String pickWord() {
        return wordArray[random.nextInt(0, wordArray.length)];
    }

    public @NotNull WordleGame startGame(@NotNull Player player) {
        log.info("Starting game: {} ({})", player.getUniqueId(), player.getName());

        Vector forward = player.getEyeLocation().getDirection();

        Vector up = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(up).normalize();

        String word = pickWord();

        Location boardLocation = player.getEyeLocation()
                .add(forward.multiply(3))
                .subtract(right.multiply(((word.length() - 1) / 2f)))
                .add(0, GUESS_AMOUNT - 0.5f, 0);

        // TODO Get from config
        WordleGame game = new WordleGame(
                plugin,
                key,
                player,
                boardLocation,
                word,
                GUESS_AMOUNT
        );
        activeGames.put(player.getUniqueId(), game);

        game.spawnBoard();

        return game;
    }

    public boolean hasActiveGame(@NotNull UUID uuid) {
        return activeGames.containsKey(uuid);
    }

    public @NotNull Optional<WordleGame> getGame(@NotNull UUID uuid) {
        return Optional.ofNullable(activeGames.get(uuid));
    }

    public void endGame(@NotNull UUID uuid) {
        WordleGame game = activeGames.remove(uuid);

        game.deleteBoard();

        log.info("Game ended: {}", uuid);
    }

    public @NotNull CompletableFuture<GuessResult> guessWord(@NotNull UUID uuid, @NotNull String word) {
        WordleGame game = getGame(uuid)
                .orElseThrow(() -> new IllegalStateException("No active game found for " + uuid));

        if (!game.canGuess()) {
            return CompletableFuture.completedFuture(
                    new GuessResult.OnDelay()
            );
        }

        if (!words.contains(word)) {
            return CompletableFuture.completedFuture(
                    new GuessResult.UnknownWord()
            );
        }

        var verified = game.verifyWord(word);
        if (!verified.isSuccess()) {
            return CompletableFuture.completedFuture(verified);
        }


        game.displayWord(word);

        CompletableFuture<GuessResult> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            GuessResult result = null;
            try {
                result = game.guessWord(word);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (game.state() == WordleState.WON) {
                Bukkit.getScheduler().runTaskLater(plugin, game::wonAnimation, 40);
                Bukkit.getScheduler().runTaskLater(plugin, game::undoWonAnimation, 44);
                for (int i = 0; i < 5; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, game::spawnFirework, 44 + i * 10);
                }
                Bukkit.getScheduler().runTaskLater(plugin, game::deleteBoard, 44 + 5 * 10 + 35);
                activeGames.remove(uuid);

                log.info("Game won: {}", uuid);
            }
            if (game.state() == WordleState.LOST) {
                endGame(uuid);
                log.info("Game lost: {}", uuid);
            }

            future.complete(result);
        }, 8);

        return future;
    }
}
