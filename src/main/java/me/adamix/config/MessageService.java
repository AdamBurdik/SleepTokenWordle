package me.adamix.config;

import me.adamix.config.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MessageService<T> {
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final Logger log;

    private final Class<T> clazz;
    private final String resourcePath;
    private final Path path;

    private @Nullable T instance;

    public MessageService(
            @NotNull JavaPlugin plugin,
            @NotNull Class<T> clazz,
            @NotNull String resourcePath,
            @NotNull Path path
    ) {
        this.plugin = plugin;
        this.log = plugin.getComponentLogger();
        this.clazz = clazz;
        this.resourcePath = resourcePath;
        this.path = path.toAbsolutePath();

        switch (load()) {
            case MessageLoadResult.InstanceInitializationFailed(Throwable e) -> throw new IllegalStateException(
                    "Initializing proxy instance failed. Please contact the plugin authors: " +
                            String.join(", ", plugin.getPluginMeta().getAuthors()),
                    e
            );
            case MessageLoadResult.MissingValues(String[] values) -> throw new IllegalStateException(
                    "Missing required values in message configuration: " +
                            Arrays.toString(values)
            );
            case MessageLoadResult.CreateDirectoriesFailed(IOException e) -> throw new RuntimeException(e);
            case MessageLoadResult.Success() ->
                    log.info("Message configuration loaded successfully from {}", this.path);
        }
    }

    private @NotNull HashMap<String, String> loadMap() throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            plugin.saveResource(resourcePath, false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                path.toFile()
        );

        HashMap<String, String> map = new HashMap<>();

        for (String key : yaml.getKeys(true)) {
            // Ignore non string stuff
            if (!yaml.isString(key)) continue;

            map.put(key, yaml.getString(key));
        }

        return map;
    }

    private @NotNull MessageLoadResult load() {
    try {
        var map = loadMap();

        List<String> missingKeys = new ArrayList<>();
        Map<String, BiFunction<String, Object[], Component>> supplierMap = new HashMap<>();

        for (Method method : clazz.getMethods()) {
            String key = StringUtils.convertCamelCaseToSnake(method.getName());

            if (!map.containsKey(key)) {
                missingKeys.add(key);
            }

            String[] paramNames = Arrays.stream(method.getParameters())
                    .map(p -> StringUtils.convertCamelCaseToSnake(p.getName()))
                    .toArray(String[]::new);

            supplierMap.put(key, (msg, args) -> {
                if (paramNames.length == 0) {
                    return mm.deserialize(msg);
                }

                TagResolver[] placeholders = new TagResolver[paramNames.length];
                for (int i = 0; i < paramNames.length; i++) {
                    placeholders[i] = Placeholder.unparsed(
                            paramNames[i],
                            args[i] != null ? args[i].toString() : "null"
                    );
                }

                return mm.deserialize(msg, placeholders);
            });
        }

        if (!missingKeys.isEmpty()) {
            var missingKeysArray = missingKeys.toArray(new String[0]);

            log.warn(
                    "Loading message configuration at {} failed. Missing values for following keys: {}",
                    path.toString(),
                    Arrays.toString(missingKeysArray)
            );
            return new MessageLoadResult.MissingValues(missingKeysArray);
        }

        try {
            //noinspection unchecked
            T temp = (T) Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class<?>[]{ clazz },
                    (proxy, method, args) -> {
                        String methodName = method.getName();

                        if (method.getDeclaringClass() == Object.class) {
                            return switch (methodName) {
                                case "equals" -> proxy == args[0];
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "toString" ->
                                        clazz.getSimpleName() + "@Proxy" + Integer.toHexString(System.identityHashCode(proxy));
                                default ->
                                        throw new UnsupportedOperationException("Unsupported Object method: " + methodName);
                            };
                        }

                        return supplierMap.get(
                                StringUtils.convertCamelCaseToSnake(methodName)
                        ).apply(
                                map.get(StringUtils.convertCamelCaseToSnake(methodName)),
                                args
                        );
                    }
            );

            this.instance = temp;
        } catch (Exception e) {
            log.error("Unable to create new proxy instance for {}", path, e);
            return new MessageLoadResult.InstanceInitializationFailed(e);
        }
    } catch (IOException e) {
        return new MessageLoadResult.CreateDirectoriesFailed(e);
    }

    return new MessageLoadResult.Success();
}

    public @NotNull MessageLoadResult reload() {
        var result = load();
        if (!result.isSuccess()) {
            log.error("Reloading message configuration at {} failed! Using previous configuration instance", path);
            if (instance == null) {
                log.error("Previous configuration instance is null! Consider calling MessageService#load() on plugin startup");
            }
        } else {
            log.info("Message configuration reloaded successfully from {}", path);
        }
        return result;
    }

    public @NotNull T get() {
        if (instance == null) {
            throw new IllegalStateException("Message configuration instance is null");
        }
        return instance;
    }
}
