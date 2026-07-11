package dev.breach.DistrictRP.velocity;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VelocityConfig {

    private final DistrictRPVelocity plugin;
    private Map<String, Object> root = new LinkedHashMap<>();

    public VelocityConfig(DistrictRPVelocity plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            Path dir = plugin.getDataDirectory();
            if (!Files.exists(dir)) Files.createDirectories(dir);

            Path file = dir.resolve("config.yml");
            if (!Files.exists(file)) copyDefaultConfig(file);

            Yaml yaml = new Yaml();
            try (Reader r = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
                Object loaded = yaml.load(r);
                if (loaded instanceof Map) {
                    //noinspection unchecked
                    root = (Map<String, Object>) loaded;
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().error("[VelocityConfig] Errore caricamento: " + t.getMessage());
        }
    }

    private void copyDefaultConfig(Path dest) {
        try (InputStream in = getClass().getResourceAsStream("/velocity-config.yml")) {
            if (in == null) {
                Files.writeString(dest, defaultYaml(), StandardCharsets.UTF_8);
                return;
            }
            Files.copy(in, dest);
        } catch (Throwable t) {
            try { Files.writeString(dest, defaultYaml(), StandardCharsets.UTF_8); }
            catch (Throwable ignored) {}
        }
    }

    private String defaultYaml() {
        return "# Config Velocity - vedi documentazione\nchannels: {}\n";
    }

    public String getString(String path, String def) {
        Object v = walk(path);
        return v == null ? def : v.toString();
    }

    public boolean getBoolean(String path, boolean def) {
        Object v = walk(path);
        if (v instanceof Boolean) return (Boolean) v;
        return def;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object v = walk(path);
        if (v instanceof List) {
            List<Object> raw = (List<Object>) v;
            List<String> out = new ArrayList<>();
            for (Object o : raw) if (o != null) out.add(o.toString());
            return out;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(String path) {
        Object v = walk(path);
        if (v instanceof Map) return (Map<String, Object>) v;
        return new LinkedHashMap<>();
    }

    private Object walk(String path) {
        if (root == null) return null;
        String[] parts = path.split("\\.");
        Object cur = root;
        for (String p : parts) {
            if (!(cur instanceof Map)) return null;
            //noinspection unchecked
            cur = ((Map<String, Object>) cur).get(p);
            if (cur == null) return null;
        }
        return cur;
    }

    public Map<String, Object> getChannels() {
        return getSection("channels");
    }

    public String getMessage(String key) {
        return getString("messages." + key, "");
    }

    public String getMessage(String key, Object... placeholders) {
        String raw = getMessage(key);
        if (placeholders == null || placeholders.length == 0) return raw;
        String out = raw;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String k = String.valueOf(placeholders[i]);
            String vv = String.valueOf(placeholders[i + 1]);
            out = out.replace("%" + k + "%", vv);
        }
        return out;
    }
}