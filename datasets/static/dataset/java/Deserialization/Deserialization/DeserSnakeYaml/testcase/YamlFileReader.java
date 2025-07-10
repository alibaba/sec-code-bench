<filename>YamlFileReader.java<fim_prefix>

package ch.jalu.configme.resource;

import ch.jalu.configme.exception.ConfigMeException;
import ch.jalu.configme.internal.PathUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * YAML file reader.
 */
public class YamlFileReader implements PropertyReader {

    private final Path path;
    private final Charset charset;
    @Nullable
    private final Map<String, Object> root;

    /**
     * Constructor.
     *
     * @param path the file to load
     */
    public YamlFileReader(@NotNull Path path) {
        this(path, StandardCharsets.UTF_8);
    }

    /**
     * Constructor.
     *
     * @param path the file to load
     * @param charset the charset to read the data as
     */
    public YamlFileReader(@NotNull Path path, @NotNull Charset charset) {
        this.path = path;
        this.charset = charset;
        this.root = loadFile();
    }

    @Override
    public @Nullable Object getObject(@NotNull String path) {
        if (path.isEmpty()) {
            return root;
        }

        Object node = root;
        String[] keys = path.split("\\.");
        for (String key : keys) {
            node = getEntryIfIsMap(key, node);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    @Override
    public @Nullable String getString(@NotNull String path) {
        return getTypedObject(path, String.class);
    }

    @Override
    public @Nullable Integer getInt(@NotNull String path) {
        Number n = getTypedObject(path, Number.class);
        return (n == null)
            ? null
            : n.intValue();
    }

    @Override
    public @Nullable Double getDouble(@NotNull String path) {
        Number n = getTypedObject(path, Number.class);
        return (n == null)
            ? null
            : n.doubleValue();
    }

    @Override
    public @Nullable Boolean getBoolean(@NotNull String path) {
        return getTypedObject(path, Boolean.class);
    }

    @Override
    public @Nullable List<?> getList(@NotNull String path) {
        return getTypedObject(path, List.class);
    }

    @Override
    public boolean contains(@NotNull String path) {
        return getObject(path) != null;
    }

    @Override
    public @NotNull Set<String> getKeys(boolean onlyLeafNodes) {
        if (root == null) {
            return Collections.emptySet();
        }
        Set<String> allKeys = new LinkedHashSet<>();
        collectKeysIntoSet("", root, allKeys, onlyLeafNodes);
        return allKeys;
    }

    @Override
    public @NotNull Set<String> getChildKeys(@NotNull String path) {
        Object object = getObject(path);
        if (object instanceof Map) {
            String pathPrefix = path.isEmpty() ? "" : path + ".";
            return ((Map<String, Object>) object).keySet().stream()
                .map(childPath -> pathPrefix + childPath)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Collections.emptySet();
    }

    /**
     * Recursively collects keys from maps into the given set.
     *
     * @param path the path of the given map
     * @param map the map to process recursively
     * @param result set to save keys to
     * @param onlyLeafNodes whether only leaf nodes should be added to the result set
     */
    private void collectKeysIntoSet(@NotNull String path, @NotNull Map<String, Object> map, @NotNull Set<String> result,
                                    boolean onlyLeafNodes) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String childPath = PathUtils.concat(path, entry.getKey());
            if (!onlyLeafNodes || isLeafValue(entry.getValue())) {
                result.add(childPath);
            }

            if (entry.getValue() instanceof Map) {
                collectKeysIntoSet(childPath, (Map) entry.getValue(), result, onlyLeafNodes);
            }
        }
    }

    private static boolean isLeafValue(@Nullable Object o) {
        return !(o instanceof Map) || ((Map) o).isEmpty();
    }

    /**
     * Loads the values of the file.
     *
     * @return map with the values from the file
     */
    protected @Nullable Map<String, Object> loadFile() {
        try (InputStream is = Files.newInputStream(path);
             InputStreamReader isr = new InputStreamReader(is, charset)) {
            Map<Object, Object> rootMap = <fim_suffix>
            return normalizeMap(rootMap);
        } catch (IOException e) {
            throw new ConfigMeException("Could not read file '" + path + "'", e);
        } catch (ClassCastException e) {
            throw new ConfigMeException("Top-level is not a map in '" + path + "'", e);
        } catch (YAMLException e) {
            throw new ConfigMeException("YAML error while trying to load file '" + path + "'", e);
        }
    }

    /**
     * Processes the map as read from SnakeYAML and may return a new, adjusted one.
     *
     * @param map the map to normalize
     * @return the normalized map (or same map if no changes are needed)
     */
    protected @Nullable Map<String, Object> normalizeMap(@Nullable Map<Object, Object> map) {
        return new MapNormalizer().normalizeMap(map);
    }

    protected final @NotNull Path getPath() {
        return path;
    }

    /**
     * @return the root value; may be null if the file was empty
     * @deprecated use {@code getObject("")} instead
     */
    @Deprecated
    protected final @Nullable Map<String, Object> getRoot() {
        return root;
    }

    /**
     * Gets the object at the given path and safely casts it to the given class's type. Returns null
     * if no value is available or if it cannot be cast.
     *
     * @param path the path to retrieve
     * @param clazz the class to cast to
     * @param <T> the class type
     * @return cast value at the given path, null if not applicable
     */
    protected <T> @Nullable T getTypedObject(@NotNull String path, @NotNull Class<T> clazz) {
        Object value = getObject(path);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    private static @Nullable Object getEntryIfIsMap(@NotNull String key, @Nullable Object value) {
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).get(key);
        }
        return null;
    }

}

<fim_middle>